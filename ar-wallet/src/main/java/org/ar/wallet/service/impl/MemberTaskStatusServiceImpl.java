package org.ar.wallet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.redis.util.RedissonUtil;
import org.ar.wallet.Enum.*;
import org.ar.wallet.entity.*;
import org.ar.wallet.mapper.MemberInfoMapper;
import org.ar.wallet.mapper.MemberTaskStatusMapper;
import org.ar.wallet.rabbitmq.RabbitMQService;
import org.ar.wallet.service.*;
import org.ar.wallet.util.OrderNumberGeneratorUtil;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 会员任务状态表, 记录会员完成任务和领取奖励的状态 服务实现类
 * </p>
 *
 * @author
 * @since 2024-03-22
 */
@Service
@Slf4j
public class MemberTaskStatusServiceImpl extends ServiceImpl<MemberTaskStatusMapper, MemberTaskStatus> implements IMemberTaskStatusService {

    @Autowired
    private RedissonUtil redissonUtil;

    @Autowired
    private OrderNumberGeneratorUtil orderNumberGenerator;

    @Autowired
    private RabbitMQService rabbitMQService;

    @Autowired
    private MemberInfoMapper memberInfoMapper;

    @Autowired
    private ITaskManagerService taskManagerService;

    @Autowired
    private IMemberTaskStatusService memberTaskStatusService;

    @Autowired
    private ITaskCollectionRecordService taskCollectionRecordService;

    @Autowired
    private IMemberAccountChangeService memberAccountChangeService;

    @Autowired
    private IControlSwitchService controlSwitchService;

    @Autowired
    private IMemberInfoService memberInfoService;


    /**
     * 处理每日买入任务
     *
     * @param memberInfo
     * @param taskManager
     * @return boolean
     */
    @Override
    public boolean handleDailyBuyTask(MemberInfo memberInfo, TaskManager taskManager) {

        Integer taskType = Integer.valueOf(RewardTaskTypeEnum.BUY.getCode());

        //分布式锁key ar-wallet-handleDailyTask+会员id
        String key = "ar-wallet-handleDailyTask" + memberInfo.getId();
        RLock lock = redissonUtil.getLock(key);

        boolean req = false;

        try {
            req = lock.tryLock(10, TimeUnit.SECONDS);

            if (req) {
                // 查看是否完成了任务

                //任务目标类型 1-次数 2-金额
                int taskTargetNum = "1".equals(taskManager.getTaskTarget())
                        ? memberInfo.getTodayBuySuccessCount()
                        : memberInfo.getTodayBuySuccessAmount().intValue();

                if (taskTargetNum >= taskManager.getTaskTargetNum()) {
                    //完成任务

                    log.info("处理每日买入任务: 完成任务 会员id: {}", memberInfo.getId());


                    LocalDate today = LocalDate.now();
                    // 检查是否已存在今日首次卖出的记录
                    long existingTaskCount = lambdaQuery()
                            .eq(MemberTaskStatus::getMemberId, memberInfo.getId())
                            .eq(MemberTaskStatus::getTaskId, taskManager.getId())
                            .eq(MemberTaskStatus::getCompletionDate, today)
                            .count();

                    if (existingTaskCount > 0) {
                        // 如果已存在记录，则表示今日任务已完成，不做处理
                        return true;
                    } else {
                        // 不存在，表示今日首次卖出，记录任务完成
                        MemberTaskStatus newRecord = new MemberTaskStatus();
                        newRecord.setMemberId(memberInfo.getId());//会员id
                        newRecord.setTaskType(taskType);//任务类型
                        newRecord.setTaskId(taskManager.getId());//任务id
                        newRecord.setCompletionStatus(1); // 任务完成
                        newRecord.setRewardClaimed(0); // 奖励未领取
                        newRecord.setCompletionDate(today);//任务完成日期 今日
                        newRecord.setOrderNo(orderNumberGenerator.generateOrderNo("RW"));//任务订单号
                        newRecord.setCreateTime(LocalDateTime.now());//任务完成时间
                        newRecord.setTaskCycle(2);//任务周期 1:一次性任务 2:周期性-每天

                        boolean save = save(newRecord);

                        if (save) {
                            log.info("处理每日任务成功: 买入任务 会员id: {}, 任务状态信息: {}", memberInfo.getId(), newRecord);

                            //发送次日凌晨自动领取奖励MQ延时消息
                            sendAutoClaimRewardMessage(newRecord, memberInfo.getId());

                        } else {
                            log.error("处理每日任务失败: 买入任务 会员id: {}, 任务状态信息: {}", memberInfo.getId(), newRecord);
                        }

                        return save;
                    }
                } else {
                    //未完成任务
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("处理会员每日任务失败 买入任务 会员id: {}, taskType: {}, e: ", memberInfo.getId(), taskType, e);
        } finally {
            //释放锁
            if (req && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        return false;
    }


    /**
     * 处理每日卖出任务
     *
     * @param memberInfo
     * @param taskManager
     * @return boolean
     */
    @Override
    public boolean handleDailySellTask(MemberInfo memberInfo, TaskManager taskManager) {

        Integer taskType = Integer.valueOf(RewardTaskTypeEnum.SELL.getCode());

        //分布式锁key ar-wallet-handleDailyTask+会员id
        String key = "ar-wallet-handleDailyTask" + memberInfo.getId();
        RLock lock = redissonUtil.getLock(key);

        boolean req = false;

        try {
            req = lock.tryLock(10, TimeUnit.SECONDS);

            if (req) {

                // 查看是否完成了任务

                //任务目标类型 1-次数 2-金额
                int taskTargetNum = "1".equals(taskManager.getTaskTarget())
                        ? memberInfo.getTodaySellSuccessCount()
                        : memberInfo.getTodaySellSuccessAmount().intValue();

                if (taskTargetNum >= taskManager.getTaskTargetNum()) {

                    LocalDate today = LocalDate.now();
                    // 检查是否已存在今日卖出的记录
                    long existingTaskCount = lambdaQuery()
                            .eq(MemberTaskStatus::getMemberId, memberInfo.getId())
                            .eq(MemberTaskStatus::getTaskId, taskManager.getId())
                            .eq(MemberTaskStatus::getCompletionDate, today)
                            .count();

                    if (existingTaskCount > 0) {
                        // 如果已存在记录，则表示今日任务已完成，不做处理
                        return true;
                    } else {
                        // 不存在，表示今日首次卖出，记录任务完成
                        MemberTaskStatus newRecord = new MemberTaskStatus();
                        newRecord.setMemberId(memberInfo.getId());//会员id
                        newRecord.setTaskType(taskType);//任务类型
                        newRecord.setTaskId(taskManager.getId());//任务id
                        newRecord.setCompletionStatus(1); // 任务完成
                        newRecord.setRewardClaimed(0); // 奖励未领取
                        newRecord.setCompletionDate(today);// 任务完成日期 今日
                        newRecord.setOrderNo(orderNumberGenerator.generateOrderNo("RW"));//任务订单号
                        newRecord.setCreateTime(LocalDateTime.now());//任务完成时间
                        newRecord.setTaskCycle(2);//任务周期 1:一次性任务 2:周期性-每天

                        boolean save = save(newRecord);

                        if (save) {
                            log.info("处理每日任务成功: 卖出任务 会员id: {}, 任务状态信息: {}", memberInfo.getId(), newRecord);

                            //发送次日凌晨自动领取奖励MQ延时消息
                            sendAutoClaimRewardMessage(newRecord, memberInfo.getId());

                        } else {
                            log.error("处理每日任务失败: 卖出任务 会员id: {}, 任务状态信息: {}", memberInfo.getId(), newRecord);
                        }

                        return save;
                    }
                } else {
                    //未完成任务
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("处理会员每日任务失败 卖出任务 会员id: {}, taskType: {}, e: ", memberInfo.getId(), taskType, e);
        } finally {
            //释放锁
            if (req && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        return false;
    }

    /**
     * 发送次日凌晨自动领取奖励MQ延时消息
     *
     * @return long
     */
    public void sendAutoClaimRewardMessage(MemberTaskStatus newRecord, Long memberId) {
        //发送次日00:10自动领取奖励的MQ
        Long lastUpdateTimestamp = System.currentTimeMillis();
        TaskInfo taskInfo = new TaskInfo(newRecord.getOrderNo() + "|" + memberId, TaskTypeEnum.MERCHANT_AUTO_CLAIM_REWARD_QUEUE.getCode(), lastUpdateTimestamp);
        //计算当前时间至次日凌晨00:10的毫秒数 (MQ延迟时间)
        long millis = calculateDelayUntilNextDay010();
        rabbitMQService.sendTimeoutTask(taskInfo, millis);
    }


    /**
     * 计算当前时间到次日凌晨00:10的毫秒数
     *
     * @return long
     */
    private long calculateDelayUntilNextDay010() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextDay010 = now.plusDays(1).withHour(0).withMinute(10).withSecond(0).withNano(0);
        long delay = ChronoUnit.MILLIS.between(now, nextDay010);
        return delay;
    }


    /**
     * 查询会员当天所有任务状态列表
     *
     * @param memberId
     * @return
     */
    @Override
    public List<MemberTaskStatus> queryMemberTodayTaskStatus(Long memberId) {
        return lambdaQuery()
                .eq(MemberTaskStatus::getMemberId, memberId)
                .eq(MemberTaskStatus::getCompletionDate, LocalDate.now()).list();
    }


    /**
     * 检查是否完成了任务 并且未领取奖励 加上排他行锁
     *
     * @param memberInfo
     * @param taskManager
     * @return {@link MemberTaskStatus}
     */
    @Override
    public MemberTaskStatus checkTaskCompletedAndRewardUnclaimed(MemberInfo memberInfo, TaskManager taskManager) {

        // 创建QueryWrapper
        QueryWrapper<MemberTaskStatus> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(MemberTaskStatus::getMemberId, memberInfo.getId()) // 会员id
                .eq(MemberTaskStatus::getTaskId, taskManager.getId()) // 任务id
                .eq(MemberTaskStatus::getCompletionStatus, 1) // 任务完成状态 1 已完成
                .eq(MemberTaskStatus::getRewardClaimed, 0); // 任务领取状态 0 未领取

        // 判断任务是周期性还是一次性
        if ("2".equals(taskManager.getTaskCycle())) {
            // 周期性任务 需要加上时间是当天
            queryWrapper.lambda().eq(MemberTaskStatus::getCompletionDate, LocalDate.now());
        }

        // 使用selectForUpdate加上排他行锁
        queryWrapper.last("FOR UPDATE");

        // 执行查询
        return baseMapper.selectOne(queryWrapper);

    }

    /**
     * 完成一次性任务
     *
     * @param memberInfo
     */
    @Override
    public Boolean completeOnceTask(MemberInfo memberInfo, TaskManager taskManager) {


        //Integer taskType = Integer.valueOf(RewardTaskTypeEnum.REAL_AUTH.getCode());

        //分布式锁key ar-wallet-handleDailyTask 和将以前实名认证过会员的任务完成的锁一致
        String key = "ar-wallet-handleDailyTask";
        RLock lock = redissonUtil.getLock(key);

        boolean req = false;

        try {
            req = lock.tryLock(10, TimeUnit.SECONDS);

            if (req) {

                log.info("完成一次性任务: 会员id: {}, type:{}", memberInfo.getId(), taskManager.getTaskType());

                LocalDate today = LocalDate.now();
                // 检查该会员是否完成过 实名认证任务
                long existingTaskCount = lambdaQuery()
                        .eq(MemberTaskStatus::getMemberId, memberInfo.getId())
                        .eq(MemberTaskStatus::getTaskId, taskManager.getId())
                        .count();

                if (existingTaskCount > 0) {
                    // 如果已存在记录，则表示实名认证任务已完成，不做处理
                    return true;
                } else {
                    // 不存在，实名认证，记录任务完成
                    MemberTaskStatus newRecord = new MemberTaskStatus();
                    newRecord.setMemberId(memberInfo.getId());//会员id
                    newRecord.setTaskType(Integer.valueOf(taskManager.getTaskType()));//任务类型
                    newRecord.setTaskId(taskManager.getId());//任务id
                    newRecord.setCompletionStatus(1); // 任务完成
                    newRecord.setRewardClaimed(0); // 奖励未领取
                    newRecord.setCompletionDate(today);//任务完成日期 今天
                    newRecord.setOrderNo(orderNumberGenerator.generateOrderNo("RW"));//任务订单号
                    newRecord.setCreateTime(LocalDateTime.now());//任务完成时间
                    newRecord.setTaskCycle(1);//任务周期 1:一次性任务 2:周期性-每天

                    log.info("完成一次性任务: 会员id: {}, 任务状态信息: {}", memberInfo.getId(), newRecord);

                    return save(newRecord);
                }
            }
        } catch (Exception e) {
            log.error("完成一次性任务失败 会员id: {}, taskType: {}, e: {}", memberInfo.getId(), taskManager.getTaskType(), e.getMessage());
        } finally {
            //释放锁
            if (req && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        return false;

    }

    /**
     * MQ 自动领取前一日任务奖励
     *
     * @param taskInfo
     * @return boolean
     */
    @Override
    @Transactional
    public boolean autoClaimReward(String taskInfo) {

        // 分割字符串
        String[] parts = taskInfo.split("\\|");

        //获取任务订单号
        String taskOrderNo = parts[0];

        //获取会员id
        String memberId = parts[1];


        //分布式锁key ar-wallet-handleDailyTask+会员id  与完成任务共用一把锁
        String key = "ar-wallet-handleDailyTask" + memberId;
        RLock lock = redissonUtil.getLock(key);

        boolean req = false;

        try {
            req = lock.tryLock(10, TimeUnit.SECONDS);

            if (req) {

                //根据任务订单号 查询任务 查看该笔任务奖励是否被领取 加上排他行锁

                // 创建QueryWrapper
                QueryWrapper<MemberTaskStatus> queryWrapper = new QueryWrapper<>();
                queryWrapper.lambda()
                        .eq(MemberTaskStatus::getOrderNo, taskOrderNo)//任务订单号
                        .last("FOR UPDATE");//加上排他行锁

                MemberTaskStatus memberTaskStatus = baseMapper.selectOne(queryWrapper);

                if (memberTaskStatus == null) {
                    log.error("MQ 自动领取前一日任务奖励失败, 获取任务信息失败, 任务订单号: {}", taskOrderNo);
                    return true;
                }

                //该任务完成时间必须是前一天的
                if (!isCompletionDateYesterday(memberTaskStatus.getCompletionDate())) {
                    log.error("MQ 自动领取前一日任务奖励失败, 该笔任务订单完成时间不是前一天, 任务订单信息: {}", memberTaskStatus);
                    //消费成功
                    return true;
                }

                //查看该笔任务奖励是否被领取
                if (memberTaskStatus.getRewardClaimed() == 0) {
                    //该任务没有被领取
                    log.info("MQ 自动领取前一日任务奖励, 该任务没有被领取过, 任务订单信息: {}", memberTaskStatus);

                    //领取任务奖励

                    //查看是否开启任务总开关
                    if (controlSwitchService.isSwitchEnabled(SwitchIdEnum.CHECK_ACTIVE_TASKS.getSwitchId())) {

                        //获取会员信息 加上排他行锁
                        MemberInfo memberInfo = memberInfoMapper.selectMemberInfoForUpdate(Long.valueOf(memberId));

                        if (memberInfo == null) {
                            log.error("MQ 自动领取前一日任务奖励处理失败: 获取会员信息失败, 任务订单号: {}, 会员id: {}", taskOrderNo, memberId);
                            return false;
                        }

                        BigDecimal previousBalance = memberInfo.getBalance();

                        //根据任务id查询任务
                        TaskManager taskManager = taskManagerService.getTaskDetailsById(memberTaskStatus.getTaskId());

                        //校验任务是否存在
                        if (taskManager == null) {
                            log.error("MQ 自动领取前一日任务奖励处理失败: 任务不存在或任务未开启, 任务状态信息: {}, 任务详情: {}, 会员信息: {}, ", memberTaskStatus, taskManager, memberInfo);
                            return true;
                        }

                        //检查奖励表是否有记录 (是否领取过奖励) 根据任务订单号进行查询
                        if (taskCollectionRecordService.hasReceivedReward(memberTaskStatus) != null) {
                            log.info("MQ 自动领取前一日任务奖励处理失败, 该任务已被领取过, 会员id: {}. 任务状态信息: {}, taskManager: {}", memberInfo.getId(), memberTaskStatus, taskManager);
                            return true;
                        }

                        //新增领取奖励记录
                        createTaskCollectionRecord(memberInfo, memberTaskStatus, taskManager);

                        //将任务状态表 改为 已领取
                        memberTaskStatus.setRewardClaimed(1);
                        //领取时间
                        memberTaskStatus.setClaimDate(LocalDate.now());
                        //更新任务状态表
                        memberTaskStatusService.updateById(memberTaskStatus);

                        //会员余额
                        memberInfo.setBalance(memberInfo.getBalance().add(taskManager.getTaskReward()));

                        //累计奖励金额
                        memberInfo.setTotalTaskRewards(memberInfo.getTotalTaskRewards().add(taskManager.getTaskReward()));

                        //更新会员信息
                        memberInfoService.updateById(memberInfo);

                        //更新会员账变
                        memberAccountChangeService.recordMemberTransaction(
                                String.valueOf(memberInfo.getId()),
                                taskManager.getTaskReward(),
                                MemberAccountChangeEnum.TASK_REWARD.getCode(),
                                memberTaskStatus.getOrderNo(),
                                previousBalance,
                                memberInfo.getBalance(),
                                "");

                        log.info("MQ 自动领取前一日任务奖励处理成功, 会员id: {}, 奖励金额: {}, 任务信息: {}", memberInfo.getId(), taskManager.getTaskReward(), taskManager);

                        return true;
                    } else {
                        log.info("MQ 自动领取前一日任务奖励处理失败, 任务总开关未开启, 任务订单号: {}", taskOrderNo);
                        return true;
                    }
                } else {
                    log.error("MQ 自动领取前一日任务奖励, 该任务奖励已被领取过, 无需再领取, 任务订单信息: {}", memberTaskStatus);
                    //消费成功
                    return true;
                }
            }
        } catch (Exception e) {
            //手动回滚
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("MQ 自动领取前一日任务奖励 失败 会员id: {}, 任务订单号: {}, e: {}", memberId, taskOrderNo, e.getMessage());
            return false;
        } finally {
            //释放锁
            if (req && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
     return true;
    }


    /**
     * 定时任务领取昨日任务奖励处理
     *
     * @return boolean
     */
    @Override
    @Transactional
    public boolean dailyRewardClaimTask() {

        //分布式锁key ar-wallet-handleDailyTask+会员id  与完成任务共用一把锁
        String key = "ar-wallet-handleDailyTask";
        RLock lock = redissonUtil.getLock(key);

        boolean req = false;

        try {
            req = lock.tryLock(10, TimeUnit.SECONDS);

            if (req) {

                //昨天日期
                LocalDate yesterday = LocalDate.now().minusDays(1);

                //查询昨天完成的每日任务并且未领取奖励的任务
                LambdaQueryWrapper<MemberTaskStatus> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper
                        .eq(MemberTaskStatus::getCompletionDate, yesterday)//昨天完成的任务
                        .eq(MemberTaskStatus::getTaskCycle, 2)//周期性每天任务
                        .eq(MemberTaskStatus::getRewardClaimed, 0)//未领取奖励
                        .last("FOR UPDATE");//加上排他行锁

                List<MemberTaskStatus> tasksToClaim = baseMapper.selectList(queryWrapper);

                log.info("定时任务领取昨日任务奖励, 待领取任务列表: {}", tasksToClaim);

                // 处理每个任务
                for (MemberTaskStatus task : tasksToClaim) {
                    if (!claimRewardForTask(task)) {
                        throw new RuntimeException();
                    }
                }

                return true;
            }
        } catch (Exception e) {
            //手动回滚
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("定时任务领取昨日任务奖励处理失败, e: {}", e.getMessage());
        } finally {
            //释放锁
            if (req && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        return false;
    }

    /**
     * 定时任务领取昨日任务奖励
     *
     * @param memberTaskStatus
     */
    private boolean claimRewardForTask(MemberTaskStatus memberTaskStatus) {

        //分布式锁key ar-wallet-handleDailyTask+会员id  与完成任务共用一把锁
        String key = "ar-wallet-handleDailyTask" + memberTaskStatus.getMemberId();
        RLock lock = redissonUtil.getLock(key);

        boolean req = false;

        try {
            req = lock.tryLock(10, TimeUnit.SECONDS);

            if (req) {

                //查看是否开启任务总开关
                if (controlSwitchService.isSwitchEnabled(SwitchIdEnum.CHECK_ACTIVE_TASKS.getSwitchId())) {

                    //获取会员信息 加上排他行锁
                    MemberInfo memberInfo = memberInfoMapper.selectMemberInfoForUpdate(memberTaskStatus.getMemberId());

                    if (memberInfo == null) {
                        log.error("定时任务领取昨日任务奖励处理失败: 获取会员信息失败, 任务订单号: {}, 会员id: {}", memberTaskStatus.getOrderNo(), memberTaskStatus.getMemberId());
                        return false;
                    }

                    BigDecimal previousBalance = memberInfo.getBalance();

                    //根据任务id查询任务
                    TaskManager taskManager = taskManagerService.getTaskDetailsById(memberTaskStatus.getTaskId());

                    //校验任务是否存在
                    if (taskManager == null) {
                        log.error("定时任务领取昨日任务奖励处理失败: 任务不存在或任务未开启, 任务状态信息: {}, 任务详情: {}, 会员信息: {}, ", memberTaskStatus, taskManager, memberInfo);
                        return true;
                    }

                    //检查奖励表是否有记录 (是否领取过奖励) 根据任务订单号进行查询
                    if (taskCollectionRecordService.hasReceivedReward(memberTaskStatus) != null) {
                        log.info("定时任务领取昨日任务奖励处理失败, 该任务已被领取过, 会员id: {}. 任务状态信息: {}, taskManager: {}", memberInfo.getId(), memberTaskStatus, taskManager);
                        return true;
                    }

                    //新增领取奖励记录
                    createTaskCollectionRecord(memberInfo, memberTaskStatus, taskManager);

                    //将任务状态表 改为 已领取
                    memberTaskStatus.setRewardClaimed(1);
                    //领取时间
                    memberTaskStatus.setClaimDate(LocalDate.now());
                    //更新任务状态表
                    memberTaskStatusService.updateById(memberTaskStatus);

                    //会员余额
                    memberInfo.setBalance(memberInfo.getBalance().add(taskManager.getTaskReward()));

                    //累计奖励金额
                    memberInfo.setTotalTaskRewards(memberInfo.getTotalTaskRewards().add(taskManager.getTaskReward()));

                    //更新会员信息
                    memberInfoService.updateById(memberInfo);

                    //更新会员账变
                    memberAccountChangeService.recordMemberTransaction(
                            String.valueOf(memberInfo.getId()),
                            taskManager.getTaskReward(),
                            MemberAccountChangeEnum.TASK_REWARD.getCode(),
                            memberTaskStatus.getOrderNo(),
                            previousBalance,
                            memberInfo.getBalance(),
                            "");

                    log.info("定时任务领取昨日任务奖励处理成功, 会员id: {}, 奖励金额: {}, 任务信息: {}", memberInfo.getId(), taskManager.getTaskReward(), taskManager);

                    return true;
                } else {
                    log.info("定时任务领取昨日任务奖励处理失败, 任务总开关未开启, 任务状态信息: {}", memberTaskStatus);
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("定时任务领取昨日任务奖励处理失败, e: {}", e.getMessage());
            //抛出异常
            throw new RuntimeException();
        } finally {
            //释放锁
            if (req && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        return false;
    }

    public boolean isCompletionDateYesterday(LocalDate completionDate) {
        // 获取当前日期
        LocalDate today = LocalDate.now();
        // 获取昨天的日期
        LocalDate yesterday = today.minusDays(1);

        // 判断completionDate是否是昨天
        return completionDate.equals(yesterday);
    }


    /**
     * 新增领取奖励记录
     *
     * @return {@link Boolean}
     */
    public Boolean createTaskCollectionRecord(MemberInfo memberInfo, MemberTaskStatus memberTaskStatus, TaskManager taskManager) {

        //插入奖励明细表
        TaskCollectionRecord taskCollectionRecord = new TaskCollectionRecord();

        //会员id
        taskCollectionRecord.setMemberId(String.valueOf(memberInfo.getId()));

        //判断会员类型 如果是商户类型 就添加以下数据

        if (!MemberTypeEnum.WALLET_MEMBER.getCode().equals(memberInfo.getMemberType())) {
            //商户会员

            //商户号
            taskCollectionRecord.setMerchantCode(memberInfo.getMerchantCode());

            //商户名称
            taskCollectionRecord.setMerchantName(memberInfo.getMerchantName());
        }

        //任务订单号
        taskCollectionRecord.setOrderNo(memberTaskStatus.getOrderNo());

        //任务名称
        taskCollectionRecord.setTaskName(taskManager.getTaskTitle());

        //任务id
        taskCollectionRecord.setTaskId(taskManager.getId());

        //任务类型
        taskCollectionRecord.setTaskType(Integer.valueOf(taskManager.getTaskType()));

        //任务周期
        taskCollectionRecord.setTaskCycle(Integer.valueOf(taskManager.getTaskCycle()));

        //任务奖励金额
        taskCollectionRecord.setRewardAmount(taskManager.getTaskReward());

        //任务完成时间
        taskCollectionRecord.setCompletionTime(memberTaskStatus.getCreateTime());

        //会员账号
        taskCollectionRecord.setMemberAccount(memberInfo.getMemberAccount());

        //领取标识 1-手动领取  2-自动领取
        taskCollectionRecord.setReceiveType(2);

        //领取日期
        taskCollectionRecord.setReceiveDate(LocalDate.now());

        //插入奖励明细表
        return taskCollectionRecordService.save(taskCollectionRecord);
    }

}

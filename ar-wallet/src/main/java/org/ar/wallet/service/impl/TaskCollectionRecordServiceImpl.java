package org.ar.wallet.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.ar.common.core.constant.GlobalConstants;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.core.result.ResultCode;
import org.ar.common.mybatis.util.PageUtils;
import org.ar.common.pay.dto.TaskCollectionRecordDTO;
import org.ar.common.pay.req.TaskCollectionRecordReq;
import org.ar.common.redis.util.RedissonUtil;
import org.ar.common.web.utils.UserContext;
import org.ar.wallet.Enum.MemberAccountChangeEnum;
import org.ar.wallet.Enum.MemberTypeEnum;
import org.ar.wallet.Enum.SwitchIdEnum;
import org.ar.wallet.config.WalletMapStruct;
import org.ar.wallet.entity.MemberInfo;
import org.ar.wallet.entity.MemberTaskStatus;
import org.ar.wallet.entity.TaskCollectionRecord;
import org.ar.wallet.entity.TaskManager;
import org.ar.wallet.mapper.MemberInfoMapper;
import org.ar.wallet.mapper.TaskCollectionRecordMapper;
import org.ar.wallet.req.ClaimTaskRewardReq;
import org.ar.wallet.req.TaskCollectionRecordPageReq;
import org.ar.wallet.service.*;
import org.ar.wallet.util.IpUtil;
import org.ar.wallet.vo.PrizeWinnersVo;
import org.ar.wallet.vo.TaskCollectionRecordListVo;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * 会员领取任务记录 服务实现类
 * </p>
 *
 * @author
 * @since 2024-03-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskCollectionRecordServiceImpl extends ServiceImpl<TaskCollectionRecordMapper, TaskCollectionRecord> implements ITaskCollectionRecordService {
    private final WalletMapStruct walletMapStruct;
    private final IMemberInfoService memberInfoService;
    private final TaskCollectionRecordMapper taskCollectionRecordMapper;

    @Autowired
    private MemberInfoMapper memberInfoMapper;

    @Autowired
    private RedissonUtil redissonUtil;

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

    @Override
    public PageReturn<TaskCollectionRecordDTO> listPage(TaskCollectionRecordReq req) {
        Page<TaskCollectionRecord> page = new Page<>();
        page.setCurrent(req.getPageNo());
        page.setSize(req.getPageSize());
        LambdaQueryChainWrapper<TaskCollectionRecord> lambdaQuery = lambdaQuery();
        if (StringUtils.isNotBlank(req.getUserId())) {
            lambdaQuery.eq(TaskCollectionRecord::getMemberId, req.getUserId());
        }
        if (StringUtils.isNotBlank(req.getMerchantName())) {
            lambdaQuery.eq(TaskCollectionRecord::getMerchantName, req.getMerchantName());
        }
        if (StringUtils.isNotBlank(req.getTaskName())) {
            lambdaQuery.eq(TaskCollectionRecord::getTaskName, req.getTaskName());
        }
        if (StringUtils.isNotBlank(req.getTaskType())) {
            lambdaQuery.eq(TaskCollectionRecord::getTaskType, req.getTaskType());
        }
        if (StringUtils.isNotBlank(req.getFrequency())) {
            lambdaQuery.eq(TaskCollectionRecord::getTaskCycle, req.getFrequency());
        }
        //--动态查询 提现结束
        if (StringUtils.isNotBlank(req.getReceiveStartTime())) {
            lambdaQuery.ge(TaskCollectionRecord::getCreateTime, req.getReceiveStartTime());
        }
        //--动态查询 提现结束
        if (StringUtils.isNotBlank(req.getReceiveEndTime())) {
            lambdaQuery.le(TaskCollectionRecord::getCreateTime, req.getReceiveEndTime());
        }

        //--动态查询 提现结束
        if (StringUtils.isNotBlank(req.getCompleteStartTime())) {
            lambdaQuery.ge(TaskCollectionRecord::getCompletionTime, req.getCompleteStartTime());
        }
        //--动态查询 提现结束
        if (StringUtils.isNotBlank(req.getCompleteEndTime())) {
            lambdaQuery.le(TaskCollectionRecord::getCompletionTime, req.getCompleteEndTime());
        }
        lambdaQuery.orderByDesc(TaskCollectionRecord::getCompletionTime);
        baseMapper.selectPage(page, lambdaQuery.getWrapper());
        List<TaskCollectionRecord> records = page.getRecords();
        List<TaskCollectionRecordDTO> list = walletMapStruct.taskCollectionRecordTransform(records);
        return PageUtils.flush(page, list);
    }


    @Override
    public RestResult<PageReturn<TaskCollectionRecordListVo>> getPageList(TaskCollectionRecordPageReq req) {
        //获取当前会员信息
        MemberInfo memberInfo = memberInfoService.getMemberInfo();
        if (memberInfo == null) {
            log.error("查询奖励明细失败: 未获取到会员认证信息");
            return RestResult.failure(ResultCode.RELOGIN);
        }
        if (req == null) {
            req = new TaskCollectionRecordPageReq();
            req.setStartDate(DateUtil.format(LocalDateTime.now(), GlobalConstants.DATE_FORMAT_DAY));
        }

        LambdaQueryChainWrapper<TaskCollectionRecord> lambdaQuery = lambdaQuery();
        //查询当前会员的奖励明细
        lambdaQuery.eq(TaskCollectionRecord::getMemberId, memberInfo.getId());
        //--动态查询 任务类型
        if (StringUtils.isNotEmpty(req.getTaskType())) {
            lambdaQuery.eq(TaskCollectionRecord::getTaskType, Integer.valueOf(req.getTaskType()));
        }
        //--动态查询 时间范围
        if (StringUtils.isNotEmpty(req.getStartDate())) {
            LocalDate localDate = LocalDate.parse(req.getStartDate());
            LocalDateTime startOfDay = localDate.atStartOfDay();
            lambdaQuery.ge(TaskCollectionRecord::getCreateTime, startOfDay);
        }
        if (StringUtils.isNotEmpty(req.getEndDate())) {
            LocalDate localDate = LocalDate.parse(req.getEndDate());
            LocalDateTime endOfDay = LocalDateTime.of(localDate, LocalTime.MAX);
            lambdaQuery.le(TaskCollectionRecord::getCreateTime, endOfDay);
        }
        // 倒序排序
        lambdaQuery.orderByDesc(TaskCollectionRecord::getCreateTime);

        Page<TaskCollectionRecord> pageCollectionOrder = new Page<>();
        pageCollectionOrder.setCurrent(req.getPageNo());
        pageCollectionOrder.setSize(req.getPageSize());
        baseMapper.selectPage(pageCollectionOrder, lambdaQuery.getWrapper());

        List<TaskCollectionRecord> records = pageCollectionOrder.getRecords();

        PageReturn<TaskCollectionRecord> flush = PageUtils.flush(pageCollectionOrder, records);

        //IPage＜实体＞转 IPage＜Vo＞
        ArrayList<TaskCollectionRecordListVo> resultList = new ArrayList<>();
        for (TaskCollectionRecord record : flush.getList()) {
            TaskCollectionRecordListVo listVo = new TaskCollectionRecordListVo();
            BeanUtil.copyProperties(record, listVo);
            resultList.add(listVo);
        }

        PageReturn<TaskCollectionRecordListVo> resultPage = new PageReturn<>();
        resultPage.setPageNo(flush.getPageNo());
        resultPage.setPageSize(flush.getPageSize());
        resultPage.setTotal(flush.getTotal());
        resultPage.setList(resultList);

        return RestResult.ok(resultPage);
    }


    /**
     * 获取领奖会员列表
     *
     * @return {@link List}<{@link PrizeWinnersVo}>
     */
    @Override
    public List<PrizeWinnersVo> getPrizeWinnersList() {

        List<TaskCollectionRecord> records = lambdaQuery()
                .select(TaskCollectionRecord::getMemberAccount, TaskCollectionRecord::getTaskType, TaskCollectionRecord::getRewardAmount)
                .orderByDesc(TaskCollectionRecord::getId)// 根据create_time字段降序排序
                .last("LIMIT 15")// 限制结果为最新的15条记录
                .list();

        List<PrizeWinnersVo> results = records.stream().map(record -> {
            PrizeWinnersVo prizeWinnersVo = new PrizeWinnersVo();
            prizeWinnersVo.setMemberAccount(maskAccount(record.getMemberAccount()));
            prizeWinnersVo.setTaskType(String.valueOf(record.getTaskType()));
            prizeWinnersVo.setRewardAmount(record.getRewardAmount());
            return prizeWinnersVo;
        }).collect(Collectors.toList());

        if (CollectionUtils.isEmpty(results) || results.size() < 15) {
            int rowCount = CollectionUtils.isEmpty(results) ? 0 : results.size();
            int supplyCount = 15 - rowCount;
            List<TaskManager> activeTaskTypes = taskManagerService.getActiveTaskTypes();
            if (CollectionUtils.isEmpty(activeTaskTypes)) {
                return results;
            }
            for (int i = 0; i < supplyCount; i++) {
                PrizeWinnersVo prizeWinnersVo = new PrizeWinnersVo();
                // 随机任务类型
                TaskManager taskType = activeTaskTypes.get(new Random().nextInt(activeTaskTypes.size()));
                // 生成开头7-9且为10位的数字账户
                String randomAccount = (new Random().nextInt(3) + 7) + RandomStringUtils.randomNumeric(9);
                prizeWinnersVo.setMemberAccount(maskAccount(randomAccount));
                prizeWinnersVo.setTaskType(taskType.getTaskType());
                prizeWinnersVo.setRewardAmount(taskType.getTaskReward());
                results.add(prizeWinnersVo);
            }
        }

        return results;
    }

    @SneakyThrows
    @Override
    public TaskCollectionRecordDTO getStatisticsData() {
        TaskCollectionRecordDTO recordDTO = new TaskCollectionRecordDTO();
        // 完成人数
        CompletableFuture<Long> finishNumFuture = CompletableFuture.supplyAsync(() -> {
            return taskCollectionRecordMapper.getFinishNum();
        });

        // 领取人数
        CompletableFuture<Long> receiveNumFuture = CompletableFuture.supplyAsync(() -> {
            return taskCollectionRecordMapper.getReceiveNum();
        });

        // 奖励金额
        CompletableFuture<BigDecimal> rewardAmountFuture = CompletableFuture.supplyAsync(() -> {
            return taskCollectionRecordMapper.getRewardAmount();
        });

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(finishNumFuture, receiveNumFuture, rewardAmountFuture);
        allFutures.get();
        recordDTO.setRewardAmount(rewardAmountFuture.get());
        recordDTO.setRecipientsNum(receiveNumFuture.get());
        recordDTO.setCompletionNum(finishNumFuture.get());
        return recordDTO;
    }

    /**
     * 查看会员是否领取过任务奖励
     *
     * @param memberId 会员id
     * @param taskId   任务id
     * @return boolean
     */
    @Override
    public boolean checkTaskCompletedByMember(Long memberId, Long taskId) {

        Integer count = lambdaQuery()
                .eq(TaskCollectionRecord::getMemberId, memberId)
                .eq(TaskCollectionRecord::getTaskId, taskId)
                .count();

        return count > 0;
    }


    /**
     * 检查会员是否领取过奖励
     *
     * @param memberTaskStatus
     * @return {@link TaskCollectionRecord}
     */
    @Override
    public TaskCollectionRecord hasReceivedReward(MemberTaskStatus memberTaskStatus) {
        //查询该笔任务是否已领取过奖励 如果存在记录 说明该笔任务订单已被领取过奖励了
        return lambdaQuery().eq(TaskCollectionRecord::getOrderNo, memberTaskStatus.getOrderNo()).one();
    }


    /**
     * 领取任务奖励
     *
     * @param claimTaskRewardReq
     * @param request
     * @return {@link RestResult}
     */
    @Override
    @Transactional
    public RestResult claimTaskReward(ClaimTaskRewardReq claimTaskRewardReq, HttpServletRequest request) {

        //获取会员信息
        Long memberId = UserContext.getCurrentUserId();

        if (memberId == null) {
            log.error("领取任务奖励处理失败: 获取会员信息失败, req: {}", claimTaskRewardReq);
            return RestResult.failure(ResultCode.RELOGIN);
        }

        //分布式锁key ar-wallet-handleDailyTask+会员id  与完成任务共用一把锁
        String key = "ar-wallet-handleDailyTask" + memberId;
        RLock lock = redissonUtil.getLock(key);

        boolean req = false;

        try {
            req = lock.tryLock(10, TimeUnit.SECONDS);

            if (req) {

                //查看是否开启任务总开关
                if (controlSwitchService.isSwitchEnabled(SwitchIdEnum.CHECK_ACTIVE_TASKS.getSwitchId())) {

                    //获取会员信息 加上排他行锁
                    MemberInfo memberInfo = memberInfoMapper.selectMemberInfoForUpdate(memberId);

                    if (memberInfo == null) {
                        log.error("领取任务奖励处理失败: 获取会员信息失败, req: {}, 会员id: {}", claimTaskRewardReq, memberId);
                        return RestResult.failure(ResultCode.RELOGIN);
                    }

                    BigDecimal previousBalance = memberInfo.getBalance();

                    //根据任务id查询任务
                    TaskManager taskManager = taskManagerService.getTaskDetailsById(claimTaskRewardReq.getTaskId());

                    //校验任务是否存在
                    if (taskManager == null) {
                        log.error("领取任务奖励处理失败: 任务不存在或任务类型不正确, req: {}, 任务详情: {}, 会员信息: {}, ", claimTaskRewardReq, taskManager, memberInfo);
                        return RestResult.failure(ResultCode.ACTIVITY_NOT_STARTED);
                    }

                    //检查是否完成了任务 并且未领取奖励 加上排他行锁
                    MemberTaskStatus memberTaskStatus = memberTaskStatusService.checkTaskCompletedAndRewardUnclaimed(memberInfo, taskManager);

                    if (memberTaskStatus == null) {
                        //该任务未完成或已被领取过 直接返回成功
                        log.info("领取任务奖励处理成功, 该任务未完成或已被领取过, req: {}, 会员id: {}", claimTaskRewardReq, memberInfo.getId());
                        return RestResult.ok();
                    }

                    //检查奖励表是否有记录 (是否领取过奖励) 根据任务订单号进行查询
                    if (taskCollectionRecordService.hasReceivedReward(memberTaskStatus) != null) {
                        log.info("领取任务奖励处理成功, 该任务已被领取过, req: {}, 会员id: {}. 任务状态信息: {}, taskManager: {}", claimTaskRewardReq, memberInfo.getId(), memberTaskStatus, taskManager);
                        return RestResult.ok();
                    }

                    //新增领取奖励记录
                    createTaskCollectionRecord(memberInfo, request, memberTaskStatus, taskManager);

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

                    log.info("领取任务奖励成功, 会员id: {}, 奖励金额: {}, 任务信息: {}", memberInfo.getId(), taskManager.getTaskReward(), taskManager);

                    return RestResult.ok();
                } else {
                    log.info("领取任务奖励失败, 任务总开关未开启, 任务id: {}", claimTaskRewardReq.getTaskId());
                    return RestResult.failure(ResultCode.ACTIVITY_NOT_STARTED);
                }
            }
        } catch (Exception e) {
            //手动回滚
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("领取任务奖励处理失败 会员id: {}, req: {} e: ", memberId, claimTaskRewardReq, e);
        } finally {
            //释放锁
            if (req && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        return RestResult.failure(ResultCode.SYSTEM_EXECUTION_ERROR);
    }


    /**
     * 新增领取奖励记录
     *
     * @return {@link Boolean}
     */
    public Boolean createTaskCollectionRecord(MemberInfo memberInfo, HttpServletRequest request, MemberTaskStatus memberTaskStatus, TaskManager taskManager) {

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

        //领取ip
        taskCollectionRecord.setReceiveIp(IpUtil.getRealIP(request));

        //领取标识 1-手动领取  2-自动领取
        taskCollectionRecord.setReceiveType(1);

        //领取日期
        taskCollectionRecord.setReceiveDate(LocalDate.now());

        //插入奖励明细表
        return taskCollectionRecordService.save(taskCollectionRecord);
    }


    /**
     * 会员账号去敏
     *
     * @param account
     * @return {@link String}
     */
    private String maskAccount(String account) {
        if (StringUtils.isBlank(account)) {
            return account;
        }
        //正常账号 屏蔽****
        if (account.length() > 7) {
            int start = (account.length() - 4) / 2;
            return account.substring(0, start) + "****" + account.substring(start + 4);
        } else if (account.length() > 5) {
            //小于7 大于5的账号 屏蔽**
            int start = (account.length() - 2) / 2;
            return account.substring(0, start) + "**" + account.substring(start + 2);
        }
        return account; // 如果长度不超过5位，直接返回原字符串
    }

}

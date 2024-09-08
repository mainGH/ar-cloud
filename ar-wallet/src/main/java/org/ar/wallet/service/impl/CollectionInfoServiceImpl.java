package org.ar.wallet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.page.PageRequestHome;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.core.result.ResultCode;
import org.ar.common.mybatis.util.PageUtils;
import org.ar.common.pay.dto.CollectionInfoDTO;
import org.ar.common.pay.req.CollectionInfoIdReq;
import org.ar.common.pay.req.CollectionInfoListPageReq;
import org.ar.common.redis.util.RedissonUtil;
import org.ar.wallet.Enum.CollectionInfoStatusEnum;
import org.ar.wallet.config.WalletMapStruct;
import org.ar.wallet.entity.CollectionInfo;
import org.ar.wallet.entity.MemberInfo;
import org.ar.wallet.mapper.CollectionInfoMapper;
import org.ar.wallet.req.*;
import org.ar.wallet.service.ICollectionInfoService;
import org.ar.wallet.service.IMatchPoolService;
import org.ar.wallet.service.IMemberInfoService;
import org.ar.wallet.service.IPaymentOrderService;
import org.ar.wallet.vo.CheckUpiIdDuplicateVo;
import org.ar.wallet.vo.CollectionInfoVo;
import org.ar.wallet.vo.NormalCollectionInfoVo;
import org.redisson.api.RLock;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CollectionInfoServiceImpl extends ServiceImpl<CollectionInfoMapper, CollectionInfo> implements ICollectionInfoService {
    private final WalletMapStruct walletMapStruct;

    @Autowired
    private IMatchPoolService matchPoolService;

    @Autowired
    private IPaymentOrderService paymentOrderService;
    private final CollectionInfoMapper collectionInfoMapper;

    @Autowired
    private ICollectionInfoService collectionInfoService;
    private final RedissonUtil redissonUtil;

    @Autowired
    private IMemberInfoService memberInfoService;


    /**
     * 开启收款时校验
     *
     * @param collectionInfo
     * @return {@link RestResult}
     */
    @Override
    public RestResult enableCollectionVerification(CollectionInfo collectionInfo, MemberInfo memberInfo) {

        String memberId = String.valueOf(memberInfo.getId());

        //先查询该收款信息是否属于该会员
        if (collectionInfo == null || !collectionInfo.getMemberId().equals(memberId)) {
            log.error("开启收款时校验失败 该收款信息不存在或收款信息不属于该会员 会员信息: {}, 收款信息: {}", memberInfo, collectionInfo);
            return RestResult.failure(ResultCode.ILLEGAL_OPERATION_COLLECTION_INFO_CHECK_FAILED);
        }

        //校验该收款信息是否已被删除
        if (collectionInfo.getDeleted() == 1) {
            log.error("开启收款时校验失败 该收款信息已被删除 会员信息: {}, 收款信息: {}", memberInfo, collectionInfo);
            return RestResult.failure(ResultCode.ILLEGAL_OPERATION_COLLECTION_INFO_CHECK_FAILED);
        }

        //校验今日收款笔数是否达到限制
        if (collectionInfo.getDailyLimitCount() != null && collectionInfo.getTodayCollectedCount() >= collectionInfo.getDailyLimitCount()) {
            log.error("开启收款时校验失败 该收款信息今日收款笔数已达到限制 会员信息: {}, 收款信息: {}", memberInfo, collectionInfo);
            return RestResult.failure(ResultCode.COLLECTION_DAILY_LIMIT_REACHED);
        }

        //判断该收款信息今日额度是否已满
        if (collectionInfo.getDailyLimitAmount() != null && collectionInfo.getTodayCollectedAmount().compareTo(collectionInfo.getDailyLimitAmount()) >= 0) {
            log.error("开启收款时校验失败 该收款信息今日额度已满 会员信息: {}, 收款信息: {}", memberInfo, collectionInfo);
            return RestResult.failure(ResultCode.COLLECTION_DAILY_AMOUNT_LIMIT_REACHED);
        }

        return null;
    }

    /**
     * 停止收款时校验
     *
     * @param collectionInfo
     * @return {@link RestResult}
     */
    @Override
    public RestResult stopCollectionVerification(CollectionInfo collectionInfo, MemberInfo memberInfo) {

        String memberId = String.valueOf(memberInfo.getId());

        //校验该收款信息是否属于该会员
        if (collectionInfo == null || !collectionInfo.getMemberId().equals(memberId)) {
            log.error("停止收款时校验失败: 该收款信息不存在或收款信息不属于该会员, 会员信息: {}, 收款信息: {}", memberInfo, collectionInfo);
            return RestResult.failure(ResultCode.ILLEGAL_OPERATION_COLLECTION_INFO_CHECK_FAILED);
        }

        //查询该收款信息 匹配池中正在匹配的订单数量
        Integer matchingOrdersMatchPoll = matchPoolService.getMatchingOrdersBycollectionId(collectionInfo.getId());
        //查询该收款信息 卖出订单表中正在匹配的订单数量
        Integer matchingOrdersPayment = paymentOrderService.getMatchingOrdersBycollectionId(collectionInfo.getId());

        //查看该收款信息 是否有存在 匹配中的订单 (查询匹配池 卖出订单)
        if ((matchingOrdersMatchPoll != null && matchingOrdersMatchPoll > 0) || (matchingOrdersPayment != null && matchingOrdersPayment > 0)) {

            log.error("停止收款时校验失败: 当前有存在匹配中的订单, 匹配池中正在匹配的订单数量: {}, 卖出订单表中正在匹配的订单数量: {}, 会员信息: {}, 收款信息: {}", matchingOrdersMatchPoll, matchingOrdersPayment, memberInfo, collectionInfo);
            return RestResult.failure(ResultCode.MATCHING_ORDER_IN_PROGRESS);
        }

        return null;
    }

    /**
     * @param req
     * @return {@link RestResult}<{@link PageReturn}<{@link CollectionInfoVo}>>
     *//*
     * 获取当前用户收款信息
     * */
    @Override
    public RestResult<PageReturn<CollectionInfoVo>> currentCollectionInfo(PageRequestHome req) {

        //获取当前会员信息
        MemberInfo memberInfo = memberInfoService.getMemberInfo();

        if (memberInfo == null) {
            log.error("获取当前用户收款信息失败: 获取会员信息失败");
            return RestResult.failure(ResultCode.RELOGIN);
        }

        String memberId = String.valueOf(memberInfo.getId());

        if (req == null) {
            req = new PageRequestHome();
        }

        Page<CollectionInfo> pageCollectionInfo = new Page<>();
        pageCollectionInfo.setCurrent(req.getPageNo());
        pageCollectionInfo.setSize(req.getPageSize());

        LambdaQueryChainWrapper<CollectionInfo> lambdaQuery = lambdaQuery();

        lambdaQuery.eq(CollectionInfo::getMemberId, memberId).eq(CollectionInfo::getDeleted, 0).orderByDesc(CollectionInfo::getId).list();

        baseMapper.selectPage(pageCollectionInfo, lambdaQuery.getWrapper());

        List<CollectionInfo> records = pageCollectionInfo.getRecords();

        //如果会员只有一个收款信息并且未设置默认收款信息, 那么将此收款信息设为默认
        if (records.size() == 1){
            CollectionInfo collectionInfo = records.get(0);

            //校验该收款信息是否属于该会员
            if (collectionInfo == null || !collectionInfo.getMemberId().equals(memberId)) {
                log.error("设置默认收款信息处理失败 该收款信息不存在或收款信息不属于该会员 会员信息: {}, 收款信息: {}", memberInfo, collectionInfo);
                return RestResult.failure(ResultCode.ILLEGAL_OPERATION_COLLECTION_INFO_CHECK_FAILED);
            }

            if (collectionInfo.getDefaultStatus() == 0){
                //用户只有此收款信息 并且此收款信息不是默认, 系统自动将该收款信息设置为默认

                //清除 该会员默认收款信息
                clearDefaultCollectionInfo(memberId);

                //设置该会员默认收款信息
                setDefaultCollectionInfo(collectionInfo.getId());
            }
        }

        ArrayList<CollectionInfoVo> CollectionInfoVoList = new ArrayList<>();

        for (CollectionInfo collectionInfo : records) {
            CollectionInfoVo collectionInfoVo = new CollectionInfoVo();
            BeanUtils.copyProperties(collectionInfo, collectionInfoVo);
            CollectionInfoVoList.add(collectionInfoVo);
        }

        PageReturn<CollectionInfoVo> flush = PageUtils.flush(pageCollectionInfo, CollectionInfoVoList);

        log.info("获取当前用户收款信息成功 会员账号: {}, 返回数据: {}", memberInfo.getMemberAccount(), flush);

        return RestResult.ok(flush);
    }

    /**
     * 获取当前用户在正常收款的收款信息
     *
     * @param req
     * @return {@link RestResult}<{@link PageReturn}<{@link NormalCollectionInfoVo}>>
     */
    @Override
    public RestResult<PageReturn<NormalCollectionInfoVo>> currentNormalCollectionInfo(PageRequestHome req) {

        //获取当前会员信息
        MemberInfo memberInfo = memberInfoService.getMemberInfo();

        if (memberInfo == null) {
            log.error("获取当前用户在正常收款的收款信息失败: 获取会员信息失败");
            return RestResult.failure(ResultCode.RELOGIN);
        }

        String memberId = String.valueOf(memberInfo.getId());

        if (req == null) {
            req = new PageRequestHome();
        }

        Page<CollectionInfo> pageCollectionInfo = new Page<>();
        pageCollectionInfo.setCurrent(req.getPageNo());
        pageCollectionInfo.setSize(req.getPageSize());

        LambdaQueryChainWrapper<CollectionInfo> lambdaQuery = lambdaQuery();

        lambdaQuery
                .eq(CollectionInfo::getMemberId, memberInfo.getId())
                .eq(CollectionInfo::getDeleted, 0)
                .eq(CollectionInfo::getCollectedStatus, CollectionInfoStatusEnum.NORMAL.getCode())
                .orderByDesc(CollectionInfo::getId)
                .list();

        baseMapper.selectPage(pageCollectionInfo, lambdaQuery.getWrapper());

        List<CollectionInfo> records = pageCollectionInfo.getRecords();

        //如果会员只有一个收款信息并且未设置默认收款信息, 那么将此收款信息设为默认
        if (records.size() == 1){
            CollectionInfo collectionInfo = records.get(0);

            //校验该收款信息是否属于该会员
            if (collectionInfo == null || !collectionInfo.getMemberId().equals(memberId)) {
                log.error("设置默认收款信息处理失败 该收款信息不存在或收款信息不属于该会员 会员信息: {}, 收款信息: {}", memberInfo, collectionInfo);
                return RestResult.failure(ResultCode.ILLEGAL_OPERATION_COLLECTION_INFO_CHECK_FAILED);
            }

            if (collectionInfo.getDefaultStatus() == 0){
                //用户只有此收款信息 并且此收款信息不是默认, 系统自动将该收款信息设置为默认

                //清除 该会员默认收款信息
                clearDefaultCollectionInfo(memberId);

                //设置该会员默认收款信息
                setDefaultCollectionInfo(collectionInfo.getId());
            }
        }

        ArrayList<NormalCollectionInfoVo> normalCollectionInfoVoList = new ArrayList<>();

        for (CollectionInfo collectionInfo : records) {
            NormalCollectionInfoVo normalCollectionInfoVo = new NormalCollectionInfoVo();
            BeanUtils.copyProperties(collectionInfo, normalCollectionInfoVo);
            normalCollectionInfoVoList.add(normalCollectionInfoVo);
        }

        PageReturn<NormalCollectionInfoVo> flush = PageUtils.flush(pageCollectionInfo, normalCollectionInfoVoList);

        log.info("获取当前用户在正常收款的收款信息成功 会员账号: {}, 返回数据: {}", memberInfo.getMemberAccount(), flush);

        return RestResult.ok(flush);
    }

    /**
     * 更新收款信息: 今日收款金额、今日收款笔数
     *
     * @param sellReq
     * @return {@link Boolean}
     */
    @Override
    public Boolean addCollectionInfoQuotaAndCount(SellReq sellReq, CollectionInfo collectionInfo) {
        //添加收款信息: 已收款金额、已收款笔数、今日收款金额、今日收款笔数
        return lambdaUpdate().eq(CollectionInfo::getId, sellReq.getCollectionInfoId())
                .set(CollectionInfo::getTodayCollectedAmount, collectionInfo.getTodayCollectedAmount().add(sellReq.getAmount()))//添加今日收款金额
                .set(CollectionInfo::getTodayCollectedCount, collectionInfo.getTodayCollectedCount() + 1).update();//今日收款笔数+1
    }


    /**
     * 删除收款信息
     *
     * @param collectionInfoId
     * @return {@link Boolean}
     */
    @Override
    public Boolean deleteCollectionInfo(Long collectionInfoId) {
        return lambdaUpdate().eq(CollectionInfo::getId, collectionInfoId).set(CollectionInfo::getDeleted, 1).set(CollectionInfo::getCollectedStatus, CollectionInfoStatusEnum.CLOSE.getCode()).update();
    }


    @Override
    public PageReturn<CollectionInfoDTO> listPage(CollectionInfoListPageReq req) {
        Page<CollectionInfo> page = new Page<>();
        page.setCurrent(req.getPageNo());
        page.setSize(req.getPageSize());
        LambdaQueryChainWrapper<CollectionInfo> lambdaQuery = lambdaQuery();
        lambdaQuery.orderByDesc(CollectionInfo::getCreateTime);
        if (!com.alibaba.nacos.api.utils.StringUtils.isBlank(req.getUpiId())) {
            lambdaQuery.eq(CollectionInfo::getUpiId, req.getUpiId());
        }

        if (!com.alibaba.nacos.api.utils.StringUtils.isBlank(req.getMemberId())) {
            lambdaQuery.eq(CollectionInfo::getMemberId, req.getMemberId());
        }

        if (!com.alibaba.nacos.api.utils.StringUtils.isBlank(req.getUpiName())) {
            lambdaQuery.eq(CollectionInfo::getUpiName, req.getUpiName());
        }

        if (!com.alibaba.nacos.api.utils.StringUtils.isBlank(req.getMemberAccount())) {
            lambdaQuery.eq(CollectionInfo::getMemberAccount, req.getMemberAccount());
        }
        lambdaQuery.eq(CollectionInfo::getDeleted, 0);
        baseMapper.selectPage(page, lambdaQuery.getWrapper());
        List<CollectionInfoDTO> list = new ArrayList<CollectionInfoDTO>();
        List<CollectionInfo> records = page.getRecords();
        for(CollectionInfo collectionInfo: records){
            CollectionInfoDTO collectionInfoDTO = new CollectionInfoDTO();
            BeanUtils.copyProperties(collectionInfo,collectionInfoDTO);
            collectionInfoDTO.setCollectedNumber(collectionInfo.getCollectedCount());
            list.add(collectionInfoDTO);
        }
       // List<CollectionInfoDTO> list = walletMapStruct.collectionInfoTransform(records);
        return PageUtils.flush(page, list);
    }


    @Override
    public List<CollectionInfoDTO> getListByUid(CollectionInfoIdReq req) {

        LambdaQueryChainWrapper<CollectionInfo> lambdaQuery = lambdaQuery();
        if (!com.alibaba.nacos.api.utils.StringUtils.isBlank(req.getId().toString())) {
            lambdaQuery.eq(CollectionInfo::getId, req.getId().toString());
        }
        lambdaQuery.eq(CollectionInfo::getDeleted, 0);
        List<CollectionInfo> list = baseMapper.selectList(lambdaQuery.getWrapper());
        List<CollectionInfoDTO> listDto = new ArrayList<>();
        for(CollectionInfo collectionInfo: list){
            CollectionInfoDTO collectionInfoDTO = new CollectionInfoDTO();
            BeanUtils.copyProperties(collectionInfo,collectionInfoDTO);
            collectionInfoDTO.setCollectedNumber(collectionInfo.getCollectedCount());
            listDto.add(collectionInfoDTO);
        }
        //List<CollectionInfoDTO> listDto = walletMapStruct.collectionInfoTransform(list);
        return listDto;

    }

    /**
     * 开启收款处理
     *
     * @param collectioninfoIdReq
     * @return {@link RestResult}
     */
    @Override
    @Transactional
    public RestResult enableCollectionProcessing(CollectioninfoIdReq collectioninfoIdReq) {

        //获取当前会员信息
        MemberInfo memberInfo = memberInfoService.getMemberInfo();

        if (memberInfo == null) {
            log.error("开启收款处理失败: 获取会员信息失败");
            return RestResult.failure(ResultCode.RELOGIN);
        }

        //获取收款信息 加上排他行锁
        CollectionInfo collectionInfo = collectionInfoMapper.selectCollectionInfoForUpdate(collectioninfoIdReq.getCollectionInfoId());

        //校验收款信息今日额度是否已满
        RestResult restResult = enableCollectionVerification(collectionInfo, memberInfo);
        if (restResult != null) {
            log.error("开启收款处理失败 会员信息: {}, 收款信息: {}, 错误信息: {}", memberInfo, collectionInfo, restResult);
            return restResult;
        }

        //将收款信息状态改为: 开启
        collectionInfo.setCollectedStatus(CollectionInfoStatusEnum.NORMAL.getCode());
        //更新收款信息
        if (updateById(collectionInfo)) {
            log.info("开启收款信息成功 会员账号: {}, req: {}, 收款信息: {}", memberInfo.getMemberAccount(), collectioninfoIdReq, collectionInfo);
            return RestResult.ok();
        }

        log.error("开启收款信息失败 会员账号: {}, req: {}, 收款信息: {}", memberInfo.getMemberAccount(), collectioninfoIdReq, collectionInfo);
        return RestResult.failure(ResultCode.SYSTEM_EXECUTION_ERROR);
    }

    /**
     * 停止收款处理
     *
     * @param collectioninfoIdReq
     * @return {@link RestResult}
     */
    @Override
    @Transactional
    public RestResult stopCollectionProcessing(CollectioninfoIdReq collectioninfoIdReq) {

        //获取当前会员信息
        MemberInfo memberInfo = memberInfoService.getMemberInfo();

        if (memberInfo == null) {
            log.error("停止收款处理失败: 获取会员信息失败");
            return RestResult.failure(ResultCode.RELOGIN);
        }

        //获取收款信息 加上排他行锁
        CollectionInfo collectionInfo = collectionInfoMapper.selectCollectionInfoForUpdate(collectioninfoIdReq.getCollectionInfoId());

        //查看当前是否有正在进行匹配中的订单
        RestResult restResult = stopCollectionVerification(collectionInfo, memberInfo);
        if (restResult != null) {
            log.error("停止收款处理失败: 会员信息: {}, 收款信息: {}, 失败信息: {}", memberInfo, collectionInfo, restResult);
            return restResult;
        }

        //将收款信息状态改为: 关闭
        collectionInfo.setCollectedStatus(CollectionInfoStatusEnum.CLOSE.getCode());
        //更新收款信息
        if (updateById(collectionInfo)) {
            log.info("停止收款处理成功 会员账号: {}, req: {}, 收款信息: {}", memberInfo.getMemberAccount(), collectioninfoIdReq, collectionInfo);
            return RestResult.ok();
        }

        log.error("停止收款处理失败 会员账号: {}, req: {}, 收款信息: {}", memberInfo.getMemberAccount(), collectioninfoIdReq, collectionInfo);
        return RestResult.failure(ResultCode.SYSTEM_EXECUTION_ERROR);
    }

    /**
     * 删除收款处理
     *
     * @param collectionInfoId
     * @return {@link RestResult}
     */
    @Override
    public RestResult deleteCollectionInfoProcessing(Long collectionInfoId) {


        //获取当前会员信息
        MemberInfo memberInfo = memberInfoService.getMemberInfo();

        if (memberInfo == null) {
            log.error("删除收款处理失败: 获取会员信息失败");
            return RestResult.failure(ResultCode.RELOGIN);
        }

        //校验该收款信息是否属于该会员
        CollectionInfo collectionInfo = getById(collectionInfoId);

        String memberId = String.valueOf(memberInfo.getId());

        if (collectionInfo != null && collectionInfo.getMemberId().equals(memberId)) {
            if (deleteCollectionInfo(collectionInfoId)) {
                log.info("删除收款信息成功, 会员账号: {}, 收款信息: {}", memberInfo.getMemberAccount(), collectionInfo);
                return RestResult.ok();
            }
        }

        log.info("删除收款信息失败, 会员信息: {}, 收款信息: {}", memberInfo, collectionInfo);

        return RestResult.failure(ResultCode.SYSTEM_EXECUTION_ERROR);
    }


    /**
     * 添加收款信息处理
     *
     * @param frontendCollectionInfoReq
     * @return {@link RestResult}
     */
    @Override
    public RestResult createcollectionInfoProcessing(FrontendCollectionInfoReq frontendCollectionInfoReq) {

        //获取当前会员信息
        MemberInfo memberInfo = memberInfoService.getMemberInfo();

        if (memberInfo == null) {
            log.error("添加收款信息处理失败: 获取会员信息失败");
            return RestResult.failure(ResultCode.RELOGIN);
        }

        //分布式锁key ar-wallet-createcollectionInfoProcessing+会员id
        String key = "ar-wallet-createcollectionInfoProcessing" + memberInfo.getId();
        RLock lock = redissonUtil.getLock(key);

        boolean req = false;

        try {
            req = lock.tryLock(10, TimeUnit.SECONDS);

            if (req) {

                //收款信息去重校验
                CollectionInfo getPaymentDetailsByUpiIdAndUpiName = collectionInfoService.getPaymentDetailsByUpiId(frontendCollectionInfoReq.getUpiId());

                if (getPaymentDetailsByUpiIdAndUpiName != null) {
                    //已存在该UPI信息了
                    log.error("添加收款信息处理失败: UPI信息重复, 手机号: {}, 验证码: {}, req: {}", memberInfo.getMobileNumber(), frontendCollectionInfoReq.getVerificationCode(), frontendCollectionInfoReq);
                    return RestResult.failure(ResultCode.DUPLICATE_UPI_ERROR);
                }

                ValidateSmsCodeReq validateSmsCodeReq = new ValidateSmsCodeReq();

                //当前手机号码
                validateSmsCodeReq.setMobileNumber(memberInfo.getMobileNumber());

                //验证码
                validateSmsCodeReq.setVerificationCode(frontendCollectionInfoReq.getVerificationCode());

                //校验验证码
                if (!memberInfoService.signUpValidateSmsCode(validateSmsCodeReq)) {
                    log.error("添加收款信息处理失败: 验证码错误, 手机号: {}, 验证码: {}, req: {}", memberInfo.getMobileNumber(), frontendCollectionInfoReq.getVerificationCode(), frontendCollectionInfoReq);
                    return RestResult.failure(ResultCode.VERIFICATION_CODE_ERROR);
                }

                log.info("添加收款信息: 验证码校验成功, 手机号: {}, 验证码: {}, req: {}", memberInfo.getMobileNumber(), frontendCollectionInfoReq.getVerificationCode(), frontendCollectionInfoReq);

                CollectionInfo collectionInfo = new CollectionInfo();
                BeanUtils.copyProperties(frontendCollectionInfoReq, collectionInfo);

                //查询该会员是否存在收款信息
                if (!collectionInfoService.hasCollectionInfo(String.valueOf(memberInfo.getId()))){
                    //会员不存在收款信息 将此收款信息设置为默认
                    collectionInfo.setDefaultStatus(1);
                }

                //设置会员id
                collectionInfo.setMemberId(String.valueOf(memberInfo.getId()));

                //设置会员账号
                collectionInfo.setMemberAccount(memberInfo.getMemberAccount());

                //设置手机号
                collectionInfo.setMobileNumber(memberInfo.getMobileNumber());

                if (save(collectionInfo)) {

                    log.info("添加收款信息处理成功 会员账号: {}, 收款信息: {}", memberInfo.getMemberAccount(), collectionInfo);

                    return RestResult.ok();
                }
            }
        } catch (Exception e) {
            log.error("添加收款信息处理失败 会员账号: {}, req: {}, e: {}", memberInfo.getMemberAccount(), frontendCollectionInfoReq, e);
        } finally {
            //释放锁
            if (req && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        log.error("添加收款信息处理失败 会员账号: {}, req: {}", memberInfo.getMemberAccount(), frontendCollectionInfoReq);
        return RestResult.failure(ResultCode.SYSTEM_EXECUTION_ERROR);
    }


    /**
     * 根据upi_id和upi_name获取收款信息
     *
     * @param upiId
     * @param upiName
     * @return {@link RestResult}
     */
    @Override
    public CollectionInfo getPaymentDetailsByUpiIdAndUpiName(String upiId, String upiName) {
        return lambdaQuery()
                .eq(CollectionInfo::getUpiId, upiId)
                .eq(CollectionInfo::getUpiName, upiName)
                .eq(CollectionInfo::getDeleted, 0)
                .last("LIMIT 1")
                .one();
    }


    /**
     * 查询 upi_id 是否存在
     *
     * @param upiId
     * @return {@link CollectionInfo}
     */
    @Override
    public CollectionInfo getPaymentDetailsByUpiId(String upiId) {
        return lambdaQuery()
                .eq(CollectionInfo::getUpiId, upiId)
                .eq(CollectionInfo::getDeleted, 0)
                .last("LIMIT 1")
                .one();
    }


    /**
     * 设置默认收款信息
     *
     * @param collectioninfoIdReq
     * @return {@link RestResult}
     */
    @Override
    @Transactional
    public RestResult setDefaultCollectionInfoReq(CollectioninfoIdReq collectioninfoIdReq) {


        try {
            //获取当前会员信息
            MemberInfo memberInfo = memberInfoService.getMemberInfo();

            if (memberInfo == null) {
                log.error("设置默认收款信息处理失败: 获取会员信息失败");
                return RestResult.failure(ResultCode.RELOGIN);
            }

            //获取收款信息
            CollectionInfo collectionInfo = collectionInfoService.getById(collectioninfoIdReq.getCollectionInfoId());

            String memberId = String.valueOf(memberInfo.getId());

            //先查询该收款信息是否属于该会员
            if (collectionInfo == null || !collectionInfo.getMemberId().equals(memberId)) {
                log.error("设置默认收款信息处理失败 该收款信息不存在或收款信息不属于该会员 会员信息: {}, 收款信息: {}", memberInfo, collectionInfo);
                return RestResult.failure(ResultCode.ILLEGAL_OPERATION_COLLECTION_INFO_CHECK_FAILED);
            }

            //校验该收款信息是否已被删除
            if (collectionInfo.getDeleted() == 1) {
                log.error("设置默认收款信息处理失败 该收款信息已被删除 会员信息: {}, 收款信息: {}", memberInfo, collectionInfo);
                return RestResult.failure(ResultCode.ILLEGAL_OPERATION_COLLECTION_INFO_CHECK_FAILED);
            }

            //清除 该会员默认收款信息
            clearDefaultCollectionInfo(memberId);

            //设置该会员默认收款信息
            setDefaultCollectionInfo(collectioninfoIdReq.getCollectionInfoId());

            return RestResult.ok();
        } catch (Exception e) {
            //手动回滚
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("设置默认收款信息处理失败: e: {}", e);
            return RestResult.failure(ResultCode.SYSTEM_EXECUTION_ERROR);
        }
    }


    /**
     * 校验UIPI_ID是否重复
     *
     * @param checkUpiIdDuplicateReq
     * @return {@link RestResult}
     */
    @Override
    public RestResult<CheckUpiIdDuplicateVo> checkUpiIdDuplicate(CheckUpiIdDuplicateReq checkUpiIdDuplicateReq) {
        //收款信息去重校验
        CollectionInfo getPaymentDetailsByUpiIdAndUpiName = getPaymentDetailsByUpiId(checkUpiIdDuplicateReq.getUpiId());

        CheckUpiIdDuplicateVo checkUpiIdDuplicateVo = new CheckUpiIdDuplicateVo();
        checkUpiIdDuplicateVo.setIsUpiIdDuplicate(false);

        if (getPaymentDetailsByUpiIdAndUpiName != null) {
            //已存在该UPI信息了
            checkUpiIdDuplicateVo.setIsUpiIdDuplicate(true);
            log.info("校验UIPI_ID是否重复: UPI_ID重复, req: {}", checkUpiIdDuplicateReq);
        }

        return RestResult.ok(checkUpiIdDuplicateVo);
    }


    /**
     * 获取会员默认收款信息
     *
     * @param memberId
     * @return {@link CollectionInfo}
     */
    @Override
    public CollectionInfo getDefaultCollectionInfoByMemberId(String memberId) {
        LambdaQueryWrapper<CollectionInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .eq(CollectionInfo::getMemberId, memberId)
                .eq(CollectionInfo::getDefaultStatus, 1)
                .eq(CollectionInfo::getDeleted, 0)
                .last("LIMIT 1"); // 确保即使有多个默认收款信息也只返回一个

        return this.getOne(queryWrapper, false); // 第二个参数false表示不抛出异常，如果未找到记录则返回null
    }


    /**
     * 检查某个会员的收款信息数量是否大于0
     *
     * @param memberId 会员ID
     * @return true 如果数量大于0，否则false
     */
    @Override
    public Boolean hasCollectionInfo(String memberId) {
        // 创建LambdaQueryWrapper
        LambdaQueryWrapper<CollectionInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CollectionInfo::getMemberId, memberId) // 根据会员ID过滤
                .eq(CollectionInfo::getDeleted, 0); // 过滤未删除的记录

        // 使用count方法获取数量
        long count = this.count(queryWrapper);

        // 检查数量是否大于0
        return count > 0;
    }


    /**
     * 清除 该会员默认收款信息
     *
     * @param memberId
     */
    public void clearDefaultCollectionInfo(String memberId) {
        lambdaUpdate()
                .eq(CollectionInfo::getMemberId, memberId)
                .set(CollectionInfo::getDefaultStatus, 0)
                .update();
    }


    /**
     * 设置该会员默认收款信息
     *
     * @param collectionInfoId
     */
    public void setDefaultCollectionInfo(Long collectionInfoId) {

        lambdaUpdate()
                .eq(CollectionInfo::getId, collectionInfoId)
                .set(CollectionInfo::getDefaultStatus, 1)
                .update();
    }
}

package org.ar.wallet.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.cloud.commons.lang.StringUtils;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.ar.common.core.page.PageRequestHome;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.core.result.ResultCode;
import org.ar.common.mybatis.util.PageUtils;
import org.ar.common.pay.dto.UsdtBuyOrderDTO;
import org.ar.common.pay.req.UsdtBuyOrderReq;
import org.ar.common.redis.util.RedissonUtil;
import org.ar.wallet.Enum.OrderStatusEnum;
import org.ar.wallet.config.WalletMapStruct;
import org.ar.wallet.entity.AppealOrder;
import org.ar.wallet.entity.MemberInfo;
import org.ar.wallet.entity.UsdtBuyOrder;
import org.ar.wallet.mapper.UsdtBuyOrderMapper;
import org.ar.wallet.oss.OssService;
import org.ar.wallet.property.ArProperty;
import org.ar.wallet.req.PlatformOrderReq;
import org.ar.wallet.service.IMemberInfoService;
import org.ar.wallet.service.ITradeConfigService;
import org.ar.wallet.service.IUsdtBuyOrderService;
import org.ar.wallet.service.IUsdtConfigService;
import org.ar.wallet.util.FileUtil;
import org.ar.wallet.vo.UsdtBuyOrderVo;
import org.ar.wallet.vo.UsdtBuyPageDataVo;
import org.ar.wallet.vo.UsdtPurchaseOrderDetailsVo;
import org.redisson.api.RLock;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UsdtBuyOrderServiceImpl extends ServiceImpl<UsdtBuyOrderMapper, UsdtBuyOrder> implements IUsdtBuyOrderService {
    private final WalletMapStruct walletMapStruct;
    private final IUsdtConfigService usdtConfigService;
    private final ITradeConfigService tradeConfigService;
    private final OssService ossService;
    private final IMemberInfoService memberInfoService;
    private final ArProperty arProperty;
    private final RedissonUtil redissonUtil;

    @Value("${oss.baseUrl}")
    private String baseUrl;

    /**
     * 根据会员id 查询usdt买入记录
     *
     * @param memberId
     * @return {@link List}<{@link UsdtBuyOrder}>
     */
    @Override
    public List<UsdtBuyOrderVo> findPagedUsdtPurchaseRecords(String memberId) {


        LambdaQueryChainWrapper<UsdtBuyOrder> lambdaQuery = lambdaQuery();

        //会员id
        lambdaQuery.eq(UsdtBuyOrder::getMemberId, memberId);

        // 倒序排序
        lambdaQuery.orderByDesc(UsdtBuyOrder::getId);

        //默认查询10条记录
        lambdaQuery.last("LIMIT 10");

        List<UsdtBuyOrder> usdtBuyOrderList = lambdaQuery.list();

        //IPage＜实体＞转 IPage＜Vo＞
        List<UsdtBuyOrderVo> usdtBuyOrderVoList = new ArrayList<>();
        for (UsdtBuyOrder usdtBuyOrder : usdtBuyOrderList) {
            UsdtBuyOrderVo usdtBuyOrderVo = new UsdtBuyOrderVo();
            BeanUtil.copyProperties(usdtBuyOrder, usdtBuyOrderVo);
            usdtBuyOrderVoList.add(usdtBuyOrderVo);
        }

        return usdtBuyOrderVoList;
    }

    /**
     * 查询全部USDT买入记录
     *
     * @param req
     * @return {@link RestResult}<{@link PageReturn}<{@link UsdtBuyOrderVo}>>
     */
    @Override
    public RestResult<PageReturn<UsdtBuyOrderVo>> findAllUsdtPurchaseRecords(PageRequestHome req) {

        //获取当前会员信息
        MemberInfo memberInfo = memberInfoService.getMemberInfo();

        if (memberInfo == null) {
            log.error("获取USDT买入页面数据失败: 获取会员信息失败, req: {}", req);
            return RestResult.failure(ResultCode.RELOGIN);
        }

        if (req == null) {
            req = new PageRequestHome();
        }

        Page<UsdtBuyOrder> pageUsdtBuyOrder = new Page<>();
        pageUsdtBuyOrder.setCurrent(req.getPageNo());
        pageUsdtBuyOrder.setSize(req.getPageSize());

        LambdaQueryChainWrapper<UsdtBuyOrder> lambdaQuery = lambdaQuery();

        //查询当前会员的USDT买入订单
        lambdaQuery.eq(UsdtBuyOrder::getMemberId, memberInfo.getId());

        // 倒序排序
        lambdaQuery.orderByDesc(UsdtBuyOrder::getId);

        baseMapper.selectPage(pageUsdtBuyOrder, lambdaQuery.getWrapper());

        List<UsdtBuyOrder> records = pageUsdtBuyOrder.getRecords();


        ArrayList<UsdtBuyOrderVo> usdtBuyOrderVoList = new ArrayList<>();

        //IPage＜实体＞转 IPage＜Vo＞
        for (UsdtBuyOrder usdtBuyOrder : records) {
            UsdtBuyOrderVo usdtBuyOrderVo = new UsdtBuyOrderVo();
            BeanUtil.copyProperties(usdtBuyOrder, usdtBuyOrderVo);
            usdtBuyOrderVoList.add(usdtBuyOrderVo);
        }

        PageReturn<UsdtBuyOrderVo> flush = PageUtils.flush(pageUsdtBuyOrder, usdtBuyOrderVoList);

        log.info("分页查询全部USDT买入记录: 会员账号: {}, req: {}, 返回数据: {}", memberInfo.getMemberAccount(), req, flush);

        return RestResult.ok(flush);
    }


    @Override
    @SneakyThrows
    public PageReturn<UsdtBuyOrderDTO> listPage(UsdtBuyOrderReq req) {
        Page<UsdtBuyOrder> page = new Page<>();
        page.setCurrent(req.getPageNo());
        page.setSize(req.getPageSize());
        LambdaQueryChainWrapper<UsdtBuyOrder> lambdaQuery = lambdaQuery();
        lambdaQuery.orderByDesc(UsdtBuyOrder::getCreateTime);

        LambdaQueryWrapper<UsdtBuyOrder> queryWrapper = new QueryWrapper<UsdtBuyOrder>()
                .select("IFNULL(sum(usdt_num),0) as usdtNumTotal, IFNULL(sum(arb_num),0) as arbNumTotal").lambda();

        if (!com.alibaba.nacos.api.utils.StringUtils.isBlank(req.getMemberId())) {
            lambdaQuery.eq(UsdtBuyOrder::getMemberId, req.getMemberId());
            queryWrapper.eq(UsdtBuyOrder::getMemberId, req.getMemberId());
        }
        if (!com.alibaba.nacos.api.utils.StringUtils.isBlank(req.getPlatformOrder())) {
            lambdaQuery.eq(UsdtBuyOrder::getPlatformOrder, req.getPlatformOrder());
            queryWrapper.eq(UsdtBuyOrder::getPlatformOrder, req.getPlatformOrder());
        }
        if (!com.alibaba.nacos.api.utils.StringUtils.isBlank(req.getStatus())) {
            lambdaQuery.eq(UsdtBuyOrder::getStatus, req.getStatus());
            queryWrapper.eq(UsdtBuyOrder::getStatus, req.getStatus());
        }
        if (!ObjectUtils.isEmpty(req.getCreateTimeStart())) {
            lambdaQuery.ge(UsdtBuyOrder::getCreateTime, req.getCreateTimeStart());
            queryWrapper.ge(UsdtBuyOrder::getCreateTime, req.getCreateTimeStart());
        }
        if (!ObjectUtils.isEmpty(req.getCreateTimeEnd())) {
            lambdaQuery.le(UsdtBuyOrder::getCreateTime, req.getCreateTimeEnd());
            queryWrapper.le(UsdtBuyOrder::getCreateTime, req.getCreateTimeEnd());
        }

        CompletableFuture<UsdtBuyOrder> totalFuture = CompletableFuture.supplyAsync(() -> baseMapper.selectOne(queryWrapper));
        Page<UsdtBuyOrder> finalPage = page;
        CompletableFuture<Page<UsdtBuyOrder>> resultFuture = CompletableFuture.supplyAsync(() -> baseMapper.selectPage(finalPage, lambdaQuery.getWrapper()));
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(totalFuture, resultFuture);
        allFutures.get();
        page = resultFuture.get();
        UsdtBuyOrder totalResult = totalFuture.get();
        JSONObject extend = new JSONObject();
        extend.put("usdtNumTotal", totalResult.getUsdtNumTotal().setScale(2, RoundingMode.HALF_UP));
        extend.put("arbNumTotal", totalResult.getArbNumTotal().setScale(2, RoundingMode.HALF_UP));
        baseMapper.selectPage(page, lambdaQuery.getWrapper());
        List<UsdtBuyOrder> records = page.getRecords();
        List<UsdtBuyOrderDTO> list = walletMapStruct.usdtBuyOrderTransform(records);
        BigDecimal usdtNumPageTotal = new BigDecimal(0);
        BigDecimal arbNumPageTotal = new BigDecimal(0);
        for (UsdtBuyOrderDTO usdtBuyOrderDTO : list) {
            BigDecimal usdtNum = usdtBuyOrderDTO.getUsdtNum() == null ? new BigDecimal(0) : usdtBuyOrderDTO.getUsdtNum();
            BigDecimal arbNum = usdtBuyOrderDTO.getArbNum() == null ? new BigDecimal(0) : usdtBuyOrderDTO.getArbNum();
            usdtNumPageTotal = usdtNumPageTotal.add(usdtNum);
            arbNumPageTotal = arbNumPageTotal.add(arbNum);
        }
        extend.put("usdtNumPageTotal", usdtNumPageTotal.setScale(2, RoundingMode.HALF_UP));
        extend.put("arbNumPageTotal", arbNumPageTotal.setScale(2, RoundingMode.HALF_UP));
        return PageUtils.flush(page, list, extend);
    }

    /**
     * 根据订单号获取买入订单
     *
     * @param platformOrder
     * @return {@link UsdtBuyOrder}
     */
    @Override
    public UsdtBuyOrder getUsdtBuyOrderByPlatformOrder(String platformOrder) {
        return lambdaQuery().eq(UsdtBuyOrder::getPlatformOrder, platformOrder).one();
    }

    /**
     * 获取USDT买入页面数据
     *
     * @return {@link RestResult}<{@link UsdtBuyPageDataVo}>
     */
    @Override
    public RestResult<UsdtBuyPageDataVo> getUsdtBuyPageData() {

        //获取当前会员信息
        MemberInfo memberInfo = memberInfoService.getMemberInfo();

        if (memberInfo == null) {
            log.error("获取USDT买入页面数据失败: 获取会员信息失败");
            return RestResult.failure(ResultCode.RELOGIN);
        }

        UsdtBuyPageDataVo usdtBuyPageDataVo = new UsdtBuyPageDataVo();
        //主网络列表
        usdtBuyPageDataVo.setNetworkProtocolList(usdtConfigService.getNetworkProtocol());
        //usdt汇率
        usdtBuyPageDataVo.setUsdtCurrency(tradeConfigService.getById(1).getUsdtCurrency());
        //分页查询 USDT 买入记录
        usdtBuyPageDataVo.setUsdtBuyOrder(findPagedUsdtPurchaseRecords(String.valueOf(memberInfo.getId())));

        log.info("获取USDT买入页面数据成功  会员账号: {}, 返回数据: {}", memberInfo.getId(), usdtBuyPageDataVo);

        return RestResult.ok(usdtBuyPageDataVo);

    }

    /**
     * USDT完成转账处理
     *
     * @param platformOrder
     * @param voucherImage
     * @return {@link RestResult}
     */
    @Override
    public RestResult usdtBuyCompleted(String platformOrder, String voucherImage) {


        if (!FileUtil.isValidImageExtension(voucherImage)) {
            // 如果有文件不符合规茨，则返回错误
            log.error("USDT完成转账处理失败: 会员上传图片文件不符合规范 直接驳回, 订单号: {}, 文件名: {}", platformOrder, voucherImage);
            return RestResult.failure(ResultCode.FILE_UPLOAD_REQUIRED);
        }

        //分布式锁key ar-wallet-usdtBuyCompleted+订单号
        String key = "ar-wallet-usdtBuyCompleted" + platformOrder;
        RLock lock = redissonUtil.getLock(key);

        boolean req = false;

        try {
            req = lock.tryLock(10, TimeUnit.SECONDS);

            if (req) {

                //获取当前会员信息
                MemberInfo memberInfo = memberInfoService.getMemberInfo();

                if (memberInfo == null) {
                    log.error("USDT完成转账处理失败: 获取会员信息失败, 订单号: {}", platformOrder);
                    return RestResult.failure(ResultCode.RELOGIN);
                }

                //获取USDT订单
                UsdtBuyOrder usdtBuyOrder = getUsdtBuyOrderByPlatformOrder(platformOrder);

                String memberId = String.valueOf(memberInfo.getId());

                //校验该笔订单是否属于该会员
                if (usdtBuyOrder == null || !usdtBuyOrder.getMemberId().equals(memberId)) {
                    log.error("USDT完成转账处理失败, 该订单不存在或不属于当前会员, 会员信息: {}, 订单号: {}, USDT订单信息: {}", memberInfo, platformOrder, usdtBuyOrder);
                    return RestResult.failure(ResultCode.ORDER_VERIFICATION_FAILED);
                }

                //校验订单状态
                if (!usdtBuyOrder.getStatus().equals(OrderStatusEnum.BE_PAID.getCode())){
                    log.error("USDT完成转账处理失败, 订单状态校验失败: 当前订单状态为: {}, 会员信息: {}, 订单号: {}, USDT订单信息: {}", usdtBuyOrder.getStatus(), memberInfo, platformOrder, usdtBuyOrder);
                    return RestResult.failure(ResultCode.ORDER_EXPIRED);
                }

                //文件校验
//                RestResult validateFile = FileUtil.validateFile(voucherImage, arProperty.getMaxImageFileSize(), "image");
//                if (validateFile != null) {
//                    log.error("USDT完成转账处理: 非法操作: 订单校验失败: {}, platformOrder: {}", validateFile, platformOrder);
//                    return validateFile;
//                }

                //调用阿里云存储服务 将图片上传上去 并获取到文件名
//                String fileName = ossService.uploadFile(voucherImage);
//                if (fileName == null) {
//                    log.error("USDT完成转账处理 阿里云上传文件失败, 会员信息: {}, 订单号: {}", memberInfo, platformOrder);
//                    return RestResult.failure(ResultCode.FILE_UPLOAD_FAILED);
//                }

                //更新USDT订单信息
                boolean update = lambdaUpdate()
                        .eq(UsdtBuyOrder::getPlatformOrder, platformOrder)
                        .set(UsdtBuyOrder::getUsdtProof, baseUrl + voucherImage)//更新usdt支付凭证
                        .set(UsdtBuyOrder::getStatus, OrderStatusEnum.CONFIRMATION.getCode())//更新USDT订单状态为确认中
                        .set(UsdtBuyOrder::getPaymentTime, LocalDateTime.now())//更新支付时间
                        .update();


                log.info("USDT完成转账处理成功 会员账号: {}, 订单信息: {}, sql执行结果: {}", memberInfo.getAppealCount(), usdtBuyOrder, update);

                return RestResult.ok();
            }
        } catch (Exception e) {
            log.error("USDT完成转账处理失败: 订单号: {}, e: {}", platformOrder, e);
        } finally {
            //释放锁
            if (req && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        log.error("USDT完成转账处理失败: 订单号: {}", platformOrder);
        return RestResult.failure(ResultCode.SYSTEM_EXECUTION_ERROR);
    }

    /**
     * 根据会员id 查看进行中的USDT订单数量
     *
     * @param memberId
     */
    @Override
    public UsdtBuyOrder countActiveUsdtBuyOrders(String memberId) {

        return lambdaQuery().in(
                        UsdtBuyOrder::getStatus,
                        OrderStatusEnum.BE_PAID.getCode(),//待支付
                        OrderStatusEnum.CONFIRMATION.getCode(),//确认中
                        OrderStatusEnum.CONFIRMATION_TIMEOUT.getCode(),//确认超时
                        OrderStatusEnum.COMPLAINT.getCode(),//申诉中
                        OrderStatusEnum.AMOUNT_ERROR.getCode())//金额错误
                .eq(UsdtBuyOrder::getMemberId, memberId).last("LIMIT 1").one();
    }

    /**
     * 获取会员待支付的USDT买入订单
     *
     * @param memberId
     * @return {@link UsdtBuyOrder}
     */
    @Override
    public UsdtBuyOrder getPendingUsdtBuyOrder(Long memberId) {
        return lambdaQuery().eq(UsdtBuyOrder::getMemberId, memberId).eq(UsdtBuyOrder::getStatus, OrderStatusEnum.BE_PAID.getCode()).one();
    }


    /**
     * 获取USDT买入订单详情
     *
     * @param platformOrderReq
     * @return {@link RestResult}<{@link UsdtPurchaseOrderDetailsVo}>
     */
    @Override
    public RestResult<UsdtPurchaseOrderDetailsVo> getUsdtPurchaseOrderDetails(PlatformOrderReq platformOrderReq) {


        //获取当前会员信息
        MemberInfo memberInfo = memberInfoService.getMemberInfo();

        if (memberInfo == null){
            log.error("获取USDT买入订单详情失败: 获取会员信息失败: {}", memberInfo);
            return RestResult.failure(ResultCode.RELOGIN);
        }

        //查询USDT订单
        UsdtBuyOrder usdtBuyOrderByPlatformOrder = getUsdtBuyOrderByPlatformOrder(platformOrderReq.getPlatformOrder());

        if (usdtBuyOrderByPlatformOrder == null){
            log.error("获取USDT买入订单详情失败: 获取订单信息失败: req: {}, 会员信息: {}", platformOrderReq, memberInfo);
            return RestResult.failure(ResultCode.ORDER_NOT_EXIST);
        }

        UsdtPurchaseOrderDetailsVo usdtPurchaseOrderDetailsVo = new UsdtPurchaseOrderDetailsVo();

        //赋值给vo
        BeanUtils.copyProperties(usdtBuyOrderByPlatformOrder, usdtPurchaseOrderDetailsVo);

        return RestResult.ok(usdtPurchaseOrderDetailsVo);
    }
}

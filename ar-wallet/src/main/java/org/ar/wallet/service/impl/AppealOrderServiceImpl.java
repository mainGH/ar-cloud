package org.ar.wallet.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.CredentialsProvider;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.vertx.core.json.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.ar.common.core.page.PageRequestHome;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.core.result.ResultCode;
import org.ar.common.mybatis.util.PageUtils;
import org.ar.common.pay.dto.AppealOrderDTO;
import org.ar.common.pay.dto.AppealOrderExportDTO;
import org.ar.common.pay.req.AppealOrderIdReq;
import org.ar.common.pay.req.AppealOrderPageListReq;
import org.ar.common.pay.req.MemberInfoCreditScoreReq;
import org.ar.common.web.exception.BizException;
import org.ar.common.web.utils.UserContext;
import org.ar.wallet.Enum.*;
import org.ar.wallet.entity.*;
import org.ar.wallet.mapper.*;
import org.ar.wallet.oss.OssService;
import org.ar.wallet.property.ArProperty;
import org.ar.wallet.req.PlatformOrderReq;
import org.ar.wallet.req.ViewTransactionHistoryReq;
import org.ar.wallet.service.IAppealOrderService;
import org.ar.wallet.service.ICollectionOrderService;
import org.ar.wallet.service.IMemberInfoService;
import org.ar.wallet.service.IPaymentOrderService;
import org.ar.wallet.util.AmountChangeUtil;
import org.ar.wallet.util.FileUtil;
import org.ar.wallet.vo.AppealDetailsVo;
import org.ar.wallet.vo.AppealOrderVo;
import org.ar.wallet.vo.ViewMyAppealVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.ar.wallet.Enum.CreditEventTypeEnum.*;

/**
 * @author
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AppealOrderServiceImpl extends ServiceImpl<AppealOrderMapper, AppealOrder> implements IAppealOrderService {

    @Value("${oss.accessKeyId}")
    String accessKeyId;
    @Value("${oss.accessKeySecret}")
    String accessKeySecret;
    @Value("${oss.endpoint}")
    String endpoint;
    @Resource
    AppealOrderMapper appealOrderMapper;
    @Resource
    MatchingOrderMapper matchingOrderMapper;
    @Resource
    PaymentOrderMapper paymentOrderMapper;
    @Resource
    CollectionOrderMapper collectionOrderMapper;

    //从nacos获取配置
    private final ArProperty arProperty;

    private final OssService ossService;
    private final ICollectionOrderService collectionOrderService;
    private final IPaymentOrderService paymentOrderService;
    private final AmountChangeUtil amountChangeUtil;


    @Autowired
    private IMemberInfoService memberInfoService;


    private final MemberAccountChangeMapper memberAccountChangeMapper;
    private final MemberInfoMapper memberInfoMapper;
    private static final String URL_HOST = "https://arb-pay.oss-ap-southeast-1.aliyuncs.com/";
    private static final String IMG_PREFIX = "image/";

    private static final String VIDEO_PREFIX = "video/";

    private static final String BUCKET_NAME = "arb-pay";


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean submitAppeal(MultipartFile[] files, MultipartFile videoUpload,
                                Integer appealType, String orderNo, String reason,
                                String mid,
                                String mAccount,
                                BigDecimal orderAmount,
                                String belongMerchantCode) throws FileNotFoundException {

        CredentialsProvider credentialsProvider = new DefaultCredentialProvider(accessKeyId, accessKeySecret);
        OSS ossClient = new OSSClientBuilder().build(endpoint, credentialsProvider);
        List<String> urlList = new ArrayList<>();
        List<CompletableFuture> comlist = new ArrayList<>();
        AppealOrder appealOrder = new AppealOrder();
        appealOrder.setMid(mid);
        appealOrder.setMAccount(mAccount);
        appealOrder.setAppealType(appealType);
        appealOrder.setAppealStatus(1);
        appealOrder.setOrderAmount(orderAmount);
        if(appealType.equals(1)){
            appealOrder.setWithdrawOrderNo(orderNo);
        }else {
            appealOrder.setRechargeOrderNo(orderNo);
        }
        appealOrder.setReason(reason);
        if (belongMerchantCode != null){
            appealOrder.setBelongMerchantCode(belongMerchantCode);
        }
        appealOrder.setCreateBy(mid);

        try {
            // 判断存储空间BUCKET_NAME是否存在。如果返回值为true，则存储空间存在，如果返回值为false，则存储空间不存在。
            boolean exists = ossClient.doesBucketExist(BUCKET_NAME);
            if(!exists){
                throw new BizException(ResultCode.BUCKET_NOT_EXIST);
            }
            for (int i= 0; i<files.length; i++) {
                String fileName = IMG_PREFIX + orderNo +"_"+ i + files[i].getOriginalFilename().substring(files[i].getOriginalFilename().lastIndexOf("."));
                String url = URL_HOST + fileName;
                log.info("图片地址->{}", url);
                urlList.add(url);
                int finalI = i;
                CompletableFuture<Void> f1 =  CompletableFuture.runAsync(()->{
                    try {
                        ossClient.putObject(BUCKET_NAME, fileName, files[finalI].getInputStream());
                    } catch (FileNotFoundException e) {
                        log.error("阿里云oss图片文件上传异常:" + e.getMessage());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                comlist.add(f1);

            }
            appealOrder.setPicInfo(JSON.toJSONString(urlList));
            CompletableFuture<Void> f2 = CompletableFuture.runAsync(()->{
                try {
                    // 视频上传
                    String videoFileName = VIDEO_PREFIX + orderNo + videoUpload.getOriginalFilename().substring(videoUpload.getOriginalFilename().lastIndexOf("."));
                    String videoUrl = URL_HOST + videoFileName;
                    appealOrder.setVideoUrl(videoUrl);
                    log.info("视频地址->{}", videoUrl);
                    ossClient.putObject(BUCKET_NAME, videoFileName, videoUpload.getInputStream());
                } catch (Exception e) {
                    log.error("阿里云oss视频文件上传异常:" + e.getMessage());
                }
            });
            comlist.add(f2);
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(comlist.toArray(new CompletableFuture[comlist.size()]));
            allFutures.join();
            appealOrderMapper.insert(appealOrder);
            // 更新订单状态为申诉中
            matchingOrderMapper.updateOrderStatusByOrderNo(orderNo, appealType, Integer.parseInt(OrderStatusEnum.COMPLAINT.getCode()));
        } catch (Exception oe) {
            log.error("OSSException: " + oe.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
        return Boolean.TRUE;
    }

    @Override
    public AppealOrderVo queryAppealOrder(String orderNo, Integer appealType) throws Exception{

        AppealOrderVo appealOrderVo = new AppealOrderVo();
        CompletableFuture<MatchingOrder> matchingOrderFuture = null;
        CompletableFuture<PaymentOrder> paymentFuture = null;
        CompletableFuture<CollectionOrder> collectionFuture = null;
        CompletableFuture<Void> allFutures = null;
        AppealOrder appealOrder = new AppealOrder();
        MatchingOrder matchingOrder = new MatchingOrder();
        PaymentOrder paymentOrder = null;
        CollectionOrder collectionOrder = null;

        // 查询申诉订单信息
        CompletableFuture<AppealOrder> f1 =  CompletableFuture.supplyAsync(()->{
            return appealOrderMapper.queryAppealOrderByOrderNo(orderNo, appealType);
        });

        // 查询匹配订单记录
        if(appealType.equals(1)){
            matchingOrderFuture = CompletableFuture.supplyAsync(()->{
                LambdaQueryWrapper<MatchingOrder> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(MatchingOrder::getPaymentPlatformOrder, orderNo);
                return matchingOrderMapper.selectOne(queryWrapper);
            });
            paymentFuture = CompletableFuture.supplyAsync(()->{
                LambdaQueryWrapper<PaymentOrder> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(PaymentOrder::getPlatformOrder, orderNo);
                return paymentOrderMapper.selectOne(queryWrapper);
            });
            allFutures = CompletableFuture.allOf(f1, paymentFuture, matchingOrderFuture);
            allFutures.get();
            appealOrder = f1.get();
            matchingOrder = matchingOrderFuture.get();
            paymentOrder = paymentFuture.get();

            appealOrderVo.setOrderNo(matchingOrder.getPaymentPlatformOrder());
            appealOrderVo.setOrderTime(paymentOrder.getCreateTime());
            appealOrderVo.setAmount(paymentOrder.getAmount());
            appealOrderVo.setPayTime(matchingOrder.getPaymentTime());

        }else {
            matchingOrderFuture = CompletableFuture.supplyAsync(()->{
                LambdaQueryWrapper<MatchingOrder> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(MatchingOrder::getCollectionPlatformOrder, orderNo);
                return matchingOrderMapper.selectOne(queryWrapper);
            });
            collectionFuture = CompletableFuture.supplyAsync(()->{
                LambdaQueryWrapper<CollectionOrder> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(CollectionOrder::getPlatformOrder, orderNo);
                return collectionOrderMapper.selectOne(queryWrapper);
            });
            allFutures = CompletableFuture.allOf(f1, collectionFuture, matchingOrderFuture);
            allFutures.get();
            appealOrder = f1.get();
            matchingOrder = matchingOrderFuture.get();
            collectionOrder = collectionFuture.get();

            appealOrderVo.setOrderNo(matchingOrder.getCollectionPlatformOrder());
            appealOrderVo.setOrderTime(collectionOrder.getCreateTime());
            appealOrderVo.setAmount(collectionOrder.getAmount());
        }

        appealOrderVo.setAppealTime(appealOrder.getCreateTime());
        appealOrderVo.setUpi(matchingOrder.getUpiId());
        appealOrderVo.setUtr(matchingOrder.getUtr());
        appealOrderVo.setReason(appealOrder.getReason());
        appealOrderVo.setPicInfo(appealOrder.getPicInfo());
        appealOrderVo.setVideoUrl(appealOrder.getVideoUrl());

        return appealOrderVo;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppealOrderDTO pay(AppealOrderIdReq req){
        String updateBy = UserContext.getCurrentUserName();
        AppealOrder appealOrde = new AppealOrder();
        appealOrde.setId(req.getId());
        appealOrde = baseMapper.selectById(appealOrde);
        if(appealOrde.getAppealStatus().equals(2) || appealOrde.getAppealStatus().equals(3)){
            throw new BizException(ResultCode.ORDER_STATUS_ERROR);
        }
        appealOrde.setAppealStatus(2);
        //  if(appealOrde.getAppealType().equals(AppealTypeEnum.WITHDRAW.getCode())){
        PaymentOrder paymentOrder = paymentOrderMapper.selectPaymentForUpdate(appealOrde.getWithdrawOrderNo());
        if(ObjectUtils.isEmpty(paymentOrder)){
            throw new BizException(ResultCode.WITHDRAW_ORDER_NOT_EXIST);
        }
        paymentOrder.setOrderStatus(OrderStatusEnum.SUCCESS.getCode());
        paymentOrderMapper.updateById(paymentOrder);
        //   }else if(appealOrde.getAppealType().equals(AppealTypeEnum.RECHARGE.getCode())){
        CollectionOrder collectionOrder = collectionOrderMapper.getOrderByOrderNo(appealOrde.getRechargeOrderNo());
        if(ObjectUtils.isEmpty(collectionOrder)){
            throw new BizException(ResultCode.RECHARGE_ORDER_NOT_EXIST);
        }
        collectionOrder.setOrderStatus(OrderStatusEnum.SUCCESS.getCode());
        collectionOrderMapper.updateById(collectionOrder);
        amountChangeUtil.insertMemberChangeAmountRecord(collectionOrder.getMemberId(),collectionOrder.getActualAmount(), ChangeModeEnum.ADD,"ARB",collectionOrder.getPlatformOrder(), MemberAccountChangeEnum.RECHARGE, updateBy);
        //   }
        baseMapper.updateById(appealOrde);
        AppealOrderDTO appealOrderDTO = new AppealOrderDTO();
        BeanUtils.copyProperties(appealOrde,appealOrderDTO);
        //CollectionOrder collectionOrder = collectionOrderMapper.getOrderByOrderNo(appealOrde.getRechargeOrderNo());
        // 变更会员信用分
        changeCreditScore(Boolean.TRUE, collectionOrder.getMemberId(), paymentOrder.getMemberId(), appealOrde);
        return appealOrderDTO;
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public  AppealOrderDTO nopay(AppealOrderIdReq req){
        String updateBy = UserContext.getCurrentUserName();
        AppealOrder appealOrde = new AppealOrder();
        appealOrde.setId(req.getId());
        appealOrde = baseMapper.selectById(appealOrde);
        if(appealOrde.getAppealStatus().equals(2) || appealOrde.getAppealStatus().equals(3)){
            throw new BizException(ResultCode.ORDER_STATUS_ERROR);
        }
        appealOrde.setAppealStatus(3);
        //   if(appealOrde.getAppealType().equals(AppealTypeEnum.WITHDRAW.getCode())){
        PaymentOrder paymentOrder = paymentOrderMapper.selectPaymentForUpdate(appealOrde.getWithdrawOrderNo());
        if(ObjectUtils.isEmpty(paymentOrder)){
            throw new BizException(ResultCode.WITHDRAW_ORDER_NOT_EXIST);
        }
        paymentOrder.setOrderStatus(OrderStatusEnum.FAIL.getCode());
        amountChangeUtil.insertMemberChangeAmountRecord(paymentOrder.getMemberId(),paymentOrder.getActualAmount(), ChangeModeEnum.ADD,"ARB",paymentOrder.getPlatformOrder(), MemberAccountChangeEnum.RECHARGE, updateBy);
        paymentOrderMapper.updateById(paymentOrder);
        CollectionOrder collectionOrder = collectionOrderMapper.getOrderByOrderNo(appealOrde.getRechargeOrderNo());
        if(ObjectUtils.isEmpty(collectionOrder)){
            throw new BizException(ResultCode.RECHARGE_ORDER_NOT_EXIST);
        }
        collectionOrder.setOrderStatus(OrderStatusEnum.BUY_FAILED.getCode());
        collectionOrderMapper.updateById(collectionOrder);
        this.baseMapper.deleteById(appealOrde);
//       }else if(appealOrde.getAppealType().equals(AppealTypeEnum.RECHARGE.getCode())){
//           CollectionOrder collectionOrder = collectionOrderMapper.getOrderByOrderNo(appealOrde.getRechargeOrderNo());
//           collectionOrder.setOrderStatus(OrderStatusEnum.BUY_FAILED.getCode());
//           collectionOrderMapper.updateById(collectionOrder);
//       }

        // 变更会员信用分
        changeCreditScore(Boolean.FALSE, collectionOrder.getMemberId(), paymentOrder.getMemberId(), appealOrde);

        AppealOrderDTO appealOrderDTO = new AppealOrderDTO();
        BeanUtils.copyProperties(appealOrde,appealOrderDTO);
        return appealOrderDTO;
    }



    @SneakyThrows
    @Override
    public PageReturn<AppealOrderDTO> listPage(AppealOrderPageListReq req) {
        Long actualAmountPageTotal = 0L;
        Long orderAmountPageTotal = 0L;
        Page<AppealOrder> page = new Page<>();
        page.setCurrent(req.getPageNo());
        page.setSize(req.getPageSize());
        LambdaQueryChainWrapper<AppealOrder> lambdaQuery = lambdaQuery();
        lambdaQuery.orderByDesc(AppealOrder::getCreateTime);
        LambdaQueryWrapper<AppealOrder> queryWrapper = new QueryWrapper<AppealOrder>()
                .select("IFNULL(sum(actual_amount),0) as actualAmountTotal, IFNULL(sum(order_amount),0) as orderAmountTotal").lambda();
        if (!com.alibaba.nacos.api.utils.StringUtils.isBlank(req.getMid())) {
            lambdaQuery.eq(AppealOrder::getMid, req.getMid());
            queryWrapper.eq(AppealOrder::getMid, req.getMid());
        }
        if (!com.alibaba.nacos.api.utils.StringUtils.isBlank(req.getRechargeOrderNo())) {
            lambdaQuery.eq(AppealOrder::getRechargeOrderNo, req.getRechargeOrderNo());
            queryWrapper.eq(AppealOrder::getRechargeOrderNo, req.getRechargeOrderNo());
        }

        if (!com.alibaba.nacos.api.utils.StringUtils.isBlank(req.getWithdrawOrderNo())) {
            lambdaQuery.eq(AppealOrder::getWithdrawOrderNo, req.getWithdrawOrderNo());
            queryWrapper.eq(AppealOrder::getWithdrawOrderNo, req.getWithdrawOrderNo());
        }

        if (!com.alibaba.nacos.api.utils.StringUtils.isBlank(req.getStatus())) {
            lambdaQuery.eq(AppealOrder::getAppealStatus, req.getStatus());
            queryWrapper.eq(AppealOrder::getAppealStatus, req.getStatus());
        }

        if (req.getCreateTimeStart() != null) {
            lambdaQuery.ge(AppealOrder::getCreateTime, req.getCreateTimeStart());
            queryWrapper.ge(AppealOrder::getCreateTime, req.getCreateTimeStart());
        }

        //--动态查询 结束时间
        if (req.getCreateTimeEnd()!= null) {
            lambdaQuery.le(AppealOrder::getCreateTime,  req.getCreateTimeEnd());
            queryWrapper.le(AppealOrder::getCreateTime,  req.getCreateTimeEnd());
        }
        CompletableFuture<AppealOrder> totalFuture = CompletableFuture.supplyAsync(() -> {
            return baseMapper.selectOne(queryWrapper);
        });

        Page<AppealOrder> finalPage = page;
        CompletableFuture<Page<AppealOrder>> resultFuture = CompletableFuture.supplyAsync(() -> {
            return baseMapper.selectPage(finalPage, lambdaQuery.getWrapper());
        });
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(totalFuture, resultFuture);
        allFutures.get();
        page = resultFuture.get();
        AppealOrder appealOrderTotal = totalFuture.get();
        JSONObject extend = new JSONObject();
        extend.put("actualAmountTotal", appealOrderTotal.getActualAmountTotal());
        extend.put("orderAmountTotal", appealOrderTotal.getOrderAmountTotal());
        //baseMapper.selectPage(page, lambdaQuery.getWrapper());
        List<AppealOrder> records = page.getRecords();
        List<AppealOrderDTO> list = new ArrayList<AppealOrderDTO>();
        for(AppealOrder appealOrder :records){
            orderAmountPageTotal += appealOrder.getOrderAmount().longValue();
            actualAmountPageTotal += appealOrder.getActualAmount().longValue();
            AppealOrderDTO appealOrderDTO = new AppealOrderDTO();
            BeanUtils.copyProperties(appealOrder,appealOrderDTO);
            list.add(appealOrderDTO);
        }
        extend.put("orderAmountPageTotal", orderAmountPageTotal);
        extend.put("actualAmountPageTotal", actualAmountPageTotal);
        // List<ApplyDistributedDTO> accountChangeVos = walletMapStruct.ApplyDistributedTransform(records);
        return PageUtils.flush(page, list, extend);
    }

    @Override
    public PageReturn<AppealOrderExportDTO> listPageExport(AppealOrderPageListReq req) {
        PageReturn<AppealOrderDTO> appealOrder = listPage(req);

        List<AppealOrderExportDTO> resultList = new ArrayList<>();
        for (AppealOrderDTO appealOrderDTO : appealOrder.getList()) {
            AppealOrderExportDTO appealOrderExportDto = new AppealOrderExportDTO();
            BeanUtils.copyProperties(appealOrderDTO, appealOrderExportDto);
            appealOrderExportDto.setAppealType(AppealTypeEnum.getNameByCode(appealOrderDTO.getAppealType().toString()));
            appealOrderExportDto.setAppealStatus(AppealStatusEnum.getNameByCode(appealOrderDTO.getAppealStatus().toString()));
            appealOrderExportDto.setOrderAmount(appealOrderDTO.getOrderAmount().toString());
            appealOrderExportDto.setActualAmount(appealOrderDTO.getActualAmount().toString());
            resultList.add(appealOrderExportDto);
        }
        Page<AppealOrderExportDTO> page = new Page<>();
        page.setCurrent(req.getPageNo());
        page.setSize(req.getPageSize());
        page.setTotal(appealOrder.getTotal());
        return PageUtils.flush(page, resultList);
    }


    /**
     * 申诉-文件处理
     *
     * @param images
     * @param video
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject saveFile(MultipartFile[] images, MultipartFile video) {

        JsonObject resJson = new JsonObject();

        MemberInfo memberInfo = memberInfoService.getMemberInfo();

        if (memberInfo == null) {
            log.error("提交申诉-图片文件处理失败: 获取会员信息失败");
            resJson.put("errMsg", "会员不存在");
            return resJson;
        }


        if (images.length < 1 || images.length > 3) {
            log.error("提交申诉-图片文件处理失败: 请上传最少1张最多3张证明图片, 用户上传的文件数: {}, 会员账号: {}", images.length, memberInfo.getMemberAccount());
            resJson.put("errMsg", "请上传最少1张最多3张证明图片");
            return resJson;
        }

        StringBuilder sb = new StringBuilder();

        for (MultipartFile image : images) {
            //图片文件校验
            RestResult validateFile = FileUtil.validateFile(image, arProperty.getMaxImageFileSize(), "image");
            if (validateFile != null) {
                log.error("提交申诉-图片文件处理失败: 失败原因:{}, 会员账号: {}", validateFile.getMsg(), memberInfo.getMemberAccount());
                resJson.put("errMsg", validateFile.getMsg());
                return resJson;
            }

            //调用阿里云存储服务 将图片上传上去 并获取到文件名
            String fileName = ossService.uploadFile(image);

            if (fileName == null) {
                log.error("提交申诉-图片文件处理失败: 图片文件上传阿里云失败, 会员账号: {}", memberInfo.getMemberAccount());
                resJson.put("errMsg", "上传失败");
                return resJson;
            }

            sb.append(fileName).append(",");
        }

        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1); // 删除最后的逗号
        }
        String appealImage = sb.toString();


        //视频文件校验
        String appealVideo = null;
        if (video != null) {
            RestResult validateFile = FileUtil.validateFile(video, arProperty.getMaxVideoFileSize(), "video");
            if (validateFile != null) {
                log.error("提交申诉-视频文件处理失败: 失败原因:{}, 会员账号: {}", validateFile.getMsg(), memberInfo.getMemberAccount());
                resJson.put("errMsg", validateFile.getMsg());
                return resJson;
            }

            //调用阿里云存储服务 将视频上传上去 并获取到文件名
            appealVideo = ossService.uploadFile(video);

            if (appealVideo == null) {
                log.error("提交申诉-视频文件处理失败: 视频文件上传阿里云失败, 会员账号: {}", memberInfo.getMemberAccount());
                resJson.put("errMsg", "上传失败");
                return resJson;
            }
        }

        resJson.put("appealImage", appealImage);
        resJson.put("appealVideo", appealVideo);

        return resJson;
    }

    /**
     * 根据买入订单号获取申诉订单
     *
     * @param platformOrder
     * @return {@link AppealOrder}
     */
    @Override
    public AppealOrder getAppealOrderByBuyOrderNo(String platformOrder) {
        return lambdaQuery().eq(AppealOrder::getRechargeOrderNo, platformOrder).one();
    }


    /**
     * 根据卖出订单号获取申诉订单
     *
     * @param platformOrder
     * @return {@link AppealOrder}
     */
    @Override
    public AppealOrder getAppealOrderBySellOrderNo(String platformOrder) {
        return lambdaQuery().eq(AppealOrder::getWithdrawOrderNo, platformOrder).last("LIMIT 1").one();
    }

    /**
     * 查看订单申诉详情
     *
     * @param platformOrderReq
     * @param type             1: 买入申诉  2: 卖出申诉
     * @return {@link RestResult}<{@link AppealDetailsVo}>
     */
    @Override
    public RestResult<AppealDetailsVo> viewAppealDetails(PlatformOrderReq platformOrderReq, String type) {

        //获取当前会员信息
        MemberInfo memberInfo = memberInfoService.getMemberInfo();

        if (memberInfo == null) {
            log.error("查看订单申诉详情失败: 获取会员信息失败");
            return RestResult.failure(ResultCode.RELOGIN);
        }

        //查询申诉订单
        AppealOrder appealOrderByOrderNo = null;

        if (type.equals("1")) {
            //买入申诉订单
            appealOrderByOrderNo = getAppealOrderByBuyOrderNo(platformOrderReq.getPlatformOrder());
        } else if (type.equals("2")) {
            //卖出申诉订单
            appealOrderByOrderNo = getAppealOrderBySellOrderNo(platformOrderReq.getPlatformOrder());
        }

        if (appealOrderByOrderNo == null) {
            log.error("查看订单申诉详情失败, 申诉订单不存在 会员信息: {}, 订单信息: {}", memberInfo, appealOrderByOrderNo);
            return RestResult.failure(ResultCode.ORDER_VERIFICATION_FAILED);
        }

        //返回对象
        AppealDetailsVo appealDetailsVo = new AppealDetailsVo();

        String memberId = String.valueOf(memberInfo.getId());

        //查看订单是买入订单还是卖出订单
        if (platformOrderReq.getPlatformOrder().startsWith("MR")) {
            //查询买入订单
            CollectionOrder collectionOrder = collectionOrderService.getCollectionOrderByPlatformOrder(platformOrderReq.getPlatformOrder());


            if (collectionOrder == null || !collectionOrder.getMemberId().equals(memberId)) {
                log.error("查看订单申诉详情失败, 买入订单不存在或该订单不属于该会员, 会员信息: {}, 买入订单信息: {}", memberInfo, collectionOrder);
                return RestResult.failure(ResultCode.ORDER_VERIFICATION_FAILED);
            }

            //将买入订单信息赋值给返回对象
            BeanUtils.copyProperties(collectionOrder, appealDetailsVo);

        } else if (platformOrderReq.getPlatformOrder().startsWith("MC")) {
            //查询卖出订单
            PaymentOrder paymentOrder = paymentOrderService.getPaymentOrderByOrderNo(platformOrderReq.getPlatformOrder());

            if (paymentOrder == null || !paymentOrder.getMemberId().equals(memberId)) {
                log.error("查看订单申诉详情失败, 卖出订单不存在或该订单不属于该会员, 会员信息: {}, 卖出订单信息: {}", memberInfo, paymentOrder);
                return RestResult.failure(ResultCode.ORDER_VERIFICATION_FAILED);
            }

            //将卖出订单信息赋值给返回对象
            BeanUtils.copyProperties(paymentOrder, appealDetailsVo);

        } else {
            return RestResult.failure(ResultCode.ORDER_NUMBER_ERROR);
        }

        appealDetailsVo.setReason(appealOrderByOrderNo.getReason());
        appealDetailsVo.setPicInfo(appealOrderByOrderNo.getPicInfo());
        appealDetailsVo.setVideoUrl(appealOrderByOrderNo.getVideoUrl());

        log.info("查看订单申诉详情 处理成功 会员账号: {}, 返回数据: {}", memberInfo.getMemberAccount(), appealDetailsVo);

        return RestResult.ok(appealDetailsVo);
    }

    /**
     * 我的申诉
     *
     * @param req
     * @return {@link RestResult}<{@link PageReturn}<{@link ViewMyAppealVo}>>
     */
    @Override
    public RestResult<PageReturn<ViewMyAppealVo>> viewMyAppeal(PageRequestHome req) {

        //获取当前会员信息
        MemberInfo memberInfo = memberInfoService.getMemberInfo();

        if (memberInfo == null) {
            log.error("查看-我的申诉失败: 获取会员信息失败");
            return RestResult.failure(ResultCode.RELOGIN);
        }

        String memberId = String.valueOf(memberInfo.getId());

        if (req == null) {
            req = new ViewTransactionHistoryReq();
        }

        Page<AppealOrder> pageAppealOrder = new Page<>();
        pageAppealOrder.setCurrent(req.getPageNo());
        pageAppealOrder.setSize(req.getPageSize());

        LambdaQueryChainWrapper<AppealOrder> lambdaQuery = lambdaQuery();

        //查询当前会员的申诉订单或是被申诉订单
        lambdaQuery.eq(AppealOrder::getMid, memberId).or().eq(AppealOrder::getAppealedMemberId, memberId);

        // 倒序排序
        lambdaQuery.orderByDesc(AppealOrder::getId);

        baseMapper.selectPage(pageAppealOrder, lambdaQuery.getWrapper());

        List<AppealOrder> records = pageAppealOrder.getRecords();

        ArrayList<ViewMyAppealVo> viewMyAppealVoList = new ArrayList<>();

        //IPage＜实体＞转 IPage＜Vo＞
        for (AppealOrder appealOrder : records) {

            //返回数据
            ViewMyAppealVo viewMyAppealVo = new ViewMyAppealVo();
            BeanUtils.copyProperties(appealOrder, viewMyAppealVo);

            //判断该笔订单是买入还是卖出
            if (appealOrder.getAppealType() == 2) {
                //买入订单
                //查看申诉id是不是自己的
                if (appealOrder.getMid().equals(memberId)) {
                    //申诉id是自己的 那么显示买入订单号
                    viewMyAppealVo.setPlatformOrder(appealOrder.getRechargeOrderNo());
                } else {
                    //申诉id不是自己的 显示卖出订单号
                    viewMyAppealVo.setPlatformOrder(appealOrder.getWithdrawOrderNo());
                }
            } else {
                //卖出订单
                //查看申诉id是不是自己的
                if (appealOrder.getMid().equals(memberId)) {
                    //申诉id是自己的 显示卖出订单号
                    viewMyAppealVo.setPlatformOrder(appealOrder.getWithdrawOrderNo());
                } else {
                    //申诉id不是自己的 显示买入订单号
                    viewMyAppealVo.setPlatformOrder(appealOrder.getRechargeOrderNo());
                }
            }

            // 调整申诉类型
            if (viewMyAppealVo.getPlatformOrder().startsWith("MC")) {
                viewMyAppealVo.setAppealType(1); // 卖出
            } else if (viewMyAppealVo.getPlatformOrder().startsWith("MR")) {
                viewMyAppealVo.setAppealType(2); // 买入
            }

            viewMyAppealVoList.add(viewMyAppealVo);
        }

        PageReturn<ViewMyAppealVo> flush = PageUtils.flush(pageAppealOrder, viewMyAppealVoList);

        log.info("查看-我的申诉成功: 会员账号: {}, req: {}, 返回数据: {}", memberInfo.getMemberAccount(), req, flush);

        return RestResult.ok(flush);
    }

    /**
     * 变更会员信用分
     *
     * @param orderSuccess
     * @param buyerId
     * @param sellerId
     * @param appealOrder
     */
    @Override
    public void changeCreditScore(boolean orderSuccess, String buyerId, String sellerId, AppealOrder appealOrder) {
        CreditEventTypeEnum buyerEventType = null;
        CreditEventTypeEnum sellerEventType = null;
        if (orderSuccess) {
            if (buyerId.equals(appealOrder.getMid())) {
                // 买家为申述方
                buyerEventType = APPEAL_SUCCESS;
                sellerEventType = BE_APPEAL_FAILED;
            } else if (sellerId.equals(appealOrder.getMid())) {
                // 卖家为申述方
                buyerEventType = BE_APPEAL_SUCCESS;
                sellerEventType = APPEAL_FAILED;
            }
        } else {
            if (buyerId.equals(appealOrder.getMid())) {
                // 买家为申述方
                buyerEventType = APPEAL_FAILED;
                sellerEventType = BE_APPEAL_SUCCESS;
            } else if (sellerId.equals(appealOrder.getMid())) {
                // 卖家为申述方
                buyerEventType = BE_APPEAL_FAILED;
                sellerEventType = APPEAL_SUCCESS;
            }
        }

        if (buyerEventType == null || sellerEventType == null) {
            log.error("更新信用分, 申述人非买家和买家, 不能更新信息分, 申诉人:{}, 申诉单Id:{}", appealOrder.getMid(), appealOrder.getId());
            return;
        }

        log.info("更新信用分, 根据申诉结果变更, 买方:{}, 卖方:{}", buyerId, sellerId);
        // 更新买家信用分
        memberInfoService.updateCreditScore(MemberInfoCreditScoreReq.builder().id(Long.valueOf(buyerId)).eventType(buyerEventType.getCode()).tradeType(1).build());
        memberInfoService.updateCreditScore(MemberInfoCreditScoreReq.builder().id(Long.valueOf(sellerId)).eventType(sellerEventType.getCode()).tradeType(2).build());
    }
}

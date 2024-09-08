package org.ar.manager.controller;

import cn.hutool.core.date.DateUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.constant.GlobalConstants;
import org.ar.common.core.constant.RedisConstants;
import org.ar.common.core.result.RestResult;
import org.ar.common.core.utils.CommonUtils;
import org.ar.common.core.utils.ExcelUtil;
import org.ar.common.core.utils.StringUtils;
import org.ar.common.pay.dto.*;
import org.ar.common.pay.req.*;
import org.ar.common.redis.util.RedisUtils;
import org.ar.common.web.utils.UserContext;
import org.ar.manager.annotation.SysLog;
import org.ar.manager.api.MerchantInfoClient;
import org.ar.manager.entity.BiPaymentOrder;
import org.ar.manager.entity.BiWithdrawOrderDaily;
import org.ar.manager.mapper.BiPaymentOrderMapper;
import org.ar.manager.mapper.BiWithdrawOrderDailyMapper;
import org.ar.manager.req.MerchantDailyReportReq;
import org.ar.manager.service.IMerchantInfoService;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(value = {"/api/v1/merchantInfoAdmin", "/merchantInfoAdmin"})
@Api(description = "商户控制器")
public class MerchantInfoController {


    private final PasswordEncoder passwordEncoder;
    private final MerchantInfoClient merchantInfoClient;
    private final RedisUtils redisUtils;
    private final BiPaymentOrderMapper biPaymentOrderMapper;
    private final BiWithdrawOrderDailyMapper biWithdrawOrderDailyMapper;
    private final IMerchantInfoService iMerchantInfoService;


    @PostMapping("/createMerchantInfo")
    @ApiOperation(value = "创建商户")
    @SysLog(title="商户控制器",content = "创建商户")
    public RestResult<MerchantInfoAddDTO> save(@RequestBody @ApiParam MerchantInfoAddReq req) {
        RestResult<MerchantInfoAddDTO> result = merchantInfoClient.createMerchantInfo(req);

        return result;
    }



    @PostMapping("/update")
    @ApiOperation(value = "更新商户信息")
    @SysLog(title="商户控制器",content = "更新商户信息")
    public RestResult<MerchantInfoAddDTO> update(@RequestBody @ApiParam MerchantInfoUpdateReq merchantInfoReq) {
        RestResult<MerchantInfoAddDTO> result =  merchantInfoClient.updateForAdmin(merchantInfoReq);
        return result;

    }



    @PostMapping("/updatePwd")
    @ApiOperation(value = "修改商户登录密码")
    @SysLog(title="商户控制器",content = "修改商户登录密码")
    public RestResult updatePwd(@RequestBody @ApiParam MerchantInfoPwdReq merchantInfoPwdReq) {
       RestResult result =  merchantInfoClient.updateMerchantPwd(merchantInfoPwdReq);
       return result;
    }

    @PostMapping("/updateUsdtAddress")
    @ApiOperation(value = "修改商户提现usdt地址")
    @SysLog(title="商户控制器",content = "修改商户提现usdt地址")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "usdtAddress", value = "商户提现usdt地址", required = true, dataType = "String"),
            @ApiImplicitParam(name = "id", value = "商户id", required = true, dataType = "Long"),
    })
    public RestResult updateUsdtAddress(@RequestParam(value = "usdtAddress") String usdtAddress, @RequestParam(value = "id") Long id) {


        return merchantInfoClient.updateUsdtAddress(id, usdtAddress);
    }


    @PostMapping("/updateMerchantPublicKey")
    @ApiOperation(value = "修改商户公钥")
    @SysLog(title="商户控制器",content = "修改商户公钥")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "merchantPublicKey", value = "商户公钥", required = true, dataType = "String"),
            @ApiImplicitParam(name = "id", value = "商户id", required = true, dataType = "Long"),
    })
    public RestResult updateMerchantPublicKey(@RequestParam(value = "merchantPublicKey") String merchantPublicKey, @RequestParam(value = "id") Long id) {
        return merchantInfoClient.updateMerchantPublicKey(id, merchantPublicKey);
    }

    @PostMapping("/listpage")
    @ApiOperation(value = "获取商户列表")
    public RestResult<List<MerchantInfoListPageDTO>> list(@RequestBody @ApiParam MerchantInfoListPageReq req) {
        RestResult<List<MerchantInfoListPageDTO>> result = merchantInfoClient.listPage(req);
        return result;
    }

    @PostMapping("/current")
    @ApiOperation(value = "获取当前商户信息")
    public RestResult<MerchantInfoDTO> currentMerchantInfo() {
        Long id =  UserContext.getCurrentUserId();
        RestResult<MerchantInfoDTO> result = merchantInfoClient.fetchMerchantInfo(id);
        return result;
    }


    /**
     * 商户后台手动下分
     * @param req
     * @return
     */
    @PostMapping("/merchantWithdraw")
    @ApiOperation(value = "商户后台手动下分")
    @SysLog(title="商户控制器",content = "商户后台手动下分")
    public RestResult merchantWithdraw(@Validated @RequestBody MerchantWithdrawReq req) {
        return merchantInfoClient.merchantWithdraw(req.getMerchantCode(), req.getAmount(), req.getCurrency(), req.getRemark());
    }



    @PostMapping("/delete")
    @ApiOperation(value = "删除")
    @SysLog(title="商户控制器",content = "删除")
    public RestResult delete(@RequestBody @ApiParam MerchantInfoDeleteReq req) {
        RestResult<MerchantInfoDTO> result =  merchantInfoClient.delete(req);
        return result;

    }

    @PostMapping("/getInfo")
    @ApiOperation(value = "商户详情")
    @SysLog(title="商户控制器",content = "商户详情")
    public RestResult<MerchantInfoDTO> getInfo(@RequestBody @ApiParam MerchantInfoGetInfoReq req) {
        RestResult<MerchantInfoDTO> result =  merchantInfoClient.getInfo(req);
        return result;

    }


    /**
     *
     * @param
     * @param
     * @return
     */
    @PostMapping("/applyRecharge")
    @ApiOperation(value = "手动上分")
    @SysLog(title="商户控制器",content = "手动上分")
    public RestResult<ApplyDistributedDTO> applyRecharge(@RequestBody ApplyDistributedReq req) {
        return merchantInfoClient.applyRecharge(req);
    }


    /**
     *
     * @return
     */
    @PostMapping("/applyWithdraw")
    @ApiOperation(value = "手动下发")
    @SysLog(title="商户控制器",content = "手动下发")
    public RestResult<ApplyDistributedDTO> applyWithdraw(@RequestBody ApplyDistributedReq req) {
        return merchantInfoClient.applyWithdraw(req);
    }


    /**
     * 获取商户首页信息
     * @return
     */
    @PostMapping("/homePage")
    @ApiOperation(value = "获取商户首页信息")
    public RestResult<MerchantFrontPageDTO> fetchHomePageInfo() throws Exception {
        Long merchantId = UserContext.getCurrentUserId();
        String name = UserContext.getCurrentUserName();
        RestResult<MerchantFrontPageDTO> result  = merchantInfoClient.fetchHomePageInfo(merchantId, name);

        return result;

    }


    /**
     * 获取商户首页信息
     * @return
     */
    @PostMapping("/overview")
    @ApiOperation(value = "总后台数据概览")
    public RestResult<MerchantFrontPageDTO> fetchOverviewInfo() throws Exception {

        String endDate = DateUtil.format(LocalDateTime.now(ZoneId.systemDefault()), GlobalConstants.DATE_FORMAT_DAY);
        String startDate = DateUtil.format(LocalDateTime.now(ZoneId.systemDefault()).minusDays(30), GlobalConstants.DATE_FORMAT_DAY);
        LambdaQueryWrapper<BiPaymentOrder> buyQuery = new LambdaQueryWrapper<>();
        buyQuery.ge(BiPaymentOrder::getDateTime, startDate);
        buyQuery.le(BiPaymentOrder::getDateTime, endDate);

        LambdaQueryWrapper<BiWithdrawOrderDaily> sellQuery = new LambdaQueryWrapper<>();
        sellQuery.ge(BiWithdrawOrderDaily::getDateTime, startDate);
        sellQuery.le(BiWithdrawOrderDaily::getDateTime, endDate);
        // 获取金额错误订单
        CompletableFuture<List<BiPaymentOrder>> buyFuture = CompletableFuture.supplyAsync(() -> {
            return biPaymentOrderMapper.selectList(buyQuery);
        });

        // 获取金额错误订单
        CompletableFuture<List<BiWithdrawOrderDaily>> sellFuture = CompletableFuture.supplyAsync(() -> {
            return biWithdrawOrderDailyMapper.selectList(sellQuery);
        });

        // 获取金额错误订单
        CompletableFuture<RestResult<MerchantFrontPageDTO>> resultFuture = CompletableFuture.supplyAsync(() -> {
            return merchantInfoClient.fetchOverviewInfo();
        });


        CompletableFuture<Void> allFutures = CompletableFuture.allOf(buyFuture, sellFuture, resultFuture);
        allFutures.get();

        List<BiPaymentOrder> biPaymentOrders = buyFuture.get();
        List<BiPaymentOrderDTO> biPaymentOrdersDTO = new ArrayList<>();
        List<BiWithdrawOrderDaily> biWithdrawOrderDailies = sellFuture.get();
        List<BiWithdrawOrderDailyDTO> biWithdrawOrderDailiesDTO = new ArrayList<>();

        for (BiPaymentOrder item : biPaymentOrders) {
            BiPaymentOrderDTO biPaymentOrderDTO = new BiPaymentOrderDTO();
            BeanUtils.copyProperties(item, biPaymentOrderDTO);
            if(item.getOrderNum() <= 0L){
                biPaymentOrderDTO.setSuccessRate(0d);
            }else {
                Double result = new BigDecimal(item.getSuccessOrderNum().toString())
                        .divide(new BigDecimal(item.getOrderNum().toString()), 2, RoundingMode.DOWN).doubleValue();
                biPaymentOrderDTO.setSuccessRate(result);
            }

            biPaymentOrdersDTO.add(biPaymentOrderDTO);
        }

        for (BiWithdrawOrderDaily item : biWithdrawOrderDailies) {
            BiWithdrawOrderDailyDTO biWithdrawOrderDailyDTO = new BiWithdrawOrderDailyDTO();
            BeanUtils.copyProperties(item, biWithdrawOrderDailyDTO);
            if(item.getOrderNum() <= 0L){
                biWithdrawOrderDailyDTO.setSuccessRate(0d);
            }else {
                double result = new BigDecimal(item.getSuccessOrderNum().toString())
                        .divide(new BigDecimal(item.getOrderNum().toString()), 2, RoundingMode.DOWN).doubleValue();
                biWithdrawOrderDailyDTO.setSuccessRate(result);
            }
            biWithdrawOrderDailiesDTO.add(biWithdrawOrderDailyDTO);
        }

        RestResult<MerchantFrontPageDTO> result  = resultFuture.get();
        result.getData().setBuyList(biPaymentOrdersDTO);
        result.getData().setSellList(biWithdrawOrderDailiesDTO);
        return result;

    }

    @PostMapping("/fetchWithdrawOrderInfo")
    @ApiOperation(value = "获取代付订单列表")
    public RestResult<List<WithdrawOrderDTO>> fetchWithdrawOrderInfo(@Validated @RequestBody WithdrawOrderReq withdrawOrderReq) {
        if(StringUtils.isEmpty(withdrawOrderReq.getMerchantCode())){
            Long merchantId = UserContext.getCurrentUserId();
            String merchantStr = (String) redisUtils.hget(RedisConstants.MERCHANT_INFO, merchantId.toString());
            String merchantCode = CommonUtils.getMerchantCode(merchantStr);
            withdrawOrderReq.setMerchantCode(merchantCode);
        }
        RestResult<List<WithdrawOrderDTO>> result = merchantInfoClient.fetchWithdrawOrderInfo(withdrawOrderReq);
        return result;
    }

    @PostMapping("/fetchWithdrawOrder")
    @ApiOperation(value = "获取总后台代付订单列表")
    public RestResult<List<WithdrawOrderDTO>> fetchWithdrawOrder(@Validated @RequestBody WithdrawOrderReq withdrawOrderReq) {
        RestResult<List<WithdrawOrderDTO>> result = merchantInfoClient.fetchWithdrawOrderInfo(withdrawOrderReq);
        return result;
    }

    @PostMapping("/fetchWithdrawOrderExport")
    @ApiOperation(value = "代付订单列表导出")
    public void fetchWithdrawOrderExport(HttpServletResponse response, @RequestBody @ApiParam WithdrawOrderReq matchingOrderReq) throws IOException {
        matchingOrderReq.setPageSize(GlobalConstants.BATCH_SIZE);
        RestResult<List<WithdrawOrderExportDTO>> result = merchantInfoClient.fetchWithdrawOrderInfoExport(matchingOrderReq);
        OutputStream outputStream = null;
        BufferedOutputStream bos = null;
        ExcelWriter excelWriter = null;
        Integer exportTotalSize = 0;
        try {
            outputStream = response.getOutputStream();
            exportTotalSize = result.getData().size();
            //必须放到循环外，否则会刷新流
            excelWriter = null;
            List<List<String>> head = null;
            bos = new BufferedOutputStream(outputStream);
            if (matchingOrderReq.getLang().equals("zh")) {
                ExcelUtil.setResponseHeader(response, "MerchantPayoutOrders");
                excelWriter = EasyExcel.write(bos, WithdrawOrderExportDTO.class).build();
                head = ExcelUtil.parseHead(WithdrawOrderExportDTO.class);
            } else {
                ExcelUtil.setResponseHeader(response, "MerchantPayoutOrders");
                excelWriter = EasyExcel.write(bos, WithdrawOrderExportDTO.class).build();
                head = ExcelUtil.parseHead(WithdrawOrderExportDTO.class);
            }

            WriteSheet testSheet = EasyExcel.writerSheet("sheet1")
                    .head(head)
                    .build();
            excelWriter.write(result.getData(), testSheet);
            long pageNo = 1;
            long totalSize = 0;
            // startTime <= time < endTime
            if (result.getTotal() > GlobalConstants.BATCH_SIZE && result.getTotal() % GlobalConstants.BATCH_SIZE > 0) {
                totalSize = (result.getTotal() / GlobalConstants.BATCH_SIZE) + 1;
            } else if (result.getTotal() > GlobalConstants.BATCH_SIZE && result.getTotal() % GlobalConstants.BATCH_SIZE <= 0) {
                totalSize = (result.getTotal() / GlobalConstants.BATCH_SIZE);
            }
            for (int i = 0; i < totalSize; i++) {
                pageNo++;
                matchingOrderReq.setPageNo(pageNo);
                matchingOrderReq.setPageSize(GlobalConstants.BATCH_SIZE);
                RestResult<List<WithdrawOrderExportDTO>> resultList = merchantInfoClient.fetchWithdrawOrderInfoExport(matchingOrderReq);
                exportTotalSize = exportTotalSize + resultList.getData().size();
                if(exportTotalSize > GlobalConstants.EXPORT_TOTAL_SIZE){
                    return;
                }
                WriteSheet testSheet1 = EasyExcel.writerSheet("sheet1")
                        .build();
                excelWriter.write(resultList.getData(), testSheet1);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            bos.flush();
            if (excelWriter != null) {
                excelWriter.finish();
            }
        }
    }



    @PostMapping("/confirmSuccess")
    @ApiOperation(value = "手动回调成功")
    @SysLog(title="商户控制器",content = "代付手动回调成功")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "记录id", required = true, dataType = "Long")
    })
    public RestResult<String> confirmSuccess(Long id) {

        RestResult<String> result = merchantInfoClient.confirmSuccess(id);
        return result;
    }

    @PostMapping("/orderStatus")
    @ApiOperation(value = "获取订单状态")
    public RestResult fetchOrderStatus() {

        RestResult<Map<Integer, String>> map = merchantInfoClient.fetchOrderStatus();
        return map;

    }

    @PostMapping("/orderCallbackStatus")
    @ApiOperation(value = "获取订单回调状态")
    public RestResult orderCallbackStatus() {

        RestResult<Map<Integer, String>> map = merchantInfoClient.orderCallbackStatus();
        return map;

    }

    @PostMapping("/rechargeConfirmSuccess")
    @ApiOperation(value = "代收手动回调成功")
    @SysLog(title="商户控制器",content = "代收手动回调成功")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "记录id", required = true, dataType = "Long")
    })
    public RestResult<Boolean> rechargeConfirmSuccess(Long id) {

        RestResult<Boolean> result = merchantInfoClient.rechargeConfirmSuccess(id);
        return result;
    }



    @PostMapping("/resetKey")
    @ApiOperation(value = "重置商户密钥")
    public RestResult resetKey(@RequestParam("code") String code) {
        return merchantInfoClient.resetKey(code);
    }

    @PostMapping("/resetMerchantGoogle")
    @ApiOperation(value = "重置商户谷歌密钥")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "merchantCode", value = "商户code", required = true, dataType = "String")
    })
    public RestResult resetMerchantGoogle(@RequestParam("merchantCode") String merchantCode) {
        return merchantInfoClient.resetMerchantGoogle(merchantCode);
    }


    /**
     *
     * @param
     * @param
     * @return
     */
    @PostMapping("/resetPassword")
    @SysLog(title="商户控制器",content = "重置密码")
    @ApiOperation(value = "重置密码")
    public RestResult resetPassword(@RequestParam("code") String code) {
        return merchantInfoClient.resetPassword(code);
    }

    @PostMapping("/fetchRechargeOrderInfo")
    @ApiOperation(value = "获取代收订单列表")
    public RestResult<List<RechargeOrderDTO>> fetchRechargeOrderInfo(@Validated @RequestBody RechargeOrderReq rechargeOrderReq) {
        if(StringUtils.isEmpty(rechargeOrderReq.getMerchantCode())){
            Long merchantId = UserContext.getCurrentUserId();
            String merchantStr = (String) redisUtils.hget(RedisConstants.MERCHANT_INFO, merchantId.toString());
            String merchantCode = CommonUtils.getMerchantCode(merchantStr);
            rechargeOrderReq.setMerchantCode(merchantCode);
        }
        RestResult<List<RechargeOrderDTO>> result = merchantInfoClient.fetchRechargeOrderInfo(rechargeOrderReq);
        return result;
    }

    @PostMapping("/fetchCollectionOrderInfo")
    @ApiOperation(value = "总后台获取代收订单列表")
    public RestResult<List<RechargeOrderDTO>> fetchCollectionOrderInfo(@Validated @RequestBody RechargeOrderReq rechargeOrderReq) {
        RestResult<List<RechargeOrderDTO>> result = merchantInfoClient.fetchRechargeOrderInfo(rechargeOrderReq);
        return result;
    }

    @PostMapping("/export")
    @ApiOperation(value = "代收订单列表导出")
    public void export(HttpServletResponse response, @RequestBody @ApiParam RechargeOrderReq matchingOrderReq) throws IOException {
        matchingOrderReq.setPageSize(GlobalConstants.BATCH_SIZE);
        RestResult<List<RechargeOrderExportDTO>> result = merchantInfoClient.fetchRechargeOrderInfoExport(matchingOrderReq);
        OutputStream outputStream = null;
        BufferedOutputStream bos = null;
        ExcelWriter excelWriter = null;
        Integer exportTotalSize = 0;
        try {
            outputStream = response.getOutputStream();
            exportTotalSize = result.getData().size();
            //必须放到循环外，否则会刷新流
            excelWriter = null;
            List<List<String>> head = null;
            bos = new BufferedOutputStream(outputStream);
            if (matchingOrderReq.getLang().equals("zh")) {
                ExcelUtil.setResponseHeader(response, "MerchantCollectionOrders");
                excelWriter = EasyExcel.write(bos, RechargeOrderExportDTO.class).build();
                head = ExcelUtil.parseHead(RechargeOrderExportDTO.class);
            } else {
                ExcelUtil.setResponseHeader(response, "MerchantCollectionOrders");
                excelWriter = EasyExcel.write(bos, RechargeOrderExportDTO.class).build();
                head = ExcelUtil.parseHead(RechargeOrderExportDTO.class);
            }

            WriteSheet testSheet = EasyExcel.writerSheet("sheet1")
                    .head(head)
                    .build();
            excelWriter.write(result.getData(), testSheet);
            long pageNo = 1;
            long totalSize = 0;
            // startTime <= time < endTime
            if (result.getTotal() > GlobalConstants.BATCH_SIZE && result.getTotal() % GlobalConstants.BATCH_SIZE > 0) {
                totalSize = (result.getTotal() / GlobalConstants.BATCH_SIZE) + 1;
            } else if (result.getTotal() > GlobalConstants.BATCH_SIZE && result.getTotal() % GlobalConstants.BATCH_SIZE <= 0) {
                totalSize = (result.getTotal() / GlobalConstants.BATCH_SIZE);
            }
            for (int i = 0; i < totalSize; i++) {
                pageNo++;
                matchingOrderReq.setPageNo(pageNo);
                matchingOrderReq.setPageSize(GlobalConstants.BATCH_SIZE);
                RestResult<List<RechargeOrderExportDTO>> resultList = merchantInfoClient.fetchRechargeOrderInfoExport(matchingOrderReq);
                exportTotalSize = exportTotalSize + resultList.getData().size();
                if(exportTotalSize > GlobalConstants.EXPORT_TOTAL_SIZE){
                    return;
                }
                WriteSheet testSheet1 = EasyExcel.writerSheet("sheet1")
                        .build();
                excelWriter.write(resultList.getData(), testSheet1);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            bos.flush();
            if (excelWriter != null) {
                excelWriter.finish();
            }
        }
    }


    @GetMapping("/getMerchantName")
    @ApiOperation(value = "获取商户名称")
    public RestResult getMerchantName() {

        RestResult<Map<Integer, String>> map = merchantInfoClient.getMerchantName();
        return map;

    }


    @GetMapping("/getCurrency")
    @ApiOperation(value = "获取币种列表")
    public RestResult getCurrency() {

        RestResult<Map<String, String>> map = merchantInfoClient.getCurrency();
        return map;

    }


    @PostMapping("/getOrderNumOverview")
    @ApiOperation(value = "获取订单数量的概览")
    public RestResult<OrderOverviewDTO> getOrderNumOverview() {
        return merchantInfoClient.getOrderNumOverview();
    }


    @PostMapping("/getMerchantOrderOverview")
    @ApiOperation(value = "获取代收代付订单统计")
    public RestResult<MerchantOrderOverviewDTO> getMerchantOrderOverview(@RequestBody @ApiParam MerchantDailyReportReq req) {
        return iMerchantInfoService.getMerchantOrderOverview(req);
    }

    @PostMapping("/todayOrderOverview")
    @ApiOperation(value = "获取今日订单统计")
    public RestResult<TodayOrderOverviewDTO> todayOrderOverview() {
        return merchantInfoClient.todayOrderOverview();
    }

}

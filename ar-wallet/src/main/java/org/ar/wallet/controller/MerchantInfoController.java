package org.ar.wallet.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.constant.GlobalConstants;
import org.ar.common.core.constant.RedisConstants;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.core.result.ResultCode;
import org.ar.common.core.utils.GoogleAuthenticatorUtil;
import org.ar.common.core.utils.StringUtils;
import org.ar.common.pay.dto.*;
import org.ar.common.pay.req.*;
import org.ar.common.redis.util.RedisUtils;
import org.ar.common.web.exception.BizException;
import org.ar.common.web.utils.UserContext;
import org.ar.wallet.Enum.AccountChangeEnum;
import org.ar.wallet.Enum.ChangeModeEnum;
import org.ar.wallet.Enum.CurrenceEnum;
import org.ar.wallet.Enum.DistributeddStatusEnum;
import org.ar.wallet.entity.ApplyDistributed;
import org.ar.wallet.entity.MerchantInfo;
import org.ar.wallet.mapper.ApplyDistributedMapper;
import org.ar.wallet.mapper.MerchantInfoMapper;
import org.ar.wallet.property.ArProperty;
import org.ar.wallet.service.IMerchantInfoService;
import org.ar.wallet.util.AmountChangeUtil;
import org.ar.wallet.util.OrderNumberGeneratorUtil;
import org.ar.wallet.util.SignUtil;
import org.ar.wallet.vo.MerchantNameListVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/**
 * @author Admin
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(value = {"/api/v1/merchantinfo", "/merchantinfo"})
@Api(description = "商户控制器")
@ApiIgnore
public class MerchantInfoController {

    private final IMerchantInfoService merchantInfoService;
    private final PasswordEncoder passwordEncoder;

    private final AmountChangeUtil amountChangeUtil;
    private final ApplyDistributedMapper applyDistributedMapper;
    private final RedisUtils redisUtils;
    private final OrderNumberGeneratorUtil orderNumberGenerator;
    private final MerchantInfoMapper merchantInfoMapper;
    private final ArProperty arProperty;

    @Value("${oss.baseUrl}")
    private String baseUrl;

    @PostMapping("/createMerchantInfo")
    @ApiOperation(value = "创建商户")
    public RestResult<MerchantInfoAddDTO> save(@RequestBody @ApiParam MerchantInfoAddReq req) {
        String passwd = passwordEncoder.encode(GlobalConstants.USER_DEFAULT_PASSWORD);

        MerchantInfo merchantInfoTmp = merchantInfoService.getMerchantInfoByCode(req.getCode(), req.getUsername());
        if(merchantInfoTmp!=null) return RestResult.failed("商户名或商户号重复");
        MerchantInfo merchantInfo = new MerchantInfo();
        BeanUtils.copyProperties(req, merchantInfo);
        merchantInfo.setPassword(passwd);
        merchantInfo.setStatus("1");
        merchantInfo.setDeleted("0");
        merchantInfo.setCurrency(CurrenceEnum.INDIA.getCode());
        merchantInfo.setPayRate(new BigDecimal(req.getPayRate()));
        merchantInfo.setTransferRate(new BigDecimal(req.getTransferRate()));
        merchantInfo.setCountry("印度");
        String googleSecretKey = GoogleAuthenticatorUtil.getSecretKey();
        merchantInfo.setGooglesecret(googleSecretKey);
        //设置平台公钥
        merchantInfo.setPlatformPublicKey(arProperty.getPublicKey());
        // 拼接icon地址
        String iconUrl = getIconUrl(merchantInfo.getIcon());
        merchantInfo.setIcon(iconUrl);
        merchantInfoService.save(merchantInfo);
        MerchantInfoAddDTO merchantInfoAddDTO = new MerchantInfoAddDTO();
        BeanUtils.copyProperties(req,merchantInfoAddDTO);
        merchantInfo = merchantInfoService.getMerchantInfoByCode(req.getCode());
        redisUtils.hset(RedisConstants.MERCHANT_INFO, merchantInfo.getId().toString(), merchantInfo.getCode() + ":" + merchantInfo.getUsername());
        return RestResult.ok(merchantInfoAddDTO);
    }




    @PostMapping("/update")
    @ApiOperation(value = "更新商户信息")
    public RestResult<MerchantInfoAddDTO> update(@RequestBody @ApiParam MerchantInfoUpdateReq merchantInfoReq) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (!merchantInfoReq.getId().equals(currentUserId)) return RestResult.ok(new MerchantInfoAddDTO());
        MerchantInfo merchantInfo = new MerchantInfo();
        BeanUtils.copyProperties(merchantInfoReq, merchantInfo);
        // 转换url
        String iconUrl = getIconUrl(merchantInfo.getIcon());
        merchantInfo.setIcon(iconUrl);
        boolean su = merchantInfoService.updateById(merchantInfo);
        MerchantInfoAddDTO merchantInfoAddDTO = new MerchantInfoAddDTO();
        BeanUtils.copyProperties(merchantInfo,merchantInfoAddDTO);
        return RestResult.ok(merchantInfoAddDTO);

    }

    private String getIconUrl(String icon){
        if (icon != null && !icon.startsWith("https://")) {
            // 如果不是以"http"开头，则进行拼接
            icon = baseUrl + icon;
        }
        return icon;
    }


    @PostMapping("/updateForAdmin")
    @ApiOperation(value = "更新商户信息")
    public RestResult<MerchantInfoAddDTO> updateForAdmin(@RequestBody @ApiParam MerchantInfoUpdateReq req) {


        MerchantInfo merchantInfo = new MerchantInfo();
        BeanUtils.copyProperties(req, merchantInfo);
        // 转换url
        String iconUrl = getIconUrl(merchantInfo.getIcon());
        merchantInfo.setIcon(iconUrl);
        boolean su = merchantInfoService.updateById(merchantInfo);
        MerchantInfoAddDTO merchantInfoDTO = new MerchantInfoAddDTO();
        BeanUtils.copyProperties(merchantInfo, merchantInfoDTO);
        return RestResult.ok(merchantInfoDTO);

    }


    @PostMapping("/updatePwd")
    @ApiOperation(value = "修改商户登录密码")
    public RestResult update(@RequestBody MerchantInfoPwdReq merchantInfoPwdReq) {


        if(!merchantInfoPwdReq.getNewPwd().equals(merchantInfoPwdReq.getConfirmNewPwd())){
            throw new BizException(ResultCode.MERCHANT_PASSWORDS_INCONSISTENT);
        }

        // 校验原始密码是否正确
        MerchantInfo merchantInfo =  merchantInfoService.userDetail(merchantInfoPwdReq.getId());
        boolean result = passwordEncoder.matches(merchantInfoPwdReq.getOriginalPwd(), merchantInfo.getPassword());
        if(!result){
            throw new BizException(ResultCode.MERCHANT_ORIGINAL_PASSWORDS_WRONG);
        }
        String newPwd = passwordEncoder.encode(merchantInfoPwdReq.getNewPwd());
        merchantInfoService.updateMerchantPwd(merchantInfoPwdReq.getId(), newPwd, merchantInfoPwdReq.getPwdTips());

        return RestResult.ok();

    }

    @SneakyThrows
    @PostMapping("/listpage")
    @ApiOperation(value = "获取商户列表")
    public RestResult<List<MerchantInfoListPageDTO>> list(@RequestBody @ApiParam MerchantInfoListPageReq req) {
        PageReturn<MerchantInfoListPageDTO> payConfigPage = merchantInfoService.listPage(req);
        return RestResult.page(payConfigPage);
    }

    @PostMapping("/current")
    @ApiOperation(value = "获取当前商户信息")
    public RestResult<MerchantInfoDTO> currentMerchantInfo(Long userId) {
        MerchantInfoDTO merchantInfoDTO = merchantInfoService.currentMerchantInfo(userId);
        return RestResult.ok(merchantInfoDTO);
    }

    @GetMapping("/merchantNameList")
    @ApiOperation(value = "获取商户名称列表")
    public RestResult merchantNameList() {
        List<MerchantNameListVo> payConfigPage = merchantInfoService.getMerchantNameList();
        return RestResult.ok(payConfigPage);
    }


    /**
     * 获取会员用户信息
     */
    @GetMapping("/merchant/username/{username}")
    @ApiOperation(value = "获取会员用户信息")
    public RestResult<UserAuthDTO> getMemberUserByUsername(@PathVariable String username) {
        log.info("获取member user info。。。");
        UserAuthDTO user = merchantInfoService.getByUsername(username);
        return RestResult.ok(user);
    }

    @PostMapping("/updateUsdtAddress")
    @ApiOperation(value = "修改商户USDT地址")
    public RestResult updateUsdtAddress(Long id, String usdtAddress) {

        merchantInfoService.updateUsdtAddress(id, usdtAddress);

        return RestResult.ok();

    }

    /**
     * 商户后台手动下分
     * @param merchantCode
     * @param amount
     * @param currency
     * @return
     */
    @PostMapping("/merchantWithdraw")
    @ApiOperation(value = "商户后台手动下分")
    public RestResult merchantWithdraw(String merchantCode, BigDecimal amount, String currency, String remark) {

        if(amount.longValue() <= 0L){
            throw new BizException(ResultCode.AMOUNT_ERROR);
        }
        String orderNo = orderNumberGenerator.generateOrderNo(AccountChangeEnum.WITHDRAW.getPrefix());
        MerchantInfo merchantInfo = merchantInfoService.getMerchantInfoByCode(merchantCode);
        if (merchantInfo.getBalance().compareTo(amount) < 0) {
            log.error("商户余额不足,商户ID->{}", merchantCode);
            throw new BizException(ResultCode.MERCHANT_OUTSTANDING_BALANCE);
        }
        if(StringUtils.isEmpty(merchantInfo.getUsdtAddress())){
            log.error("USDT地址为空,商户Code->{}", merchantCode);
            throw new BizException(ResultCode.USDT_EMPTY);
        }
        String time = DateUtil.format(LocalDateTime.now(ZoneId.systemDefault()), "yyyy-MM-dd");
        ApplyDistributed applyDistributed = new ApplyDistributed();
        applyDistributed.setOrderNo(orderNo);
        applyDistributed.setMerchantCode(merchantCode);
        applyDistributed.setUsername(merchantInfo.getUsername());
        applyDistributed.setUsdtAddr(merchantInfo.getUsdtAddress());
        applyDistributed.setAmount(amount);
        applyDistributed.setBalance(merchantInfo.getBalance());
        applyDistributed.setStatus(DistributeddStatusEnum.NOFISHED.getCode());
        applyDistributed.setRemark(remark);
        applyDistributed.setCurrence(currency);
        applyDistributedMapper.insert(applyDistributed);
        amountChangeUtil.insertChangeAmountRecord(merchantCode, amount, ChangeModeEnum.SUB, currency, orderNo,
                AccountChangeEnum.WITHDRAW, time, remark, "");

        return RestResult.ok();

    }

    /**
     * 获取商户首页信息
     * @param merchantId
     * @return
     */
    @PostMapping("/homePage")
    @ApiOperation(value = "获取商户首页信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "merchantId", value = "商户id", required = true, dataType = "Long"),
            @ApiImplicitParam(name = "name", value = "商户名称", required = true, dataType = "String")
    })
    public RestResult<MerchantFrontPageDTO> fetchHomePageInfo(Long merchantId, String name) throws Exception {

        MerchantFrontPageDTO merchantFrontPageVo  = merchantInfoService.fetchHomePageInfo(merchantId, name);

        return RestResult.ok(merchantFrontPageVo);

    }

    /**
     * 谷歌验证
     * @param totpCode
     * @return
     */
    @PostMapping("/validGoogle")
    @ApiOperation(value = "谷歌验证")
    public RestResult<Boolean> validGoogle(String totpCode) {

        Long id = UserContext.getCurrentUserId();
        MerchantInfo merchantInfo = merchantInfoMapper.selectById(id);
        boolean result = GoogleAuthenticatorUtil.checkCode(merchantInfo.getGooglesecret(),Long.parseLong(totpCode), System.currentTimeMillis());
        if(result){
            merchantInfoMapper.updateUserGoogelBindFlag(id, 1);
        }
        return RestResult.ok(result);


    }

    /**
     * 修改商户公钥
     * @param id
     * @param merchantPublicKey
     * @return
     */
    @PostMapping("/updateMerchantPublicKey")
    @ApiOperation(value = "修改商户公钥")
    public RestResult updateMerchantPublicKey(Long id, String merchantPublicKey) {

        merchantInfoMapper.updateMerchantPublicKey(id, merchantPublicKey);

        return RestResult.ok();

    }

    /**
     * 获取商户首页信息
     * @return
     */
    @PostMapping("/overview")
    @ApiOperation(value = "总后台数据概览")
    public RestResult<MerchantFrontPageDTO> fetchOverviewInfo() throws Exception {

        MerchantFrontPageDTO merchantFrontPageVo  = merchantInfoService.fetchHomePageInfo();

        return RestResult.ok(merchantFrontPageVo);

    }


    @PostMapping("/getInfo")
    @ApiOperation(value = "获取会员用户信息")
    public RestResult<MerchantInfoDTO> getInfo(@RequestBody  MerchantInfoGetInfoReq merchantInfoReq) {

       // UserAuthDTO user = merchantInfoService.getByUsername(username);
        MerchantInfo merchantInfo = new MerchantInfo();
        BeanUtils.copyProperties(merchantInfoReq,merchantInfo);
        merchantInfo = merchantInfoService.getById(merchantInfo);
        MerchantInfoDTO merchantInfoDTO = new MerchantInfoDTO();
        BeanUtils.copyProperties(merchantInfo,merchantInfoDTO);
        return RestResult.ok(merchantInfoDTO);
    }

    @PostMapping("/delete")
    @ApiOperation(value = "删除")
    public RestResult<MerchantInfoDTO> delete(@RequestBody  MerchantInfoDeleteReq merchantInfoReq) {

        // UserAuthDTO user = merchantInfoService.getByUsername(username);
        MerchantInfo merchantInfo = new MerchantInfo();
        BeanUtils.copyProperties(merchantInfoReq,merchantInfo);
        merchantInfoService.removeById(merchantInfo);
        redisUtils.hdel(RedisConstants.MERCHANT_INFO, merchantInfo.getId().toString());
        MerchantInfoDTO merchantInfoDTO = new MerchantInfoDTO();
        BeanUtils.copyProperties(merchantInfo,merchantInfoDTO);
        return RestResult.ok(merchantInfoDTO);
    }


    /**
     * 商户后台手动下分
     * @param merchantCode
     * @param amount
     * @param currency
     * @return
     */
    @PostMapping("/merchantRecharge")
    @ApiOperation(value = "商户后台手动上分")
    public RestResult merchantRecharge(String merchantCode, BigDecimal amount, String currency) {

        String orderNo = orderNumberGenerator.generateOrderNo(AccountChangeEnum.WITHDRAW.getPrefix());

        String time = DateUtil.format(LocalDateTime.now(ZoneId.systemDefault()), "yyyy-MM-dd");

        amountChangeUtil.insertChangeAmountRecord(merchantCode, amount, ChangeModeEnum.ADD, currency, orderNo,
                AccountChangeEnum.RECHARGE, time, "");

        return RestResult.ok();

    }


    @PostMapping("/fetchWithdrawOrderInfo")
    @ApiOperation(value = "获取代付订单列表")
    public RestResult<List<WithdrawOrderDTO>> fetchWithdrawOrderInfo(@Validated @RequestBody WithdrawOrderReq withdrawOrderReq) {

        PageReturn<WithdrawOrderDTO> result = merchantInfoService.fetchWithdrawOrderInfo(withdrawOrderReq);
        return RestResult.page(result);
    }

    @PostMapping("/fetchWithdrawOrderInfoExport")
    @ApiOperation(value = "获取代付订单列表")
    public RestResult<List<WithdrawOrderExportDTO>> fetchWithdrawOrderInfoExport(@Validated @RequestBody WithdrawOrderReq withdrawOrderReq) {
        PageReturn<WithdrawOrderExportDTO> result = merchantInfoService.fetchWithdrawOrderInfoExport(withdrawOrderReq);
        return RestResult.page(result);
    }


    @PostMapping("/confirmSuccess")
    @ApiOperation(value = "代付手动回调成功")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "记录id", required = true, dataType = "Long")
    })
    public RestResult<String> confirmSuccess(Long id) {

        Boolean result = merchantInfoService.confirmSuccess(id);
        return result ? RestResult.ok("回调成功") : RestResult.failed("回调失败");
    }


    /**
     * 商户后台手动下分
     * @param
     * @param
     * @param
     * @return
     */
    @PostMapping("/applyWithdraw")
    @ApiOperation(value = "手动下发")
    @Transactional
    public RestResult<ApplyDistributedDTO> applyWithdraw(@RequestBody @ApiParam ApplyDistributedReq req) {

        String orderNo = orderNumberGenerator.generateOrderNo(AccountChangeEnum.WITHDRAW.getPrefix());

        if(req.getAmount().longValue() <= 0L){
            throw new BizException(ResultCode.AMOUNT_ERROR);
        }
        MerchantInfo merchantInfo = merchantInfoService.getMerchantInfoByCode(req.getMerchantCode());
        if (merchantInfo.getBalance().compareTo(req.getAmount()) < 0) {
            log.error("商户余额不足,商户ID->{}", req.getMerchantCode());
            throw new BizException(ResultCode.MERCHANT_OUTSTANDING_BALANCE);
        }
        String time = DateUtil.format(LocalDateTime.now(ZoneId.systemDefault()), "yyyy-MM-dd");
        ApplyDistributed applyDistributed = new ApplyDistributed();
        applyDistributed.setOrderNo(orderNo);
        applyDistributed.setMerchantCode(req.getMerchantCode());
        applyDistributed.setUsername(merchantInfo.getUsername());
        applyDistributed.setUsdtAddr(merchantInfo.getUsdtAddress());
        applyDistributed.setAmount(req.getAmount());
        applyDistributed.setBalance(merchantInfo.getBalance());
        applyDistributed.setStatus(DistributeddStatusEnum.FINISHED.getCode());
        applyDistributed.setRemark(req.getRemark());
        applyDistributed.setCurrence(CurrenceEnum.INDIA.getCode());
        applyDistributedMapper.insert(applyDistributed);
        amountChangeUtil.insertChangeAmountRecord(req.getMerchantCode(), req.getAmount(), ChangeModeEnum.SUB, CurrenceEnum.INDIA.getCode(), orderNo,
                AccountChangeEnum.WITHDRAW, time, req.getRemark(), "");

        ApplyDistributedDTO applyDistributedDTO = new ApplyDistributedDTO();
        BeanUtil.copyProperties(applyDistributed, applyDistributedDTO);

        return RestResult.ok(applyDistributedDTO);

    }



    /**
     * 商户后台手动下分
     * @param
     * @param
     * @param
     * @return
     */
    @PostMapping("/applyRecharge")
    @ApiOperation(value = "手动上分")
    @Transactional
    public RestResult<ApplyDistributedDTO> applyRecharge(@RequestBody @ApiParam ApplyDistributedReq req) {

        String orderNo = orderNumberGenerator.generateOrderNo(AccountChangeEnum.RECHARGE.getPrefix());
        MerchantInfo merchantInfo = merchantInfoService.getMerchantInfoByCode(req.getMerchantCode());
        if(req.getAmount().longValue() <= 0L){
            throw new BizException(ResultCode.AMOUNT_ERROR);
        }
        String time = DateUtil.format(LocalDateTime.now(ZoneId.systemDefault()), "yyyy-MM-dd");
        ApplyDistributed applyDistributed = new ApplyDistributed();
        applyDistributed.setOrderNo(orderNo);
        applyDistributed.setMerchantCode(req.getMerchantCode());
        applyDistributed.setUsername(merchantInfo.getUsername());
        applyDistributed.setUsdtAddr(merchantInfo.getUsdtAddress());
        applyDistributed.setAmount(req.getAmount());
        applyDistributed.setBalance(merchantInfo.getBalance());
        applyDistributed.setStatus(DistributeddStatusEnum.FINISHED.getCode());
        applyDistributed.setRemark(req.getRemark());
        applyDistributed.setCurrence(CurrenceEnum.INDIA.getCode());
        //applyDistributedMapper.insert(applyDistributed);
        amountChangeUtil.insertChangeAmountRecord(req.getMerchantCode(), req.getAmount(), ChangeModeEnum.ADD, CurrenceEnum.INDIA.getCode(), orderNo,
                AccountChangeEnum.RECHARGE, time, req.getRemark(), "");
        ApplyDistributedDTO applyDistributedDTO = new ApplyDistributedDTO();
        BeanUtils.copyProperties(applyDistributed,applyDistributedDTO);


        return RestResult.ok(applyDistributedDTO);

    }


    @PostMapping("/resetPassword")
    @ApiOperation(value = "充值密码")
    public RestResult resetPassword(@RequestParam("code") String code) {
        MerchantInfo merchantInfo = merchantInfoService.userDetailByCode(code);
        if(merchantInfo==null) return RestResult.ok("商户不存在");
        String passwd = passwordEncoder.encode(merchantInfo.getCode()+GlobalConstants.USER_DEFAULT_PASSWORD);
        merchantInfo.setPassword(passwd);
        merchantInfoService.updateById(merchantInfo);
        return RestResult.ok(merchantInfo.getCode()+GlobalConstants.USER_DEFAULT_PASSWORD);

    }


    @PostMapping("/resetKey")
    @ApiOperation(value = "重置商家密钥")
    public RestResult resetKey(@RequestParam("code") String code) {
        MerchantInfo merchantInfo = merchantInfoService.userDetailByCode(code);
        if(merchantInfo==null) return RestResult.ok("商户不存在");
        merchantInfo.setMd5Key(SignUtil.generateMd5Key());
        merchantInfoService.updateById(merchantInfo);
        return RestResult.ok(merchantInfo.getMd5Key());

    }


    @PostMapping("/resetMerchantGoogle")
    @ApiOperation(value = "重置商家谷歌密钥")
    public RestResult resetMerchantGoogle(@RequestParam("merchantCode") String merchantCode) {
        MerchantInfo merchantInfo = merchantInfoService.userDetailByCode(merchantCode);
        if(merchantInfo==null) return RestResult.ok("商户不存在");
        String newGoogleSecretKey = GoogleAuthenticatorUtil.getSecretKey();
        merchantInfoMapper.updateMerchantGoogleSecretKey(merchantCode, newGoogleSecretKey, 0);
        return RestResult.ok();

    }



    @PostMapping("/orderStatus")
    @ApiOperation(value = "获取订单状态")
    public RestResult fetchOrderStatus() {

        Map<Integer, String> map = merchantInfoService.fetchOrderStatus();
        return RestResult.ok(map);

    }

    @PostMapping("/orderCallbackStatus")
    @ApiOperation(value = "获取订单回调状态")
    public RestResult orderCallbackStatus() {

        Map<Integer, String> map = merchantInfoService.orderCallbackStatus();
        return RestResult.ok(map);

    }

    @PostMapping("/fetchRechargeOrderInfo")
    @ApiOperation(value = "获取代收订单列表")
    public RestResult<List<RechargeOrderDTO>> fetchRechargeOrderInfo(@Validated @RequestBody RechargeOrderReq rechargeOrderReq) {

        PageReturn<RechargeOrderDTO> result = merchantInfoService.fetchRechargeOrderInfo(rechargeOrderReq);
        return RestResult.page(result);
    }

    @PostMapping("/fetchRechargeOrderInfoExport")
    @ApiOperation(value = "获取代收订单列表导出")
    public RestResult<List<RechargeOrderExportDTO>> fetchRechargeOrderInfoExport(@Validated @RequestBody RechargeOrderReq rechargeOrderReq) {
        PageReturn<RechargeOrderExportDTO> result = merchantInfoService.fetchRechargeOrderInfoExport(rechargeOrderReq);
        return RestResult.page(result);
    }

    @PostMapping("/getCurrency")
    @ApiOperation(value = "获取币种列表")
    public RestResult<Map<String, String>> getCurrency() {

        Map<String, String> result = merchantInfoService.getCurrency();
        return RestResult.ok(result);
    }

    @PostMapping("/getMerchantName")
    @ApiOperation(value = "获取商户名称")
    public RestResult<Map<Long, String>> getMerchantName() {

        Map<Long, String> result = merchantInfoService.getMerchantName();
        return RestResult.ok(result);
    }

    @PostMapping("/rechargeConfirmSuccess")
    @ApiOperation(value = "代收手动回调成功")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "记录id", required = true, dataType = "Long")
    })
    public RestResult<Boolean> rechargeConfirmSuccess(Long id) {

        Boolean result = merchantInfoService.rechargeConfirmSuccess(id);
        return result ? RestResult.ok() : RestResult.failed();
    }

    @SneakyThrows
    @PostMapping("/getOrderNumOverview")
    @ApiOperation(value = "获取订单数量概览")
    public RestResult<OrderOverviewDTO> getOrderNumOverview() {
        OrderOverviewDTO result = merchantInfoService.getOrderNumOverview();
        return RestResult.ok(result);
    }

    @PostMapping("/todayOrderOverview")
    @ApiOperation(value = "获取今日概览信息")
    public RestResult<TodayOrderOverviewDTO> todayOrderOverview() {
        TodayOrderOverviewDTO result = merchantInfoService.todayOrderOverview();
        return RestResult.ok(result);
    }

    @PostMapping("/getLatestOrderTime")
    @ApiOperation(value = "获取最后一笔代收/代付订单发生时间")
    public List<MerchantLastOrderWarnDTO> getLatestOrderTime() {
        return merchantInfoService.getLatestOrderTime();
    }


}

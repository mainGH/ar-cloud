package org.ar.wallet.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.ObjectUtils;
import org.ar.common.core.constant.GlobalConstants;
import org.ar.common.core.constant.SecurityConstants;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.ResultCode;
import org.ar.common.core.utils.AssertUtil;
import org.ar.common.mybatis.util.PageUtils;
import org.ar.common.pay.dto.*;
import org.ar.common.pay.req.*;
import org.ar.common.redis.util.RedisUtils;
import org.ar.common.web.exception.BizException;
import org.ar.common.web.utils.UserContext;
import org.ar.wallet.Enum.*;
import org.ar.wallet.config.WalletMapStruct;
import org.ar.wallet.entity.*;
import org.ar.wallet.mapper.*;
import org.ar.wallet.service.AsyncNotifyService;
import org.ar.wallet.service.IMerchantInfoService;
import org.ar.wallet.service.ITradeConfigService;
import org.ar.wallet.util.DurationCalculatorUtil;
import org.ar.wallet.vo.MerchantNameListVo;
import org.ar.wallet.vo.OrderInfoVo;
import org.h2.util.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


/**
 * @author Admin
 */
@Service
@RequiredArgsConstructor
public class MerchantInfoServiceImpl extends ServiceImpl<MerchantInfoMapper, MerchantInfo> implements IMerchantInfoService {
    private final PasswordEncoder passwordEncoder;

    private final WalletMapStruct walletMapStruct;
    private final PaymentOrderMapper paymentOrderMapper;
    private final CollectionOrderMapper collectionOrderMapper;
    private final RedisUtils redisUtils;
    private final ITradeConfigService iTradeConfigService;
    private final MerchantCollectOrdersMapper merchantCollectOrdersMapper;
    private final MerchantPaymentOrdersMapper merchantPaymentOrdersMapper;
    private final MatchingOrderMapper matchingOrderMapper;
    private final MemberInfoMapper memberInfoMapper;
    private final MatchPoolMapper matchPoolMapper;
    private final ITradeConfigService tradeConfigService;
    @Autowired
    AsyncNotifyService asyncNotifyService;

    @Override
    public PageReturn<MerchantInfoListPageDTO> listPage(MerchantInfoListPageReq req) throws ExecutionException, InterruptedException {
        Page<MerchantInfo> page = new Page<>();
        page.setCurrent(req.getPageNo());
        page.setSize(req.getPageSize());
        LambdaQueryChainWrapper<MerchantInfo> lambdaQuery = lambdaQuery();
        // 新增统计金额字段总计字段
        LambdaQueryWrapper<MerchantInfo> queryWrapper = new QueryWrapper<MerchantInfo>()
                .select("IFNULL(sum(balance), 0) as balanceTotal").lambda();
        lambdaQuery.orderByDesc(MerchantInfo::getCreateTime);
        if (!com.alibaba.nacos.api.utils.StringUtils.isBlank(req.getCode())) {
            lambdaQuery.eq(MerchantInfo::getCode, req.getCode());
            queryWrapper.eq(MerchantInfo::getCode, req.getCode());
        }
        if (!com.alibaba.nacos.api.utils.StringUtils.isBlank(req.getUsername())) {
            lambdaQuery.eq(MerchantInfo::getUsername, req.getUsername());
            queryWrapper.eq(MerchantInfo::getUsername, req.getUsername());
        }
        if (!com.alibaba.nacos.api.utils.StringUtils.isBlank(req.getMerchantType())) {
            lambdaQuery.eq(MerchantInfo::getMerchantType, req.getMerchantType());
            queryWrapper.eq(MerchantInfo::getMerchantType, req.getMerchantType());
        }
        if(org.apache.commons.lang3.StringUtils.isNotBlank(req.getStatus()) && (req.getStatus().equals("0"))){
            lambdaQuery.eq(MerchantInfo::getRechargeStatus, 0);
            queryWrapper.eq(MerchantInfo::getRechargeStatus, 0);
        }else if(org.apache.commons.lang3.StringUtils.isNotBlank(req.getStatus()) && (req.getStatus().equals("1"))){
            lambdaQuery.eq(MerchantInfo::getRechargeStatus, 1);
            queryWrapper.eq(MerchantInfo::getRechargeStatus, 1);
        }else if(org.apache.commons.lang3.StringUtils.isNotBlank(req.getStatus()) && (req.getStatus().equals("2"))){
            lambdaQuery.eq(MerchantInfo::getWithdrawalStatus, 0);
            queryWrapper.eq(MerchantInfo::getWithdrawalStatus, 0);
        }else if(org.apache.commons.lang3.StringUtils.isNotBlank(req.getStatus()) && (req.getStatus().equals("3"))){
            lambdaQuery.eq(MerchantInfo::getWithdrawalStatus, 1);
            queryWrapper.eq(MerchantInfo::getWithdrawalStatus, 1);
        }
        if (!com.alibaba.nacos.api.utils.StringUtils.isBlank(req.getRechargeStatus())) {
            lambdaQuery.eq(MerchantInfo::getRechargeStatus,req.getRechargeStatus());
            queryWrapper.eq(MerchantInfo::getRechargeStatus,req.getRechargeStatus());
        }
        if (!com.alibaba.nacos.api.utils.StringUtils.isBlank(req.getWithdrawalStatus())) {
            lambdaQuery.eq(MerchantInfo::getWithdrawalStatus,req.getWithdrawalStatus());
            queryWrapper.eq(MerchantInfo::getWithdrawalStatus,req.getWithdrawalStatus());
        }
        // 获取阈值
        TradeConfig tradeConfig = tradeConfigService.getById(1);
        BigDecimal warningBalance = tradeConfig.getWarningBalance();

        if (!com.alibaba.nacos.api.utils.StringUtils.isBlank(req.getRiskTag())) {
            if (Objects.equals(req.getRiskTag(), RiskTagEnum.INSUFFICIENT_BALANCE.getCode())) {
                lambdaQuery.le(MerchantInfo::getBalance, warningBalance);
                queryWrapper.le(MerchantInfo::getBalance, warningBalance);
            } else if (Objects.equals(req.getRiskTag(), RiskTagEnum.Normal.getCode())){
                lambdaQuery.ge(MerchantInfo::getBalance, warningBalance);
                queryWrapper.ge(MerchantInfo::getBalance, warningBalance);
            }else{
                lambdaQuery.eq(MerchantInfo::getId, -1);
                queryWrapper.eq(MerchantInfo::getId, -1);
            }
        }
        CompletableFuture<Page<MerchantInfo>> merchantListFuture = CompletableFuture.supplyAsync(() -> {
            return baseMapper.selectPage(page, lambdaQuery.getWrapper());
        });

        CompletableFuture<MerchantInfo> merchantTotalFuture = CompletableFuture.supplyAsync(() -> {
            return baseMapper.selectOne(queryWrapper);
        });

        CompletableFuture<List<MerchantActivationInfoDTO>> merchantActivationInfo = CompletableFuture.supplyAsync(() -> {
            return memberInfoMapper.selectMerchantInfoList();
        });

        CompletableFuture<BigDecimal> memberBalanceTotal = CompletableFuture.supplyAsync(() -> {
            return memberInfoMapper.selectMemberTotalBalance();
        });

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(merchantListFuture, merchantActivationInfo, merchantTotalFuture, memberBalanceTotal);
        allFutures.get();
        MerchantInfo merchantTotalInfo = merchantTotalFuture.get();
        BigDecimal memberTotalInfo = memberBalanceTotal.get();
        JSONObject extent = new JSONObject();
        extent.put("balanceTotal", merchantTotalInfo.getBalanceTotal().toPlainString());
        extent.put("memberBalanceTotal", memberTotalInfo.toPlainString());
        List<MerchantInfo> records = merchantListFuture.get().getRecords();
        List<MerchantInfoListPageDTO>  list = walletMapStruct.merchantInfoListTransform(records);
        BigDecimal balancePageTotal = BigDecimal.ZERO;
        BigDecimal memberBalancePageTotal = BigDecimal.ZERO;
        for (MerchantInfoListPageDTO item : list) {
            item.setRiskTag(RiskTagEnum.Normal.getCode());
            if(item.getBalance().compareTo(warningBalance) <= 0){
                item.setRiskTag(RiskTagEnum.INSUFFICIENT_BALANCE.getCode());
            }
            for (MerchantActivationInfoDTO innerItem : merchantActivationInfo.get()) {
                if(item.getCode().equals(innerItem.getMerchantCode())){
                    item.setMemberTotalBalance(innerItem.getBalance());
                    item.setMemberTotalNum(innerItem.getActivationTotalNum());
                }
            }
            memberBalancePageTotal = memberBalancePageTotal.add(item.getMemberTotalBalance());
            balancePageTotal = balancePageTotal.add(item.getBalance());
        }
        extent.put("balancePageTotal", balancePageTotal.toPlainString());
        extent.put("memberBalancePageTotal", memberBalancePageTotal.toPlainString());
        return PageUtils.flush(page, list, extent);
    }

    @Override
    public List<MerchantInfo> getAllMerchantByStatus() {
        LambdaQueryChainWrapper<MerchantInfo> lambdaQuery = lambdaQuery();
        List<MerchantInfo> list = lambdaQuery().eq(MerchantInfo::getStatus, "1").list();
        return list;
    }


    @Override
    public String getMd5KeyByCode(String merchantCode) {
        QueryWrapper<MerchantInfo> MerchantInfoQueryWrapper = new QueryWrapper<>();
        MerchantInfoQueryWrapper.select("md5_key").eq("code", merchantCode);
        return getOne(MerchantInfoQueryWrapper).getMd5Key();
    }


    @Override
    public boolean getIp(String code, String addr) {
        MerchantInfo merchantInfo = lambdaQuery().eq(MerchantInfo::getCode, code).one();
        if (merchantInfo != null && !StringUtils.isNullOrEmpty(merchantInfo.getWhiteList())) {
            String whiteStr = merchantInfo.getWhiteList();
            List<String> list = Arrays.asList(",");
            if (list.contains(addr)) return true;
        }
        return false;
    }

    @Override
    public MerchantInfo getMerchantInfoByCode(String code) {
        MerchantInfo merchantInfo = lambdaQuery()
                .eq(MerchantInfo::getCode, code)
                .eq(MerchantInfo::getDeleted, 0)
                .one();
        return merchantInfo;
    }

    @Override
    public MerchantInfo getMerchantInfoByCode(String code, String name) {
        MerchantInfo merchantInfo = lambdaQuery().eq(MerchantInfo::getCode, code).or().eq(MerchantInfo::getUsername, name).one();
        return merchantInfo;
    }


    @Override
    public List<MerchantNameListVo> getMerchantNameList() {
        //获取当前商户id
        Long currentUserId = UserContext.getCurrentUserId();
        //查询当前商户名称和商户号
        QueryWrapper<MerchantInfo> merchantInfoQueryWrapper = new QueryWrapper<>();
        merchantInfoQueryWrapper.select("code", "username").eq("id", currentUserId);
        List<Map<String, Object>> maps = listMaps(merchantInfoQueryWrapper);
        ArrayList<MerchantNameListVo> merchantNameListVos = new ArrayList<>();

        for (Map<String, Object> map : maps) {
            MerchantNameListVo merchantNameListVo = new MerchantNameListVo();
            merchantNameListVo.setValue(String.valueOf(map.get("code")));
            merchantNameListVo.setLabel(String.valueOf(map.get("username")));
            merchantNameListVos.add(merchantNameListVo);
        }
        return merchantNameListVos;
    }


    @Override
    public MerchantInfoDTO currentMerchantInfo(Long userId) {

        TradeConfigListPageReq tradeConfigReq = new TradeConfigListPageReq();
        tradeConfigReq.setPageNo(1L);
        tradeConfigReq.setPageSize(10L);
        PageReturn<TradeConfigDTO> payConfigPage = iTradeConfigService.listPage(tradeConfigReq);
        List<TradeConfigDTO> list =  payConfigPage.getList();
        TradeConfigDTO tradeConfigDTO = list.get(0);
        MerchantInfo merchantInfo = userDetail(userId);
        AssertUtil.notEmpty(merchantInfo, ResultCode.USERNAME_OR_PASSWORD_ERROR);
        // 查询绑定的菜单

        MerchantInfoDTO merchantInfoDTO = new MerchantInfoDTO();
        BeanUtils.copyProperties(merchantInfo, merchantInfoDTO);
        merchantInfoDTO.setUsdtRate(tradeConfigDTO.getUsdtCurrency());

        return merchantInfoDTO;
    }


    @Override
    public MerchantInfo userDetail(Long userId) {
        MerchantInfo merchantInfo = lambdaQuery().eq(MerchantInfo::getId, userId).one();
        AssertUtil.notEmpty(merchantInfo, ResultCode.USERNAME_OR_PASSWORD_ERROR);

        // 查询绑定的角色IDs


        return merchantInfo;
    }


    @Override
    public UserAuthDTO getByUsername(String username) {
        String lastLoginIp = (String) redisUtils.hget(SecurityConstants.LOGIN_USER_NAME + username, SecurityConstants.LOGIN_LAST_LOGIN_IP);
        String lastLoginTime = (String) redisUtils.hget(SecurityConstants.LOGIN_USER_NAME + username, SecurityConstants.LOGIN_LAST_LOGIN_TIME);
        Integer loginCount = (Integer) redisUtils.hget(SecurityConstants.LOGIN_USER_NAME + username, SecurityConstants.LOGIN_COUNT);
        UserAuthDTO userAuthDTO = this.baseMapper.getByUserName(username);
        MerchantInfo merchantInfo = new MerchantInfo();
        if(!ObjectUtils.isEmpty(userAuthDTO)){
            merchantInfo.setId(userAuthDTO.getUserId());
            merchantInfo.setLastLoginTime(DateUtil.parseLocalDateTime(lastLoginTime, GlobalConstants.DATE_FORMAT));
            merchantInfo.setLoginIp(lastLoginIp);
            merchantInfo.setLogins(loginCount);
            this.baseMapper.updateById(merchantInfo);
        }
        return userAuthDTO;
    }

    /*
     * 根据商户号查询支付费率和代付费率
     * */
    @Override
    public Map<String, Object> getRateByCode(String merchantCode) {
        QueryWrapper<MerchantInfo> merchantInfoQueryWrapper = new QueryWrapper<>();
        merchantInfoQueryWrapper.select("pay_rate", "transfer_rate").eq("code", merchantCode);
        return getMap(merchantInfoQueryWrapper);
    }

    @Override
    public Boolean updateMerchantPwd(Long userId, String password, String passwordTips) {
        return this.baseMapper.updateMerchantPwd(userId, password, passwordTips) > 0;
    }

    @Override
    public Boolean updateUsdtAddress(Long userId, String usdtAddress) {
        return this.baseMapper.updateUsdtAddress(userId, usdtAddress) > 0;
    }

    @Override
    public MerchantFrontPageDTO fetchHomePageInfo(Long merchantId, String name) throws Exception {

        MerchantFrontPageDTO merchantFrontPageVo = new MerchantFrontPageDTO();
        String todayDateStr = DateUtil.format(LocalDateTime.now(ZoneId.systemDefault()), "yyyy-MM-dd");
        // 获取商户信息
        CompletableFuture<MerchantInfo> merchantFuture = CompletableFuture.supplyAsync(() -> {
            return userDetail(merchantId);
        });

        // 查询代付总订单数量
        CompletableFuture<Long> withdrawTotalNumFuture = CompletableFuture.supplyAsync(() -> {
            return paymentOrderMapper.queryWithdrawTotalNumByName(name);
        });

        // 查询今日付收笔数
        CompletableFuture<BigDecimal> todayWithdrawAmountFuture = CompletableFuture.supplyAsync(() -> {
            return paymentOrderMapper.calcTodayWithdrawAmount(name, todayDateStr);
        });

        // 查询今日付收费用
        CompletableFuture<BigDecimal> todayWithdrawCommissionFuture = CompletableFuture.supplyAsync(() -> {
            return paymentOrderMapper.calcTodayWithdrawCommission(name, todayDateStr);
        });

        // 今日付收笔数
        CompletableFuture<Long> todayWithdrawFinishNumFuture = CompletableFuture.supplyAsync(() -> {
            return paymentOrderMapper.calcTodayWithdrawFinishNum(name, todayDateStr);
        });

        // 查询支付当日金额
        CompletableFuture<BigDecimal> todayPayAmountFuture = CompletableFuture.supplyAsync(() -> {
            return collectionOrderMapper.calcTodayPayAmount(name, todayDateStr);
        });

        // 查询今日支付手续费
        CompletableFuture<BigDecimal> todayPayCommissionFuture = CompletableFuture.supplyAsync(() -> {
            return collectionOrderMapper.calcTodayPayCommission(name, todayDateStr);
        });

        // 查询今日代收笔数
        CompletableFuture<Long> todayTodayPayFinishNumFuture = CompletableFuture.supplyAsync(() -> {
            return collectionOrderMapper.calcTodayPayFinishNum(name, todayDateStr);
        });

        // 查询代收总订单数量
        CompletableFuture<Long> payTotalNumFuture = CompletableFuture.supplyAsync(() -> {
            return collectionOrderMapper.queryPayTotalNumByName(name);
        });


        // 代收未回调订单数量
        CompletableFuture<Long> payNotCallNumFuture = CompletableFuture.supplyAsync(() -> {
            return collectionOrderMapper.queryPayNotCallNumByName(name);
        });

        // 代收回调失败订单数量
        CompletableFuture<Long> payCallFailedNumFuture = CompletableFuture.supplyAsync(() -> {
            return collectionOrderMapper.queryPayCallFailedNumByName(name);
        });

        // 代付未回调订单数量
        CompletableFuture<Long> withdrawNotCallNumFuture = CompletableFuture.supplyAsync(() -> {
            return paymentOrderMapper.queryWithdrawNotCallNumByName(name);
        });

        // 代付回调失败订单数量
        CompletableFuture<Long> withdrawCallFailedNumFuture = CompletableFuture.supplyAsync(() -> {
            return paymentOrderMapper.queryWithdrawCallFailedNumByName(name);
        });


        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                merchantFuture, withdrawTotalNumFuture, payTotalNumFuture,
                payNotCallNumFuture,
                withdrawCallFailedNumFuture, withdrawNotCallNumFuture,
                payCallFailedNumFuture, todayPayAmountFuture, todayPayCommissionFuture,
                todayTodayPayFinishNumFuture, todayWithdrawAmountFuture, todayWithdrawCommissionFuture, todayWithdrawFinishNumFuture);

        allFutures.get();
        MerchantInfo merchantInfo = merchantFuture.get();
        // 剩余额度
        merchantFrontPageVo.setRemainingBalance(merchantFuture.get().getBalance());
        merchantFrontPageVo.setPayTotalNum(payTotalNumFuture.get());
        merchantFrontPageVo.setWithdrawTotalNum(withdrawTotalNumFuture.get());
        merchantFrontPageVo.setPayFinishTotalNum(merchantInfo.getTotalPayCount());
        merchantFrontPageVo.setWithdrawFinishTotalNum(merchantInfo.getTotalWithdrawCount());
        merchantFrontPageVo.setPayNotNotifyTotalNum(payNotCallNumFuture.get());
        merchantFrontPageVo.setPayNotifyFailedTotalNum(payCallFailedNumFuture.get());
        merchantFrontPageVo.setWithdrawNotNotifyTotalNum(withdrawNotCallNumFuture.get());
        merchantFrontPageVo.setWithdrawNotifyFailedTotalNum(withdrawCallFailedNumFuture.get());
        merchantFrontPageVo.setPayAndWithdrawSuccessTotalNum(merchantFrontPageVo.getPayFinishTotalNum() + merchantFrontPageVo.getWithdrawFinishTotalNum());
        merchantFrontPageVo.setTransferDownAmount(merchantInfo.getTransferDownAmount());
        merchantFrontPageVo.setTransferDownCount(merchantInfo.getTransferDownCount());
        merchantFrontPageVo.setTransferUpAmount(merchantInfo.getTransferUpAmount());
        merchantFrontPageVo.setTransferUpCount(merchantInfo.getTransferUpCount());
        merchantFrontPageVo.setWithdrawFinishTotalAmount(merchantInfo.getTotalWithdrawAmount());
        merchantFrontPageVo.setWithdrawTotalCommission(merchantInfo.getTotalWithdrawFee());
        merchantFrontPageVo.setPayFinishTotalAmount(merchantInfo.getTotalPayAmount());
        merchantFrontPageVo.setPayTotalCommission(merchantInfo.getTotalPayFee());
        merchantFrontPageVo.setLastLoginTime(merchantInfo.getLastLoginTime());
        merchantFrontPageVo.setLoginIp(merchantInfo.getLoginIp());


        // 今日代收额
        merchantFrontPageVo.setTodayPayAmount(todayPayAmountFuture.get());

        // 今日手续费
        merchantFrontPageVo.setTodayPayCommission(todayPayCommissionFuture.get());

        // 今日代收笔数
        merchantFrontPageVo.setTodayPayFinishNum(todayTodayPayFinishNumFuture.get());

        // 今日付收额
        merchantFrontPageVo.setTodayWithdrawAmount(todayWithdrawAmountFuture.get());

        // 今日代付手续费
        merchantFrontPageVo.setTodayWithdrawCommission(todayWithdrawCommissionFuture.get());

        // 今日付收笔数
        merchantFrontPageVo.setTodayWithdrawFinishNum(todayWithdrawFinishNumFuture.get());


        return merchantFrontPageVo;
    }

    /**
     * 总管理后台
     * @return
     * @throws Exception
     */
    @Override
    public MerchantFrontPageDTO fetchHomePageInfo() throws Exception {
        MerchantFrontPageDTO merchantFrontPageVo = new MerchantFrontPageDTO();

        String todayDateStr = DateUtil.format(LocalDateTime.now(ZoneId.systemDefault()), "yyyy-MM-dd");



        // 统计商户今日代收交易额
        CompletableFuture<BigDecimal> todayMerchantPayAmountFuture = CompletableFuture.supplyAsync(() -> {
            return collectionOrderMapper.todayMerchantPayAmount(todayDateStr);
        });

        // 统计商户今日代收交易笔数
        CompletableFuture<Long> todayMerchantPayTransNumFuture = CompletableFuture.supplyAsync(() -> {
            return collectionOrderMapper.todayMerchantPayTransNum(todayDateStr);
        });

        // 统计商户代收交易总金额
        CompletableFuture<BigDecimal> merchantPayTotalAmountFuture = CompletableFuture.supplyAsync(() -> {
            return collectionOrderMapper.merchantPayTotalAmount();
        });

        // 统计商户代收交易总笔数
        CompletableFuture<Long> merchantPayTransTotalNumFuture = CompletableFuture.supplyAsync(() -> {
            return collectionOrderMapper.merchantPayTransTotalNum();
        });

        // 统计商户代收今日费率
        CompletableFuture<BigDecimal> todayMerchantPayCommissionFuture = CompletableFuture.supplyAsync(() -> {
            return collectionOrderMapper.todayMerchantPayCommission(todayDateStr);
        });

        // 统计商户代收总费率
        CompletableFuture<BigDecimal> merchantPayTotalCommissionFuture = CompletableFuture.supplyAsync(() -> {
            return collectionOrderMapper.merchantPayTotalCommission();
        });

        // 查询代付总订单数量
        CompletableFuture<Long> withdrawTotalNumFuture = CompletableFuture.supplyAsync(() -> {
            return paymentOrderMapper.queryWithdrawTotalNum();
        });


        // 统计商户今日代付交易额
        CompletableFuture<BigDecimal> todayMerchantWithdrawAmountFuture = CompletableFuture.supplyAsync(() -> {
            return paymentOrderMapper.todayMerchantWithdrawAmount(todayDateStr);
        });

        // 统计商户今日代付交易笔数
        CompletableFuture<Long> todayMerchantWithdrawTransNumFuture = CompletableFuture.supplyAsync(() -> {
            return paymentOrderMapper.todayMerchantWithdrawTransNum(todayDateStr);
        });

        // 统计商户代付交易总金额
        CompletableFuture<BigDecimal> merchantWithdrawTotalAmountFuture = CompletableFuture.supplyAsync(() -> {
            return paymentOrderMapper.merchantWithdrawTotalAmount();
        });

        // 统计商户代付交易总笔数
        CompletableFuture<Long> merchantWithdrawTransTotalNumFuture = CompletableFuture.supplyAsync(() -> {
            return paymentOrderMapper.merchantWithdrawTransTotalNum();
        });
        // 统计商户代付今日费率
        CompletableFuture<BigDecimal> todayMerchantWithdrawCommissionFuture = CompletableFuture.supplyAsync(() -> {
            return paymentOrderMapper.todayMerchantWithdrawCommission(todayDateStr);
        });

        // 统计商户代付总费率
        CompletableFuture<BigDecimal> merchantWithdrawTotalCommissionFuture = CompletableFuture.supplyAsync(() -> {
            return paymentOrderMapper.merchantWithdrawTotalCommission();
        });


        // 查询代收总订单数量
        CompletableFuture<Long> payTotalNumFuture = CompletableFuture.supplyAsync(() -> {
            return collectionOrderMapper.queryPayTotalNum();
        });

        // 获取金额错误订单
        CompletableFuture<Long> amountErrorNumFuture = CompletableFuture.supplyAsync(() -> {
            return matchingOrderMapper.fethchAmountErrorNum();
        });

        // 获取匹配成功订单数量
        CompletableFuture<Long> matchSuccessNumFuture = CompletableFuture.supplyAsync(() -> {
            return matchingOrderMapper.matchSuccessNum();
        });


        // 统计今日买入订单
        CompletableFuture<OrderInfoVo> todayBuyInfoFuture = CompletableFuture.supplyAsync(() -> {
            return collectionOrderMapper.fetchTodayBuyInfoFuture(todayDateStr);
        });

        // 统计今日买入
        CompletableFuture<Long> todayBuyInfoTotalFuture = CompletableFuture.supplyAsync(() -> {
            return collectionOrderMapper.todayBuyInfoFuture(todayDateStr);
        });

        // 统计今日卖出订单
        CompletableFuture<OrderInfoVo> todaySellInfoFuture = CompletableFuture.supplyAsync(() -> {
            return paymentOrderMapper.fetchTodaySellInfoFuture(todayDateStr);
        });

        // 统计今日卖出总笔数
        CompletableFuture<Long> todaySellInfoTotalFuture = CompletableFuture.supplyAsync(() -> {
            return paymentOrderMapper.todaySellInfoFuture(todayDateStr);
        });

        // 统计今日买入总订单信息
        CompletableFuture<OrderInfoVo> buyTotalInfoFuture = CompletableFuture.supplyAsync(() -> {
            return collectionOrderMapper.fetchBuyTotalInfoFuture();
        });

        // 统计今日卖出总订单信息
        CompletableFuture<OrderInfoVo> sellTotalInfoFuture = CompletableFuture.supplyAsync(() -> {
            return paymentOrderMapper.fetchSellTotalInfoFuture();
        });

        // 统计今日usdt信息
        CompletableFuture<OrderInfoVo> todayUsdtInfoFuture = CompletableFuture.supplyAsync(() -> {
            return paymentOrderMapper.fetchTodayUsdtInfoFuture(todayDateStr);
        });

        // 统计usdt总信息
        CompletableFuture<OrderInfoVo> usdtTotalInfoFuture = CompletableFuture.supplyAsync(() -> {
            return paymentOrderMapper.fetchUsdtTotalInfoFuture();
        });


        // 充值订单取消支付订单数量
        CompletableFuture<Long> payCancelNumFuture = CompletableFuture.supplyAsync(() -> {
            return collectionOrderMapper.queryPayCancelNum();
        });

        // 充值订单取消订单数量
        CompletableFuture<Long> payCancelOrderNumFuture = CompletableFuture.supplyAsync(() -> {
            return collectionOrderMapper.queryPayCancelOrderNum();
        });

        // 充值订单取消订单数量
        CompletableFuture<Long> payAppealNumFuture = CompletableFuture.supplyAsync(() -> {
            return collectionOrderMapper.queryPayAppealNum();
        });

        // 充值订单取消订单数量
        CompletableFuture<Long> payAppealTotalNumFuture = CompletableFuture.supplyAsync(() -> {
            return collectionOrderMapper.queryPayAppealTotalNum();
        });

        // 代付匹配超时订单数量
        CompletableFuture<Long> withdrawOverTimeNumFuture = CompletableFuture.supplyAsync(() -> {
            return paymentOrderMapper.withdrawOverTimeNumFuture();
        });

        // 代付取消匹配订单数量
        CompletableFuture<Long> withdrawCancelMatchNumFuture = CompletableFuture.supplyAsync(() -> {
            return paymentOrderMapper.withdrawCancelMatchNum();
        });

        // 代付取消匹配订单数量
        CompletableFuture<Long> withdrawAppealNumFuture = CompletableFuture.supplyAsync(() -> {
            return paymentOrderMapper.withdrawAppealNum();
        });

        // 代付取消匹配订单数量
        CompletableFuture<Long> withdrawAppealTotalNumFuture = CompletableFuture.supplyAsync(() -> {
            return paymentOrderMapper.withdrawAppealTotalNum();
        });

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
               payCancelNumFuture,
                payCancelOrderNumFuture, payAppealNumFuture,
                withdrawOverTimeNumFuture, withdrawCancelMatchNumFuture,
                withdrawAppealNumFuture,
                todayBuyInfoFuture, todaySellInfoFuture, buyTotalInfoFuture,
                sellTotalInfoFuture, matchSuccessNumFuture,
                todayMerchantPayAmountFuture, todayMerchantPayTransNumFuture,
                merchantPayTotalAmountFuture, merchantPayTransTotalNumFuture,
                todayMerchantPayCommissionFuture, merchantPayTotalCommissionFuture,
                todayMerchantWithdrawAmountFuture, todayMerchantWithdrawTransNumFuture,
                merchantWithdrawTotalAmountFuture, merchantWithdrawTransTotalNumFuture,
                todayMerchantWithdrawCommissionFuture, merchantWithdrawTotalCommissionFuture,
                payAppealTotalNumFuture, withdrawAppealTotalNumFuture,
                todaySellInfoTotalFuture,todayBuyInfoTotalFuture
        );

        allFutures.get();
        // 剩余额度

        merchantFrontPageVo.setPayCancelNum(payCancelNumFuture.get());
        merchantFrontPageVo.setPayCancelOrderNum(payCancelOrderNumFuture.get());
        merchantFrontPageVo.setPayAppealNum(payAppealNumFuture.get());
        merchantFrontPageVo.setWithdrawTotalNum(withdrawTotalNumFuture.get());
        merchantFrontPageVo.setPayTotalNum(payTotalNumFuture.get());
        merchantFrontPageVo.setWithdrawOverTimeNum(withdrawOverTimeNumFuture.get());
        merchantFrontPageVo.setWithdrawCancelMatchNum(withdrawCancelMatchNumFuture.get());
        merchantFrontPageVo.setWithdrawAppealNum(withdrawAppealNumFuture.get());
        merchantFrontPageVo.setAmountErrorNum(amountErrorNumFuture.get());
        // 今日买入
        merchantFrontPageVo.setTodayPayAmount(todayBuyInfoFuture.get().getActualAmount());
        merchantFrontPageVo.setTodayPayCommission(todayBuyInfoFuture.get().getTotalCost());
        merchantFrontPageVo.setTodayPayFinishNum(todayBuyInfoFuture.get().getTotalNum());
        // 总
        merchantFrontPageVo.setPayFinishTotalNum(buyTotalInfoFuture.get().getTotalNum());
        merchantFrontPageVo.setPayFinishTotalAmount(buyTotalInfoFuture.get().getActualAmount());
        merchantFrontPageVo.setPayTotalCommission(buyTotalInfoFuture.get().getTotalCost());

        // 今日卖出
        merchantFrontPageVo.setTodayWithdrawAmount(todaySellInfoFuture.get().getActualAmount());
        merchantFrontPageVo.setTodayWithdrawCommission(todaySellInfoFuture.get().getTotalCost());
        merchantFrontPageVo.setTodayWithdrawFinishNum(todaySellInfoFuture.get().getTotalNum());
        // 总
        merchantFrontPageVo.setWithdrawFinishTotalNum(sellTotalInfoFuture.get().getTotalNum());
        merchantFrontPageVo.setWithdrawTotalCommission(sellTotalInfoFuture.get().getTotalCost());
        merchantFrontPageVo.setWithdrawFinishTotalAmount(sellTotalInfoFuture.get().getActualAmount());

        merchantFrontPageVo.setUsdtTotalAmount(usdtTotalInfoFuture.get().getActualAmount());
        merchantFrontPageVo.setTodayUsdtAmount(todayUsdtInfoFuture.get().getActualAmount());
        merchantFrontPageVo.setUsdtTotalNum(usdtTotalInfoFuture.get().getTotalNum());
        merchantFrontPageVo.setPayAndWithdrawSuccessTotalNum(merchantFrontPageVo.getPayFinishTotalNum() + merchantFrontPageVo.getWithdrawFinishTotalNum());
        merchantFrontPageVo.setMatchSuccessNum(matchSuccessNumFuture.get());
        merchantFrontPageVo.setTodayMerchantPayAmount(todayMerchantPayAmountFuture.get());
        merchantFrontPageVo.setTodayMerchantPayCommission(todayMerchantPayCommissionFuture.get());
        merchantFrontPageVo.setTodayMerchantPayTransNum(todayMerchantPayTransNumFuture.get());
        merchantFrontPageVo.setMerchantPayTotalAmount(merchantPayTotalAmountFuture.get());
        merchantFrontPageVo.setMerchantPayTransTotalNum(merchantPayTransTotalNumFuture.get());
        merchantFrontPageVo.setMerchantPayTotalCommission(merchantPayTotalCommissionFuture.get());
        merchantFrontPageVo.setTodayMerchantWithdrawAmount(todayMerchantWithdrawAmountFuture.get());
        merchantFrontPageVo.setTodayMerchantWithdrawTransNum(todayMerchantWithdrawTransNumFuture.get());
        merchantFrontPageVo.setMerchantWithdrawTotalAmount(merchantWithdrawTotalAmountFuture.get());
        merchantFrontPageVo.setMerchantWithdrawTransTotalNum(merchantWithdrawTransTotalNumFuture.get());
        merchantFrontPageVo.setTodayMerchantWithdrawCommission(todayMerchantWithdrawCommissionFuture.get());
        merchantFrontPageVo.setMerchantWithdrawTotalCommission(merchantWithdrawTotalCommissionFuture.get());
        merchantFrontPageVo.setPayAppealTotalNum(payAppealTotalNumFuture.get());
        merchantFrontPageVo.setWithdrawAppealTotalNum(withdrawAppealTotalNumFuture.get());

        merchantFrontPageVo.setTodayPayTotalNum(todayBuyInfoTotalFuture.get());
        merchantFrontPageVo.setTodayWithdrawTotalNum(todaySellInfoTotalFuture.get());
        return merchantFrontPageVo;
    }

    @Override
    @SneakyThrows
    public PageReturn<WithdrawOrderDTO> fetchWithdrawOrderInfo(WithdrawOrderReq withdrawOrderReq) {

        Page<MerchantPaymentOrders> page = new Page<>();
        page.setCurrent(withdrawOrderReq.getPageNo());
        page.setSize(withdrawOrderReq.getPageSize());
        LambdaQueryWrapper<MerchantPaymentOrders> paymentOrder = new LambdaQueryWrapper<>();

        // 新增统计金额字段总计字段
        LambdaQueryWrapper<MerchantPaymentOrders> queryWrapper = new QueryWrapper<MerchantPaymentOrders>()
                .select("IFNULL(sum(amount),0) as amountTotal,IFNULL(sum(cost), 0) as costTotal").lambda();



        if(org.apache.commons.lang3.StringUtils.isNotBlank(withdrawOrderReq.getMerchantOrder())){
            paymentOrder.eq(MerchantPaymentOrders::getMerchantOrder, withdrawOrderReq.getMerchantOrder());
            queryWrapper.eq(MerchantPaymentOrders::getMerchantOrder, withdrawOrderReq.getMerchantOrder());
        }
        if (org.apache.commons.lang3.StringUtils.isNotBlank(withdrawOrderReq.getMerchantCode())){
            paymentOrder.eq(MerchantPaymentOrders::getMerchantCode, withdrawOrderReq.getMerchantCode());
            queryWrapper.eq(MerchantPaymentOrders::getMerchantCode, withdrawOrderReq.getMerchantCode());
        }
        if(org.apache.commons.lang3.StringUtils.isNotBlank(withdrawOrderReq.getPlatformOrder())){
            paymentOrder.eq(MerchantPaymentOrders::getPlatformOrder, withdrawOrderReq.getPlatformOrder());
            queryWrapper.eq(MerchantPaymentOrders::getPlatformOrder, withdrawOrderReq.getPlatformOrder());
        }
        if(org.apache.commons.lang3.StringUtils.isNotBlank(withdrawOrderReq.getMerchantName())){
            paymentOrder.eq(MerchantPaymentOrders::getMerchantName, withdrawOrderReq.getMerchantName());
            queryWrapper.eq(MerchantPaymentOrders::getMerchantName, withdrawOrderReq.getMerchantName());
        }
        if(!ObjectUtils.isEmpty(withdrawOrderReq.getOrderStatus())){
            paymentOrder.eq(MerchantPaymentOrders::getOrderStatus, withdrawOrderReq.getOrderStatus());
            queryWrapper.eq(MerchantPaymentOrders::getOrderStatus, withdrawOrderReq.getOrderStatus());
        }
        if(!ObjectUtils.isEmpty(withdrawOrderReq.getCallbackStatus())){
            paymentOrder.eq(MerchantPaymentOrders::getTradeCallbackStatus, withdrawOrderReq.getCallbackStatus());
            queryWrapper.eq(MerchantPaymentOrders::getTradeCallbackStatus, withdrawOrderReq.getCallbackStatus());
        }
        if(!org.ar.common.core.utils.StringUtils.isEmpty(withdrawOrderReq.getMemberId())){
            paymentOrder.eq(MerchantPaymentOrders::getMemberId, withdrawOrderReq.getMemberId());
            queryWrapper.eq(MerchantPaymentOrders::getMemberId, withdrawOrderReq.getMemberId());
        }
        if(!org.ar.common.core.utils.StringUtils.isEmpty(withdrawOrderReq.getExternalMemberId())){
            paymentOrder.like(MerchantPaymentOrders::getExternalMemberId, withdrawOrderReq.getExternalMemberId());
            queryWrapper.like(MerchantPaymentOrders::getExternalMemberId, withdrawOrderReq.getExternalMemberId());
        }
        // 下单时间
        if(!ObjectUtils.isEmpty(withdrawOrderReq.getTimeType()) && withdrawOrderReq.getTimeType().equals(1)){
            if (ObjectUtils.isNotEmpty(withdrawOrderReq.getStartTime())) {
                paymentOrder.ge(MerchantPaymentOrders::getCreateTime, withdrawOrderReq.getStartTime());
                queryWrapper.ge(MerchantPaymentOrders::getCreateTime, withdrawOrderReq.getStartTime());
            }

            if (ObjectUtils.isNotEmpty(withdrawOrderReq.getEndTime())) {
                paymentOrder.le(MerchantPaymentOrders::getCreateTime, withdrawOrderReq.getEndTime());
                queryWrapper.le(MerchantPaymentOrders::getCreateTime, withdrawOrderReq.getEndTime());
            }
        }else if(!ObjectUtils.isEmpty(withdrawOrderReq.getTimeType()) && withdrawOrderReq.getTimeType().equals(2)){
            if (ObjectUtils.isNotEmpty(withdrawOrderReq.getStartTime())) {
                paymentOrder.ge(MerchantPaymentOrders::getUpdateTime, withdrawOrderReq.getStartTime());
                queryWrapper.ge(MerchantPaymentOrders::getUpdateTime, withdrawOrderReq.getStartTime());
            }

            if (ObjectUtils.isNotEmpty(withdrawOrderReq.getEndTime())) {
                paymentOrder.le(MerchantPaymentOrders::getUpdateTime, withdrawOrderReq.getEndTime());
                queryWrapper.le(MerchantPaymentOrders::getUpdateTime, withdrawOrderReq.getEndTime());
            }
        }else {

            if (ObjectUtils.isNotEmpty(withdrawOrderReq.getStartTime())) {
                paymentOrder.ge(MerchantPaymentOrders::getCreateTime, withdrawOrderReq.getStartTime());
                queryWrapper.ge(MerchantPaymentOrders::getCreateTime, withdrawOrderReq.getStartTime());
            }

            if (ObjectUtils.isNotEmpty(withdrawOrderReq.getEndTime())) {
                paymentOrder.le(MerchantPaymentOrders::getCreateTime, withdrawOrderReq.getEndTime());
                queryWrapper.le(MerchantPaymentOrders::getCreateTime, withdrawOrderReq.getEndTime());
            }

            if (ObjectUtils.isNotEmpty(withdrawOrderReq.getCompleteStartTime())) {
                paymentOrder.ge(MerchantPaymentOrders::getUpdateTime, withdrawOrderReq.getStartTime());
                queryWrapper.ge(MerchantPaymentOrders::getUpdateTime, withdrawOrderReq.getStartTime());
            }

            if (ObjectUtils.isNotEmpty(withdrawOrderReq.getCompleteEndTime())) {
                paymentOrder.le(MerchantPaymentOrders::getUpdateTime, withdrawOrderReq.getEndTime());
                queryWrapper.le(MerchantPaymentOrders::getUpdateTime, withdrawOrderReq.getEndTime());
            }
        }
        paymentOrder.orderByDesc(MerchantPaymentOrders::getId);


        Page<MerchantPaymentOrders> finalPage = page;
        CompletableFuture<MerchantPaymentOrders> totalFuture = CompletableFuture.supplyAsync(() -> merchantPaymentOrdersMapper.selectOne(queryWrapper));
        CompletableFuture<Page<MerchantPaymentOrders>> resultFuture = CompletableFuture.supplyAsync(() -> merchantPaymentOrdersMapper.selectPage(finalPage, paymentOrder));
        CompletableFuture.allOf(totalFuture, resultFuture);

        page = resultFuture.get();
        MerchantPaymentOrders totalInfo = totalFuture.get();
        JSONObject extent = new JSONObject();
        extent.put("amountTotal", totalInfo.getAmountTotal());
        extent.put("costTotal", totalInfo.getCostTotal());
        BigDecimal amountPageTotal = BigDecimal.ZERO;
        BigDecimal costPageTotal = BigDecimal.ZERO;
        List<MerchantPaymentOrders> records = page.getRecords();
        List<WithdrawOrderDTO> withdrawOrderDTOList = walletMapStruct.withdrawOrderTransform(records);
        for (WithdrawOrderDTO item : withdrawOrderDTOList) {
            if(org.apache.commons.lang3.StringUtils.isNotBlank(item.getExternalMemberId())){
                String externalMemberId = item.getExternalMemberId().substring(item.getMerchantCode().length());
                item.setExternalMemberId(externalMemberId);
            }
            amountPageTotal = amountPageTotal.add(item.getAmount());
            costPageTotal = costPageTotal.add(item.getCost());
        }
        extent.put("amountPageTotal", amountPageTotal);
        extent.put("costPageTotal", costPageTotal);
        return PageUtils.flush(page, withdrawOrderDTOList, extent);
    }

    @Override
    public PageReturn<WithdrawOrderExportDTO> fetchWithdrawOrderInfoExport(WithdrawOrderReq req) {
        PageReturn<WithdrawOrderDTO> withdrawOrderReturn = fetchWithdrawOrderInfo(req);

        List<WithdrawOrderExportDTO> resultList = new ArrayList<>();
        for (WithdrawOrderDTO withdrawOrderDTO : withdrawOrderReturn.getList()) {
            WithdrawOrderExportDTO withdrawOrderExportDTO = new WithdrawOrderExportDTO();
            BeanUtils.copyProperties(withdrawOrderDTO, withdrawOrderExportDTO);
            String nameByCode = CollectionOrderStatusEnum.getNameByCode(withdrawOrderDTO.getOrderStatus());
            withdrawOrderExportDTO.setOrderStatus(nameByCode);
            withdrawOrderExportDTO.setTradeCallbackStatus(NotifyStatusEnum.getNameByCode(withdrawOrderDTO.getTradeCallbackStatus()));
            if(withdrawOrderDTO.getAmount() != null){
                withdrawOrderExportDTO.setAmount(withdrawOrderDTO.getAmount().toString());
            }
            if(withdrawOrderDTO.getCost() != null){
                withdrawOrderExportDTO.setCost(withdrawOrderDTO.getCost().toString());
            }
            resultList.add(withdrawOrderExportDTO);
        }
        Page<WithdrawOrderExportDTO> page = new Page<>();
        page.setCurrent(req.getPageNo());
        page.setSize(req.getPageSize());
        page.setTotal(withdrawOrderReturn.getTotal());
        return PageUtils.flush(page, resultList);
    }

    /**
     * 代付手动回调成功
     * @param id
     * @return
     */
    @Override
    public Boolean confirmSuccess(Long id) {
        boolean result = false;
        try {
            MerchantPaymentOrders paymentOrder = merchantPaymentOrdersMapper.selectById(id);

            if(ObjectUtils.isEmpty(paymentOrder)){
                throw new BizException(ResultCode.ORDER_NOT_EXIST);
            }
            // 判断交易回调状态
            if(paymentOrder.getTradeCallbackStatus().equals(NotifyStatusEnum.SUCCESS.getCode()) || paymentOrder.getTradeCallbackStatus().equals(NotifyStatusEnum.MANUAL_SUCCESS.getCode())){
                throw new BizException(ResultCode.ORDER_ALREADY_CALLBACK);
            }else {
                result = asyncNotifyService.sendWithdrawalSuccessCallback(paymentOrder.getMerchantOrder(), "2");
            }
        }catch (Exception ex){
            log.error("confirmSuccess->" + ex.getMessage());
        }

        return result;

    }


    @Override
    public MerchantInfo userDetailByCode(String code) {
        MerchantInfo merchantInfo = lambdaQuery().eq(MerchantInfo::getCode, code).one();
        AssertUtil.notEmpty(merchantInfo, ResultCode.USERNAME_OR_PASSWORD_ERROR);

        // 查询绑定的角色IDs


        return merchantInfo;
    }

    @Override
    public Map<Integer, String> fetchOrderStatus() {

        Map<Integer, String> map = new HashMap<>(15);
        map.put(Integer.parseInt(CollectionOrderStatusEnum.PAID.getCode()), CollectionOrderStatusEnum.PAID.getName());
        map.put(Integer.parseInt(CollectionOrderStatusEnum.BE_PAID.getCode()), CollectionOrderStatusEnum.BE_PAID.getName());
        map.put(Integer.parseInt(CollectionOrderStatusEnum.WAS_CANCELED.getCode()), CollectionOrderStatusEnum.WAS_CANCELED.getName());

        return map;
    }

    @Override
    public Map<Integer, String> orderCallbackStatus() {

        Map<Integer, String> map = new HashMap<>(15);
        map.put(Integer.parseInt(NotifyStatusEnum.NOTCALLBACK.getCode()), NotifyStatusEnum.NOTCALLBACK.getName());
        map.put(Integer.parseInt(NotifyStatusEnum.SUCCESS.getCode()), NotifyStatusEnum.SUCCESS.getName());
        map.put(Integer.parseInt(NotifyStatusEnum.FAILED.getCode()), NotifyStatusEnum.FAILED.getName());
        map.put(Integer.parseInt(NotifyStatusEnum.MANUAL_SUCCESS.getCode()), NotifyStatusEnum.MANUAL_SUCCESS.getName());
        map.put(Integer.parseInt(NotifyStatusEnum.MANUAL_FAILED.getCode()), NotifyStatusEnum.MANUAL_FAILED.getName());

        return map;
    }


    @Override
    @SneakyThrows
    public PageReturn<RechargeOrderDTO> fetchRechargeOrderInfo(RechargeOrderReq rechargeOrderReq) {
        Page<MerchantCollectOrders> page = new Page<>();
        page.setCurrent(rechargeOrderReq.getPageNo());
        page.setSize(rechargeOrderReq.getPageSize());
        LambdaQueryWrapper<MerchantCollectOrders> collectionOrder = new LambdaQueryWrapper<>();

        // 新增统计金额字段总计字段
        LambdaQueryWrapper<MerchantCollectOrders> queryWrapper = new QueryWrapper<MerchantCollectOrders>()
                .select("IFNULL(sum(amount),0) as amountTotal,IFNULL(sum(cost), 0) as costTotal").lambda();


        if(!StringUtils.isNullOrEmpty(rechargeOrderReq.getMerchantOrder())){
            collectionOrder.eq(MerchantCollectOrders::getMerchantOrder, rechargeOrderReq.getMerchantOrder());
            queryWrapper.eq(MerchantCollectOrders::getMerchantOrder, rechargeOrderReq.getMerchantOrder());
        }
        if(!StringUtils.isNullOrEmpty(rechargeOrderReq.getPlatformOrder())){
            collectionOrder.eq(MerchantCollectOrders::getPlatformOrder, rechargeOrderReq.getPlatformOrder());
            queryWrapper.eq(MerchantCollectOrders::getPlatformOrder, rechargeOrderReq.getPlatformOrder());
        }
        if(!ObjectUtils.isEmpty(rechargeOrderReq.getOrderStatus())){
            collectionOrder.eq(MerchantCollectOrders::getOrderStatus, rechargeOrderReq.getOrderStatus());
            queryWrapper.eq(MerchantCollectOrders::getOrderStatus, rechargeOrderReq.getOrderStatus());
        }
        if(!ObjectUtils.isEmpty(rechargeOrderReq.getCallbackStatus())){
            collectionOrder.eq(MerchantCollectOrders::getTradeCallbackStatus, rechargeOrderReq.getCallbackStatus());
            queryWrapper.eq(MerchantCollectOrders::getTradeCallbackStatus, rechargeOrderReq.getCallbackStatus());
        }
        if(!org.ar.common.core.utils.StringUtils.isEmpty(rechargeOrderReq.getMerchantCode())){
            collectionOrder.eq(MerchantCollectOrders::getMerchantCode, rechargeOrderReq.getMerchantCode());
            queryWrapper.eq(MerchantCollectOrders::getMerchantCode, rechargeOrderReq.getMerchantCode());
        }

        if(!org.ar.common.core.utils.StringUtils.isEmpty(rechargeOrderReq.getMerchantName())){
            collectionOrder.eq(MerchantCollectOrders::getMerchantName, rechargeOrderReq.getMerchantName());
            queryWrapper.eq(MerchantCollectOrders::getMerchantName, rechargeOrderReq.getMerchantName());
        }
        if(!org.ar.common.core.utils.StringUtils.isEmpty(rechargeOrderReq.getMemberId())){
            collectionOrder.eq(MerchantCollectOrders::getMemberId, rechargeOrderReq.getMemberId());
            queryWrapper.eq(MerchantCollectOrders::getMemberId, rechargeOrderReq.getMemberId());
        }
        if(!org.ar.common.core.utils.StringUtils.isEmpty(rechargeOrderReq.getExternalMemberId())){
            collectionOrder.like(MerchantCollectOrders::getExternalMemberId, rechargeOrderReq.getExternalMemberId());
            queryWrapper.like(MerchantCollectOrders::getExternalMemberId, rechargeOrderReq.getExternalMemberId());
        }
        // 下单时间
        if(!ObjectUtils.isEmpty(rechargeOrderReq.getTimeType()) && rechargeOrderReq.getTimeType().equals(1)){
            if (ObjectUtils.isNotEmpty(rechargeOrderReq.getStartTime())) {
                collectionOrder.ge(MerchantCollectOrders::getCreateTime, rechargeOrderReq.getStartTime());
                queryWrapper.ge(MerchantCollectOrders::getCreateTime, rechargeOrderReq.getStartTime());
            }

            if (ObjectUtils.isNotEmpty(rechargeOrderReq.getEndTime())) {
                collectionOrder.le(MerchantCollectOrders::getCreateTime, rechargeOrderReq.getEndTime());
                queryWrapper.le(MerchantCollectOrders::getCreateTime, rechargeOrderReq.getEndTime());
            }
        }else if(!ObjectUtils.isEmpty(rechargeOrderReq.getTimeType()) && rechargeOrderReq.getTimeType().equals(2)){
            if (ObjectUtils.isNotEmpty(rechargeOrderReq.getStartTime())) {
                collectionOrder.ge(MerchantCollectOrders::getUpdateTime, rechargeOrderReq.getStartTime());
                queryWrapper.ge(MerchantCollectOrders::getUpdateTime, rechargeOrderReq.getStartTime());
            }

            if (ObjectUtils.isNotEmpty(rechargeOrderReq.getEndTime())) {
                collectionOrder.le(MerchantCollectOrders::getUpdateTime, rechargeOrderReq.getEndTime());
                queryWrapper.le(MerchantCollectOrders::getUpdateTime, rechargeOrderReq.getEndTime());
            }
        }else {
            if (ObjectUtils.isNotEmpty(rechargeOrderReq.getStartTime())) {
                collectionOrder.ge(MerchantCollectOrders::getCreateTime, rechargeOrderReq.getStartTime());
                queryWrapper.ge(MerchantCollectOrders::getCreateTime, rechargeOrderReq.getStartTime());
            }

            if (ObjectUtils.isNotEmpty(rechargeOrderReq.getEndTime())) {
                collectionOrder.le(MerchantCollectOrders::getCreateTime, rechargeOrderReq.getEndTime());
                queryWrapper.le(MerchantCollectOrders::getCreateTime, rechargeOrderReq.getEndTime());
            }
            if (ObjectUtils.isNotEmpty(rechargeOrderReq.getCompleteStartTime())) {
                collectionOrder.ge(MerchantCollectOrders::getUpdateTime, rechargeOrderReq.getCompleteStartTime());
                queryWrapper.ge(MerchantCollectOrders::getUpdateTime, rechargeOrderReq.getCompleteStartTime());
            }

            if (ObjectUtils.isNotEmpty(rechargeOrderReq.getCompleteEndTime())) {
                collectionOrder.le(MerchantCollectOrders::getUpdateTime, rechargeOrderReq.getCompleteEndTime());
                queryWrapper.le(MerchantCollectOrders::getUpdateTime, rechargeOrderReq.getCompleteEndTime());
            }
        }
        collectionOrder.orderByDesc(MerchantCollectOrders::getCreateTime);

        Page<MerchantCollectOrders> finalPage = page;
        CompletableFuture<MerchantCollectOrders> totalFuture = CompletableFuture.supplyAsync(() -> merchantCollectOrdersMapper.selectOne(queryWrapper));
        CompletableFuture<Page<MerchantCollectOrders>> resultFuture = CompletableFuture.supplyAsync(() -> merchantCollectOrdersMapper.selectPage(finalPage, collectionOrder));
        CompletableFuture.allOf(totalFuture, resultFuture);
        page = resultFuture.get();
        MerchantCollectOrders totalInfo = totalFuture.get();
        JSONObject extent = new JSONObject();
        extent.put("amountTotal", totalInfo.getAmountTotal());
        extent.put("costTotal", totalInfo.getCostTotal());
        List<MerchantCollectOrders> records = page.getRecords();
        BigDecimal amountPageTotal = BigDecimal.ZERO;
        BigDecimal costPageTotal = BigDecimal.ZERO;
        List<RechargeOrderDTO> rechargeOrderDTOList = walletMapStruct.rechargeOrderTransform(records);
        for (RechargeOrderDTO item : rechargeOrderDTOList) {
            if(org.apache.commons.lang3.StringUtils.isNotBlank(item.getExternalMemberId())){
                String externalMemberId = item.getExternalMemberId().substring(item.getMerchantCode().length());
                item.setExternalMemberId(externalMemberId);
            }
            amountPageTotal = amountPageTotal.add(item.getAmount());
            costPageTotal = costPageTotal.add(item.getCost());
        }
        extent.put("amountPageTotal", amountPageTotal);
        extent.put("costPageTotal", costPageTotal);
        return PageUtils.flush(page, rechargeOrderDTOList, extent);
    }

    @Override
    public PageReturn<RechargeOrderExportDTO> fetchRechargeOrderInfoExport(RechargeOrderReq req) {
        PageReturn<RechargeOrderDTO> rechargeOrderReturn = fetchRechargeOrderInfo(req);

        List<RechargeOrderExportDTO> resultList = new ArrayList<>();

        for (RechargeOrderDTO rechargeOrderDTO : rechargeOrderReturn.getList()) {
            RechargeOrderExportDTO rechargeOrderExportDTO = new RechargeOrderExportDTO();
            BeanUtils.copyProperties(rechargeOrderDTO, rechargeOrderExportDTO);

            String nameByCode = PaymentOrderStatusEnum.getNameByCode(rechargeOrderDTO.getOrderStatus());
            String notifyStatus = NotifyStatusEnum.getNameByCode(rechargeOrderDTO.getTradeCallbackStatus());
            rechargeOrderExportDTO.setOrderStatus(nameByCode);
            rechargeOrderExportDTO.setTradeCallbackStatus(notifyStatus);
            if(rechargeOrderDTO.getAmount() != null){
                rechargeOrderExportDTO.setAmount(rechargeOrderDTO.getAmount().toString());
            }
            if(rechargeOrderDTO.getCost() != null){
                rechargeOrderExportDTO.setCost(rechargeOrderDTO.getCost().toString());
            }
            resultList.add(rechargeOrderExportDTO);
        }
        Page<RechargeOrderExportDTO> page = new Page<>();
        page.setCurrent(req.getPageNo());
        page.setSize(req.getPageSize());
        page.setTotal(rechargeOrderReturn.getTotal());
        return PageUtils.flush(page, resultList);

    }

    @Override
    public Boolean rechargeConfirmSuccess(Long id) {

        Boolean result = false;
        try {
            MerchantCollectOrders collectionOrder = merchantCollectOrdersMapper.selectById(id);
            if(ObjectUtils.isEmpty(collectionOrder)){
                throw new BizException(ResultCode.ORDER_NOT_EXIST);
            }
            // 判断交易回调状态
            if(collectionOrder.getTradeCallbackStatus().equals(NotifyStatusEnum.SUCCESS.getCode())){
                throw new BizException(ResultCode.ORDER_ALREADY_CALLBACK);
            }else {
                result = asyncNotifyService.sendRechargeSuccessCallback(collectionOrder.getPlatformOrder(), "2");
            }
        }catch (Exception ex){
            log.error("rechargeConfirmSuccess->" + ex.getMessage());
            result = false;
        }

        return result;
    }

    @Override
    public Map<Long, String> getMerchantName() {
        List<MerchantInfo> result = this.baseMapper.selectList(null);
        Map<Long, String>  merchantMap = new HashMap<>();
        for (MerchantInfo merchant : result) {
            merchantMap.put(merchant.getId(), merchant.getCode());
        }
        return merchantMap;
    }

    @Override
    public Map<String, String> getCurrency() {

        Map<String, String> map = new HashMap<>(15);
        map.put(CurrenceEnum.INDIA.getCode(), CurrenceEnum.INDIA.getName());
        map.put(CurrenceEnum.CHINA.getCode(), CurrenceEnum.CHINA.getName());
        return map;
    }

    @Override
    @SneakyThrows
    public OrderOverviewDTO getOrderNumOverview() {
        // 获取金额错误订单
        // CompletableFuture<Long> amountErrorNumFuture = CompletableFuture.supplyAsync(matchingOrderMapper::fethchAmountErrorNum);
        // 获取买入申诉订单
        CompletableFuture<Long> payAppealNumFuture = CompletableFuture.supplyAsync(collectionOrderMapper::queryPayAppealNum);
        // 获取卖出申述订单
        CompletableFuture<Long> withdrawAppealNumFuture = CompletableFuture.supplyAsync(paymentOrderMapper::withdrawAppealNum);
        // 获取匹配池中匹配中的订单
        CompletableFuture<Long> poolMatchingOrderFuture = CompletableFuture.supplyAsync(() -> matchPoolMapper.getOrderNumByOrderStatus(OrderStatusEnum.BE_MATCHED.getCode()));
        // 获取代付订单匹配中的订单
        CompletableFuture<Long> paymentMatchingOrderFuture = CompletableFuture.supplyAsync(() -> paymentOrderMapper.getOrderNumByOrderStatus(OrderStatusEnum.BE_MATCHED.getCode()));
        // 获取待支付订单
        CompletableFuture<Long> waitForPayFuture = CompletableFuture.supplyAsync(() -> collectionOrderMapper.getOrderNumByOrderStatus(OrderStatusEnum.BE_PAID.getCode()));
        // 获取确认中订单
        CompletableFuture<Long> waitForConfirmFuture = CompletableFuture.supplyAsync(() -> collectionOrderMapper.getOrderNumByOrderStatus(OrderStatusEnum.CONFIRMATION.getCode()));

        CompletableFuture<Void> allFuture = CompletableFuture.allOf(payAppealNumFuture, withdrawAppealNumFuture, poolMatchingOrderFuture, paymentMatchingOrderFuture, waitForPayFuture, waitForConfirmFuture);
        allFuture.get();
        // 计算匹配中订单
        long poolMatchingOrderNum = poolMatchingOrderFuture.get();
        long paymentMatchingOrderNum = paymentMatchingOrderFuture.get();
        long matchingOrderNum = poolMatchingOrderNum + paymentMatchingOrderNum;
        // 计算待处理申诉订单
//        long amountErrorOrderNum = amountErrorNumFuture.get();
        long payAppealNum = payAppealNumFuture.get();
        long withdrawAppealNum = withdrawAppealNumFuture.get();
        long pendingOrdersNum =  payAppealNum + withdrawAppealNum;
        // 计算进行中订单
        long waitForPay = waitForPayFuture.get();
        long waitForConfirm = waitForConfirmFuture.get();
        long beProcessedOrderNum = waitForPay + waitForConfirm;

        OrderOverviewDTO result = new OrderOverviewDTO();
//        result.setAmountErrorNum(amountErrorOrderNum);
        result.setPayAppealNum(payAppealNum);
        result.setMatchingOrderNum(matchingOrderNum);
        result.setBeProcessedOrderNum(beProcessedOrderNum);
        result.setPendingOrdersNum(pendingOrdersNum);
        result.setWithdrawAppealNum(withdrawAppealNum);
        result.setWaitForConfirmOrderNum(waitForConfirm);
        result.setWaitForPaymentOrderNum(waitForPay);

        return result;
    }

    @Override
    @SneakyThrows
    public TodayOrderOverviewDTO todayOrderOverview() {
        TodayOrderOverviewDTO todayOrderOverview = new TodayOrderOverviewDTO();
        String todayDateStr = DateUtil.format(LocalDateTime.now(ZoneId.systemDefault()), "yyyy-MM-dd");
        // 统计商户今日代收交易额
        CompletableFuture<BigDecimal> todayMerchantPayAmountFuture = CompletableFuture.supplyAsync(() -> collectionOrderMapper.todayMerchantPayAmount(todayDateStr));

        // 统计商户今日代收交易笔数
        CompletableFuture<Long> todayMerchantPayTransNumFuture = CompletableFuture.supplyAsync(() -> collectionOrderMapper.todayMerchantPayTransNum(todayDateStr));

        // 统计商户代收今日费率
        CompletableFuture<BigDecimal> todayMerchantPayCommissionFuture = CompletableFuture.supplyAsync(() -> collectionOrderMapper.todayMerchantPayCommission(todayDateStr));

        // 统计商户今日代付交易额
        CompletableFuture<BigDecimal> todayMerchantWithdrawAmountFuture = CompletableFuture.supplyAsync(() -> paymentOrderMapper.todayMerchantWithdrawAmount(todayDateStr));

        // 统计商户今日代付交易笔数
        CompletableFuture<Long> todayMerchantWithdrawTransNumFuture = CompletableFuture.supplyAsync(() -> paymentOrderMapper.todayMerchantWithdrawTransNum(todayDateStr));

        // 统计商户代付今日费率
        CompletableFuture<BigDecimal> todayMerchantWithdrawCommissionFuture = CompletableFuture.supplyAsync(() -> paymentOrderMapper.todayMerchantWithdrawCommission(todayDateStr));

        // 今日买入订单
        CompletableFuture<OrderInfoVo> todayBuyInfoFuture = CompletableFuture.supplyAsync(() -> collectionOrderMapper.fetchTodayBuyInfoFuture(todayDateStr));

        // 今日买入订单数量
        CompletableFuture<Long> todayBuyInfoTotalFuture = CompletableFuture.supplyAsync(() -> collectionOrderMapper.todayBuyInfoFuture(todayDateStr));

        // 今日卖出订单
        CompletableFuture<OrderInfoVo> todaySellInfoFuture = CompletableFuture.supplyAsync(() -> paymentOrderMapper.fetchTodaySellInfoFuture(todayDateStr));

        // 今日卖出订单数量
        CompletableFuture<Long> todaySellInfoTotalFuture = CompletableFuture.supplyAsync(() -> paymentOrderMapper.todaySellInfoFuture(todayDateStr));

        // 统计今日usdt信息
        CompletableFuture<OrderInfoVo> todayUsdtInfoFuture = CompletableFuture.supplyAsync(() -> paymentOrderMapper.fetchTodayUsdtInfoFuture(todayDateStr));

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                todayBuyInfoFuture, todaySellInfoFuture,
                todayMerchantPayAmountFuture, todayMerchantPayTransNumFuture,
                todayMerchantPayCommissionFuture,
                todayMerchantWithdrawAmountFuture, todayMerchantWithdrawTransNumFuture,
                todayMerchantWithdrawCommissionFuture,
                todaySellInfoTotalFuture, todayBuyInfoTotalFuture
        );

        allFutures.get();

        todayOrderOverview.setTodayMerchantPayAmount(todayMerchantPayAmountFuture.get());
        todayOrderOverview.setTodayMerchantPayTransNum(todayMerchantPayTransNumFuture.get());
        todayOrderOverview.setTodayMerchantWithdrawAmount(todayMerchantWithdrawAmountFuture.get());
        todayOrderOverview.setTodayMerchantWithdrawTransNum(todayMerchantWithdrawTransNumFuture.get());
        todayOrderOverview.setTodayPayAmount(todayBuyInfoFuture.get().getActualAmount());
        todayOrderOverview.setTodayPayCommission(todayBuyInfoFuture.get().getTotalCost());
        todayOrderOverview.setTodayPayFinishNum(todayBuyInfoFuture.get().getTotalNum());
        todayOrderOverview.setTodayUsdtAmount(todayUsdtInfoFuture.get().getActualAmount());
        todayOrderOverview.setTodayPayTotalNum(todayBuyInfoTotalFuture.get());
        todayOrderOverview.setTodayWithdrawTotalNum(todaySellInfoTotalFuture.get());
        todayOrderOverview.setTodayWithdrawAmount(todaySellInfoFuture.get().getActualAmount());
        todayOrderOverview.setTodayWithdrawCommission(todaySellInfoFuture.get().getTotalCost());
        todayOrderOverview.setTodayWithdrawFinishNum(todaySellInfoFuture.get().getTotalNum());
        // 计算成功率
        BigDecimal paySuccessRate = BigDecimal.ZERO;
        BigDecimal withdrawSuccessRate = BigDecimal.ZERO;
        if (Objects.nonNull(todayOrderOverview.getTodayPayTotalNum()) && todayOrderOverview.getTodayPayTotalNum() > 0
                && Objects.nonNull(todayOrderOverview.getTodayPayFinishNum()) && todayOrderOverview.getTodayPayFinishNum() > 0
        ) {
            paySuccessRate = BigDecimal.valueOf(todayOrderOverview.getTodayPayTotalNum()).divide(new BigDecimal(todayOrderOverview.getTodayPayFinishNum())).setScale(4, BigDecimal.ROUND_HALF_UP);
        }

        if (Objects.nonNull(todayOrderOverview.getTodayWithdrawTotalNum()) && todayOrderOverview.getTodayWithdrawTotalNum() > 0
                && Objects.nonNull(todayOrderOverview.getTodayWithdrawFinishNum()) && todayOrderOverview.getTodayWithdrawFinishNum() > 0
        ) {
            withdrawSuccessRate = BigDecimal.valueOf(todayOrderOverview.getTodayWithdrawTotalNum()).divide(new BigDecimal(todayOrderOverview.getTodayWithdrawFinishNum())).setScale(4, BigDecimal.ROUND_HALF_UP);
        }
        todayOrderOverview.setTodayPaySuccessRate(paySuccessRate);
        todayOrderOverview.setTodayWithdrawSuccessRate(withdrawSuccessRate);
        return todayOrderOverview;
    }

    @Override
    public List<MerchantLastOrderWarnDTO> getLatestOrderTime() {
        List<MerchantLastOrderWarnDTO> resultList = new ArrayList<>();
        // 获取阈值
        TradeConfig tradeConfig = tradeConfigService.getById(1);
        Integer limitHours = tradeConfig.getMerchantOrderUncreatedTime();
        List<LastOrderWarnDTO> collectLastOrderCreditTime = merchantCollectOrdersMapper.getCollectLastOrderCreditTime();
        HashSet<String> collectOvertime = new HashSet<>();
        for (LastOrderWarnDTO lastOrderWarnDTO : collectLastOrderCreditTime) {
            check(lastOrderWarnDTO, limitHours, collectOvertime);
        }
        // 获取代付最后一笔订单时间，根据商户分组
        List<LastOrderWarnDTO> paymentLastOrderCreditTime = merchantPaymentOrdersMapper.getPaymentLastOrderCreditTime();
        HashSet<String> paymentOvertime = new HashSet<>();
        for (LastOrderWarnDTO lastOrderWarnDTO : paymentLastOrderCreditTime) {
            check(lastOrderWarnDTO, limitHours, paymentOvertime);
        }
        HashSet<String> result = new HashSet<>(collectOvertime);
        result.retainAll(paymentOvertime);
        for (String merchantName : result) {
            MerchantLastOrderWarnDTO dto = new MerchantLastOrderWarnDTO();
            // 获取代收最后一笔订单时间，根据商户分组
            dto.setThreshold(limitHours);
            dto.setMerchantName(merchantName);
            dto.setWarn(true);
            resultList.add(dto);
        }
        return resultList;
    }

    private void check(LastOrderWarnDTO lastOrderWarnDTO, int limit, Set<String> set){
        String diff = DurationCalculatorUtil.secondsBetween(lastOrderWarnDTO.getLastOrderCreateTime(), LocalDateTime.now(ZoneId.systemDefault()));
        int diffHour = (Integer.parseInt(diff) / 60 / 60);
        if(ObjectUtils.isNotEmpty(lastOrderWarnDTO)
                && ObjectUtils.isNotEmpty(lastOrderWarnDTO.getLastOrderCreateTime())
                && diffHour >= limit
        ){
            set.add(lastOrderWarnDTO.getMerchantName());
        }
    }


}

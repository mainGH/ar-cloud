package org.ar.manager.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mysql.cj.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.core.result.ResultCode;
import org.ar.common.core.utils.AssertUtil;
import org.ar.common.mybatis.util.PageUtils;
import org.ar.common.pay.dto.MemberOrderOverviewDTO;
import org.ar.common.pay.dto.MerchantOrderOverviewDTO;
import org.ar.common.pay.dto.TodayOrderOverviewDTO;
import org.ar.common.pay.dto.UserAuthDTO;
import org.ar.common.pay.req.CommonDateLimitReq;
import org.ar.common.pay.req.MemberInfoIdReq;
import org.ar.common.web.utils.UserContext;
import org.ar.manager.entity.BiMerchantWithdrawOrderDaily;
import org.ar.manager.entity.MerchantInfo;
import org.ar.manager.mapper.ManagerMerchantInfoMapper;
import org.ar.manager.req.MerchantDailyReportReq;
import org.ar.manager.req.MerchantInfoReq;
import org.ar.manager.service.*;
import org.ar.manager.vo.MerchantInfoVo;
import org.ar.manager.vo.MerchantNameListVo;

import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.CompletableFuture;


@Service
@RequiredArgsConstructor
public class MerchantInfoServiceImpl extends ServiceImpl<ManagerMerchantInfoMapper, MerchantInfo> implements IMerchantInfoService {
    private final PasswordEncoder passwordEncoder;

//    private final AdminMapStruct adminMapStruct;
//    private final ICollectionOrderService collectionOrderService;
//    private final IPaymentOrderService paymentOrderService;
    private final IBiMerchantPayOrderDailyService iBiMerchantPayOrderDailyService;
    private final IBiMerchantWithdrawOrderDailyService iBiMerchantWithdrawOrderDailyService;




    @Override
    public PageReturn<MerchantInfo> listPage(MerchantInfoReq req) {
        Page<MerchantInfo> page = new Page<>();
        page.setCurrent(req.getPageNo());
        page.setSize(req.getPageSize());
        LambdaQueryChainWrapper<MerchantInfo> lambdaQuery = lambdaQuery();
        if (!com.alibaba.nacos.api.utils.StringUtils.isBlank(req.getCode())) {
            lambdaQuery.eq(MerchantInfo::getCode, req.getCode());
        }
        if (!com.alibaba.nacos.api.utils.StringUtils.isBlank(req.getUsername())) {
            lambdaQuery.eq(MerchantInfo::getUsername, req.getUsername());
        }
        baseMapper.selectPage(page, lambdaQuery.getWrapper());
        List<MerchantInfo> records = page.getRecords();
        return PageUtils.flush(page, records);
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
        MerchantInfo merchantInfo = lambdaQuery().eq(MerchantInfo::getCode, code).one();
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
    public MerchantInfoVo currentMerchantInfo() {
        Long currentUserId = UserContext.getCurrentUserId();
        AssertUtil.notEmpty(currentUserId, ResultCode.RELOGIN);
        MerchantInfo merchantInfo = userDetail(currentUserId);
        //MerchantInfo merchantInfo = lambdaQuery().eq(MerchantInfo::getId, currentUserId).one();
        AssertUtil.notEmpty(merchantInfo, ResultCode.USERNAME_OR_PASSWORD_ERROR);
        // 查询绑定的菜单


        MerchantInfoVo merchantInfoVo = new MerchantInfoVo();
        BeanUtils.copyProperties(merchantInfo, merchantInfoVo);




        return merchantInfoVo;
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
        UserAuthDTO userAuthDTO = this.baseMapper.getByUsername(username);
        return userAuthDTO;
    }

    @Override
    @SneakyThrows
    public RestResult<MerchantOrderOverviewDTO> getMerchantOrderOverview(MerchantDailyReportReq req) {
        // 获取时间段内代收订单交易额、订单数、手续费
        CompletableFuture<MerchantOrderOverviewDTO> merchantPayAmountFuture = CompletableFuture.supplyAsync(() -> iBiMerchantPayOrderDailyService.getMerchantOrderOverview(req));
        // 获取时间段内代付订单交易额、订单数、手续费
        CompletableFuture<MerchantOrderOverviewDTO> merchantWithdrawAmountFuture = CompletableFuture.supplyAsync(() -> iBiMerchantWithdrawOrderDailyService.getMerchantOrderOverview(req));

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(merchantPayAmountFuture, merchantWithdrawAmountFuture);
        allFutures.get();
        MerchantOrderOverviewDTO payDto = merchantPayAmountFuture.get();
        MerchantOrderOverviewDTO withdrawDto = merchantWithdrawAmountFuture.get();
        // 合并
        final CopyOptions copyOptions = CopyOptions.create();
        copyOptions.setIgnoreNullValue(true);
        BeanUtil.copyProperties(payDto, withdrawDto, copyOptions);
        // 代收订单交易额
        BigDecimal merchantPayAmount = withdrawDto.getMerchantPayAmount();
        // 代收订单数
        long merchantPayTransNum = withdrawDto.getMerchantPayTransNum();
        // 代付订单数
        long merchantWithdrawTransNum = withdrawDto.getMerchantWithdrawTransNum();
        // 代付订单交易额
        BigDecimal merchantWithdrawAmount = withdrawDto.getMerchantWithdrawAmount();

        // 计算平均值和差异值
        // 代收每笔平均交易额
        BigDecimal payAvgTransAmount = BigDecimal.ZERO;
        // 代付每笔平均交易额
        BigDecimal withdrawAvgTransAmount = BigDecimal.ZERO;
        // 对比值平均交易额
        BigDecimal diffAvgAmount;

        if (merchantPayAmount.compareTo(BigDecimal.ZERO) != 0 && merchantPayTransNum != 0) {
            payAvgTransAmount = merchantPayAmount.divide(new BigDecimal(merchantPayTransNum), 2, RoundingMode.HALF_UP);
        }
        if (merchantWithdrawAmount.compareTo(BigDecimal.ZERO) != 0 && merchantWithdrawTransNum != 0) {
            withdrawAvgTransAmount = merchantWithdrawAmount.divide(new BigDecimal(merchantWithdrawTransNum), 2, RoundingMode.HALF_UP);
        }

        // 交易额差值
        BigDecimal transAmountDiff = merchantPayAmount.subtract(merchantWithdrawAmount);
        // 交易订单数差值
        long transNumDiff = merchantPayTransNum - merchantWithdrawTransNum;
        diffAvgAmount = payAvgTransAmount.subtract(withdrawAvgTransAmount);
        // 手续费
        BigDecimal payFee = withdrawDto.getPayFee();
        BigDecimal withdrawFee = withdrawDto.getWithdrawFee();
        BigDecimal diffFee = payFee.subtract(withdrawFee);
        withdrawDto.setPayAverageAmount(payAvgTransAmount);
        withdrawDto.setWithdrawAverageAmount(withdrawAvgTransAmount);
        withdrawDto.setTransNumDiff(transNumDiff);
        withdrawDto.setTransAmountDiff(transAmountDiff);
        withdrawDto.setAverageAmountDiff(diffAvgAmount);
        withdrawDto.setFeeAmountDiff(diffFee);
        return RestResult.ok(withdrawDto);
    }
}

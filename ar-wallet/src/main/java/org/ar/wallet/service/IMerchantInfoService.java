package org.ar.wallet.service;


import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.common.core.page.PageReturn;
import org.ar.common.pay.dto.*;
import org.ar.common.pay.req.*;
import org.ar.wallet.entity.MerchantInfo;
import org.ar.wallet.vo.AccountChangeVo;
import org.ar.wallet.vo.MerchantNameListVo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;


/**
 * @author
 */
public interface IMerchantInfoService extends IService<MerchantInfo> {


    PageReturn<MerchantInfoListPageDTO> listPage(MerchantInfoListPageReq req) throws ExecutionException, InterruptedException;

    List<MerchantInfo> getAllMerchantByStatus();


    String getMd5KeyByCode(String merchantCode);


    boolean getIp(String code, String addr);

    MerchantInfo getMerchantInfoByCode(String code);

    MerchantInfo getMerchantInfoByCode(String code, String name);




    /*
     * 根据商户号获取md5Key
     * */


    /*
     * 获取商户名称列表
     * */
    List<MerchantNameListVo> getMerchantNameList();


    MerchantInfoDTO currentMerchantInfo(Long userId);


    MerchantInfo userDetail(Long userId);


    UserAuthDTO getByUsername(String username);

    /*
     * 根据商户号查询支付费率和代付费率
     * */
    Map<String, Object> getRateByCode(String merchantCode);

    /**
     * 修改商户密码
     * @param userId
     * @return
     */
    Boolean updateMerchantPwd(Long userId, String password, String passwordTips);

    /**
     * 修改商户提现usdt地址
     * @param userId
     * @param usdtAddress
     * @return
     */
    Boolean updateUsdtAddress(Long userId, String usdtAddress);

    /**
     * 获取商户首页信息
     * @param merchantId
     * @return
     */
    MerchantFrontPageDTO fetchHomePageInfo(Long merchantId, String name) throws Exception;

    MerchantFrontPageDTO fetchHomePageInfo() throws Exception;

    /**
     * 获取代付订单列表
     * @param withdrawOrderReq
     * @return
     */
    PageReturn<WithdrawOrderDTO> fetchWithdrawOrderInfo(WithdrawOrderReq withdrawOrderReq);
    PageReturn<WithdrawOrderExportDTO> fetchWithdrawOrderInfoExport(WithdrawOrderReq withdrawOrderReq);

    Boolean confirmSuccess(Long id);

    Map<Integer, String> fetchOrderStatus();

    Map<Integer, String> orderCallbackStatus();


    MerchantInfo userDetailByCode(String code);

    PageReturn<RechargeOrderDTO> fetchRechargeOrderInfo(RechargeOrderReq rechargeOrderReq);

    PageReturn<RechargeOrderExportDTO> fetchRechargeOrderInfoExport(RechargeOrderReq rechargeOrderReq);

    Boolean rechargeConfirmSuccess(Long id);

    Map<Long, String> getMerchantName();

    Map<String, String> getCurrency();

    OrderOverviewDTO getOrderNumOverview();

    TodayOrderOverviewDTO todayOrderOverview();

    List<MerchantLastOrderWarnDTO> getLatestOrderTime();

}

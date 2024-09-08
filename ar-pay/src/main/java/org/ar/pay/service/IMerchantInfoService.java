package org.ar.pay.service;


import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.common.core.page.PageReturn;
import org.ar.common.pay.dto.UserAuthDTO;
import org.ar.pay.entity.MerchantInfo;
import org.ar.pay.req.MerchantInfoReq;
import org.ar.pay.vo.MerchantInfoVo;
import org.ar.pay.vo.MerchantNameListVo;

import java.util.List;
import java.util.Map;


/**
 * @author
 */
public interface IMerchantInfoService extends IService<MerchantInfo> {
    void createMerchantInfo(MerchantInfo merchantInfo);

    PageReturn<MerchantInfo> listPage(MerchantInfoReq req);

    List<MerchantInfo> getAllMerchantByStatus();


    UserAuthDTO getByUsername(String username);

    boolean getIp(String code, String addr);

    MerchantInfo getMerchantInfoByCode(String code);

    /*
     * 获取当前商户信息
     * */
    MerchantInfoVo currentMerchantInfo();


    MerchantInfo userDetail(Long userId);

    /*
     * 根据商户号获取md5Key
     * */
    String getMd5KeyByCode(String merchantCode);

    /*
     * 获取商户名称列表
     * */
    List<MerchantNameListVo> getMerchantNameList();

    /*
     * 根据商户号查询支付费率和代付费率
     * */
    Map<String, Object> getRateByCode(String merchantCode);
}

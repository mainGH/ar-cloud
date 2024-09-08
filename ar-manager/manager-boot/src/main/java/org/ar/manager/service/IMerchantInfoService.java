package org.ar.manager.service;


import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.MemberOrderOverviewDTO;
import org.ar.common.pay.dto.MerchantOrderOverviewDTO;
import org.ar.common.pay.dto.TodayOrderOverviewDTO;
import org.ar.common.pay.dto.UserAuthDTO;
import org.ar.common.pay.req.CommonDateLimitReq;
import org.ar.common.pay.req.MemberInfoIdReq;
import org.ar.manager.entity.MerchantInfo;
import org.ar.manager.req.MerchantDailyReportReq;
import org.ar.manager.req.MerchantInfoReq;
import org.ar.manager.vo.MerchantInfoVo;
import org.ar.manager.vo.MerchantNameListVo;

import java.util.List;


/**
 * @author
 */
public interface IMerchantInfoService extends IService<MerchantInfo> {


    PageReturn<MerchantInfo> listPage(MerchantInfoReq req);

    List<MerchantInfo> getAllMerchantByStatus();


     String getMd5KeyByCode(String merchantCode);



    boolean getIp(String code, String addr);

    MerchantInfo getMerchantInfoByCode(String code);




    /*
     * 根据商户号获取md5Key
     * */


    /*
     * 获取商户名称列表
     * */
    List<MerchantNameListVo> getMerchantNameList();


     MerchantInfoVo currentMerchantInfo();


     MerchantInfo userDetail(Long userId);


     UserAuthDTO getByUsername(String username);
    RestResult<MerchantOrderOverviewDTO>  getMerchantOrderOverview(MerchantDailyReportReq req);
}

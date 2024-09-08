package org.ar.wallet.service;


import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.common.core.page.PageReturn;
import org.ar.common.pay.dto.MerchantInfoReportDTO;
import org.ar.wallet.entity.MerchantInfo;
import org.ar.common.pay.req.MerchantInfoReq;



/**
 * @author
 */
public interface IReportMerchantInfoService extends IService<MerchantInfo> {


    PageReturn<MerchantInfoReportDTO> listDayPage(MerchantInfoReq req);


}

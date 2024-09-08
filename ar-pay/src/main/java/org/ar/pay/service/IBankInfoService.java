package org.ar.pay.service;

import org.ar.common.core.page.PageReturn;
import org.ar.pay.entity.BankInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.pay.entity.PayCard;
import org.ar.pay.req.BankInfoReq;
import org.ar.pay.req.PayCardReq;

/**
* @author 
*/
    public interface IBankInfoService extends IService<BankInfo> {
     PageReturn<BankInfo> listPage(BankInfoReq req);

    }

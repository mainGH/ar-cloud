package org.ar.pay.service;


import org.ar.pay.entity.PayConfig;
import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.pay.entity.PaymentOrder;
import org.ar.pay.req.PayConfigReq;
import org.ar.pay.req.PaymentOrderReq;
import org.ar.common.core.page.PageReturn;

import java.util.List;

/**
* @author 
*/
    public interface IPayConfigService extends IService<PayConfig> {

        PageReturn<PayConfig> listPage(PayConfigReq req);

    List<PayConfig> getPayConfigByCondtion(PayConfigReq req);


    List<PayConfig> getPaymentConfigByCondtion(PayConfigReq req);

    }

package org.ar.pay.service;


import org.ar.common.core.page.PageReturn;
import org.ar.pay.entity.PayCard;
import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.pay.entity.PayConfig;
import org.ar.pay.req.PayCardReq;

/**
    * @author
    */
    public interface IPayCardService extends IService<PayCard> {

     PageReturn<PayCard> listPage(PayCardReq req);

}

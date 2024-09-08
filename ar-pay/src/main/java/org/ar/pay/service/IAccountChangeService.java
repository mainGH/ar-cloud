package org.ar.pay.service;


import org.ar.pay.entity.AccountChange;
import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.pay.entity.CollectionOrder;
import org.ar.pay.req.AccountChangeReq;
import org.ar.common.core.page.PageReturn;

/**
* @author 
*/
    public interface IAccountChangeService extends IService<AccountChange> {

     PageReturn<AccountChange> listPage(AccountChangeReq req) ;

    }

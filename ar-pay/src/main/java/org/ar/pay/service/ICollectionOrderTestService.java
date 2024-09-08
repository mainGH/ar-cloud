package org.ar.pay.service;


import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.common.core.page.PageReturn;
import org.ar.pay.entity.CollectionOrder;
import org.ar.pay.entity.CollectionOrderTest;
import org.ar.pay.req.CollectionOrderReq;

import java.util.List;

/**
* @author 
*/
    public interface ICollectionOrderTestService extends IService<CollectionOrderTest> {

     PageReturn<CollectionOrderTest> listPage(CollectionOrderReq req) ;

     List<CollectionOrderTest> getCollectionOrderBySatus();

     boolean updateOrderByOrderNo(String orderId);

    }

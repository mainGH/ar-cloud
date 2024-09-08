package org.ar.wallet.service;



import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.common.core.page.PageReturn;

import org.ar.common.pay.dto.CollectionOrderDTO;

import org.ar.wallet.entity.CollectionOrder;
import org.ar.common.pay.req.CollectionOrderReq;


/**
 * @author
 */
public interface IReportCollectionOrderService extends IService<CollectionOrder> {

    PageReturn<CollectionOrderDTO> listDayPage(CollectionOrderReq req);

    PageReturn<CollectionOrderDTO> listMothPage(CollectionOrderReq req);


    PageReturn<CollectionOrderDTO> listDayPageTotal(CollectionOrderReq req);

    PageReturn<CollectionOrderDTO> listMothPageTotal(CollectionOrderReq req);





}

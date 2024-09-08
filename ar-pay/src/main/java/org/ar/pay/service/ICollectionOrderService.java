package org.ar.pay.service;


import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.pay.entity.CollectionOrder;
import org.ar.pay.req.CollectionOrderReq;
import org.ar.pay.vo.CollectionOrderInfoVo;
import org.ar.pay.vo.CollectionOrderListVo;

import java.util.List;

/**
 * @author
 */
public interface ICollectionOrderService extends IService<CollectionOrder> {

    PageReturn<CollectionOrderListVo> listPage(CollectionOrderReq req);

    List<CollectionOrder> getCollectionOrderBySatus();

    boolean updateOrderByOrderNo(String merchantCode, String platformOrder, String realAmount, String payType);

    //CollectionOrder getCollectionOrderBySatus(String code);

    /*
     * 手动回调
     * */
    RestResult manualCallback(String merchantOrder);

    /*
     * 查询代收订单详情
     * */
    RestResult<CollectionOrderInfoVo> getCollectionOrderInfoByOrderNo(String merchantOrder);


    /*
     * 查询下拉列表数据(币种,支付类型)
     * */
    RestResult selectList();


    /*
    * 根据id更改订单已发送状态
    * */
    int updateOrderSendById(String  id);

}

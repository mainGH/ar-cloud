package org.ar.pay.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.ar.common.core.page.PageReturn;
import org.ar.pay.entity.CollectionOrder;
import org.ar.pay.entity.CollectionOrderTest;
import org.ar.pay.mapper.CollectionOrderMapper;
import org.ar.pay.mapper.CollectionOrderTestMapper;
import org.ar.pay.req.CollectionOrderReq;
import org.ar.pay.service.ICollectionOrderService;
import org.ar.pay.service.ICollectionOrderTestService;
import org.ar.pay.util.PageUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author 
*/
    @Service
    public class CollectionOrderTestServiceImpl extends ServiceImpl<CollectionOrderTestMapper, CollectionOrderTest> implements ICollectionOrderTestService {

    @Override
    public PageReturn<CollectionOrderTest> listPage(CollectionOrderReq req) {
        Page<CollectionOrderTest> page = new Page<>();
        page.setCurrent(req.getPageNo());
        page.setSize(req.getPageSize());
        LambdaQueryChainWrapper<CollectionOrderTest> lambdaQuery = lambdaQuery();

        baseMapper.selectPage(page, lambdaQuery.getWrapper());

        List<CollectionOrderTest> records = page.getRecords();
        return PageUtils.flush(page, records);
    }

    @Override
    public List<CollectionOrderTest> getCollectionOrderBySatus() {

        LambdaQueryChainWrapper<CollectionOrderTest> lambdaQuery = lambdaQuery();
        List<CollectionOrderTest> list = lambdaQuery().eq(CollectionOrderTest::getOrderStatus, "2").list();
          return list;
    }

    @Override
    public boolean updateOrderByOrderNo(String orderId) {
        LambdaQueryChainWrapper<CollectionOrderTest> lambdaQuery = lambdaQuery();
        CollectionOrderTest  collectionOrder = lambdaQuery().eq(CollectionOrderTest::getPlatformOrder, orderId).one();
        if(collectionOrder != null){
            collectionOrder.setOrderStatus("2");
            return this.saveOrUpdate(collectionOrder);
        }
        return false;
    }

    }

package org.ar.pay.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import org.ar.common.core.page.PageReturn;
import org.ar.pay.entity.PayConfig;

import org.ar.pay.mapper.PayConfigMapper;
import org.ar.pay.req.PayConfigReq;

import org.ar.pay.service.IPayConfigService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.ar.pay.util.PageUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author 
*/
    @Service
    public class PayConfigServiceImpl extends ServiceImpl<PayConfigMapper, PayConfig> implements IPayConfigService {

    @Override
    public PageReturn<PayConfig> listPage(PayConfigReq req) {
        Page<PayConfig> page = new Page<>();
        page.setCurrent(req.getPageNo());
        page.setSize(req.getPageSize());
        LambdaQueryChainWrapper<PayConfig> lambdaQuery = lambdaQuery();

        baseMapper.selectPage(page, lambdaQuery.getWrapper());

        List<PayConfig> records = page.getRecords();
        return PageUtils.flush(page, records);
    }

  public  List<PayConfig> getPayConfigByCondtion(PayConfigReq req){
        LambdaQueryChainWrapper<PayConfig> lambdaQuery = lambdaQuery();
        lambdaQuery.eq(PayConfig ::getPayType,req.getPayType()).eq(PayConfig ::getCurrency,req.getCurrency()).eq(PayConfig::getCountry,req.getCountry()).eq(PayConfig::getStatus,req.getStatus());
        List<PayConfig> list =  baseMapper.selectList(lambdaQuery.getWrapper());
        return list;
    }


    public  List<PayConfig> getPaymentConfigByCondtion(PayConfigReq req){
        LambdaQueryChainWrapper<PayConfig> lambdaQuery = lambdaQuery();


        lambdaQuery.eq(PayConfig ::getCurrency,req.getCurrency()).eq(PayConfig::getCountry,req.getCountry()).eq(PayConfig::getStatus,req.getStatus()).eq(PayConfig::getChannelType,"2");
        List<PayConfig> list =  baseMapper.selectList(lambdaQuery.getWrapper());
        return list;
    }


}

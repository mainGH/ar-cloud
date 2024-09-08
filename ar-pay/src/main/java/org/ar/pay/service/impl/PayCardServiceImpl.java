package org.ar.pay.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import org.ar.common.core.page.PageReturn;
import org.ar.pay.entity.PayCard;

import org.ar.pay.mapper.PayCardMapper;
import org.ar.pay.req.PayCardReq;

import org.ar.pay.service.IPayCardService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.ar.pay.util.PageUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author 
*/
    @Service
    public class PayCardServiceImpl extends ServiceImpl<PayCardMapper, PayCard> implements IPayCardService {

    @Override
    public PageReturn<PayCard> listPage(PayCardReq req) {
        Page<PayCard> page = new Page<>();
        page.setCurrent(req.getPageNo());
        page.setSize(req.getPageSize());
        LambdaQueryChainWrapper<PayCard> lambdaQuery = lambdaQuery();

        baseMapper.selectPage(page, lambdaQuery.getWrapper());

        List<PayCard> records = page.getRecords();
        return PageUtils.flush(page, records);
    }

    }

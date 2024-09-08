package org.ar.pay.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import org.ar.common.core.page.PageReturn;
import org.ar.pay.entity.AccountChange;

import org.ar.pay.mapper.AccountChangeMapper;
import org.ar.pay.req.AccountChangeReq;

import org.ar.pay.service.IAccountChangeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.ar.pay.util.PageUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author 
*/
    @Service
    public class AccountChangeServiceImpl extends ServiceImpl<AccountChangeMapper, AccountChange> implements IAccountChangeService {

        @Override
        public PageReturn<AccountChange> listPage(AccountChangeReq req) {
            Page<AccountChange> page = new Page<>();
            page.setCurrent(req.getPageNo());
            page.setSize(req.getPageSize());
            LambdaQueryChainWrapper<AccountChange> lambdaQuery = lambdaQuery();
            baseMapper.selectPage(page, lambdaQuery.getWrapper());
            List<AccountChange> records = page.getRecords();
            return PageUtils.flush(page, records);
        }

    }

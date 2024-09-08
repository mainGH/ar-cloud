package org.ar.pay.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.ar.common.core.page.PageReturn;
import org.ar.pay.entity.BankInfo;

import org.ar.pay.mapper.BankInfoMapper;
import org.ar.pay.req.BankInfoReq;

import org.ar.pay.service.IBankInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.ar.pay.util.PageUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author 
*/
@Service
    public class BankInfoServiceImpl extends ServiceImpl<BankInfoMapper, BankInfo> implements IBankInfoService {

    @Override
    public PageReturn<BankInfo> listPage(BankInfoReq req) {
        Page<BankInfo> page = new Page<>();
        page.setCurrent(req.getPageNo());
        page.setSize(req.getPageSize());
        LambdaQueryChainWrapper<BankInfo> lambdaQuery = lambdaQuery();

        baseMapper.selectPage(page, lambdaQuery.getWrapper());

        List<BankInfo> records = page.getRecords();
        return PageUtils.flush(page, records);
    }

    }

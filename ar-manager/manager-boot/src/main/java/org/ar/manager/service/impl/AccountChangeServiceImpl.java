//package org.ar.manager.service.impl;
//
//import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
//import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
//import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
//import org.ar.common.core.page.PageReturn;
//import org.ar.common.mybatis.util.PageUtils;
//import org.ar.manager.entity.AccountChange;
//import org.ar.manager.entity.MerchantInfo;
//import org.ar.manager.mapper.AccountChangeMapper;
//import org.ar.manager.req.MerchantInfoReq;
//import org.ar.manager.service.IAccountChangeService;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//
///**
// * @author
// */
//@Service
//public class AccountChangeServiceImpl extends ServiceImpl<AccountChangeMapper, AccountChange> implements IAccountChangeService {
//
//    @Override
//    public PageReturn<MerchantInfo> listPage(MerchantInfoReq req) {
//        Page<MerchantInfo> page = new Page<>();
//        page.setCurrent(req.getPageNo());
//        page.setSize(req.getPageSize());
//        LambdaQueryChainWrapper<MerchantInfo> lambdaQuery = lambdaQuery();
//        if (!com.alibaba.nacos.api.utils.StringUtils.isBlank(req.getCode())) {
//            lambdaQuery.eq(MerchantInfo::getCode, req.getCode());
//        }
//        if (!com.alibaba.nacos.api.utils.StringUtils.isBlank(req.getUsername())) {
//            lambdaQuery.eq(MerchantInfo::getUsername, req.getUsername());
//        }
//        baseMapper.selectPage(page, lambdaQuery.getWrapper());
//        List<MerchantInfo> records = page.getRecords();
//        return PageUtils.flush(page, records);
//    }
//
//}

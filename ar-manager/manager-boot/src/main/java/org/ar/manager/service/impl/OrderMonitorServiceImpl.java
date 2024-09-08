package org.ar.manager.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.ar.common.pay.req.OrderMonitorReq;
import org.ar.manager.entity.MerchantInfo;
import org.ar.manager.entity.OrderMonitor;
import org.ar.manager.mapper.OrderMonitorMapper;
import org.ar.manager.service.IOrderMonitorService;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 
 * @since 2024-04-04
 */
@RequiredArgsConstructor
@Service
public class OrderMonitorServiceImpl extends ServiceImpl<OrderMonitorMapper, OrderMonitor> implements IOrderMonitorService {

    @Override
    public List<OrderMonitor> getOrderMonitorList(OrderMonitorReq req) {
        LambdaQueryChainWrapper<OrderMonitor> lambdaQuery = lambdaQuery();
        List<OrderMonitor> list = lambdaQuery().like(OrderMonitor::getStatisticalTime, req.getCreateTime()).eq(OrderMonitor::getCode,req.getCode()).list();
        return list;
    }
    @Override
    public List<OrderMonitor> getAllOrderMonitorListByDay(OrderMonitorReq req){
        LambdaQueryChainWrapper<OrderMonitor> lambdaQuery = lambdaQuery();
        List<OrderMonitor> list = lambdaQuery().like(OrderMonitor::getStatisticalTime, req.getCreateTime()).list();
        return list;
    }




}

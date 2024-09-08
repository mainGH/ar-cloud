package org.ar.manager.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.lang3.StringUtils;
import org.ar.common.core.constant.GlobalConstants;
import org.ar.common.core.page.PageReturn;
import org.ar.manager.entity.SysLog;
import org.ar.manager.entity.SysUser;
import org.ar.manager.mapper.SysLogMapper;
import org.ar.manager.req.SysLogReq;
import org.ar.manager.service.ISysLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.ar.manager.util.PageUtils;
import org.ar.manager.vo.SysLogVo;
import org.ar.manager.vo.SysUserVO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author 
*/
    @Service
    public class SysLogServiceImpl extends ServiceImpl<SysLogMapper, SysLog> implements ISysLogService {

       @Override
       public  PageReturn<SysLog> listPage(SysLogReq req){

           Page<SysLog> page = new Page<>();
           page.setCurrent(req.getPageNo());
           page.setSize(req.getPageSize());
           LambdaQueryChainWrapper<SysLog> lambdaQuery = lambdaQuery();
           lambdaQuery.orderByDesc(SysLog::getCreateTime);
           if (!StringUtils.isEmpty(req.getStartTime())) {
               lambdaQuery.ge(SysLog::getCreateTime, req.getStartTime());
           }
           if (!StringUtils.isEmpty(req.getEndTime())) {
               lambdaQuery.le(SysLog::getCreateTime, req.getEndTime());
           }
           if (!StringUtils.isEmpty(req.getModule())) {
               lambdaQuery.eq(SysLog::getModule, req.getModule());
           }
           if (!StringUtils.isEmpty(req.getKeyStr())) {
               lambdaQuery.or().like(SysLog::getPath,req.getKeyStr());
           }

           baseMapper.selectPage(page, lambdaQuery.getWrapper());
           List<SysLog> records = page.getRecords();
           return PageUtils.flush(page, records);

        }

    }

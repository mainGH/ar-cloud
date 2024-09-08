package org.ar.manager.service;


import org.ar.common.core.page.PageReturn;

import org.ar.common.core.result.RestResult;
import org.ar.manager.entity.SysWhite;
import com.baomidou.mybatisplus.extension.service.IService;

import org.ar.manager.req.SysWhiteReq;


/**
* @author 
*/
    public interface ISysWhiteService extends IService<SysWhite> {


     PageReturn<SysWhite> listPage(SysWhiteReq req);

    boolean getIp(String addr, String clientCode);

    boolean del(String id);

    /**
     * 添加ip白名单
     * @param req req
     * @return RestResult
     */
    RestResult<?> saveDeduplication(SysWhiteReq req);
}

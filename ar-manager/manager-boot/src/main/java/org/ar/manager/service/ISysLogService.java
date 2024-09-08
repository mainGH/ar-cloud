package org.ar.manager.service;

import org.ar.common.core.page.PageReturn;
import org.ar.manager.entity.SysLog;
import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.manager.req.RoleListPageReq;
import org.ar.manager.req.SysLogReq;
import org.ar.manager.vo.SysLogVo;
import org.ar.manager.vo.SysRoleVO;

/**
* @author 
*/
    public interface ISysLogService extends IService<SysLog> {

    PageReturn<SysLog> listPage(SysLogReq req);

    }

package org.ar.pay.service;



import com.baomidou.mybatisplus.extension.service.IService;

import org.ar.common.core.page.PageReturn;
import org.ar.pay.entity.SysRole;
import org.ar.pay.req.RoleListPageReq;
import org.ar.pay.req.SaveSysRoleReq;
import org.ar.pay.vo.SysRoleSelectVO;
import org.ar.pay.vo.SysRoleVO;

import java.util.List;


public interface ISysRoleService extends IService<SysRole> {

    /**
     * 角色列表select
     *
     * @return
     */
    List<SysRoleSelectVO> roleSelect();

    /**
     * 列表分页
     * @param req
     * @return
     */
    PageReturn<SysRoleVO> listPage(RoleListPageReq req);

    /**
     * 创建角色
     * @param req
     */
    void createRole(SaveSysRoleReq req);

    /**
     * 更新角色
     * @param req
     */
    void updateRole(SaveSysRoleReq req);

    /**
     * 删除角色
     */
    void deletes(List<Long> ids);


    /**
     * 更新角色状态
     * @param id
     * @param status
     */
    void updateStatus(Long id, int status);
}

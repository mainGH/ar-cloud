package org.ar.wallet.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.common.core.page.PageReturn;
import org.ar.common.pay.dto.MemberGroupListPageDTO;
import org.ar.common.pay.req.MemberGroupListPageReq;
import org.ar.wallet.entity.MemberGroup;
import org.ar.wallet.entity.MemberInfo;

/**
* @author
*/
    public interface IMemberGroupService extends IService<MemberGroup> {

     PageReturn<MemberGroupListPageDTO> listPage(MemberGroupListPageReq req);


    /**
     * 根据会员的交易数据 进行会员分组
     *
     * @param memberInfo
     * @return {@link MemberInfo}
     */
    MemberInfo determineMemberGroup(MemberInfo memberInfo);


    /**
     * 根据分组id获取权限列表
     *
     * @param id
     * @return {@link String}
     */
    String getAuthListById(Long id);

}

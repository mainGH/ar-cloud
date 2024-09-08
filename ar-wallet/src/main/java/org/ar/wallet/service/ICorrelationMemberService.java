package org.ar.wallet.service;

import org.ar.common.core.page.PageReturn;
import org.ar.common.pay.dto.CorrelationMemberDTO;
import org.ar.common.pay.req.MemberBlackReq;
import org.ar.wallet.entity.CorrelationMember;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 关联会员信息 服务类
 * </p>
 *
 * @author 
 * @since 2024-03-30
 */
public interface ICorrelationMemberService extends IService<CorrelationMember> {

    PageReturn<CorrelationMemberDTO> listPage(MemberBlackReq req);
}

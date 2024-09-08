package org.ar.wallet.service;

import org.ar.common.core.page.PageReturn;
import org.ar.common.pay.dto.MemberLevelChangeDTO;
import org.ar.common.pay.dto.MemberLevelConfigDTO;
import org.ar.wallet.entity.MemberLevelChange;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 会员等级变化记录 服务类
 * </p>
 *
 * @author 
 * @since 2024-04-10
 */
public interface IMemberLevelChangeService extends IService<MemberLevelChange> {

    List<MemberLevelChangeDTO> listPage(String memberId);
}

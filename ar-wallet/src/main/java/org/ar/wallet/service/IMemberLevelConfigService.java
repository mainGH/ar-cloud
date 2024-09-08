package org.ar.wallet.service;

import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.MemberLevelConfigDTO;
import org.ar.common.pay.req.MemberManualLogsReq;
import org.ar.wallet.entity.MemberLevelConfig;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.boot.CommandLineRunner;

/**
 * <p>
 * 会员等级配置 服务类
 * </p>
 *
 * @author 
 * @since 2024-04-09
 */
public interface IMemberLevelConfigService extends IService<MemberLevelConfig>, CommandLineRunner {

    PageReturn<MemberLevelConfigDTO> listPage(MemberManualLogsReq req);

    RestResult updateInfo(MemberLevelConfigDTO req);
}

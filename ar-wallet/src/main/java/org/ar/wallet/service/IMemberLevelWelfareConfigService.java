package org.ar.wallet.service;

import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.MemberLevelWelfareConfigDTO;
import org.ar.common.pay.req.MemberManualLogsReq;
import org.ar.wallet.entity.MemberLevelWelfareConfig;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.boot.CommandLineRunner;

/**
 * <p>
 * 会员等级福利配置 服务类
 * </p>
 *
 * @author 
 * @since 2024-04-10
 */
public interface IMemberLevelWelfareConfigService extends IService<MemberLevelWelfareConfig>, CommandLineRunner {

    PageReturn<MemberLevelWelfareConfigDTO> listPage(MemberManualLogsReq req);

    RestResult updateInfo(MemberLevelWelfareConfigDTO req);

    /**
     * 根据等级查询对应福利
     *
     * @param level
     * @return
     */
    MemberLevelWelfareConfig getWelfareByLevel(Integer level);
}

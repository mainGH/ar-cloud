package org.ar.manager.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.manager.entity.SysMessage;
import org.ar.manager.req.SysMessageIdReq;
import org.ar.manager.req.SysMessageReq;
import org.ar.manager.req.SysMessageSendReq;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author admin
 * @since 2024-05-06
 */
public interface ISysMessageService extends IService<SysMessage> {
    PageReturn<SysMessage> listPage(SysMessageReq req);

    RestResult deleted(SysMessageIdReq req);

    RestResult read(SysMessageIdReq req);

    RestResult sendMessage(SysMessageSendReq sysMessage);

    Integer unReadMessageCount(String userId);

}

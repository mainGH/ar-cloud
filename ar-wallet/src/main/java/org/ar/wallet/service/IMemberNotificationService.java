package org.ar.wallet.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.wallet.entity.MemberInfo;
import org.ar.wallet.entity.MemberNotification;
import org.ar.wallet.req.BindEmailReq;
import org.ar.wallet.req.VerifySmsCodeReq;

/**
 * @author
 */
public interface IMemberNotificationService extends IService<MemberNotification> {

    /**
     * 根据会员id获取通知数量
     *
     * @param memberId
     * @return {@link Integer}
     */
    Integer getNotificationCountByMemberId(String memberId);

    /**
     * 校验短信验证码
     *
     * @param verifySmsCodeReq
     * @return {@link Boolean}
     */
    Boolean validateSmsCode(VerifySmsCodeReq verifySmsCodeReq);

    /**
     * 校验邮箱验证码
     *
     * @param bindEmailReq
     * @param memberInfo
     * @return {@link Boolean}
     */
    Boolean validateEmailCode(BindEmailReq bindEmailReq, MemberInfo memberInfo);

    Boolean generateNotifications(MemberNotification memberNotification);
}

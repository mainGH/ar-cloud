package org.ar.wallet.service;


import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.common.core.page.PageReturn;
import org.ar.common.pay.dto.UserVerificationCodeslistPageDTO;
import org.ar.wallet.entity.UserVerificationCodes;
import org.ar.common.pay.req.UserTextMessageReq;

/**
 * <p>
 * 用户验证码记录表 服务类
 * </p>
 *
 * @author
 * @since 2024-01-20
 */
public interface IUserVerificationCodesService extends IService<UserVerificationCodes> {

    PageReturn<UserVerificationCodeslistPageDTO> listPage(UserTextMessageReq userTextMessageReq);
}

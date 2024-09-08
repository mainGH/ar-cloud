package org.ar.wallet.service;

import org.ar.common.core.page.PageReturn;
import org.ar.common.pay.req.AccountChangeReq;
import org.ar.wallet.entity.AccountChange;
import com.baomidou.mybatisplus.extension.service.IService;

import org.ar.wallet.vo.AccountChangeVo;

import java.util.List;
import java.util.Map;

/**
 * @author
 */
public interface IAccountChangeService extends IService<AccountChange> {

    /**
     * 查询商户账变记录
     * @param accountChangeReq
     * @return
     */
    PageReturn<AccountChangeVo> queryAccountChangeList(AccountChangeReq accountChangeReq);

    Map<Integer, String> fetchAccountType();


     AccountChangeVo queryTotal(AccountChangeReq req);

}

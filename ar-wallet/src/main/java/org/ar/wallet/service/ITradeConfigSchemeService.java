package org.ar.wallet.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.common.core.page.PageReturn;
import org.ar.common.pay.dto.TradeConfigSchemeDTO;
import org.ar.common.pay.req.TradeConfigSchemeListPageReq;
import org.ar.common.pay.req.TradeConfigSchemeReq;
import org.ar.wallet.entity.TradeConfigScheme;

/**
 * <p>
 * 交易配置方案表 服务类
 * </p>
 *
 * @author
 * @since 2024-03-18
 */
public interface ITradeConfigSchemeService extends IService<TradeConfigScheme> {
    PageReturn<TradeConfigSchemeDTO> listPage(TradeConfigSchemeListPageReq req);
    TradeConfigSchemeDTO getDetail(Long id);

    TradeConfigSchemeDTO updateScheme(TradeConfigSchemeReq req);


    /**
     * 根据标签获取方案配置
     *
     * @param schemeTag
     * @return {@link TradeConfigScheme}
     */
    TradeConfigScheme getSchemeConfigByTag(String schemeTag);
}

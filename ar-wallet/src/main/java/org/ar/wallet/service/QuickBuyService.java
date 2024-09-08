package org.ar.wallet.service;

import org.ar.common.core.result.RestResult;
import org.ar.wallet.req.BuyReq;
import org.ar.wallet.vo.QuickBuyMatchResult;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;

/**
 * 快捷买入服务接口类
 */
public interface QuickBuyService {

    /**
     * 匹配卖出订单
     *
     * @param amount
     * @return
     */
    QuickBuyMatchResult matchSellOrder(BigDecimal amount);

    /**
     * 确认买入
     *
     * @param buyReq
     * @return
     */
    RestResult confirmBuy(BuyReq buyReq, HttpServletRequest request);
}

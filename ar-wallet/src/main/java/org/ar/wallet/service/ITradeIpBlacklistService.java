package org.ar.wallet.service;


import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.TradeIpBlackListPageDTO;
import org.ar.common.pay.req.TradeIpBlackListReq;
import org.ar.wallet.entity.TradIpBlackMessage;
import org.ar.wallet.entity.TradeIpBlacklist;

/**
 * <p>
 * 交易IP黑名单表，用于存储不允许进行交易的IP地址 服务类
 * </p>
 *
 * @author
 * @since 2024-02-21
 */
public interface ITradeIpBlacklistService extends IService<TradeIpBlacklist> {

    /**
     * 查看交易ip是否在黑名单中
     *
     * @param ip
     * @return {@link Boolean}
     */
    Boolean isIpBlacklisted(String ip);

    PageReturn<TradeIpBlackListPageDTO> listPage(TradeIpBlackListReq req);

    RestResult save(TradeIpBlackListReq req);

    boolean del(String id);

    /**
     * 添加Ip黑名单回调方法
     *
     * @param tradIpBlackMessage
     * @return
     */
    void addBlackIpCallback(TradIpBlackMessage tradIpBlackMessage);
}

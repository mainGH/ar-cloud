package org.ar.wallet.service;

/**
 * UPI收款信息单日数据统计
 *
 * @author Simon
 * @date 2024/03/05
 */
public interface UpiTransactionService {


    /**
     * 增加单日交易笔数并且标记为已处理
     *
     * @param upiId
     * @param orderId
     */
    void incrementDailyTransactionCountAndMarkAsProcessed(String upiId, String orderId);

    /**
     * 减少当日收款次数
     *
     * @param upiId
     * @param orderId
     */
    void decrementDailyTransactionCountIfApplicable(String upiId, String orderId);

    /**
     * 获取单日交易笔数
     *
     * @param upiId
     * @return {@link Long}
     */
    Long getDailyTransactionCount(String upiId);


    /**
     * 生成交易笔数的键
     *
     * @param upiId
     * @return {@link String}
     */
    String generateTransactionCountKey(String upiId);


    /**
     * 设置键的过期时间为当天午夜
     *
     * @param key
     */
    void setExpirationAtMidnight(String key);


    /**
     * 计算当前时间至午夜的秒数
     *
     * @return long
     */
    long calculateSecondsUntilMidnight();
}

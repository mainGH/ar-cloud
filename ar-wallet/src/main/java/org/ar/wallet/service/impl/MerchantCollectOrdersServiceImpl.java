package org.ar.wallet.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.redis.util.RedissonUtil;
import org.ar.wallet.Enum.CollectionOrderStatusEnum;
import org.ar.wallet.entity.MerchantCollectOrders;
import org.ar.wallet.mapper.MerchantCollectOrdersMapper;
import org.ar.wallet.service.IMerchantCollectOrdersService;
import org.redisson.api.RLock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 商户代收订单表 服务实现类
 * </p>
 *
 * @author
 * @since 2024-01-05
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MerchantCollectOrdersServiceImpl extends ServiceImpl<MerchantCollectOrdersMapper, MerchantCollectOrders> implements IMerchantCollectOrdersService {

    private final RedissonUtil redissonUtil;
    private final MerchantCollectOrdersMapper merchantCollectOrdersMapper;

    /**
     * 根据商户订单号 获取订单信息
     *
     * @return {@link MerchantCollectOrders}
     */
    @Override
    public MerchantCollectOrders getOrderInfoByOrderNumber(String merchantOrder) {
        return lambdaQuery()
                .eq(MerchantCollectOrders::getMerchantOrder, merchantOrder)
                .or().eq(MerchantCollectOrders::getPlatformOrder, merchantOrder)
                .one();
    }


    /**
     * 取消充值订单
     *
     * @param platformOrder 平台订单号
     * @return {@link Boolean}
     */
    @Override
    public Boolean cancelPayment(String platformOrder) {
        return lambdaUpdate()
                .eq(MerchantCollectOrders::getPlatformOrder, platformOrder)
                .set(MerchantCollectOrders::getOrderStatus, CollectionOrderStatusEnum.WAS_CANCELED.getCode())
                .update();
    }


    /**
     * 支付超时处理
     *
     * @param orderNo
     * @return boolean
     */
    @Override
    @Transactional
    public boolean handlePaymentTimeout(String orderNo) {

        //加上分布式锁 锁名和确认支付的锁名一致 保证同时只有一个线程再操作支付超时或确认支付

        //分布式锁key ar-wallet-merchantCollectOrderPaymentTimeoutConsumer+订单号
        String key = "ar-wallet-merchantCollectOrderPaymentTimeoutConsumer" + orderNo;
        RLock lock = redissonUtil.getLock(key);

        boolean req = false;

        try {
            req = lock.tryLock(10, TimeUnit.SECONDS);

            if (req) {

                //获取代收订单信息 加上排他行锁
                MerchantCollectOrders merchantCollectOrders = merchantCollectOrdersMapper.selectMerchantCollectOrdersForUpdate(orderNo);

                if (merchantCollectOrders == null) {
                    //订单不存在或该订单不是待支付状态, 直接将该消息消费成功
                    log.error("商户代收订单支付超时处理失败, 获取订单信息失败, 订单号: {}", orderNo);
                    return false;
                }

                //校验订单状态是否是 支付中 如果不是则不进行处理
                if (!CollectionOrderStatusEnum.BE_PAID.getCode().equals(merchantCollectOrders.getOrderStatus())) {
                    //订单不存在或该订单不是待支付状态, 直接将该消息消费成功
                    log.info("商户代收订单支付超时处理成功, 该笔订单状态不是待支付, 订单号: {}, 订单信息: {}", orderNo, merchantCollectOrders);
                    return true;
                }

                //分布式锁key ar-wallet-sell+会员id
                String key2 = "ar-wallet-sell" + merchantCollectOrders.getMemberId();
                RLock lock2 = redissonUtil.getLock(key2);

                boolean req2 = false;

                try {
                    req2 = lock2.tryLock(10, TimeUnit.SECONDS);

                    if (req2) {

                        //将订单状态改为支付超时
                        boolean update = lambdaUpdate()
                                .eq(MerchantCollectOrders::getPlatformOrder, merchantCollectOrders.getPlatformOrder())
                                .eq(MerchantCollectOrders::getOrderStatus, CollectionOrderStatusEnum.BE_PAID.getCode())
                                .set(MerchantCollectOrders::getOrderStatus, CollectionOrderStatusEnum.PAYMENT_TIMEOUT.getCode())
                                .update();

                        if (update) {
                            log.info("商户代收订单支付超时处理成功, 订单号: {}", orderNo);
                            return true;
                        } else {
                            log.error("商户代收订单支付超时处理成功, 订单号: {}", orderNo);
                            return false;
                        }
                    }
                } catch (Exception e) {
                    //手动回滚
                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                    log.error("商户代收订单支付超时处理失败: 订单号: {}, e: {}", orderNo, e);
                } finally {
                    //释放锁
                    if (req && lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }

                    if (req2 && lock2.isHeldByCurrentThread()) {
                        lock2.unlock();
                    }
                }
            }
        } catch (Exception e) {
            //手动回滚
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("商户代收订单支付超时处理失败: 订单号: {}, e: {}", orderNo, e);
        } finally {
            //释放锁
            if (req && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        return false;
    }
}

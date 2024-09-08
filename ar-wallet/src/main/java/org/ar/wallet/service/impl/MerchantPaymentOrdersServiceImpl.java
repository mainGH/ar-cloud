package org.ar.wallet.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.ar.wallet.entity.MerchantPaymentOrders;
import org.ar.wallet.mapper.MerchantPaymentOrdersMapper;
import org.ar.wallet.service.IMerchantPaymentOrdersService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 商户代付订单表 服务实现类
 * </p>
 *
 * @author
 * @since 2024-01-05
 */
@Service
public class MerchantPaymentOrdersServiceImpl extends ServiceImpl<MerchantPaymentOrdersMapper, MerchantPaymentOrders> implements IMerchantPaymentOrdersService {


    /**
     * 根据商户订单号 获取订单信息
     *
     * @param merchantOrder
     * @return {@link MerchantPaymentOrders}
     */
    @Override
    public MerchantPaymentOrders getOrderInfoByOrderNumber(String merchantOrder) {
        return lambdaQuery()
                .eq(MerchantPaymentOrders::getMerchantOrder, merchantOrder)
                .or()
                .eq(MerchantPaymentOrders::getPlatformOrder, merchantOrder)
                .one();
    }
}

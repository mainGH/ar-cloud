package org.ar.wallet.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import org.ar.wallet.entity.CashBackOrder;
import org.ar.wallet.mapper.CashBackOrderMapper;
import org.ar.wallet.service.ICashBackOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 退回订单表 服务实现类
 * </p>
 *
 * @author 
 * @since 2024-05-09
 */
@Service
public class CashBackOrderServiceImpl extends ServiceImpl<CashBackOrderMapper, CashBackOrder> implements ICashBackOrderService {

    @Override
    public CashBackOrder getCashBackOrder(String orderNo) {
        LambdaQueryChainWrapper<CashBackOrder> queryChainWrapper = lambdaQuery().eq(CashBackOrder::getMerchantOrder, orderNo);
        return baseMapper.selectOne(queryChainWrapper.getWrapper());
    }
}

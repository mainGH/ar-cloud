package org.ar.wallet.mapper;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.ar.common.pay.dto.LastOrderWarnDTO;
import org.ar.wallet.entity.CollectionOrder;
import org.ar.wallet.entity.MemberInfo;
import org.ar.wallet.entity.MerchantCollectOrders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 商户代收订单表 Mapper 接口
 * </p>
 *
 * @author 
 * @since 2024-01-05
 */
@Mapper
public interface MerchantCollectOrdersMapper extends BaseMapper<MerchantCollectOrders> {


    /**
     * 根据订单号查询代收订单 加上排他行锁
     *
     * @param platform_order
     * @return {@link CollectionOrder}
     */
    @Select("SELECT * FROM merchant_collect_orders WHERE platform_order = #{platform_order} FOR UPDATE")
    MerchantCollectOrders selectMerchantCollectOrdersForUpdate(String platform_order);

    Long selectRechargeNum();

    List<MemberInfo> selectMerchantRechargeNum();

    List<MerchantCollectOrders> selectCountGroupByCode(@Param("startTime")String startTime, @Param("endTime")String endTime);

    List<MemberInfo> selectCostByDate(@Param("dateStr")String dateStr);

    List<LastOrderWarnDTO> getCollectLastOrderCreditTime();
}

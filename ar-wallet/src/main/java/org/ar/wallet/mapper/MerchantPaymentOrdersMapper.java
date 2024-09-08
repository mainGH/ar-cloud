package org.ar.wallet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.ar.common.pay.dto.LastOrderWarnDTO;
import org.ar.wallet.entity.MemberInfo;
import org.ar.wallet.entity.MerchantPaymentOrders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 商户代付订单表 Mapper 接口
 * </p>
 *
 * @author
 * @since 2024-01-05
 */
@Mapper
public interface MerchantPaymentOrdersMapper extends BaseMapper<MerchantPaymentOrders> {

    Long selectWithdrawFuture();

    List<MemberInfo> selectMerchantWithdrawNum();

    List<MerchantPaymentOrders> selectCountGroupByCode(@Param("startTime")String startTime, @Param("endTime")String endTime);

    List<MemberInfo> selectCostByDate(@Param("dateStr")String dateStr);

    List<LastOrderWarnDTO> getPaymentLastOrderCreditTime();
}

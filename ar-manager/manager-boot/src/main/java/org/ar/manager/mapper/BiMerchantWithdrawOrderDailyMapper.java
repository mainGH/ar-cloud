package org.ar.manager.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.ar.manager.entity.BiMerchantWithdrawOrderDaily;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.ar.manager.entity.BiWithdrawOrderDaily;

/**
 * @author
 */
@Mapper
public interface BiMerchantWithdrawOrderDailyMapper extends BaseMapper<BiMerchantWithdrawOrderDaily> {

    void updateByDateTime(@Param("vo") BiMerchantWithdrawOrderDaily biPaymentOrder);

    void deleteDailyByDateTime(@Param("dateTime")String dateTime);
}

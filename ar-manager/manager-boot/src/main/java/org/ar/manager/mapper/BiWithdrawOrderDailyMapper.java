package org.ar.manager.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.ar.manager.entity.BiWithdrawOrderDaily;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * @author
 */
@Mapper
public interface BiWithdrawOrderDailyMapper extends BaseMapper<BiWithdrawOrderDaily> {

    void updateByDateTime(@Param("vo") BiWithdrawOrderDaily biPaymentOrder);

    void deleteDailyByDateTime(@Param("dateTime")String dateTime);

}

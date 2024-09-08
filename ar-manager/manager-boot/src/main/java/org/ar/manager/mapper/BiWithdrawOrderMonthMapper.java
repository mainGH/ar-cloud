package org.ar.manager.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.ar.manager.entity.BiPaymentOrderMonth;
import org.ar.manager.entity.BiWithdrawOrderMonth;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 * @author
 */
@Mapper
public interface BiWithdrawOrderMonthMapper extends BaseMapper<BiWithdrawOrderMonth> {

    void updateByDateTime(@Param("vo")BiWithdrawOrderMonth biWithdrawOrderMonth);

    void deleteMonthByDateTime(@Param("dateTime")String dateTime);

    List<BiWithdrawOrderMonth> selectDataInfoByMonth(@Param("dateTime")String dateTime);
}

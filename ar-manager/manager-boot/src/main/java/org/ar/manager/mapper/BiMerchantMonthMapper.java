package org.ar.manager.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.ar.manager.entity.BiMerchantDaily;
import org.ar.manager.entity.BiMerchantMonth;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.ar.manager.entity.BiPaymentOrderMonth;

import java.util.List;

/**
 * @author
 */
@Mapper
public interface BiMerchantMonthMapper extends BaseMapper<BiMerchantMonth> {

    void updateByDateTime(@Param("vo") BiMerchantMonth biMerchantMonth);

    void updateWithdrawByDateTime(@Param("vo") BiMerchantMonth biMerchantMonth);

    void deleteMonthByDateTime(@Param("dateTime") String dateTime);

    List<BiMerchantMonth> selectPayDataInfoByMonth(@Param("dateTime") String dateTime);

    List<BiMerchantMonth> selectWithdrawDataInfoByMonth(@Param("dateTime") String dateTime);
}

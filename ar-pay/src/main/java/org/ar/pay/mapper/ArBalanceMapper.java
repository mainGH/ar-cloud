package org.ar.pay.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.ar.common.pay.dto.UserAuthDTO;
import org.ar.pay.entity.ArBalance;
import org.ar.pay.entity.MerchantInfo;

import java.math.BigDecimal;

/**
* @author 
*/  @Mapper
    public interface ArBalanceMapper extends BaseMapper<ArBalance> {



        boolean updateBalanceByCurrence(@Param("amount") BigDecimal amount,@Param("currence") String currence);


        BigDecimal  getCurrentBlanceByCurrentce(@Param("currence") String currence);

    }

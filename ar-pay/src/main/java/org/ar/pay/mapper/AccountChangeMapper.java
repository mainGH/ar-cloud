package org.ar.pay.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.ar.common.pay.dto.UserAuthDTO;
import org.ar.pay.entity.AccountChange;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.math.BigDecimal;

/**
* @author 
*/  @Mapper
    public interface AccountChangeMapper extends BaseMapper<AccountChange> {
    //  boolean updateAccountChangeByOrder(@Param("orderno") String orderno, @Param("realAmount") BigDecimal realAmount);

    }

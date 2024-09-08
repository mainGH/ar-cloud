package org.ar.pay.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.ar.common.pay.dto.MerchantAuthDTO;
import org.ar.common.pay.dto.UserAuthDTO;
import org.ar.pay.entity.MerchantInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
* @author 
*/  @Mapper
    public interface MerchantInfoMapper extends BaseMapper<MerchantInfo> {
       UserAuthDTO getByUsername(@Param("userName") String userName);

       MerchantInfo getMerchantInfoById(@Param("code") String code);

        boolean updateBalanceByCode(MerchantInfo MerchantInfo);

    }

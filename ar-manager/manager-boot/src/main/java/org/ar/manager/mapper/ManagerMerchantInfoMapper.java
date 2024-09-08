package org.ar.manager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.ar.common.pay.dto.UserAuthDTO;
import org.ar.manager.entity.MerchantInfo;

/**
 * @author
 */
@Mapper
public interface ManagerMerchantInfoMapper extends BaseMapper<MerchantInfo> {
    UserAuthDTO getByUsername(@Param("userName") String userName);

    MerchantInfo getMerchantInfoById(@Param("code") String code);

    boolean updateBalanceByCode(MerchantInfo MerchantInfo);

}

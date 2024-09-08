package org.ar.wallet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.ar.wallet.entity.MemberAccountChange;

import java.util.List;

/**
 * @author
 */
@Mapper
public interface MemberAccountChangeMapper extends BaseMapper<MemberAccountChange> {

    List<MemberAccountChange> selectUpSumInfo(@Param("dateStr")String dateStr);

    List<MemberAccountChange> selectDownSumInfo(@Param("dateStr")String dateStr);
}

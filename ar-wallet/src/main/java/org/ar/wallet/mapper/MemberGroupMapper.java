package org.ar.wallet.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.ar.wallet.entity.MatchingOrder;
import org.ar.wallet.entity.MemberGroup;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 * @author
 */
@Mapper
public interface MemberGroupMapper extends BaseMapper<MemberGroup> {

    IPage<MemberGroup> fetchMemberGroup(Page<MemberGroup> page);

}

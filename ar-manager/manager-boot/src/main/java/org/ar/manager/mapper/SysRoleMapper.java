package org.ar.manager.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.ar.manager.entity.SysRole;


@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    SysRole selectOneByName(@Param("name")String name);

    void updateRoleById(@Param("id")Long id);
}


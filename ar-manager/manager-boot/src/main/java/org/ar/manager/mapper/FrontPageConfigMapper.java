package org.ar.manager.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.ar.common.pay.dto.FrontPageConfigDTO;
import org.ar.manager.entity.FrontPageConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * <p>
 * 首页弹窗内容 Mapper 接口
 * </p>
 *
 * @author 
 * @since 2024-04-27
 */
@Mapper
public interface FrontPageConfigMapper extends BaseMapper<FrontPageConfigDTO> {

}

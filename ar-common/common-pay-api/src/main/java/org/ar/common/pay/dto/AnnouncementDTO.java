package org.ar.common.pay.dto;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "获取公告信息返回数据")
public class AnnouncementDTO {

    /**
     * id
     */
    @ApiModelProperty("id")
    private Long id;


    /**
     * 公告标题
     */
    @ApiModelProperty(value = "公告标题")
    private String announcementTitle;


    /**
     * 公告内容
     */
    @ApiModelProperty(value = "公告内容")
    private String announcementContent;


    /**
     * 排序权重
     */
    @ApiModelProperty(value = "排序权重 (小排在前)")
    private Integer sortOrder;


    /**
     * 状态（1为启用，0为禁用）
     */
    @ApiModelProperty(value = "状态（1为启用，0为禁用）")
    private Integer status;
}

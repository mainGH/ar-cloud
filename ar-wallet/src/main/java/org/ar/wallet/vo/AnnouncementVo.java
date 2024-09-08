package org.ar.wallet.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author
 */
@Data
@ApiModel(description = "前台-获取公告信息返回数据")
public class AnnouncementVo implements Serializable {

    @ApiModelProperty("id")
    private Long id;

    /**
     * 公告标题
     */
    @ApiModelProperty("公告标题")
    private String announcementTitle;

    /**
     * 公告内容
     */
    @ApiModelProperty("公告内容")
    private String announcementContent;

    /**
     * 公告内容
     */
    @ApiModelProperty("公告时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
package org.ar.wallet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * Banner信息表
 * </p>
 *
 * @author 
 * @since 2024-02-29
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class Announcement extends BaseEntityOrder {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 公告标题
     */
    private String announcementTitle;

    /**
     * 公告内容
     */
    private String announcementContent;

    /**
     * 排序权重
     */
    private Integer sortOrder;

    /**
     * 状态（1为启用，0为禁用）
     */
    private Integer status;

    /**
     * 删除表示: 0未删除，1已删除
     */
    private Integer deleted;
}

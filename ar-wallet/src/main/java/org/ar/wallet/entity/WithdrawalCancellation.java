package org.ar.wallet.entity;

    import com.baomidou.mybatisplus.annotation.*;

    import java.time.LocalDateTime;
    import java.io.Serializable;

    import com.fasterxml.jackson.annotation.JsonFormat;
    import lombok.Data;
    import lombok.EqualsAndHashCode;
    import lombok.experimental.Accessors;

/**
* 提现取消原因配置表
*
* @author 
*/
    @Data
        @EqualsAndHashCode(callSuper = false)
    @Accessors(chain = true)
    @TableName("withdrawal_cancellation")
    public class WithdrawalCancellation implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;


            /**
            * 原因
            */
    private String reason;

            /**
            * 排序
            */
    private Integer sort;



    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;

    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private String updateBy;


}
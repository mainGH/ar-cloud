package org.ar.pay.entity;

    import com.baomidou.mybatisplus.annotation.IdType;
    import com.baomidou.mybatisplus.annotation.TableId;
    import com.baomidou.mybatisplus.annotation.TableName;
    import java.io.Serializable;
    import lombok.Data;
    import lombok.EqualsAndHashCode;
    import lombok.experimental.Accessors;

/**
* 银行信息
*
* @author 
*/
    @Data
        @EqualsAndHashCode(callSuper = false)
    @Accessors(chain = true)
    @TableName("bank_info")
    public class BankInfo implements Serializable {


    @TableId(type = IdType.AUTO)
    private Long id;


            /**
            * 三方编码
            */
    private String thirdCode;

            /**
            * guo'j
            */
    private Integer county;

            /**
            * 银行编码
            */
    private String bankCode;

            /**
            * 银行名称
            */
    private String bankName;


}
package org.ar.wallet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * kyc信息表
 * </p>
 *
 * @author
 * @since 2024-04-20
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("kyc_partners")
public class KycPartners extends BaseEntityOrder {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 会员id
     */
    private String memberId;

    /**
     * 会员账号
     */
    private String memberAccount;

    /**
     * 银行登录令牌
     */
    private String token;

    /**
     * 手机号
     */
    private String mobileNumber;

    /**
     * 银行编码
     */
    private String bankCode;

    /**
     * 银行名称
     */
    private String bankName;

    /**
     * upi_id
     */
    private String upiId;

    /**
     * 账户姓名
     */
    private String name;

    /**
     * 账户
     */
    private String account;

    /**
     * 连接状态: 0: 未连接, 1: 已连接
     */
    private Integer linkStatus;

    /**
     * 备注
     */
    private String remark;


    /**
     * 删除表示: 0: 未删除, 1: 已删除
     */
    private Integer deleted;


    /**
     * 卖出状态: 0: 关闭, 1: 开启
     */
    private Integer sellStatus;


    /**
     * 图标地址
     */
    private String iconUrl;

}

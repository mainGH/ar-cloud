package org.ar.wallet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.ar.wallet.Enum.CollectionInfoStatusEnum;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 收款信息
 *
 * @author
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("collection_info")
public class CollectionInfo extends BaseEntityOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * UPI_ID
     */
    private String upiId;

    /**
     * UPI_Name
     */
    private String upiName;

    /**
     * 会员id
     */
    private String memberId;

    /**
     * 手机号
     */
    private String mobileNumber;

    /**
     * 日限额
     */
    private BigDecimal dailyLimitAmount;

    /**
     * 日限笔数
     */
    private Integer dailyLimitCount;

    /**
     * 最小金额
     */
    private BigDecimal minimumAmount;

    /**
     * 最大金额
     */
    private BigDecimal maximumAmount;

    /**
     * 已收款金额
     */
    private BigDecimal collectedAmount;

    /**
     * 已收款笔数
     */
    private Integer collectedCount;

    /**
     * 收款状态 默认值: 正常
     */
    private String collectedStatus = CollectionInfoStatusEnum.NORMAL.getCode();

    /**
     * 会员账号
     */
    private String memberAccount;

    /**
     * 今日收款笔数
     */
    private Integer todayCollectedCount;

    /**
     * 今日收款金额
     */
    private BigDecimal todayCollectedAmount;

    /**
     * 今日成功收款笔数
     */
    private Integer todaySuccessCollectedCount;

    /**
     * 今日成功收款金额
     */
    private BigDecimal todaySuccessCollectedAmount;

    /**
     * 是否删除 默认值: 0
     */
    private Integer deleted = 0;


    /**
     * 是否默认 默认值: 0
     */
    private Integer defaultStatus;


}
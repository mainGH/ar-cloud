package org.ar.wallet.Enum;

import java.time.LocalDateTime;
import java.util.Arrays;

/*
 * 订单状态枚举
 * */
public enum OrderStatusEnum {


    BE_MATCHED("1", "待匹配", "进行中"),
    MATCH_TIMEOUT("2", "匹配超时", "进行中"),
    BE_PAID("3", "待支付", "进行中"),
    CONFIRMATION("4", "确认中", "进行中"),
    CONFIRMATION_TIMEOUT("5", "确认超时", "进行中"),
    COMPLAINT("6", "申诉中", "进行中"),
    SUCCESS("7", "已完成", "已结束"),
    WAS_CANCELED("8", "已取消", "已结束"),
    FAIL("9", "订单失效", "已结束"),
    BUY_FAILED("10", "买入失败", "已结束"),
    AMOUNT_ERROR("11", "金额错误", "进行中"),
    NO_PAY("12", "未支付", "已结束"),
    PAYMENT_TIMEOUT("13", "支付超时", "已结束"),
    IN_PROGRESS("14", "进行中", "进行中"),

    MANUAL_COMPLETION("15", "手动完成", "已结束"),
    AUDITING("16", "人工审核", "人工审核中");


    private final String code;

    private final String name;

    private final String remark;

    // 买入进行中的状态
    public static String[] BUYING_STATUS = {OrderStatusEnum.BE_PAID.getCode(), OrderStatusEnum.CONFIRMATION.getCode(), OrderStatusEnum.CONFIRMATION_TIMEOUT.getCode(), OrderStatusEnum.COMPLAINT.getCode()};
    // 卖出进行中的状态
    public static String[] SELLING_STATUS = {OrderStatusEnum.BE_MATCHED.getCode(), OrderStatusEnum.MATCH_TIMEOUT.getCode(), OrderStatusEnum.BE_PAID.getCode(), OrderStatusEnum.CONFIRMATION.getCode(), OrderStatusEnum.CONFIRMATION_TIMEOUT.getCode(), OrderStatusEnum.COMPLAINT.getCode()};


    OrderStatusEnum(String code, String name, String remark) {
        this.code = code;
        this.name = name;
        this.remark = remark;
    }

    public static String getNameByCode(String code) {
        for (OrderStatusEnum c : OrderStatusEnum.values()) {
            if (c.getCode().equals(code)) {
                return c.getName();
            }

        }
        return null;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getRemark() {
        return remark;
    }

    /**
     * 是否为人工审核状态
     *
     * @param code
     * @param delayTime
     * @return
     */
    public static boolean isAuditing(String code, LocalDateTime delayTime) {
        return (CONFIRMATION.getCode().equals(code) || CONFIRMATION_TIMEOUT.getCode().equals(code)) && delayTime != null;
    }

    /**
     * 是否买入进行中
     *
     * @param code
     * @return
     */
    public static boolean isBuyingStatus(String code) {
        return Arrays.stream(BUYING_STATUS).anyMatch(status -> status.equals(code));
    }

    /**
     * 是否卖出进行中
     * @param code
     * @return
     */
    public static boolean isSellingStatus(String code) {
        return Arrays.stream(SELLING_STATUS).anyMatch(status -> status.equals(code));
    }
}

package org.ar.job.Enum;

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
    NO_PAY("12", "未支付", "已结束")
    ;


    private final String code;

    private final String name;

    private final String remark;


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
}

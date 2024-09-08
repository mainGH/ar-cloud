package org.ar.pay.Enum;

/*
 * 代付订单状态枚举
 * */
public enum PaymentOrderStatusEnum {
    UNCOMMITTED("1", "未提交"),
    SUBMITTED("2", "提交中"),
    HANDLING("3", "代付处理中"),
    FAILED("4", "代付失败"),
    SUCCESS("5", "代付成功");


    private final String code;

    private final String name;


    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static String getNameByCode(String code) {
        for (PaymentOrderStatusEnum c : PaymentOrderStatusEnum.values()) {
            if (c.getCode().equals(code)) {
                return c.getName();
            }

        }
        return null;
    }

    PaymentOrderStatusEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
}

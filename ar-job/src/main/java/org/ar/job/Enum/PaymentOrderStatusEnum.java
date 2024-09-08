package org.ar.job.Enum;

/*
 * 代付订单状态枚举
 * */
public enum PaymentOrderStatusEnum {
    HANDLING("1", "代付处理中"),
    SUCCESS("2", "代付成功"),
    FAILED("3", "代付失败");



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

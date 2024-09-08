package org.ar.job.Enum;

/*
 * 支付类型枚举
 * */
public enum PayTypeEnum {

    INDIAN_CARD("1", "印度银行卡"),
    INDIAN_UPI("3", "印度upi"),
    INDIAN_PIX("4", "印度pix"),
    INDIAN_USDT("2", "印度USDT");



    private final String code;

    private final String name;


    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static String getNameByCode(String code) {
        for (PayTypeEnum c : PayTypeEnum.values()) {
            if (c.getCode().equals(code)) {
                return c.getName();
            }

        }
        return null;
    }

    PayTypeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
}

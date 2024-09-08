package org.ar.pay.Enum;

/*
 * 支付订单状态枚举
 * */
public enum CollectionOrderStatusEnum {
    BE_PAID("1", "待支付"),
    PAID("2", "已支付"),
    WAS_CANCELED("3", "已取消");


    private final String code;

    private final String name;


    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static String getNameByCode(String code) {
        for (CollectionOrderStatusEnum c : CollectionOrderStatusEnum.values()) {
            if (c.getCode().equals(code)) {
                return c.getName();
            }

        }
        return null;
    }

    CollectionOrderStatusEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
}

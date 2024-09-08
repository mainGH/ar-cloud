package org.ar.wallet.Enum;

/*
 * 订单类型枚举
 * */
public enum OrderTypeEnum {

    /**
     * 买入
     */
    COLLECTION("0", "买入"),
    /**
     * 卖出
     */
    PAYMENT("1", "卖出"),
    /**
     * 撮合
     */
    MATCH("2", "撮合");



    private final String code;

    private final String name;


    OrderTypeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static String getNameByCode(String code) {
        for (OrderTypeEnum c : OrderTypeEnum.values()) {
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
}

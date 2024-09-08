package org.ar.job.Enum;

/**
 * @author Admin
 */

/*
* 账变类型枚举
* */
public enum AccountChangeEnum {
    COLLECTION("1", "代收", "MC"),
    PAYMENT("2", "代付", "MP"),
    WITHDRAW("3", "下发", "MW"),
    RECHARGE("4", "上分", "MR");


    private final String code;

    private final String name;

    private final String prefix;


    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getPrefix() {
        return prefix;
    }

    public static String getNameByCode(String code) {
        for (AccountChangeEnum c : AccountChangeEnum.values()) {
            if (c.getCode().equals(code)) {
                return c.getName();
            }

        }
        return null;
    }

    AccountChangeEnum(String code, String name, String prefix) {
        this.code = code;
        this.name = name;
        this.prefix = prefix;
    }


}

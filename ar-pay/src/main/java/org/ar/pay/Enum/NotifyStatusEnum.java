package org.ar.pay.Enum;

/*
 * 回调状态枚举
 * */
public enum NotifyStatusEnum {
    NOTCALLBACK("1", "未回调"),
    SUCCESS("2", "回调成功"),
    FAILED("3", "回调失败"),
    MANUAL_SUCCESS("4", "手动回调成功"),
    MANUAL_FAILED("5", "手动回调失败");


    private final String code;

    private final String name;


    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static String getNameByCode(String code) {
        for (NotifyStatusEnum c : NotifyStatusEnum.values()) {
            if (c.getCode().equals(code)) {
                return c.getName();
            }

        }
        return null;
    }

    NotifyStatusEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
}

package org.ar.pay.Enum;

public  enum  ChannelEnum {

    WEIPAY("1","微信"),
    ALIPAY("2","支付宝");
    private final String code;

    private final String name;

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static String getNameByCode(String code) {
        for (ChannelEnum c : ChannelEnum.values()) {
            if (c.getCode().equals(code)) {
                return c.getName();
            }

        }
        return null;
    }






    ChannelEnum(String code,String name){
        this.code = code;
        this.name = name;
    }


}

package org.ar.auth.comm.enums;

import lombok.Getter;


public enum ClientEnums {
    MERCHANT_CLIENT("merchant", "商户客户端"),
    WALLET_CLIENT("wallet", "钱包客户端"),
    ADMIN_CLIENT("ar", "后台客户端"),
    APP_CLIENT("app", "app客户端"),
    MEMBER_CLIENT("member", "会员客户端");

    @Getter
    private String name;
    @Getter
    private String desc;

    ClientEnums(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }
}

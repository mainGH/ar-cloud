package org.ar.wallet.Enum;

/**
 * @author Admin
 */

/*
 * 会员账变类型枚举
 * */
public enum MemberAccountChangeEnum {
    RECHARGE("1", "买入", "MR"),
    WITHDRAW("2", "卖出", "MC"),
    USDT_RECHARGE("3", "充值额度(usdt充值)", "UR"),
    UPPER_DIVISION("4", "人工上分", "UD"),
    FREEZE("5", "冻结", "DJ"),
    UNFREEZE("6", "解冻", "JD"),
    LOWER_DIVISION("7", "人工下分", "LD"),
    BUY_BONUS("8", "买入奖励", "MRJL"),
    SELL_BONUS("9", "卖出奖励", "MCJL"),
    CANCEL_RETURN("10", "取消退回", "QXTH"),
    WITHDRAWAL("11", "支付", "ZF"),
    DEPOSIT ("12", "到账", "DZ"),

    AMOUNT_ERROR("13", "金额错误退回", "AE"),

    TASK_REWARD("14", "任务奖励", "TR");


    private final String code;

    private final String name;

    private final String prefix;


    MemberAccountChangeEnum(String code, String name, String prefix) {
        this.code = code;
        this.name = name;
        this.prefix = prefix;
    }

    public static String getNameByCode(String code) {
        for (MemberAccountChangeEnum c : MemberAccountChangeEnum.values()) {
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

    public String getPrefix() {
        return prefix;
    }

}

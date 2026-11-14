package dev.jdguo.bank.ui;

import java.util.List;

/**
 * Page chrome. Function tabs stay on the bottom left. Issuance and management
 * are separate top-right buttons. Close sits alone at the bottom right.
 * There is no side panel.
 */
public final class BankChrome {
    public static final boolean SIDEBAR = false;
    public static final String SAVINGS_TITLE = "个人储蓄";
    public static final String ISSUANCE = "个人发行";
    public static final String MANAGEMENT = "中央银行管理";
    public static final String CLOSE = "关闭";
    public static final List<String> FUNCTION_TABS = List.of("储蓄", "存入", "取出", "兑换", "汇票", "交易");

    private BankChrome() {
    }
}

package dev.jdguo.bank.ui;

import java.util.List;

/**
 * Bottom page tabs. There is no side panel and no side tab strip.
 */
public final class BankChrome {
    public static final boolean SIDEBAR = false;
    public static final List<String> TABS = List.of(
            "储蓄", "存入", "取出", "兑换", "汇票", "交易", "个人发行", "管理");

    private BankChrome() {
    }
}

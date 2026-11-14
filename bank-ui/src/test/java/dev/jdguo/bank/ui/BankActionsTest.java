package dev.jdguo.bank.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BankActionsTest {
    @Test
    void usesTheActionsTheBankCoreAlreadyAccepts() {
        assertEquals("deposit-transfer diamond 5", BankActions.deposit("diamond", "5"));
        assertEquals("withdraw note all", BankActions.withdraw("note", "all"));
        assertEquals("exchange copper note 10", BankActions.exchange("copper", "note", "10"));
        assertEquals("check note 64 bearer", BankActions.check("note", "64", false));
        assertEquals("check note 64 self", BankActions.check("note", "64", true));
        assertEquals("deposit-held", BankActions.presentHeld());
        assertEquals("兑付手里拿着的那一张汇票。请把要兑付的汇票拿在主手。一次只兑付这一张，不会扫描背包，也不会存入铜锭、钻石或银行券。",
                BankActions.MAIN_HAND_BILL);
        assertEquals("确认兑付手里拿着的那一张汇票", BankActions.MAIN_HAND_BUTTON);
        assertEquals("note-buy 张三 1", BankActions.noteBuy("张三", "1"));
        assertEquals("note-sell 张三 5", BankActions.noteSell("张三", "5"));
        assertEquals("note-swap 张三 李四 10", BankActions.noteSwap("张三", "李四", "10"));
        assertEquals("note-redeem 张三 note 1", BankActions.noteRedeem("张三", "note", "1"));
        assertEquals("note-issue 64", BankActions.noteIssue("64"));
        assertEquals("note-unbacked 64", BankActions.noteUnbacked("64"));
        assertEquals("admin-freeze", BankActions.adminFreeze());
        assertEquals("admin-unbacked note 1000", BankActions.adminUnbacked("note", "1000"));
        assertEquals("admin-unbacked note 200000", BankActions.adminUnbacked("note", "200000"));
        assertEquals("admin-retire note 200000", BankActions.adminRetire("note", "200000"));
        assertEquals("admin-retire note 1000", BankActions.adminRetire("note", "1000"));
        assertEquals("admin-backed diamond", BankActions.adminBacked("diamond"));
        assertEquals("admin-backed note", BankActions.adminBacked("note"));
        assertEquals(2, BankActions.adminBacked("copper").split(" ").length);
    }

    @Test
    void namingFillsTheExistingCreateCommand() {
        assertEquals("/bank note create float ", BankActions.noteNameCommand("float"));
        assertEquals("/bank note create peg ", BankActions.noteNameCommand("钉住"));
        assertEquals("/bank note create hard ", BankActions.noteNameCommand("定额兑付"));
        assertEquals("/bank note create float ", BankActions.noteNameCommand("浮动汇率制"));
        assertEquals("/bank note create peg ", BankActions.noteNameCommand("联系汇率制"));
        assertEquals("/bank note create hard ", BankActions.noteNameCommand("固定汇率制"));
        assertEquals("note-switch float", BankActions.noteSwitch("FLOAT", null));
        assertEquals("note-switch peg note", BankActions.noteSwitch("peg", "银行券"));
        assertEquals("note-switch hard diamond", BankActions.noteSwitch("hard", "diamond"));
        assertEquals("note-switch hard", BankActions.noteSwitch("hard", "note"));
    }
}

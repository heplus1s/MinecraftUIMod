package dev.jdguo.bank.ui;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BankHeaderTest {
    @Test
    void showsSixDecimalsRateAndNormalStatusWithoutASidebar() {
        JsonObject data = new JsonObject();
        data.addProperty("m0", "1");
        data.addProperty("m1", "4");
        data.addProperty("m2", "3.5");
        data.addProperty("status", "兑付正常");
        data.addProperty("copperPerDiamond", "1000");

        List<String> lines = BankHeader.lines(data);

        assertEquals(List.of(
                "M0 1.000000",
                "M1 4.000000",
                "M2 3.500000",
                "汇率 1000.000000 铜/钻",
                "兑付正常"), lines);
        assertFalse(BankChrome.SIDEBAR);
        assertFalse(String.join("\n", lines).contains("兑付能力"));
        assertTrue(BankChrome.TABS.contains("储蓄"));
        assertTrue(BankChrome.TABS.contains("个人发行"));
    }

    @Test
    void chestHeaderKeepsRateAndDropsExtraRedemptionLines() {
        List<String> kept = BankHeader.keepChestHeader(List.of(
                "Central bank",
                "兑付正常",
                "汇率 1000 铜/钻",
                "手续费 2%",
                "兑付能力 80%",
                "兑付：正常",
                "兑付：暂停",
                "Redemption available",
                "汇票请到「汇票」页兑付",
                "定额兑付"));

        assertEquals(List.of("汇率 1000.000000 铜/钻", "兑付正常"), kept);
    }

    @Test
    void missingRateStillLeavesTheRateLine() {
        JsonObject data = new JsonObject();
        data.addProperty("m0", "0");
        data.addProperty("m1", "0");
        data.addProperty("m2", "0");
        data.addProperty("status", "准备金紧张");

        List<String> lines = BankHeader.lines(data);

        assertTrue(lines.contains("汇率 —"));
        assertTrue(lines.contains("准备金紧张"));
        assertFalse(BankHeader.showStatus("兑付能力 12%"));
        assertFalse(BankHeader.showStatus("兑付：暂停"));
        assertEquals("1.234568", BankHeader.fixed6("1.2345678"));
    }
}

package dev.jdguo.bank.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BankRegimesTest {
    @Test
    void missingSwitchDataDoesNotInventAButton() {
        JsonObject data = new JsonObject();
        data.addProperty("regimeName", "浮动汇率制");

        assertFalse(BankRegimes.switchesKnown(data));
        assertTrue(BankRegimes.offers(data).isEmpty());
        assertTrue(BankRegimes.figures(data).contains("当前发行制度：浮动汇率制"));
        assertTrue(BankRegimes.figures(data).contains("现价：暂无牌价"));
        assertTrue(String.join("\n", BankRegimes.rules()).contains("固定汇率制"));
        assertTrue(BankRegimes.HARD_RULE.contains("面值并不改变"));
    }

    @Test
    void allowedOffersBecomeSwitchCommandsAndRefusalsDoNot() {
        JsonObject data = new JsonObject();
        JsonObject note = new JsonObject();
        note.addProperty("regime", "FLOAT");
        note.addProperty("regimeName", "浮动汇率制");
        note.addProperty("live", "0.000000");
        JsonArray switches = new JsonArray();
        switches.add(offer("peg", "note", "联系汇率制，锚为银行券", true, null));
        switches.add(offer("hard", "diamond", "固定汇率制，锚为钻石", false, "现价已经偏离面值"));
        switches.add(offer("hard", "note", "固定汇率制，锚为银行券", true, null));
        note.add("switches", switches);
        data.add("ownNote", note);

        assertTrue(BankRegimes.switchesKnown(data));
        List<BankRegimes.Offer> offers = BankRegimes.offers(data);
        assertEquals("note-switch peg note", offers.get(0).command());
        assertEquals("可以切换为联系汇率制，锚为银行券", offers.get(0).button());
        assertNull(offers.get(1).button());
        assertEquals("现价已经偏离面值", offers.get(1).refusal());
        assertNull(offers.get(2).command());
        assertTrue(BankRegimes.figures(data).contains("现价：0"));
    }

    @Test
    void bankruptcyBlocksSwitching() {
        JsonObject data = new JsonObject();
        data.addProperty("bankrupt", true);
        data.add("switches", new JsonArray());

        assertEquals("这张个人银行券已经破产，不能切换发行制度。", BankRegimes.blockReason(data));
    }

    private static JsonObject offer(String regime, String anchor, String name, boolean allowed, String reason) {
        JsonObject row = new JsonObject();
        row.addProperty("regime", regime);
        row.addProperty("anchor", anchor);
        row.addProperty("name", name);
        row.addProperty("allowed", allowed);
        if (reason != null) {
            row.addProperty("reason", reason);
        }
        return row;
    }
}

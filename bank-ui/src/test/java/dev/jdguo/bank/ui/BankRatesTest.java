package dev.jdguo.bank.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BankRatesTest {
    @Test
    void missingQuotesAreNamedInsteadOfInvented() {
        JsonObject data = new JsonObject();
        data.addProperty("status", "兑付正常");
        JsonArray currencies = new JsonArray();
        JsonObject copper = new JsonObject();
        copper.addProperty("id", "copper");
        copper.addProperty("name", "铜锭");
        copper.addProperty("balance", "12");
        currencies.add(copper);
        data.add("currencies", currencies);

        var lines = BankRates.column(data);
        assertTrue(lines.contains("汇率  暂无牌价"));
        assertTrue(lines.contains("铜锭  暂无牌价"));
        assertEquals(BankRates.UNAVAILABLE, BankRates.centralQuote(data));
        assertNull(BankSnapshot.currencies(data).get(0).rate());
        assertFalse(String.join("\n", lines).contains("M0"));
    }

    @Test
    void presentQuoteAndCurrencyRateAreShown() {
        JsonObject data = new JsonObject();
        data.addProperty("copperPerDiamond", "1000");
        JsonArray currencies = new JsonArray();
        JsonObject diamond = new JsonObject();
        diamond.addProperty("id", "diamond");
        diamond.addProperty("name", "钻石");
        diamond.addProperty("balance", "2");
        diamond.addProperty("rate", "1");
        currencies.add(diamond);
        data.add("currencies", currencies);

        assertEquals("1 钻石 = 1000.000000 铜锭", BankRates.centralQuote(data));
        assertEquals("1", BankSnapshot.currencies(data).get(0).rate());
        assertTrue(BankRates.column(data).contains("钻石  牌价：1"));
        assertEquals(1, BankSnapshot.hardCurrencies(data).size());
    }
}

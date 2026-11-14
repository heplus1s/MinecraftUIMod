package dev.jdguo.bank.ui;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/** Left-column exchange rates. Missing quotes stay missing. */
public final class BankRates {
    public static final String UNAVAILABLE = "暂无牌价";

    private BankRates() {
    }

    public static List<String> column(JsonObject data) {
        List<String> lines = new ArrayList<>(BankHeader.lines(data));
        lines.add("各货币牌价");
        List<BankSnapshot.MoneyRow> currencies = BankSnapshot.currencies(data);
        if (currencies.isEmpty()) {
            lines.add(UNAVAILABLE);
            return lines;
        }
        for (BankSnapshot.MoneyRow row : currencies) {
            lines.add(row.name() + "  " + (row.rate() == null ? UNAVAILABLE : "牌价：" + row.rate()));
        }
        return lines;
    }

    public static String centralQuote(JsonObject data) {
        String raw = BankHeader.centralRateRaw(data);
        if (raw == null) {
            return UNAVAILABLE;
        }
        String shown = BankHeader.fixed6(raw);
        if ("—".equals(shown)) {
            return UNAVAILABLE;
        }
        return "1 钻石 = " + shown + " 铜锭";
    }
}

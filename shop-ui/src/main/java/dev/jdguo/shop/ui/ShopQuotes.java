package dev.jdguo.shop.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Pay currencies and unit prices for the shop screen.
 * Copper and diamonds are never offered. A missing live quote stays 暂无牌价.
 */
public final class ShopQuotes {
    public static final String UNAVAILABLE = "暂无牌价";
    public static final String NOTE = "note";

    private ShopQuotes() {
    }

    public record Pay(String id, String label) {
    }

    public record Item(String id, String name, String diamondPrice) {
    }

    /** 银行券 first, then one slot for each personal note on the snapshot. */
    public static List<Pay> payCurrencies(JsonObject data) {
        List<Pay> rows = new ArrayList<>();
        rows.add(new Pay(NOTE, "银行券"));
        for (JsonObject note : array(data, "notes")) {
            String name = text(note, "name");
            if (name == null || hard(text(note, "id"), name)) {
                continue;
            }
            rows.add(new Pay(personalId(note, name), "个人银行券 " + name));
        }
        return rows;
    }

    public static boolean offersHardCurrency(List<Pay> rows) {
        for (Pay row : rows) {
            if (hard(row.id(), row.label()) || "铜锭".equals(row.label()) || "钻石".equals(row.label())) {
                return true;
            }
        }
        return false;
    }

    public static List<Item> items(JsonObject data) {
        List<Item> rows = new ArrayList<>();
        for (JsonObject row : array(data, "items")) {
            String id = text(row, "id");
            String name = text(row, "name");
            if (id == null || name == null) {
                continue;
            }
            rows.add(new Item(id, name, first(row, "diamondPrice", "diamonds", "priceDiamonds")));
        }
        return rows;
    }

    public static int columns(int contentWidth) {
        return contentWidth >= 460 ? 3 : 2;
    }

    /**
     * Diamond anchor times the live quote of the selected currency.
     * 银行券 uses notes per diamond. A personal note converts through that quote and the note's price in 银行券.
     */
    public static String unitPrice(JsonObject data, String diamondPrice, String currencyId) {
        BigDecimal diamonds = parse(diamondPrice);
        BigDecimal notesPerDiamond = notesPerDiamond(data);
        if (diamonds == null || notesPerDiamond == null) {
            return UNAVAILABLE;
        }
        BigDecimal inNotes = diamonds.multiply(notesPerDiamond);
        if (NOTE.equals(currencyId)) {
            return figure(inNotes);
        }
        BigDecimal perNote = personalPrice(data, currencyId);
        if (perNote == null || perNote.signum() == 0) {
            return UNAVAILABLE;
        }
        return figure(inNotes.divide(perNote, 6, RoundingMode.HALF_UP));
    }

    public static String personalId(JsonObject note, String name) {
        String id = text(note, "id");
        if (id == null || NOTE.equals(id) || "copper".equals(id) || "diamond".equals(id)) {
            return "pn:" + name;
        }
        return id;
    }

    private static BigDecimal notesPerDiamond(JsonObject data) {
        for (JsonObject row : array(data, "currencies")) {
            String id = text(row, "id");
            String name = text(row, "name");
            if (!NOTE.equals(id) && !"银行券".equals(name)) {
                continue;
            }
            String raw = first(row, "unitsPerDiamond", "rate");
            BigDecimal parsed = parse(raw);
            if (parsed != null && parsed.signum() > 0) {
                return parsed;
            }
        }
        return null;
    }

    private static BigDecimal personalPrice(JsonObject data, String currencyId) {
        for (JsonObject note : array(data, "notes")) {
            String name = text(note, "name");
            if (name == null) {
                continue;
            }
            if (!currencyId.equals(personalId(note, name))) {
                continue;
            }
            BigDecimal parsed = parse(text(note, "price"));
            if (parsed != null && parsed.signum() > 0) {
                return parsed;
            }
        }
        return null;
    }

    static String figure(BigDecimal value) {
        BigDecimal scaled = value.setScale(6, RoundingMode.HALF_UP).stripTrailingZeros();
        if (scaled.scale() < 0) {
            scaled = scaled.setScale(0);
        }
        return scaled.toPlainString();
    }

    static BigDecimal parse(String raw) {
        if (raw == null || raw.isBlank() || UNAVAILABLE.equals(raw)) {
            return null;
        }
        try {
            return new BigDecimal(raw.trim().replace(",", "").replace("_", ""));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static boolean hard(String id, String name) {
        return "copper".equals(id) || "diamond".equals(id) || "铜锭".equals(name) || "钻石".equals(name);
    }

    private static String first(JsonObject row, String... keys) {
        for (String key : keys) {
            String value = text(row, key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    static String text(JsonObject data, String key) {
        if (data == null || !data.has(key) || data.get(key).isJsonNull() || !data.get(key).isJsonPrimitive()) {
            return null;
        }
        String value = data.get(key).getAsString();
        return value == null || value.isBlank() ? null : value;
    }

    private static List<JsonObject> array(JsonObject data, String key) {
        List<JsonObject> rows = new ArrayList<>();
        if (data == null || !data.has(key) || !data.get(key).isJsonArray()) {
            return rows;
        }
        JsonArray array = data.getAsJsonArray(key);
        for (JsonElement element : array) {
            if (element.isJsonObject()) {
                rows.add(element.getAsJsonObject());
            }
        }
        return rows;
    }
}

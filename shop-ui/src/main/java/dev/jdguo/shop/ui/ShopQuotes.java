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

    public record Item(String id, String name, String diamondPrice, String notes, String category) {
        public Item(String id, String name, String diamondPrice) {
            this(id, name, diamondPrice, null, null);
        }
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

    /** True only when the server asked the screen to open. A join catalog does not. */
    public static boolean shouldPresent(JsonObject data) {
        return data != null && data.has("open") && data.get("open").isJsonPrimitive() && data.get("open").getAsBoolean();
    }

    public static List<Item> items(JsonObject data) {
        List<Item> rows = new ArrayList<>();
        rows.addAll(readItems(data, "listings", "item"));
        if (rows.isEmpty()) {
            rows.addAll(readItems(data, "items", "id"));
        }
        return rows;
    }

    /** Tabs that already have a server row. An empty catalog stays empty. */
    public static List<String> categories(List<Item> items) {
        java.util.LinkedHashSet<String> found = new java.util.LinkedHashSet<>();
        for (Item item : items) {
            found.add(tab(item));
        }
        List<String> order = java.util.List.of(
                "建筑方块", "染色方块", "自然方块", "功能方块", "红石",
                "工具与实用物品", "战斗", "食物与饮品", "原材料", "其他");
        List<String> tabs = new ArrayList<>();
        for (String name : order) {
            if (found.remove(name)) {
                tabs.add(name);
            }
        }
        tabs.addAll(found);
        return tabs;
    }

    public static String tab(Item item) {
        if (item != null && item.category() != null && !item.category().isBlank()) {
            return item.category();
        }
        return "其他";
    }

    public static List<Item> inCategory(List<Item> items, String category) {
        List<Item> rows = new ArrayList<>();
        for (Item item : items) {
            if (tab(item).equals(category)) {
                rows.add(item);
            }
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
        return figure(inNotes.divide(perNote, 4, RoundingMode.HALF_UP));
    }

    /**
     * Amount due in the selected currency.
     * When that currency is the one the server already priced, its {@code notes} figure is used.
     * Otherwise the diamond anchor is converted, and a missing quote stays 暂无牌价.
     */
    public static String due(JsonObject data, Item item, String currencyId) {
        if (item == null) {
            return UNAVAILABLE;
        }
        if (item.notes() != null && serverCurrency(data, currencyId)) {
            String shown = figure(parse(item.notes()));
            return shown == null ? UNAVAILABLE : shown;
        }
        return unitPrice(data, item.diamondPrice(), currencyId);
    }

    public static boolean serverCurrency(JsonObject data, String currencyId) {
        String key = text(data, "currencyKey");
        if (key == null) {
            return NOTE.equals(currencyId);
        }
        if (key.equals(currencyId)) {
            return true;
        }
        return NOTE.equals(currencyId) && ("note".equals(key) || "银行券".equals(text(data, "currency")));
    }

    /** Quantity times the unit price. A total below one 银行券 is still a real total. */
    public static String total(String unitPrice, String quantity) {
        BigDecimal unit = parse(unitPrice);
        BigDecimal count = parse(quantity);
        if (unit == null || count == null) {
            return UNAVAILABLE;
        }
        return figure(unit.multiply(count));
    }

    /** A positive total below one 银行券 is still payable. */
    public static boolean blocksSmallTotal(String total) {
        return false;
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
        if (value == null) {
            return null;
        }
        return value.setScale(4, RoundingMode.HALF_UP).toPlainString();
    }

    private static List<Item> readItems(JsonObject data, String arrayName, String idKey) {
        List<Item> rows = new ArrayList<>();
        for (JsonObject row : array(data, arrayName)) {
            String id = text(row, idKey);
            if (id == null) {
                id = text(row, "id");
            }
            String name = text(row, "name");
            if (id == null || name == null) {
                continue;
            }
            rows.add(new Item(id, name, first(row, "diamond", "anchor", "diamondPrice", "diamonds", "priceDiamonds"),
                    text(row, "notes"), text(row, "category")));
        }
        return rows;
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

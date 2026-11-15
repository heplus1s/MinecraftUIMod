package dev.jdguo.shop.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopQuotesTest {
    @Test
    void currencyListKeepsBankNotesAndPersonalNotesAndDropsHardCurrency() {
        JsonObject data = sample();
        List<ShopQuotes.Pay> pays = ShopQuotes.payCurrencies(data);

        assertEquals("note", pays.get(0).id());
        assertEquals("银行券", pays.get(0).label());
        assertEquals(3, pays.size());
        assertEquals("个人银行券 张三", pays.get(1).label());
        assertEquals("个人银行券 李四", pays.get(2).label());
        assertFalse(ShopQuotes.offersHardCurrency(pays));
        assertTrue(pays.stream().noneMatch(pay -> "copper".equals(pay.id()) || "diamond".equals(pay.id())));
        assertEquals(3, ShopQuotes.columns(480));
        assertEquals(2, ShopQuotes.columns(400));
    }

    @Test
    void unitPriceUsesTheLiveQuoteAndDoesNotInventOne() {
        JsonObject data = sample();

        assertEquals("2000.0000", ShopQuotes.unitPrice(data, "2", "note"));
        assertEquals("800.0000", ShopQuotes.unitPrice(data, "2", "zhang"));
        assertEquals("0.2500", ShopQuotes.total("0.2500", "1"));
        assertFalse(ShopQuotes.blocksSmallTotal("0.2500"));
        assertEquals(ShopQuotes.UNAVAILABLE, ShopQuotes.unitPrice(data, null, "note"));
        assertEquals(ShopQuotes.UNAVAILABLE, ShopQuotes.unitPrice(data, "2", "li"));
        assertEquals("buy minecraft:diamond note 1", ShopActions.buy("minecraft:diamond", "note", "1"));
        assertEquals("sell minecraft:oak_log zhang 4", ShopActions.sell("minecraft:oak_log", "zhang", "4"));

        JsonObject bare = new JsonObject();
        assertEquals(List.of(new ShopQuotes.Pay("note", "银行券")), ShopQuotes.payCurrencies(bare));
        assertEquals(ShopQuotes.UNAVAILABLE, ShopQuotes.unitPrice(bare, "2", "note"));
        assertFalse(ShopQuotes.shouldPresent(bare));
        bare.addProperty("open", true);
        assertTrue(ShopQuotes.shouldPresent(bare));
    }

    @Test
    void listingsKeepServerRowsAndDoNotInventAny() {
        JsonObject data = new JsonObject();
        JsonArray listings = new JsonArray();
        listings.add(listing("minecraft:oak_log", "橡木原木", "0.002", "2.0000", "自然方块"));
        listings.add(listing("minecraft:bread", "面包", "0.01", null, "食物与饮品"));
        data.add("listings", listings);
        data.addProperty("currencyKey", "note");

        List<ShopQuotes.Item> items = ShopQuotes.items(data);
        assertEquals(2, items.size());
        assertEquals(List.of("自然方块", "食物与饮品"), ShopQuotes.categories(items));
        assertEquals("2.0000", ShopQuotes.due(data, items.get(0), "note"));
        assertEquals(ShopQuotes.UNAVAILABLE, ShopQuotes.due(data, items.get(1), "zhang"));
        assertTrue(ShopQuotes.items(new JsonObject()).isEmpty());
        assertEquals(ShopLayout.frameX(800), ShopLayout.frameX(800));
        assertEquals(ShopLayout.frameY(1), ShopLayout.frameY(1));
        assertTrue(ShopLayout.currencyHeight(true, 3) > ShopLayout.currencyHeight(false, 3));
        assertEquals(ShopLayout.frameH(400, 1), ShopLayout.frameH(400, 1));
    }

    private static JsonObject listing(String item, String name, String diamond, String notes, String category) {
        JsonObject row = new JsonObject();
        row.addProperty("item", item);
        row.addProperty("name", name);
        row.addProperty("diamond", diamond);
        if (notes != null) {
            row.addProperty("notes", notes);
        }
        row.addProperty("category", category);
        return row;
    }

    private static JsonObject sample() {
        JsonObject data = new JsonObject();
        JsonArray currencies = new JsonArray();
        currencies.add(money("copper", "铜锭", null));
        currencies.add(money("diamond", "钻石", null));
        currencies.add(money("note", "银行券", "1000"));
        data.add("currencies", currencies);
        JsonArray notes = new JsonArray();
        notes.add(note("zhang", "张三", "2.5"));
        notes.add(note("li", "李四", null));
        data.add("notes", notes);
        return data;
    }

    private static JsonObject money(String id, String name, String unitsPerDiamond) {
        JsonObject row = new JsonObject();
        row.addProperty("id", id);
        row.addProperty("name", name);
        if (unitsPerDiamond != null) {
            row.addProperty("unitsPerDiamond", unitsPerDiamond);
        }
        return row;
    }

    private static JsonObject note(String id, String name, String price) {
        JsonObject row = new JsonObject();
        row.addProperty("id", id);
        row.addProperty("name", name);
        if (price != null) {
            row.addProperty("price", price);
        }
        return row;
    }
}

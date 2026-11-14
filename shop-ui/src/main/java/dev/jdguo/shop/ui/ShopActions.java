package dev.jdguo.shop.ui;

/** Payloads the shop core can accept once the reserved UI hook is wired. */
public final class ShopActions {
    private ShopActions() {
    }

    public static String buy(String itemId, String currencyId, String quantity) {
        return "buy " + itemId + " " + currencyId + " " + quantity;
    }

    public static String sell(String itemId, String currencyId, String quantity) {
        return "sell " + itemId + " " + currencyId + " " + quantity;
    }
}

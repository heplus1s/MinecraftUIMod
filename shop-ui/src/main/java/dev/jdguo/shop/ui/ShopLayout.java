package dev.jdguo.shop.ui;

/**
 * The currency column and the catalog do not share a height.
 * Opening the currency list grows only the left column.
 */
public final class ShopLayout {
    private ShopLayout() {
    }

    public static int currencyWidth(int screenWidth) {
        return Math.min(168, Math.max(120, screenWidth / 5));
    }

    public static int currencyHeight(boolean open, int options) {
        if (!open) {
            return 20;
        }
        return 20 + Math.min(90, Math.max(36, Math.max(1, options) * 18));
    }

    public static int frameX(int screenWidth) {
        return 8 + currencyWidth(screenWidth) + 8;
    }

    public static int frameW(int screenWidth) {
        return Math.max(40, screenWidth - frameX(screenWidth) - 8);
    }

    /** Tabs sit above the catalog. The currency drawer does not move this edge. */
    public static int frameY(int tabRows) {
        return 8 + Math.max(1, tabRows) * 22 + 6;
    }

    public static int frameH(int screenHeight, int tabRows) {
        return Math.max(72, screenHeight - frameY(tabRows) - 36);
    }
}

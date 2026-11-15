package dev.jdguo.market.ui;

/** Coordinates are GUI-scaled pixels, not physical display pixels. */
public final class MarketLayout {
    public record Box(int x, int y, int width, int height) {}
    public record Layout(Box listings, Box details, Box order, boolean compact, int bottom) {}
    private MarketLayout() {}
    public static Layout at(int width, int height) {
        int usable = Math.max(1, width - 16), top = 62, bottom = Math.max(top + 1, height - 48);
        int h = bottom - top;
        if (width < 660) {
            Box full = new Box(8, top, usable, h);
            return new Layout(full, full, full, true, bottom);
        }
        int left = usable * 30 / 100, right = usable * 30 / 100;
        return new Layout(new Box(8, top, left, h),
                new Box(8 + left + 6, top, usable - left - right - 12, h),
                new Box(width - 8 - right, top, right, h), false, bottom);
    }
}

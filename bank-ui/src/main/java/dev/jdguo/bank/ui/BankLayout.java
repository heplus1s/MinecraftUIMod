package dev.jdguo.bank.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * Positions the chrome so the function buttons stay on screen and are not covered
 * by a list. Two rows, left-aligned. Close is alone at the bottom right.
 */
public final class BankLayout {
    public static final int EDGE = 8;
    public static final int GAP = 4;
    public static final int BUTTON_HEIGHT = 20;

    public record Box(String label, int x, int y, int width, int height) {
        public int right() {
            return x + width;
        }

        public int bottom() {
            return y + height;
        }

        public boolean fits(int screenWidth, int screenHeight) {
            return x >= 0 && y >= 0 && right() <= screenWidth && bottom() <= screenHeight;
        }
    }

    private BankLayout() {
    }

    public static int functionTop(int screenHeight) {
        return screenHeight - EDGE - BUTTON_HEIGHT - GAP - BUTTON_HEIGHT;
    }

    public static int contentBottom(int screenHeight) {
        return functionTop(screenHeight) - GAP;
    }

    public static List<Box> functionTabs(int screenWidth, int screenHeight) {
        Box close = closeBox(screenWidth, screenHeight);
        int available = close.x() - GAP - EDGE;
        int tabWidth = Math.min(72, Math.max(36, (available - 2 * GAP) / 3));
        List<Box> boxes = new ArrayList<>();
        String[] first = {"储蓄", "存入", "取出"};
        String[] second = {"兑换", "汇票", "交易"};
        int y = functionTop(screenHeight);
        addRow(boxes, first, EDGE, y, tabWidth);
        addRow(boxes, second, EDGE, y + BUTTON_HEIGHT + GAP, tabWidth);
        return boxes;
    }

    public static Box closeBox(int screenWidth, int screenHeight) {
        int width = 64;
        int y = functionTop(screenHeight) + BUTTON_HEIGHT + GAP;
        return new Box(BankChrome.CLOSE, screenWidth - EDGE - width, y, width, BUTTON_HEIGHT);
    }

    public static Box issuanceBox(int screenWidth) {
        int width = 96;
        return new Box(BankChrome.ISSUANCE, screenWidth - EDGE - width, EDGE, width, BUTTON_HEIGHT);
    }

    public static Box managementBox(int screenWidth) {
        int width = 120;
        return new Box(BankChrome.MANAGEMENT, screenWidth - EDGE - width, EDGE + BUTTON_HEIGHT + GAP, width, BUTTON_HEIGHT);
    }

    public static int headerRight(int screenWidth, boolean admin) {
        Box block = admin ? managementBox(screenWidth) : issuanceBox(screenWidth);
        return block.x() - GAP;
    }

    public static int contentTop(int headerLines, boolean admin) {
        int header = EDGE + Math.max(1, headerLines) * 12;
        int buttons = admin ? managementBox(800).bottom() : issuanceBox(800).bottom();
        return Math.max(header, buttons) + GAP;
    }

    private static void addRow(List<Box> boxes, String[] labels, int x, int y, int width) {
        for (String label : labels) {
            boxes.add(new Box(label, x, y, width, BUTTON_HEIGHT));
            x += width + GAP;
        }
    }
}

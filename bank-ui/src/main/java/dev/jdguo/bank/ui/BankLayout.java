package dev.jdguo.bank.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * Left rate column, centered main area, two function rows, and a separate close row.
 * Top-right buttons are stacked with a gap so their hit boxes do not touch.
 */
public final class BankLayout {
    public static final int EDGE = 8;
    public static final int GAP = 4;
    public static final int BUTTON_HEIGHT = 20;
    public static final int TOP_GAP = 8;

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

        public boolean intersects(Box other) {
            return x < other.right() && other.x < right() && y < other.bottom() && other.y < bottom();
        }
    }

    private BankLayout() {
    }

    public static int labelWidth(String label) {
        return label.codePointCount(0, label.length()) * 12 + 18;
    }

    public static int columnWidth(int screenWidth) {
        int room = screenWidth - EDGE * 2 - GAP * 2 - 150;
        return Math.max(96, Math.min(132, room));
    }

    public static int columnRight(int screenWidth) {
        return EDGE + columnWidth(screenWidth);
    }

    public static int mainLeft(int screenWidth) {
        return columnRight(screenWidth) + GAP * 2;
    }

    public static int mainRight(int screenWidth) {
        return screenWidth - EDGE;
    }

    public static int mainWidth(int screenWidth) {
        return Math.max(0, mainRight(screenWidth) - mainLeft(screenWidth));
    }

    public static int closeY(int screenHeight) {
        return screenHeight - EDGE - BUTTON_HEIGHT;
    }

    public static int row2Y(int screenHeight) {
        return closeY(screenHeight) - GAP - BUTTON_HEIGHT;
    }

    public static int row1Y(int screenHeight) {
        return row2Y(screenHeight) - GAP - BUTTON_HEIGHT;
    }

    public static int contentBottom(int screenHeight) {
        return row1Y(screenHeight) - GAP;
    }

    public static Box issuanceBox(int screenWidth) {
        return topRight(screenWidth, EDGE, BankChrome.ISSUANCE, labelWidth(BankChrome.ISSUANCE));
    }

    public static Box managementBox(int screenWidth) {
        return topRight(screenWidth, EDGE + BUTTON_HEIGHT + TOP_GAP, BankChrome.MANAGEMENT, labelWidth(BankChrome.MANAGEMENT));
    }

    public static Box topRight(int screenWidth, int y, String label, int width) {
        return new Box(label, screenWidth - EDGE - width, y, width, BUTTON_HEIGHT);
    }

    public static int contentTop(boolean admin) {
        int buttons = admin ? EDGE + BUTTON_HEIGHT + TOP_GAP + BUTTON_HEIGHT : EDGE + BUTTON_HEIGHT;
        return buttons + GAP;
    }

    public static Box closeBox(int screenWidth, int screenHeight) {
        int width = labelWidth(BankChrome.CLOSE);
        return new Box(BankChrome.CLOSE, screenWidth - EDGE - width, closeY(screenHeight), width, BUTTON_HEIGHT);
    }

    public static List<Box> functionTabs(int screenWidth, int screenHeight) {
        List<Box> boxes = new ArrayList<>();
        boxes.addAll(row(EDGE, row1Y(screenHeight), BankChrome.ROW_ONE));
        boxes.addAll(row(EDGE, row2Y(screenHeight), BankChrome.ROW_TWO));
        return boxes;
    }

    public static List<Box> row(int x, int y, List<String> labels) {
        List<Box> boxes = new ArrayList<>();
        int cursor = x;
        for (String label : labels) {
            int width = labelWidth(label);
            boxes.add(new Box(label, cursor, y, width, BUTTON_HEIGHT));
            cursor += width + GAP;
        }
        return boxes;
    }
}

package dev.jdguo.bank.ui;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BankLayoutTest {
    @Test
    void chromeStaysOnScreenAndCloseStaysClearOfTheFunctionTabs() {
        for (int width : new int[] {320, 427, 640, 960}) {
            for (int height : new int[] {240, 360, 540}) {
                List<BankLayout.Box> boxes = new ArrayList<>(BankLayout.functionTabs(width, height));
                BankLayout.Box close = BankLayout.closeBox(width, height);
                boxes.add(close);
                boxes.add(BankLayout.issuanceBox(width));
                boxes.add(BankLayout.managementBox(width));
                for (BankLayout.Box box : boxes) {
                    assertTrue(box.fits(width, height), box.label() + " overflows " + width + "x" + height);
                }
                for (BankLayout.Box tab : BankLayout.functionTabs(width, height)) {
                    assertTrue(gap(tab, close), tab.label() + " hits 关闭 at " + width);
                }
                assertEquals(6, BankLayout.functionTabs(width, height).size());
                assertTrue(BankLayout.contentBottom(height) > BankLayout.contentTop(5, true));
            }
        }
    }

    private static boolean gap(BankLayout.Box tab, BankLayout.Box close) {
        boolean horizontal = tab.right() <= close.x() || close.right() <= tab.x();
        boolean vertical = tab.bottom() <= close.y() || close.bottom() <= tab.y();
        return horizontal || vertical;
    }
}

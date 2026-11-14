package dev.jdguo.bank.ui;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BankLayoutTest {
    @Test
    void chromeFitsAndTheTopButtonsDoNotOverlap() {
        for (int width : new int[] {360, 427, 640, 960}) {
            for (int height : new int[] {240, 360, 540}) {
                List<BankLayout.Box> tabs = BankLayout.functionTabs(width, height);
                BankLayout.Box close = BankLayout.closeBox(width, height);
                BankLayout.Box issue = BankLayout.issuanceBox(width);
                BankLayout.Box manage = BankLayout.managementBox(width);
                List<BankLayout.Box> boxes = new ArrayList<>(tabs);
                boxes.add(close);
                boxes.add(issue);
                boxes.add(manage);
                for (BankLayout.Box box : boxes) {
                    assertTrue(box.fits(width, height), box.label() + " overflows " + width + "x" + height);
                }
                for (BankLayout.Box tab : tabs) {
                    assertTrue(!tab.intersects(close), tab.label() + " hits 关闭");
                }
                assertTrue(!issue.intersects(manage));
                assertTrue(manage.y() - issue.bottom() >= 6);
                assertTrue(issue.x() >= BankLayout.mainLeft(width));
                assertTrue(manage.x() >= BankLayout.mainLeft(width));
                assertTrue(BankLayout.mainLeft(width) > BankLayout.columnRight(width));
                assertTrue(BankLayout.contentBottom(height) > BankLayout.contentTop(true));
                assertEquals(6, tabs.size());
                assertEquals("硬通货兑换", tabs.get(3).label());
                assertEquals("个人银行券交易", tabs.get(5).label());
            }
        }
    }
}

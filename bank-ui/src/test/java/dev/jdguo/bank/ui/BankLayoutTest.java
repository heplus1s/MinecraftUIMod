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
                assertEquals(7, tabs.size());
                assertEquals("硬通货兑换", tabs.get(3).label());
                assertEquals("开立汇票", tabs.get(4).label());
                assertEquals("兑付汇票", tabs.get(5).label());
                assertEquals("个人银行券交易", tabs.get(6).label());
                assertTrue(manage.width() >= BankLayout.labelWidth(BankChrome.MANAGEMENT));
                int top = BankLayout.contentTop(true);
                int bottom = BankLayout.contentBottom(height);
                for (int gap : new int[] {28, 36, 40}) {
                    BankLayout.Band band = BankLayout.band(top, bottom, gap);
                    assertTrue(band.separated(), width + "x" + height + " gap " + gap + " " + band);
                    assertTrue(band.listTop() >= top);
                    assertTrue(band.actionY() + 18 <= bottom);
                    assertTrue(BankLayout.mainLeft(width) > BankLayout.columnRight(width));
                }
                int buttonWidth = BankLayout.labelWidth(BankBook.BACKED_BUTTON);
                int textWidth = BankBook.BACKED_EXPLANATION.codePointCount(0, BankBook.BACKED_EXPLANATION.length()) * 9;
                int block = BankLayout.backedBlockHeight(BankLayout.mainWidth(width), buttonWidth, textWidth);
                BankLayout.ManagementPane pane = BankLayout.management(top, bottom, 6, block);
                assertTrue(pane.separated(), width + "x" + height + " management " + pane);
                assertTrue(pane.bookBesideList());
                assertTrue(pane.backedY() + 18 <= bottom);
                assertTrue(pane.backedY() >= pane.fieldY());
                BankLayout.BackedControl control = BankLayout.backedControl(width, pane.backedY(), buttonWidth, textWidth);
                assertTrue(control.inside(BankLayout.mainLeft(width), BankLayout.mainRight(width)), control.toString());
                assertTrue(control.buttonX() > BankLayout.columnRight(width));
                if (pane.backedTop() < pane.backedY()) {
                    assertTrue(control.textY() >= pane.backedTop());
                    assertTrue(control.textY() >= pane.fieldY());
                    assertTrue(control.textY() + control.textLines() * 12 <= control.buttonY());
                } else if (control.textX() >= control.buttonX() + control.buttonWidth()) {
                    assertTrue(control.textY() >= control.buttonY());
                }
            }
        }
    }
}

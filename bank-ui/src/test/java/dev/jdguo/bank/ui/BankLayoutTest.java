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
                List<BankLayout.Box> boxes = new ArrayList<>(tabs);
                boxes.add(close);
                boxes.add(issue);
                for (BankLayout.Box box : boxes) {
                    assertTrue(box.fits(width, height), box.label() + " overflows " + width + "x" + height);
                }
                assertEquals(close.y(), tabs.get(3).y(), "lower nav row shares 关闭's row");
                assertEquals(close.bottom(), tabs.get(3).bottom());
                for (BankLayout.Box tab : tabs) {
                    if (tab.y() != close.y()) {
                        assertTrue(!tab.intersects(close), tab.label() + " hits 关闭");
                    }
                    assertTrue(!tab.intersects(issue), tab.label() + " hits 个人发行");
                }
                if (width >= 427) {
                    for (int i = 3; i < tabs.size(); i++) {
                        assertTrue(!tabs.get(i).intersects(close), tabs.get(i).label() + " hits 关闭");
                    }
                }
                assertTrue(issue.x() >= BankLayout.mainLeft(width));
                assertTrue(BankLayout.mainLeft(width) > BankLayout.columnRight(width));
                assertEquals(BankLayout.EDGE + BankLayout.BUTTON_HEIGHT + BankLayout.GAP, BankLayout.contentTop());
                assertTrue(BankLayout.contentBottom(height) > BankLayout.contentTop());
                assertEquals(7, tabs.size());
                assertEquals("硬通货兑换", tabs.get(3).label());
                assertEquals("开立汇票", tabs.get(4).label());
                assertEquals("兑付汇票", tabs.get(5).label());
                assertEquals("个人银行券交易", tabs.get(6).label());
                assertEquals(20, BankLayout.drawerHeight(false, 4));
                assertTrue(BankLayout.drawerHeight(true, 4) > 20);
                assertTrue(BankLayout.drawerHeight(true, 8) <= 20 + 72);
                for (int rows : new int[] {1, 2}) {
                    BankLayout.PlayerControls controls = BankLayout.playerControls(height, rows);
                    assertTrue(controls.drawerY() >= BankLayout.contentTop(), controls.toString());
                    assertTrue(controls.amountBelowDrawers(rows), width + "x" + height + " rows " + rows + " " + controls);
                    assertTrue(controls.actionY() + 18 <= BankLayout.contentBottom(height), controls.toString());
                    int openRoom = controls.fieldY() - 12 - (controls.drawerY() + (rows - 1) * 24);
                    assertTrue(openRoom >= 36, "open drawer room " + openRoom + " " + controls);
                }
                int headerBottom = BankLayout.contentTop() + 36;
                for (boolean firstOpen : new boolean[] {false, true}) {
                    for (boolean secondOpen : new boolean[] {false, true}) {
                        int limit = BankLayout.contentBottom(height);
                        BankLayout.MarketStack market = BankLayout.marketStack(headerBottom, limit, firstOpen, 4, secondOpen, 3);
                        assertTrue(market.firstY() >= headerBottom, market.toString());
                        assertTrue(market.secondY() >= market.firstY() + market.firstH() + 4, market.toString());
                        assertTrue(market.fieldY() >= market.secondY() + market.secondH() + 4, market.toString());
                        assertTrue(market.quickY() >= market.fieldY() + 16, market.toString());
                        assertTrue(market.actionY() >= market.quickY() + 18, market.toString());
                        if (height >= 360) {
                            assertTrue(market.separated(limit), width + "x" + height + " " + market);
                            if (firstOpen) {
                                assertTrue(market.firstH() >= 38, market.toString());
                            }
                            if (secondOpen) {
                                assertTrue(market.secondH() >= 38, market.toString());
                            }
                        }
                    }
                }
                BankLayout.AdminBands admin = BankLayout.adminBands(width, height);
                assertTrue(admin.separated(), width + "x" + height + " admin " + admin);
                assertTrue(admin.centered(width), admin.toString());
                assertTrue(admin.fits(height), admin.toString());
                assertTrue(admin.listW() * 2 < width);
            }
        }
    }
}

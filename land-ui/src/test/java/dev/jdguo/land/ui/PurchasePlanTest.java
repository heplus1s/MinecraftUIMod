package dev.jdguo.land.ui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PurchasePlanTest {
    @Test
    void claimAndJoinStayOnWholeChunks() {
        assertEquals("land claim", PurchasePlan.claim(false, false).command());
        assertFalse(PurchasePlan.claim(true, true).allowed());

        long neighbor = ChunkGrid.pack(4, 5);
        assertTrue(PurchasePlan.adjacentToAny(4, 6, List.of(neighbor)));
        assertFalse(PurchasePlan.adjacentToAny(5, 6, List.of(neighbor)));

        PurchasePlan.Decision join = PurchasePlan.join("东边林地", false, true);
        assertEquals("land claim join 东边林地", join.command());
        assertFalse(PurchasePlan.join("东边林地", true, true).allowed());
        assertFalse(PurchasePlan.join("东边林地", false, false).allowed());
    }

    @Test
    void splitUsesACleanNameOrNone() {
        assertEquals("land split", PurchasePlan.split("  ").command());
        assertEquals("land split 河边", PurchasePlan.split("河边").command());
        assertFalse(PurchasePlan.split("坏\n名字").allowed());
        assertEquals("未命名", PurchasePlan.token("", "未命名"));
        assertEquals("河边", PurchasePlan.token("河边", "abcd"));
    }
}

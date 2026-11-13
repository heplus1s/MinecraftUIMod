package dev.jdguo.land.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapPickTest {
    @AfterEach
    void clean() {
        MapPick.resetForTests();
    }

    @Test
    void blockTextSelectsTheWholeChunk() {
        MapPick.toggleBlockText("minecraft:overworld", "17", "-1");

        assertTrue(MapPick.contains("minecraft:overworld", 1, -1));
        assertFalse(MapPick.contains("minecraft:overworld", 17, -1));
        assertEquals(1, MapPick.selected().size());
        assertTrue(MapPick.notice().contains("16×16"));
    }

    @Test
    void secondPickRemovesTheSameChunk() {
        MapPick.toggle("minecraft:the_nether", 3, 4);
        MapPick.toggle("minecraft:the_nether", 3, 4);

        assertTrue(MapPick.selected().isEmpty());
        assertTrue(MapPick.notice().contains("取消"));
    }

    @Test
    void refusesACoordinateThatIsNotANumber() {
        MapPick.toggleBlockText("minecraft:overworld", "东", "1");

        assertTrue(MapPick.selected().isEmpty());
        assertTrue(MapPick.notice().contains("不是数字"));
    }
}

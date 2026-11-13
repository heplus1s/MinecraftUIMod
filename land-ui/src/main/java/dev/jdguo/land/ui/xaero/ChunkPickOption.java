package dev.jdguo.land.ui.xaero;

import dev.jdguo.land.ui.MapPick;
import net.minecraft.client.gui.screen.Screen;
import xaero.map.gui.IRightClickableElement;
import xaero.map.gui.dropdown.rightclick.RightClickOption;

/** Right-click entry on Xaero's world map. It selects one whole chunk. */
public final class ChunkPickOption extends RightClickOption {
    private final String dimension;
    private final int chunkX;
    private final int chunkZ;

    public ChunkPickOption(String name, int index, IRightClickableElement element, String dimension, int chunkX, int chunkZ) {
        super(name, index, element);
        this.dimension = dimension;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    @Override
    public void onAction(Screen screen) {
        MapPick.toggle(dimension, chunkX, chunkZ);
    }
}

package dev.jdguo.land.ui.mixin;

import dev.jdguo.land.ui.MapPick;
import dev.jdguo.land.ui.xaero.ChunkPickOption;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.gui.GuiMap;
import xaero.map.gui.IRightClickableElement;
import xaero.map.gui.dropdown.rightclick.RightClickOption;

import java.util.ArrayList;

/**
 * Adds 「选取此区块」 on Xaero's world map, both as a button and as a right-click entry.
 * The stored coordinate is the chunk, never a sub-chunk square.
 */
@Mixin(value = GuiMap.class, remap = false)
public class WorldMapPickMixin {
    @Shadow
    private int mouseBlockPosX;
    @Shadow
    private int mouseBlockPosZ;
    @Shadow
    private RegistryKey<World> mouseBlockDim;
    @Shadow
    private int rightClickX;
    @Shadow
    private int rightClickZ;
    @Shadow
    private RegistryKey<World> rightClickDim;

    @Unique
    private ButtonWidget landui$pick;

    @Inject(method = "method_25426", at = @At("TAIL"), remap = false)
    private void landui$button(CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        landui$pick = ButtonWidget.builder(Text.literal("选取此区块"), button -> landui$toggle(
                        mouseBlockDim, mouseBlockPosX, mouseBlockPosZ))
                .dimensions(screen.width / 2 - 60, 6, 120, 20)
                .build();
        ((GuiMap) (Object) this).addButton(landui$pick);
    }

    @Inject(method = "method_25394", at = @At("TAIL"), remap = false)
    private void landui$label(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (landui$pick == null || mouseBlockDim == null) {
            return;
        }
        boolean selected = MapPick.contains(mouseBlockDim.getValue().toString(), mouseBlockPosX >> 4, mouseBlockPosZ >> 4);
        landui$pick.setMessage(Text.literal(selected ? "取消此区块" : "选取此区块"));
    }

    @Inject(method = "getRightClickOptions", at = @At("RETURN"), remap = false)
    private void landui$rightClick(CallbackInfoReturnable<ArrayList<RightClickOption>> cir) {
        ArrayList<RightClickOption> options = cir.getReturnValue();
        if (options == null || rightClickDim == null) {
            return;
        }
        String dimension = rightClickDim.getValue().toString();
        int chunkX = rightClickX >> 4;
        int chunkZ = rightClickZ >> 4;
        String name = MapPick.contains(dimension, chunkX, chunkZ) ? "取消此区块" : "选取此区块";
        options.add(new ChunkPickOption(name, options.size(), (IRightClickableElement) this, dimension, chunkX, chunkZ));
    }

    @Unique
    private static void landui$toggle(RegistryKey<World> dimension, int blockX, int blockZ) {
        if (dimension == null) {
            return;
        }
        MapPick.toggle(dimension.getValue().toString(), blockX >> 4, blockZ >> 4);
    }
}

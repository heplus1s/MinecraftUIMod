package dev.jdguo.land.ui.mixin;

import dev.jdguo.land.ui.MapPick;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.AXaeroMinimap;
import xaero.common.gui.GuiAddWaypoint;
import xaero.common.gui.ScreenBase;

/**
 * The minimap opens this screen when a player right-clicks the map to place a waypoint.
 * The extra button selects the whole chunk under those block coordinates.
 */
@Mixin(value = GuiAddWaypoint.class, remap = false)
public abstract class MinimapWaypointPickMixin extends ScreenBase {
    @Shadow
    private TextFieldWidget xTextField;
    @Shadow
    private TextFieldWidget zTextField;

    protected MinimapWaypointPickMixin(AXaeroMinimap modMain, Screen parent, Screen escape, Text title) {
        super(modMain, parent, escape, title);
    }

    @Inject(method = "method_25426", at = @At("TAIL"), remap = false)
    private void landui$pick(CallbackInfo ci) {
        addDrawableChild(ButtonWidget.builder(Text.literal("选取此区块"), button -> {
            MinecraftClient client = MinecraftClient.getInstance();
            String dimension = client.world == null ? "" : client.world.getRegistryKey().getValue().toString();
            MapPick.toggleBlockText(dimension, xTextField.getText(), zTextField.getText());
            String selected = MapPick.contains(dimension,
                    dev.jdguo.land.ui.ChunkGrid.chunkOfBlock(parse(xTextField.getText())),
                    dev.jdguo.land.ui.ChunkGrid.chunkOfBlock(parse(zTextField.getText())))
                    ? "取消此区块" : "选取此区块";
            button.setMessage(Text.literal(selected));
        }).dimensions(this.width / 2 - 60, 6, 120, 20).build());
    }

    private static int parse(String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (RuntimeException exception) {
            return 0;
        }
    }
}

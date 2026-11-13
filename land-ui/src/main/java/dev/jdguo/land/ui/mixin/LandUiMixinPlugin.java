package dev.jdguo.land.ui.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Map hooks exist only when that map mod is installed.
 * If the full landmod jar is also on the client, it already paints claims, so this jar does not register a second highlighter.
 */
public final class LandUiMixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        boolean landmod = FabricLoader.getInstance().isModLoaded("landmod");
        if (mixinClassName.endsWith("WorldMapRegistryMixin")) {
            return FabricLoader.getInstance().isModLoaded("xaeroworldmap") && !landmod;
        }
        if (mixinClassName.endsWith("MinimapRegistryMixin")) {
            return FabricLoader.getInstance().isModLoaded("xaerominimap") && !landmod;
        }
        if (mixinClassName.endsWith("WorldMapPickMixin")) {
            return FabricLoader.getInstance().isModLoaded("xaeroworldmap");
        }
        if (mixinClassName.endsWith("MinimapWaypointPickMixin")) {
            return FabricLoader.getInstance().isModLoaded("xaerominimap");
        }
        if (mixinClassName.endsWith("ClientPlayNetworkHandlerMixin")) {
            return landmod;
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}

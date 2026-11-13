package dev.jdguo.land.ui;

import dev.jdguo.land.ui.net.ClaimSnapshot;
import dev.jdguo.land.ui.xaero.XaeroRefresh;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LandUi implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("landmod-ui");
    public static final Identifier CLAIMS = new Identifier("landmod", "claims");

    private static KeyBinding openScreen;

    @Override
    public void onInitializeClient() {
        openScreen = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.landmod-ui.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "category.landmod-ui"));
        if (!FabricLoader.getInstance().isModLoaded("landmod")) {
            ClientPlayNetworking.registerGlobalReceiver(CLAIMS, (client, handler, buf, responseSender) -> {
                ClaimSnapshot snapshot;
                try {
                    snapshot = ClaimSnapshot.read(buf);
                } catch (RuntimeException exception) {
                    LOGGER.warn("Ignored a land claim sync packet", exception);
                    return;
                }
                client.execute(() -> {
                    ClientClaims.replace(snapshot);
                    XaeroRefresh.mark();
                });
            });
        }
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            XaeroRefresh.tick();
            while (openScreen.wasPressed()) {
                client.setScreen(new LandScreen());
            }
        });
    }
}

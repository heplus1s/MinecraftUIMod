package dev.jdguo.shop.ui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/** Client half of the optional shop screen. It only speaks if this jar is installed. */
public final class ShopUiClient implements ClientModInitializer {
    static final int PROTOCOL = 1;
    static final Identifier HELLO = new Identifier("shopmod", "hello");
    static final Identifier ACTION = new Identifier("shopmod", "action");
    static final Identifier SYNC = new Identifier("shopmod", "sync");

    private static JsonObject pending = new JsonObject();
    private static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.shopmod-ui.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "category.shopmod-ui"));
        ClientPlayNetworking.registerGlobalReceiver(SYNC, (client, handler, buf, response) -> {
            String json = buf.readString(32767);
            client.execute(() -> receive(client, json));
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            var buf = PacketByteBufs.create();
            buf.writeVarInt(PROTOCOL);
            ClientPlayNetworking.send(HELLO, buf);
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                present(client);
            }
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(literal("shopui").executes(context -> {
                    MinecraftClient.getInstance().execute(() -> present(MinecraftClient.getInstance()));
                    return 1;
                })));
    }

    /** Buy and sell use the server's four-field action packet. */
    static void sendTrade(boolean buy, String itemId, int count) {
        sendAction(buy ? "buy" : "sell", itemId, count, 1);
    }

    static void sendList() {
        sendAction("list", "", 1, 1);
    }

    private static void sendAction(String action, String itemId, int count, int page) {
        var buf = PacketByteBufs.create();
        buf.writeString(action, 32);
        buf.writeString(itemId == null ? "" : itemId, 128);
        buf.writeVarInt(Math.max(0, count));
        buf.writeVarInt(Math.max(1, page));
        ClientPlayNetworking.send(ACTION, buf);
    }

    private static void receive(MinecraftClient client, String json) {
        JsonObject data = JsonParser.parseString(json).getAsJsonObject();
        pending = data;
        if (client.currentScreen instanceof ShopScreen screen) {
            screen.show(data);
            return;
        }
        if (ShopQuotes.shouldPresent(data)) {
            client.setScreen(new ShopScreen(data));
        }
    }

    private static void present(MinecraftClient client) {
        if (client.currentScreen instanceof ShopScreen) {
            return;
        }
        client.setScreen(new ShopScreen(pending));
        sendList();
    }
}

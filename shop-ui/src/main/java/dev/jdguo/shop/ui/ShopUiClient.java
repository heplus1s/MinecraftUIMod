package dev.jdguo.shop.ui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

/** Client half of the optional shop screen. It only speaks if this jar is installed. */
public final class ShopUiClient implements ClientModInitializer {
    static final int PROTOCOL = 1;
    static final Identifier HELLO = new Identifier("shopmod", "hello");
    static final Identifier ACTION = new Identifier("shopmod", "action");
    static final Identifier SYNC = new Identifier("shopmod", "sync");

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(SYNC, (client, handler, buf, response) -> {
            String json = buf.readString(32767);
            client.execute(() -> open(client, json));
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            var buf = PacketByteBufs.create();
            buf.writeVarInt(PROTOCOL);
            ClientPlayNetworking.send(HELLO, buf);
        });
    }

    static void send(String action) {
        var buf = PacketByteBufs.create();
        buf.writeString(action, 200);
        ClientPlayNetworking.send(ACTION, buf);
    }

    private static void open(MinecraftClient client, String json) {
        JsonObject data = JsonParser.parseString(json).getAsJsonObject();
        if (client.currentScreen instanceof ShopScreen screen) {
            screen.show(data);
            return;
        }
        client.setScreen(new ShopScreen(data));
    }
}

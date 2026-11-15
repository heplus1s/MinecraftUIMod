package dev.jdguo.market.ui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class MarketUiClient implements ClientModInitializer {
    static final Identifier HELLO = new Identifier("marketmod", "hello");
    static final Identifier ACTION = new Identifier("marketmod", "action");
    static final Identifier SYNC = new Identifier("marketmod", "sync");
    static final Identifier RESULT = new Identifier("marketmod", "result");
    private static JsonObject latest = new JsonObject();
    @Override public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(SYNC, (client, handler, buf, response) -> {
            String json = buf.readString(262144);
            client.execute(() -> {
                try {
                    latest = JsonParser.parseString(json).getAsJsonObject();
                    if (client.currentScreen instanceof MarketScreen screen) screen.update(latest);
                } catch (RuntimeException ignored) { }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(RESULT, (client, handler, buf, response) -> {
            String json = buf.readString(262144);
            client.execute(() -> {
                try {
                    JsonObject result = JsonParser.parseString(json).getAsJsonObject();
                    if (client.currentScreen instanceof MarketScreen screen) screen.result(result);
                } catch (RuntimeException ignored) { }
            });
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> latest = new JsonObject());
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> dispatcher.register(literal("marketui").executes(context -> {
            MinecraftClient client = MinecraftClient.getInstance();
            client.execute(() -> {
                client.setScreen(new MarketScreen(latest));
                refresh();
            });
            return 1;
        })));
    }
    static boolean available() { return ClientPlayNetworking.canSend(HELLO) && ClientPlayNetworking.canSend(ACTION); }
    static boolean refresh() {
        if (!available()) return false;
        var buf = PacketByteBufs.create(); buf.writeVarInt(1); ClientPlayNetworking.send(HELLO, buf); return true;
    }
    static boolean action(String action) {
        if (!available()) return false;
        var buf = PacketByteBufs.create(); buf.writeString(action, 512); ClientPlayNetworking.send(ACTION, buf); return true;
    }
}

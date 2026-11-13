package dev.jdguo.land.ui.mixin;

import dev.jdguo.land.ui.ClientClaims;
import dev.jdguo.land.ui.LandUi;
import dev.jdguo.land.ui.net.ClaimSnapshot;
import dev.jdguo.land.ui.xaero.XaeroRefresh;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * When the full landmod jar is also installed, it already owns the claim channel.
 * Copy the bytes without moving the read index, so both sides see the same packet.
 */
@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onCustomPayload", at = @At("HEAD"))
    private void landui$peekClaims(CustomPayloadS2CPacket packet, CallbackInfo ci) {
        if (!LandUi.CLAIMS.equals(packet.getChannel())) {
            return;
        }
        PacketByteBuf data = packet.getData();
        int index = data.readerIndex();
        byte[] copy = new byte[data.readableBytes()];
        data.getBytes(index, copy);
        PacketByteBuf duplicate = new PacketByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(copy));
        ClaimSnapshot snapshot;
        try {
            snapshot = ClaimSnapshot.read(duplicate);
        } catch (RuntimeException exception) {
            LandUi.LOGGER.warn("Ignored a land claim sync packet", exception);
            return;
        }
        MinecraftClient.getInstance().execute(() -> {
            ClientClaims.replace(snapshot);
            XaeroRefresh.mark();
        });
    }
}

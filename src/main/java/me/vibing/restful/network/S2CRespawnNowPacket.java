package me.vibing.restful.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// server -> client: trigger immediate respawn
// Required because server cannot directly force respawn - client must
// send ServerboundClientCommandPacket.PERFORM_RESPAWN to trigger
// vanilla respawn logic with proper player state reset.
public record S2CRespawnNowPacket() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<S2CRespawnNowPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.parse("restful:respawn_now"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CRespawnNowPacket> STREAM_CODEC =
            StreamCodec.unit(new S2CRespawnNowPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

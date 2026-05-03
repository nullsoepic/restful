package me.vibing.restful.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RespawnNowPacket() implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<RespawnNowPacket> TYPE = 
            new CustomPacketPayload.Type<>(ResourceLocation.parse("restful:respawn_now"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, RespawnNowPacket> STREAM_CODEC = 
            StreamCodec.unit(new RespawnNowPacket());
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

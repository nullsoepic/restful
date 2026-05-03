package me.vibing.restful.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SelectBedPacket(int selectedIndex) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<SelectBedPacket> TYPE = 
            new CustomPacketPayload.Type<>(ResourceLocation.parse("restful:select_bed"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, SelectBedPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SelectBedPacket::selectedIndex,
            SelectBedPacket::new
    );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

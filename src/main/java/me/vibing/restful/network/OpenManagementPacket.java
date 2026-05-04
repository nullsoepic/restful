package me.vibing.restful.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

// server -> client: open bed management GUI with current beds
public record OpenManagementPacket(List<BedSelectionPacket.BedInfo> beds) implements CustomPacketPayload {
    
    public static final Type<OpenManagementPacket> TYPE = 
            new Type<>(ResourceLocation.parse("restful:open_management"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenManagementPacket> STREAM_CODEC = StreamCodec.composite(
            BedSelectionPacket.BedInfo.STREAM_CODEC.apply(ByteBufCodecs.list()), OpenManagementPacket::beds,
            OpenManagementPacket::new
    );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

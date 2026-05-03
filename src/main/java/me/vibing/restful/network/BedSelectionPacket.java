package me.vibing.restful.network;

import me.vibing.restful.BedData;
import me.vibing.restful.BedIdUtil;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record BedSelectionPacket(List<BedInfo> beds) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<BedSelectionPacket> TYPE = 
            new CustomPacketPayload.Type<>(ResourceLocation.parse("restful:bed_selection"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, BedSelectionPacket> STREAM_CODEC = StreamCodec.composite(
            BedInfo.STREAM_CODEC.apply(ByteBufCodecs.list()), BedSelectionPacket::beds,
            BedSelectionPacket::new
    );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    public record BedInfo(GlobalPos position, String name, String itemId, int index, String bedId, boolean isFavorite) {
        public static final StreamCodec<RegistryFriendlyByteBuf, BedInfo> STREAM_CODEC = StreamCodec.composite(
                GlobalPos.STREAM_CODEC, BedInfo::position,
                ByteBufCodecs.STRING_UTF8, BedInfo::name,
                ByteBufCodecs.STRING_UTF8, BedInfo::itemId,
                ByteBufCodecs.VAR_INT, BedInfo::index,
                ByteBufCodecs.STRING_UTF8, BedInfo::bedId,
                ByteBufCodecs.BOOL, BedInfo::isFavorite,
                BedInfo::new
        );
        
        public static BedInfo fromBedData(BedData data, int index, boolean isFavorite) {
            return new BedInfo(
                    data.position(),
                    data.displayName(),
                    data.bedItem().toString(),
                    index,
                    BedIdUtil.generateId(data.position()),
                    isFavorite
            );
        }
    }
}

package me.vibing.restful.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

// client -> server: bed actions (select, favorite, rename, remove, reorder)
public record C2SBedActionPacket(Action action, int index, Optional<String> data, List<Integer> order) implements CustomPacketPayload {

    public enum Action {
        SELECT,
        FAVORITE,
        RENAME,
        REMOVE,
        REORDER
    }

    public static final Type<C2SBedActionPacket> TYPE =
            new Type<>(ResourceLocation.parse("restful:bed_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SBedActionPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public C2SBedActionPacket decode(RegistryFriendlyByteBuf buf) {
            int actionId = ByteBufCodecs.VAR_INT.decode(buf);
            Action action = actionId >= 0 && actionId < Action.values().length ? Action.values()[actionId] : Action.SELECT;
            int index = ByteBufCodecs.VAR_INT.decode(buf);
            Optional<String> data = ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).decode(buf);
            List<Integer> order = ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()).decode(buf);
            return new C2SBedActionPacket(action, index, data, order);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, C2SBedActionPacket value) {
            ByteBufCodecs.VAR_INT.encode(buf, value.action.ordinal());
            ByteBufCodecs.VAR_INT.encode(buf, value.index);
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).encode(buf, value.data);
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()).encode(buf, value.order);
        }
    };

    // convenience constructors
    public static C2SBedActionPacket select(int index) {
        return new C2SBedActionPacket(Action.SELECT, index, Optional.empty(), List.of());
    }

    public static C2SBedActionPacket favorite(int index) {
        return new C2SBedActionPacket(Action.FAVORITE, index, Optional.empty(), List.of());
    }

    public static C2SBedActionPacket rename(int index, String newName) {
        return new C2SBedActionPacket(Action.RENAME, index, Optional.of(newName), List.of());
    }

    public static C2SBedActionPacket remove(int index) {
        return new C2SBedActionPacket(Action.REMOVE, index, Optional.empty(), List.of());
    }

    public static C2SBedActionPacket reorder(List<Integer> newOrder) {
        return new C2SBedActionPacket(Action.REORDER, 0, Optional.empty(), newOrder);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

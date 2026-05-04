package me.vibing.restful.network;

import me.vibing.restful.BedData;
import me.vibing.restful.BedTracker;
import me.vibing.restful.BedValidator;
import me.vibing.restful.Restful;
import net.minecraft.client.Minecraft;
import me.vibing.restful.client.BedManagementScreen;
import me.vibing.restful.client.BedSelectionScreen;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;

public class RestfulNetwork {

    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(Restful.MODID);

        registrar.playToClient(
                S2CBedListPacket.TYPE,
                S2CBedListPacket.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        Minecraft minecraft = Minecraft.getInstance();
                        if (minecraft.player != null) {
                            minecraft.execute(() -> {
                                minecraft.setScreen(new BedSelectionScreen(payload.beds()));
                            });
                        }
                    });
                }
        );

        registrar.playToClient(
                S2COpenManagementPacket.TYPE,
                S2COpenManagementPacket.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        Minecraft minecraft = Minecraft.getInstance();
                        if (minecraft.player != null) {
                            minecraft.execute(() -> {
                                minecraft.setScreen(new BedManagementScreen(payload.beds()));
                            });
                        }
                    });
                }
        );

        registrar.playToClient(
                S2CRespawnNowPacket.TYPE,
                S2CRespawnNowPacket.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        Minecraft minecraft = Minecraft.getInstance();
                        if (minecraft.player != null && minecraft.getConnection() != null) {
                            minecraft.execute(() -> {
                                minecraft.getConnection().send(
                                        new ServerboundClientCommandPacket(
                                                ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
                            });
                        }
                    });
                }
        );

        registrar.playToServer(
                C2SBedActionPacket.TYPE,
                C2SBedActionPacket.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer serverPlayer) {
                            BedTracker tracker = serverPlayer.getData(Restful.BED_TRACKER);
                            switch (payload.action()) {
                                case SELECT -> {
                                    var result = validateBed(payload.index(), serverPlayer, tracker);
                                    if (!result.valid()) {
                                        handleInvalidBed(serverPlayer, tracker, payload.index(), result.reason());
                                        return;
                                    }
                                    tracker.setSelectedBedIndex(payload.index());
                                    PacketDistributor.sendToPlayer(serverPlayer, new S2CRespawnNowPacket());
                                }
                                case FAVORITE -> {
                                    boolean current = tracker.isFavorite(payload.index());
                                    tracker.setFavorite(payload.index(), !current);
                                }
                                case RENAME -> payload.data().ifPresent(name -> tracker.renameBed(payload.index(), name));
                                case REMOVE -> tracker.removeBed(payload.index());
                                case REORDER -> {
                                    var oldBeds = new ArrayList<>(tracker.getBeds());
                                    tracker.clear();
                                    for (int i : payload.order()) {
                                        if (i >= 0 && i < oldBeds.size()) {
                                            BedData bed = oldBeds.get(i);
                                            tracker.addBed(bed.position(), bed.name(), bed.bedItem(), 100);
                                            if (bed.favorite()) {
                                                tracker.setFavorite(tracker.size() - 1, true);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    });
                }
        );
    }

    private record ValidationResult(boolean valid, ValidationFailureReason reason) {}

    private enum ValidationFailureReason {
        OUT_OF_BOUNDS, NULL_BED, MISSING_DIMENSION, INVALID_BLOCK
    }

    private static ValidationResult validateBed(int index, ServerPlayer player, BedTracker tracker) {
        if (index < 0 || index >= tracker.size()) {
            return new ValidationResult(false, ValidationFailureReason.OUT_OF_BOUNDS);
        }

        BedData bed = tracker.getBed(index);
        if (bed == null) {
            return new ValidationResult(false, ValidationFailureReason.NULL_BED);
        }

        ServerLevel targetLevel = player.server.getLevel(bed.position().dimension());
        if (targetLevel == null) {
            return new ValidationResult(false, ValidationFailureReason.MISSING_DIMENSION);
        }

        ensureChunkLoaded(targetLevel, bed.position().pos());

        if (!BedValidator.isValidRespawnPoint(player, bed.position())) {
            return new ValidationResult(false, ValidationFailureReason.INVALID_BLOCK);
        }

        return new ValidationResult(true, null);
    }

    private static void ensureChunkLoaded(ServerLevel level, net.minecraft.core.BlockPos pos) {
        level.getChunkSource().getChunk(pos.getX() >> 4, pos.getZ() >> 4, true);
    }

    private static void handleInvalidBed(ServerPlayer player, BedTracker tracker, int index, ValidationFailureReason reason) {
        if (reason == ValidationFailureReason.MISSING_DIMENSION || reason == ValidationFailureReason.INVALID_BLOCK) {
            tracker.removeBed(index);
        }

        if (tracker.isEmpty()) {
            PacketDistributor.sendToPlayer(player, new S2CRespawnNowPacket());
        } else {
            sendBedSelection(player);
        }
    }

    public static void sendBedSelection(ServerPlayer player) {
        BedTracker tracker = player.getData(Restful.BED_TRACKER);

        List<BedData> validBeds = new ArrayList<>();
        List<Boolean> validFavorites = new ArrayList<>();
        List<Integer> validIndices = new ArrayList<>();

        for (int i = 0; i < tracker.size(); i++) {
            BedData bed = tracker.getBed(i);
            if (bed == null) continue;

            var targetLevel = player.server.getLevel(bed.position().dimension());
            if (targetLevel == null) {
                Restful.LOGGER.debug("Bed {} dimension not found, removing", bed.position());
                continue;
            }

            ensureChunkLoaded(targetLevel, bed.position().pos());

            if (BedValidator.isValidRespawnPoint(player, bed.position())) {
                validBeds.add(bed);
                validFavorites.add(tracker.isFavorite(i));
                validIndices.add(i);
            } else {
                Restful.LOGGER.debug("Bed {} invalid, removing", bed.position());
            }
        }

        tracker.clear();
        for (int i = 0; i < validBeds.size(); i++) {
            tracker.addBed(validBeds.get(i).position(), validBeds.get(i).name(), validBeds.get(i).bedItem(), 100);
            tracker.setFavorite(i, validFavorites.get(i));
        }

        List<BedInfo> bedInfos = tracker.toBedInfoList();

        if (!bedInfos.isEmpty()) {
            PacketDistributor.sendToPlayer(player, new S2CBedListPacket(bedInfos));
        } else {
            PacketDistributor.sendToPlayer(player, new S2CRespawnNowPacket());
        }
    }
}

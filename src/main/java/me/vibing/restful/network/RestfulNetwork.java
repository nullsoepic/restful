package me.vibing.restful.network;

import me.vibing.restful.BedData;
import me.vibing.restful.BedTracker;
import me.vibing.restful.BedValidator;
import me.vibing.restful.Config;
import me.vibing.restful.Restful;
import net.minecraft.client.Minecraft;
import me.vibing.restful.client.BedManagementScreen;
import me.vibing.restful.client.BedSelectionScreen;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
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
                        // Capture player reference on this thread to avoid race condition
                        var player = minecraft.player;
                        if (player != null && player.level() != null) {
                            minecraft.execute(() -> {
                                // Check again in case player disconnected during execute scheduling
                                if (minecraft.player == player) {
                                    minecraft.setScreen(new BedSelectionScreen(payload.beds()));
                                }
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
                        var player = minecraft.player;
                        if (player != null && player.level() != null) {
                            minecraft.execute(() -> {
                                if (minecraft.player == player) {
                                    minecraft.setScreen(new BedManagementScreen(payload.beds()));
                                }
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
                        var player = minecraft.player;
                        var connection = minecraft.getConnection();
                        if (player != null && player.level() != null && connection != null) {
                            minecraft.execute(() -> {
                                // Verify connection still valid before sending
                                if (minecraft.getConnection() != null) {
                                    minecraft.getConnection().send(
                                            new ServerboundClientCommandPacket(
                                                    ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
                                }
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
                                        handleInvalidBed(serverPlayer, tracker, payload.index(), result.shouldRemove());
                                        return;
                                    }
                                    tracker.setSelectedBedIndex(payload.index());
                                    PacketDistributor.sendToPlayer(serverPlayer, new S2CRespawnNowPacket());
                                }
                                case FAVORITE -> {
                                    int idx = payload.index();
                                    if (idx < 0 || idx >= tracker.size()) break;
                                    boolean current = tracker.isFavorite(idx);
                                    tracker.setFavorite(idx, !current);
                                }
                                case RENAME -> {
                                    int idx = payload.index();
                                    if (idx < 0 || idx >= tracker.size()) break;
                                    payload.data().ifPresent(name -> {
                                        // defense in depth: also truncate server-side
                                        String truncated = name.length() > 40 ? name.substring(0, 40) : name;
                                        tracker.renameBed(idx, truncated);
                                    });
                                }
                                case REMOVE -> {
                                    int idx = payload.index();
                                    if (idx < 0 || idx >= tracker.size()) break;
                                    tracker.removeBed(idx);
                                }
                                case REORDER -> {
                                    var oldBeds = new ArrayList<>(tracker.getBeds());
                                    var order = payload.order();
                                    
                                    // Validate BEFORE mutating - ensure all indices valid, no duplicates, correct size
                                    if (order.size() != oldBeds.size()) {
                                        break; // Invalid reorder, ignore
                                    }
                                    
                                    boolean valid = true;
                                    boolean[] seen = new boolean[oldBeds.size()];
                                    for (int i : order) {
                                        if (i < 0 || i >= oldBeds.size() || seen[i]) {
                                            valid = false;
                                            break;
                                        }
                                        seen[i] = true;
                                    }
                                    
                                    if (!valid) {
                                        break; // Invalid indices or duplicates, ignore
                                    }
                                    
                                    // Now safe to mutate
                                    tracker.clear();
                                    int maxPoints = Config.MAX_POINTS.get();
                                    for (int i : order) {
                                        BedData bed = oldBeds.get(i);
                                        tracker.addBed(bed.position(), bed.name(), bed.bedItem(), maxPoints);
                                        if (bed.favorite()) {
                                            tracker.setFavorite(tracker.size() - 1, true);
                                        }
                                    }
                                }
                            }
                        }
                    });
                }
        );
    }

    private record ValidationResult(boolean valid, boolean shouldRemove) {}

    private static ValidationResult validateBed(int index, ServerPlayer player, BedTracker tracker) {
        if (index < 0 || index >= tracker.size()) {
            return new ValidationResult(false, false);
        }

        BedData bed = tracker.getBed(index);
        if (bed == null) {
            return new ValidationResult(false, false);
        }

        if (!BedValidator.isValidRespawnPoint(player, bed.position())) {
            return new ValidationResult(false, true);
        }

        return new ValidationResult(true, false);
    }

    private static void handleInvalidBed(ServerPlayer player, BedTracker tracker, int index, boolean shouldRemove) {
        if (shouldRemove) {
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

        for (int i = 0; i < tracker.size(); i++) {
            BedData bed = tracker.getBed(i);
            if (bed == null) continue;

            if (BedValidator.isValidRespawnPoint(player, bed.position())) {
                validBeds.add(bed);
                validFavorites.add(tracker.isFavorite(i));
            } else {
                Restful.LOGGER.debug("Bed {} invalid, removing", bed.position());
            }
        }

        tracker.clear();
        int maxPoints = Config.MAX_POINTS.get();
        for (int i = 0; i < validBeds.size(); i++) {
            tracker.addBed(validBeds.get(i).position(), validBeds.get(i).name(), validBeds.get(i).bedItem(), maxPoints);
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

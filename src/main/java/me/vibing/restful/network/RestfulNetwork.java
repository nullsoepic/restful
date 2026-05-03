package me.vibing.restful.network;

import me.vibing.restful.BedData;
import me.vibing.restful.BedTracker;
import me.vibing.restful.BedValidator;
import me.vibing.restful.Restful;
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
                BedSelectionPacket.TYPE,
                BedSelectionPacket.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
                        if (minecraft.player != null) {
                            minecraft.execute(() -> {
                                minecraft.setScreen(new me.vibing.restful.client.BedSelectionScreen(payload.beds()));
                            });
                        }
                    });
                }
        );

        registrar.playToClient(
                RespawnNowPacket.TYPE,
                RespawnNowPacket.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
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
                SelectBedPacket.TYPE,
                SelectBedPacket.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer serverPlayer) {
                            BedTracker tracker = serverPlayer.getData(Restful.BED_TRACKER);
                            BedData selectedBed = tracker.getBed(payload.selectedIndex());

                            if (selectedBed == null) {
                                sendBedSelection(serverPlayer);
                                return;
                            }

                            var targetLevel = serverPlayer.server.getLevel(selectedBed.position().dimension());
                            if (targetLevel == null) {
                                tracker.removeBed(payload.selectedIndex());
                                sendBedSelection(serverPlayer);
                                return;
                            }

                            var chunk = targetLevel.getChunkSource().getChunk(
                                    selectedBed.position().pos().getX() >> 4,
                                    selectedBed.position().pos().getZ() >> 4,
                                    true);

                            if (!BedValidator.isValidRespawnPoint(serverPlayer, selectedBed.position())) {
                                tracker.removeBed(payload.selectedIndex());

                                if (tracker.isEmpty()) {
                                    PacketDistributor.sendToPlayer(serverPlayer, new RespawnNowPacket());
                                } else {
                                    sendBedSelection(serverPlayer);
                                }
                                return;
                            }

                            tracker.setSelectedBedIndex(payload.selectedIndex());
                            PacketDistributor.sendToPlayer(serverPlayer, new RespawnNowPacket());
                        }
                    });
                }
        );
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
            
            var chunk = targetLevel.getChunkSource().getChunk(
                    bed.position().pos().getX() >> 4,
                    bed.position().pos().getZ() >> 4,
                    true);
            
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

        List<BedSelectionPacket.BedInfo> bedInfos = new ArrayList<>();
        List<BedData> beds = tracker.getBeds();

        for (int i = 0; i < beds.size(); i++) {
            BedData bed = beds.get(i);
            bedInfos.add(BedSelectionPacket.BedInfo.fromBedData(bed, i, tracker.isFavorite(i)));
        }

        if (!bedInfos.isEmpty()) {
            PacketDistributor.sendToPlayer(player, new BedSelectionPacket(bedInfos));
        } else {
            PacketDistributor.sendToPlayer(player, new RespawnNowPacket());
        }
    }
}

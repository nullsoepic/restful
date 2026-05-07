package me.vibing.restful.network;

import me.vibing.restful.client.BedManagementScreen;
import me.vibing.restful.client.BedSelectionScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

// Client-only network handlers - registered only on CLIENT dist
@EventBusSubscriber(modid = "restful", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class RestfulNetworkClient {

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("restful");

        registrar.playToClient(
                S2CBedListPacket.TYPE,
                S2CBedListPacket.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        Minecraft minecraft = Minecraft.getInstance();
                        var player = minecraft.player;
                        if (player != null && player.level() != null) {
                            minecraft.execute(() -> {
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
    }
}

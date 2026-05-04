package me.vibing.restful.command;

import me.vibing.restful.BedTracker;
import me.vibing.restful.Restful;
import me.vibing.restful.network.S2COpenManagementPacket;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

// commands for managing respawn points - now just opens the GUI
public class BedCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("restful")
                .executes(context -> openGui(context.getSource()))
        );
    }

    private static int openGui(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.restful.usage"));
            return 0;
        }

        BedTracker tracker = player.getData(Restful.BED_TRACKER);
        PacketDistributor.sendToPlayer(player, new S2COpenManagementPacket(tracker.toBedInfoList()));

        return 1;
    }
}

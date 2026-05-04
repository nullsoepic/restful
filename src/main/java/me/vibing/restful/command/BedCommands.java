package me.vibing.restful.command;

import me.vibing.restful.BedData;
import me.vibing.restful.BedTracker;
import me.vibing.restful.Restful;
import me.vibing.restful.network.BedSelectionPacket;
import me.vibing.restful.network.OpenManagementPacket;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

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
        
        // build bed info list
        List<BedSelectionPacket.BedInfo> bedInfos = new ArrayList<>();
        for (int i = 0; i < tracker.size(); i++) {
            BedData bed = tracker.getBed(i);
            if (bed != null) {
                bedInfos.add(BedSelectionPacket.BedInfo.fromBedData(bed, i, tracker.isFavorite(i)));
            }
        }
        
        // send packet to open management screen on client
        PacketDistributor.sendToPlayer(player, new OpenManagementPacket(bedInfos));
        
        return 1;
    }
}

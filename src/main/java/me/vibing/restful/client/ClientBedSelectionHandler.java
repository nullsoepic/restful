package me.vibing.restful.client;

import me.vibing.restful.network.BedSelectionPacket;
import net.minecraft.client.Minecraft;

import java.util.List;

public class ClientBedSelectionHandler {
    
    public static void openSelectionScreen(List<BedSelectionPacket.BedInfo> beds) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        
        minecraft.execute(() -> {
            minecraft.setScreen(new BedSelectionScreen(beds));
        });
    }
}

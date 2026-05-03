package me.vibing.restful.mixin;

import me.vibing.restful.Restful;
import me.vibing.restful.BedTracker;
import me.vibing.restful.network.RestfulNetwork;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket.Action;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// intercepts respawn requests to show bed selection BEFORE vanilla respawn handles it
@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {

    @Shadow
    public ServerPlayer player;

    @Inject(method = "handleClientCommand",
            at = @At("HEAD"),
            cancellable = true)
    private void backupBeds$onRespawnClick(ServerboundClientCommandPacket packet, CallbackInfo ci) {
        if (packet.getAction() != Action.PERFORM_RESPAWN) {
            return;
        }

        if (!player.isDeadOrDying()) {
            return;
        }

        BedTracker tracker = player.getData(Restful.BED_TRACKER);

        if (tracker.isEmpty()) {
            return;
        }

        if (tracker.getSelectedBedIndex() >= 0) {
            // dont clear here - ServerPlayerRespawnMixin needs it first
            // PlayerRespawnEvent will clear it later
            return;
        }

        ci.cancel();
        RestfulNetwork.sendBedSelection(player);
    }
}

package me.vibing.restful.mixin;

import me.vibing.restful.Restful;
import me.vibing.restful.BedData;
import me.vibing.restful.BedTracker;
import me.vibing.restful.BedValidator;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerRespawnPointMixin extends Player {
    
    @Shadow
    private boolean respawnForced;
    
    @Shadow
    @Nullable
    public abstract BlockPos getRespawnPosition();
    
    @Shadow
    public abstract ResourceKey<Level> getRespawnDimension();
    
    @Unique
    private BedData restful$selectedRespawn = null;

    public ServerPlayerRespawnPointMixin(Level level, BlockPos pos, float yRot, GameProfile gameProfile) {
        super(level, pos, yRot, gameProfile);
    }
    
    @Inject(method = "getRespawnPosition", at = @At("RETURN"), cancellable = true)
    private void restful$getRespawnPos(CallbackInfoReturnable<BlockPos> cir) {
        if (respawnForced) {
            return;
        }
        
        BedTracker tracker = getData(Restful.BED_TRACKER);
        
        int selectedIndex = tracker.getSelectedBedIndex();
        if (selectedIndex >= 0) {
            BedData selectedBed = tracker.getBed(selectedIndex);
            if (selectedBed != null) {
                // if chunk isnt loaded, assume its valid - minecraft handles loading during teleport
                var serverLevel = ((ServerPlayer)(Object)this).serverLevel().getServer().getLevel(selectedBed.position().dimension());
                if (serverLevel != null && !serverLevel.isLoaded(selectedBed.position().pos())) {
                    restful$selectedRespawn = selectedBed;
                    cir.setReturnValue(selectedBed.position().pos());
                    return;
                }
                if (BedValidator.isValidRespawnPoint(this, selectedBed.position())) {
                    restful$selectedRespawn = selectedBed;
                    cir.setReturnValue(selectedBed.position().pos());
                    return;
                }
            }
        }
        
        BlockPos vanillaPos = cir.getReturnValue();
        ResourceKey<Level> vanillaDim = getRespawnDimension();
        
        if (vanillaPos != null && vanillaDim != null) {
            var vanillaGlobalPos = net.minecraft.core.GlobalPos.of(vanillaDim, vanillaPos);
            if (BedValidator.isValidRespawnPoint(this, vanillaGlobalPos)) {
                restful$selectedRespawn = null;
                return;
            }
        }
        
        BedData respawnBed = tracker.findBestValidBed(this);
        if (respawnBed != null) {
            restful$selectedRespawn = respawnBed;
            cir.setReturnValue(respawnBed.position().pos());
        }
    }
    
    @Inject(method = "getRespawnAngle", at = @At("RETURN"), cancellable = true)
    private void restful$getRespawnAngle(CallbackInfoReturnable<Float> cir) {
        if (!respawnForced && restful$selectedRespawn != null) {
            // avoid infinite recursion by using current rotation instead of vanilla method
            cir.setReturnValue(this.getYRot());
        }
    }
    
    @Inject(method = "getRespawnDimension", at = @At("RETURN"), cancellable = true)
    private void restful$getRespawnDimension(CallbackInfoReturnable<ResourceKey<Level>> cir) {
        if (respawnForced) {
            return;
        }
        
        if (restful$selectedRespawn != null) {
            cir.setReturnValue(restful$selectedRespawn.position().dimension());
        }
    }
}

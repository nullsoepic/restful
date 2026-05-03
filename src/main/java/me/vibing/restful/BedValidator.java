package me.vibing.restful;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class BedValidator {
    
    public static boolean isValidRespawnPoint(Player player, GlobalPos globalPos) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        
        ServerLevel targetLevel = serverLevel.getServer().getLevel(globalPos.dimension());
        if (targetLevel == null) {
            return false;
        }
        
        if (!targetLevel.isLoaded(globalPos.pos())) {
            return false;
        }
        
        BlockState state = targetLevel.getBlockState(globalPos.pos());
        
        if (state.getBlock() instanceof RespawnAnchorBlock) {
            return isValidAnchor(state, targetLevel, globalPos);
        }
        
        // beds explode in nether/end so we only check overworld
        if (state.getBlock() instanceof BedBlock) {
            return isValidBed(targetLevel, globalPos);
        }
        
        return false;
    }
    
    private static boolean isValidAnchor(BlockState state, ServerLevel level, GlobalPos pos) {
        if (state.getValue(RespawnAnchorBlock.CHARGE) <= 0) {
            return false;
        }
        
        if (!RespawnAnchorBlock.canSetSpawn(level)) {
            return false;
        }
        
        Optional<Vec3> standPos = RespawnAnchorBlock.findStandUpPosition(
                EntityType.PLAYER, level, pos.pos());
        return standPos.isPresent();
    }
    
    private static boolean isValidBed(ServerLevel level, GlobalPos pos) {
        if (!level.dimension().equals(Level.OVERWORLD)) {
            return false;
        }
        
        BlockState state = level.getBlockState(pos.pos());
        if (!(state.getBlock() instanceof BedBlock)) {
            return false;
        }
        
        for (int yOffset = -1; yOffset <= 2; yOffset++) {
            for (int xOffset = -2; xOffset <= 2; xOffset++) {
                for (int zOffset = -2; zOffset <= 2; zOffset++) {
                    BlockPos checkPos = pos.pos().offset(xOffset, yOffset, zOffset);
                    if (level.isEmptyBlock(checkPos) && level.isEmptyBlock(checkPos.above())) {
                        BlockPos below = checkPos.below();
                        if (!level.isEmptyBlock(below) || level.getFluidState(below).isSource()) {
                            return true;
                        }
                    }
                }
            }
        }
        
        return false;
    }
    
    public static float getRespawnAngle(Player player, GlobalPos pos) {
        // dont call player.getRespawnAngle() or we get infinite recursion from the mixin
        return player.getYRot();
    }
}

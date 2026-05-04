package me.vibing.restful;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class BedValidator {

    // Validates bed with chunk loading - used during death/management when we need to know NOW
    public static boolean isValidRespawnPoint(Player player, GlobalPos globalPos) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        ServerLevel targetLevel = serverLevel.getServer().getLevel(globalPos.dimension());
        if (targetLevel == null) {
            return false;
        }

        // Load chunk if needed - temporary load just for validation
        ChunkPos chunkPos = new ChunkPos(globalPos.pos());
        if (!targetLevel.hasChunk(chunkPos.x, chunkPos.z)) {
            ChunkAccess chunk = targetLevel.getChunkSource().getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FULL, true);
            if (chunk == null) {
                return false; // chunk failed to load somehow
            }
        }

        BlockState state = targetLevel.getBlockState(globalPos.pos());

        if (state.getBlock() instanceof RespawnAnchorBlock) {
            return isValidAnchor(state, targetLevel, globalPos);
        }

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
        BlockState state = level.getBlockState(pos.pos());
        if (!(state.getBlock() instanceof BedBlock)) {
            return false;
        }

        // Check for valid spawn position around the bed
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
}

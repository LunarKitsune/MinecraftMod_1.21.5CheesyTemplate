package net.minecraft.world.level.block;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public interface BonemealableBlock {
    boolean isValidBonemealTarget(LevelReader pLevel, BlockPos pPos, BlockState pState);

    boolean isBonemealSuccess(Level pLevel, RandomSource pRandom, BlockPos pPos, BlockState pState);

    void performBonemeal(ServerLevel pLevel, RandomSource pRandom, BlockPos pPos, BlockState pState);

    static boolean hasSpreadableNeighbourPos(LevelReader pLevel, BlockPos pPos, BlockState pState) {
        return getSpreadableNeighbourPos(Direction.Plane.HORIZONTAL.stream().toList(), pLevel, pPos, pState).isPresent();
    }

    static Optional<BlockPos> findSpreadableNeighbourPos(Level pLevel, BlockPos pPos, BlockState pState) {
        return getSpreadableNeighbourPos(Direction.Plane.HORIZONTAL.shuffledCopy(pLevel.random), pLevel, pPos, pState);
    }

    private static Optional<BlockPos> getSpreadableNeighbourPos(List<Direction> pDirections, LevelReader pLevel, BlockPos pPos, BlockState pState) {
        for (Direction direction : pDirections) {
            BlockPos blockpos = pPos.relative(direction);
            if (pLevel.isEmptyBlock(blockpos) && pState.canSurvive(pLevel, blockpos)) {
                return Optional.of(blockpos);
            }
        }

        return Optional.empty();
    }

    default BlockPos getParticlePos(BlockPos pPos) {
        return switch (this.getType()) {
            case NEIGHBOR_SPREADER -> pPos.above();
            case GROWER -> pPos;
        };
    }

    default BonemealableBlock.Type getType() {
        return BonemealableBlock.Type.GROWER;
    }

    public static enum Type {
        NEIGHBOR_SPREADER,
        GROWER;
    }
}
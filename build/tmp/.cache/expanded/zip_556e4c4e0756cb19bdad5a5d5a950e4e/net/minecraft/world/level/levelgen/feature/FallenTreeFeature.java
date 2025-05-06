package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FallenTreeConfiguration;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;

public class FallenTreeFeature extends Feature<FallenTreeConfiguration> {
    private static final int STUMP_HEIGHT = 1;
    private static final int STUMP_HEIGHT_PLUS_EMPTY_SPACE = 2;
    private static final int FALLEN_LOG_MAX_FALL_HEIGHT_TO_GROUND = 5;
    private static final int FALLEN_LOG_MAX_GROUND_GAP = 2;
    private static final int FALLEN_LOG_MAX_SPACE_FROM_STUMP = 2;
    private static final int BLOCK_UPDATE_FLAGS = 19;

    public FallenTreeFeature(Codec<FallenTreeConfiguration> p_392549_) {
        super(p_392549_);
    }

    @Override
    public boolean place(FeaturePlaceContext<FallenTreeConfiguration> p_393978_) {
        this.placeFallenTree(p_393978_.config(), p_393978_.origin(), p_393978_.level(), p_393978_.random());
        return true;
    }

    private void placeFallenTree(FallenTreeConfiguration pConfig, BlockPos pOrigin, WorldGenLevel pLevel, RandomSource pRandom) {
        this.placeStump(pConfig, pLevel, pRandom, pOrigin.mutable());
        Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(pRandom);
        int i = pConfig.logLength.sample(pRandom) - 2;
        BlockPos.MutableBlockPos blockpos$mutableblockpos = pOrigin.relative(direction, 2 + pRandom.nextInt(2)).mutable();
        this.setGroundHeightForFallenLogStartPos(pLevel, blockpos$mutableblockpos);
        if (this.canPlaceEntireFallenLog(pLevel, i, blockpos$mutableblockpos, direction)) {
            this.placeFallenLog(pConfig, pLevel, pRandom, i, blockpos$mutableblockpos, direction);
        }
    }

    private void setGroundHeightForFallenLogStartPos(WorldGenLevel pLevel, BlockPos.MutableBlockPos pPos) {
        pPos.move(Direction.UP, 1);

        for (int i = 0; i < 6; i++) {
            if (this.mayPlaceOn(pLevel, pPos)) {
                return;
            }

            pPos.move(Direction.DOWN);
        }
    }

    private void placeStump(FallenTreeConfiguration pConfig, WorldGenLevel pLevel, RandomSource pRandom, BlockPos.MutableBlockPos pPos) {
        BlockPos blockpos = this.placeLogBlock(pConfig, pLevel, pRandom, pPos, Function.identity());
        this.decorateLogs(pLevel, pRandom, Set.of(blockpos), pConfig.stumpDecorators);
    }

    private boolean canPlaceEntireFallenLog(WorldGenLevel pLevel, int pLogLength, BlockPos.MutableBlockPos pPos, Direction pDirection) {
        int i = 0;

        for (int j = 0; j < pLogLength; j++) {
            if (!TreeFeature.validTreePos(pLevel, pPos)) {
                return false;
            }

            if (!this.isOverSolidGround(pLevel, pPos)) {
                if (++i > 2) {
                    return false;
                }
            } else {
                i = 0;
            }

            pPos.move(pDirection);
        }

        pPos.move(pDirection.getOpposite(), pLogLength);
        return true;
    }

    private void placeFallenLog(
        FallenTreeConfiguration pConfig,
        WorldGenLevel pLevel,
        RandomSource pRandom,
        int pLogLength,
        BlockPos.MutableBlockPos pPos,
        Direction pDirection
    ) {
        Set<BlockPos> set = new HashSet<>();

        for (int i = 0; i < pLogLength; i++) {
            set.add(this.placeLogBlock(pConfig, pLevel, pRandom, pPos, getSidewaysStateModifier(pDirection)));
            pPos.move(pDirection);
        }

        this.decorateLogs(pLevel, pRandom, set, pConfig.logDecorators);
    }

    private boolean mayPlaceOn(LevelAccessor pLevel, BlockPos pPos) {
        return TreeFeature.validTreePos(pLevel, pPos) && this.isOverSolidGround(pLevel, pPos);
    }

    private boolean isOverSolidGround(LevelAccessor pLevel, BlockPos pPos) {
        return pLevel.getBlockState(pPos.below()).isFaceSturdy(pLevel, pPos, Direction.UP);
    }

    private BlockPos placeLogBlock(
        FallenTreeConfiguration pConfig,
        WorldGenLevel pLevel,
        RandomSource pRandom,
        BlockPos.MutableBlockPos pPos,
        Function<BlockState, BlockState> pStateModifier
    ) {
        pLevel.setBlock(pPos, pStateModifier.apply(pConfig.trunkProvider.getState(pRandom, pPos)), 3);
        this.markAboveForPostProcessing(pLevel, pPos);
        return pPos.immutable();
    }

    private void decorateLogs(WorldGenLevel pLevel, RandomSource pRandom, Set<BlockPos> pLogPositions, List<TreeDecorator> pDecorators) {
        if (!pDecorators.isEmpty()) {
            TreeDecorator.Context treedecorator$context = new TreeDecorator.Context(
                pLevel, this.getDecorationSetter(pLevel), pRandom, pLogPositions, Set.of(), Set.of()
            );
            pDecorators.forEach(p_393767_ -> p_393767_.place(treedecorator$context));
        }
    }

    private BiConsumer<BlockPos, BlockState> getDecorationSetter(WorldGenLevel pLevel) {
        return (p_391644_, p_394520_) -> pLevel.setBlock(p_391644_, p_394520_, 19);
    }

    private static Function<BlockState, BlockState> getSidewaysStateModifier(Direction pDirection) {
        return p_391565_ -> p_391565_.trySetValue(RotatedPillarBlock.AXIS, pDirection.getAxis());
    }
}
package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;

public class ChorusPlantBlock extends PipeBlock {
    public static final MapCodec<ChorusPlantBlock> CODEC = simpleCodec(ChorusPlantBlock::new);

    @Override
    public MapCodec<ChorusPlantBlock> codec() {
        return CODEC;
    }

    public ChorusPlantBlock(BlockBehaviour.Properties p_51707_) {
        super(10.0F, p_51707_);
        this.registerDefaultState(
            this.stateDefinition
                .any()
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false)
        );
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return getStateWithConnections(pContext.getLevel(), pContext.getClickedPos(), this.defaultBlockState());
    }

    public static BlockState getStateWithConnections(BlockGetter pLevel, BlockPos pPos, BlockState pState) {
        BlockState blockstate = pLevel.getBlockState(pPos.below());
        BlockState blockstate1 = pLevel.getBlockState(pPos.above());
        BlockState blockstate2 = pLevel.getBlockState(pPos.north());
        BlockState blockstate3 = pLevel.getBlockState(pPos.east());
        BlockState blockstate4 = pLevel.getBlockState(pPos.south());
        BlockState blockstate5 = pLevel.getBlockState(pPos.west());
        Block block = pState.getBlock();
        return pState.trySetValue(DOWN, blockstate.is(block) || blockstate.is(Blocks.CHORUS_FLOWER) || blockstate.is(Blocks.END_STONE) || blockstate.is(net.minecraftforge.common.Tags.Blocks.CHORUS_ADDITIONALLY_GROWS_ON))
            .trySetValue(UP, blockstate1.is(block) || blockstate1.is(Blocks.CHORUS_FLOWER))
            .trySetValue(NORTH, blockstate2.is(block) || blockstate2.is(Blocks.CHORUS_FLOWER))
            .trySetValue(EAST, blockstate3.is(block) || blockstate3.is(Blocks.CHORUS_FLOWER))
            .trySetValue(SOUTH, blockstate4.is(block) || blockstate4.is(Blocks.CHORUS_FLOWER))
            .trySetValue(WEST, blockstate5.is(block) || blockstate5.is(Blocks.CHORUS_FLOWER));
    }

    @Override
    protected BlockState updateShape(
        BlockState p_51728_,
        LevelReader p_369826_,
        ScheduledTickAccess p_364837_,
        BlockPos p_51732_,
        Direction p_51729_,
        BlockPos p_51733_,
        BlockState p_51730_,
        RandomSource p_368636_
    ) {
        if (!p_51728_.canSurvive(p_369826_, p_51732_)) {
            p_364837_.scheduleTick(p_51732_, this, 1);
            return super.updateShape(p_51728_, p_369826_, p_364837_, p_51732_, p_51729_, p_51733_, p_51730_, p_368636_);
        } else {
            boolean flag = p_51730_.is(this) || p_51730_.is(Blocks.CHORUS_FLOWER) || p_51729_ == Direction.DOWN && (p_51730_.is(Blocks.END_STONE) || p_51730_.is(net.minecraftforge.common.Tags.Blocks.CHORUS_ADDITIONALLY_GROWS_ON));
            return p_51728_.setValue(PROPERTY_BY_DIRECTION.get(p_51729_), flag);
        }
    }

    @Override
    protected void tick(BlockState p_220985_, ServerLevel p_220986_, BlockPos p_220987_, RandomSource p_220988_) {
        if (!p_220985_.canSurvive(p_220986_, p_220987_)) {
            p_220986_.destroyBlock(p_220987_, true);
        }
    }

    @Override
    protected boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        BlockState blockstate = pLevel.getBlockState(pPos.below());
        boolean flag = !pLevel.getBlockState(pPos.above()).isAir() && !blockstate.isAir();

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos blockpos = pPos.relative(direction);
            BlockState blockstate1 = pLevel.getBlockState(blockpos);
            if (blockstate1.is(this)) {
                if (flag) {
                    return false;
                }

                BlockState blockstate2 = pLevel.getBlockState(blockpos.below());
                if (blockstate2.is(this) || blockstate2.is(Blocks.END_STONE) || blockstate2.is(net.minecraftforge.common.Tags.Blocks.CHORUS_ADDITIONALLY_GROWS_ON)) {
                    return true;
                }
            }
        }

        return blockstate.is(this) || blockstate.is(Blocks.END_STONE) || blockstate.is(net.minecraftforge.common.Tags.Blocks.CHORUS_ADDITIONALLY_GROWS_ON);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    protected boolean isPathfindable(BlockState p_51719_, PathComputationType p_51722_) {
        return false;
    }
}

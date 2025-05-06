package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class ConcretePowderBlock extends FallingBlock {
    public static final MapCodec<ConcretePowderBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_360424_ -> p_360424_.group(BuiltInRegistries.BLOCK.byNameCodec().fieldOf("concrete").forGetter(p_313163_ -> p_313163_.concrete), propertiesCodec())
            .apply(p_360424_, ConcretePowderBlock::new)
    );
    private final Block concrete;

    @Override
    public MapCodec<ConcretePowderBlock> codec() {
        return CODEC;
    }

    public ConcretePowderBlock(Block pConcrete, BlockBehaviour.Properties pProperties) {
        super(pProperties);
        this.concrete = pConcrete;
    }

    @Override
    public void onLand(Level p_52068_, BlockPos p_52069_, BlockState p_52070_, BlockState p_52071_, FallingBlockEntity p_52072_) {
        if (shouldSolidify(p_52068_, p_52069_, p_52070_, p_52071_.getFluidState())) { // Forge: Use block of falling entity instead of block at replaced position, and check if shouldSolidify with the FluidState of the replaced block
            p_52068_.setBlock(p_52069_, this.concrete.defaultBlockState(), 3);
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        BlockGetter blockgetter = pContext.getLevel();
        BlockPos blockpos = pContext.getClickedPos();
        BlockState blockstate = blockgetter.getBlockState(blockpos);
        return shouldSolidify(blockgetter, blockpos, blockstate) ? this.concrete.defaultBlockState() : super.getStateForPlacement(pContext);
    }

    private static boolean shouldSolidify(BlockGetter pLevel, BlockPos pPos, BlockState pState, net.minecraft.world.level.material.FluidState fluidState) {
        return pState.canBeHydrated(pLevel, pPos, fluidState, pPos) || touchesLiquid(pLevel, pPos, pState);
    }

    private static boolean shouldSolidify(BlockGetter pLevel, BlockPos pPos, BlockState pState) {
        return shouldSolidify(pLevel, pPos, pState, pLevel.getFluidState(pPos));
    }

    private static boolean touchesLiquid(BlockGetter pLevel, BlockPos pPos, BlockState state) {
        boolean flag = false;
        BlockPos.MutableBlockPos blockpos$mutableblockpos = pPos.mutable();

        for (Direction direction : Direction.values()) {
            BlockState blockstate = pLevel.getBlockState(blockpos$mutableblockpos);
            if (direction != Direction.DOWN || state.canBeHydrated(pLevel, pPos, blockstate.getFluidState(), blockpos$mutableblockpos)) {
                blockpos$mutableblockpos.setWithOffset(pPos, direction);
                blockstate = pLevel.getBlockState(blockpos$mutableblockpos);
                if (state.canBeHydrated(pLevel, pPos, blockstate.getFluidState(), blockpos$mutableblockpos) && !blockstate.isFaceSturdy(pLevel, pPos, direction.getOpposite())) {
                    flag = true;
                    break;
                }
            }
        }

        return flag;
    }

    private static boolean canSolidify(BlockState pState) {
        return pState.getFluidState().is(FluidTags.WATER);
    }

    @Override
    protected BlockState updateShape(
        BlockState p_52074_,
        LevelReader p_361484_,
        ScheduledTickAccess p_362145_,
        BlockPos p_52078_,
        Direction p_52075_,
        BlockPos p_52079_,
        BlockState p_52076_,
        RandomSource p_369257_
    ) {
        return touchesLiquid(p_361484_, p_52078_, p_52074_)
            ? this.concrete.defaultBlockState()
            : super.updateShape(p_52074_, p_361484_, p_362145_, p_52078_, p_52075_, p_52079_, p_52076_, p_369257_);
    }

    @Override
    public int getDustColor(BlockState pState, BlockGetter pReader, BlockPos pPos) {
        return pState.getMapColor(pReader, pPos).col;
    }
}

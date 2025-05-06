package net.minecraft.world.level.block;

import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.Tilt;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BigDripleafBlock extends HorizontalDirectionalBlock implements BonemealableBlock, SimpleWaterloggedBlock {
    public static final MapCodec<BigDripleafBlock> CODEC = simpleCodec(BigDripleafBlock::new);
    private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final EnumProperty<Tilt> TILT = BlockStateProperties.TILT;
    private static final int NO_TICK = -1;
    private static final Object2IntMap<Tilt> DELAY_UNTIL_NEXT_TILT_STATE = Util.make(new Object2IntArrayMap<>(), p_152305_ -> {
        p_152305_.defaultReturnValue(-1);
        p_152305_.put(Tilt.UNSTABLE, 10);
        p_152305_.put(Tilt.PARTIAL, 10);
        p_152305_.put(Tilt.FULL, 100);
    });
    private static final int MAX_GEN_HEIGHT = 5;
    private static final int ENTITY_DETECTION_MIN_Y = 11;
    private static final int LOWEST_LEAF_TOP = 13;
    private static final Map<Tilt, VoxelShape> SHAPE_LEAF = Maps.newEnumMap(
        Map.of(
            Tilt.NONE,
            Block.column(16.0, 11.0, 15.0),
            Tilt.UNSTABLE,
            Block.column(16.0, 11.0, 15.0),
            Tilt.PARTIAL,
            Block.column(16.0, 11.0, 13.0),
            Tilt.FULL,
            Shapes.empty()
        )
    );
    private final Function<BlockState, VoxelShape> shapes;

    @Override
    public MapCodec<BigDripleafBlock> codec() {
        return CODEC;
    }

    public BigDripleafBlock(BlockBehaviour.Properties p_152214_) {
        super(p_152214_);
        this.registerDefaultState(this.stateDefinition.any().setValue(WATERLOGGED, false).setValue(FACING, Direction.NORTH).setValue(TILT, Tilt.NONE));
        this.shapes = this.makeShapes();
    }

    private Function<BlockState, VoxelShape> makeShapes() {
        Map<Direction, VoxelShape> map = Shapes.rotateHorizontal(Block.column(6.0, 0.0, 13.0).move(0.0, 0.0, 0.25).optimize());
        return this.getShapeForEachState(
            p_390895_ -> Shapes.or(SHAPE_LEAF.get(p_390895_.getValue(TILT)), map.get(p_390895_.getValue(FACING))), new Property[]{WATERLOGGED}
        );
    }

    public static void placeWithRandomHeight(LevelAccessor pLevel, RandomSource pRandom, BlockPos pPos, Direction pDirection) {
        int i = Mth.nextInt(pRandom, 2, 5);
        BlockPos.MutableBlockPos blockpos$mutableblockpos = pPos.mutable();
        int j = 0;

        while (j < i && canPlaceAt(pLevel, blockpos$mutableblockpos, pLevel.getBlockState(blockpos$mutableblockpos))) {
            j++;
            blockpos$mutableblockpos.move(Direction.UP);
        }

        int k = pPos.getY() + j - 1;
        blockpos$mutableblockpos.setY(pPos.getY());

        while (blockpos$mutableblockpos.getY() < k) {
            BigDripleafStemBlock.place(pLevel, blockpos$mutableblockpos, pLevel.getFluidState(blockpos$mutableblockpos), pDirection);
            blockpos$mutableblockpos.move(Direction.UP);
        }

        place(pLevel, blockpos$mutableblockpos, pLevel.getFluidState(blockpos$mutableblockpos), pDirection);
    }

    private static boolean canReplace(BlockState pState) {
        return pState.isAir() || pState.is(Blocks.WATER) || pState.is(Blocks.SMALL_DRIPLEAF);
    }

    protected static boolean canPlaceAt(LevelHeightAccessor pLevel, BlockPos pPos, BlockState pState) {
        return !pLevel.isOutsideBuildHeight(pPos) && canReplace(pState);
    }

    protected static boolean place(LevelAccessor pLevel, BlockPos pPos, FluidState pFluidState, Direction pDirection) {
        BlockState blockstate = Blocks.BIG_DRIPLEAF.defaultBlockState().setValue(WATERLOGGED, pFluidState.isSourceOfType(Fluids.WATER)).setValue(FACING, pDirection);
        return pLevel.setBlock(pPos, blockstate, 3);
    }

    @Override
    protected void onProjectileHit(Level p_152228_, BlockState p_152229_, BlockHitResult p_152230_, Projectile p_152231_) {
        this.setTiltAndScheduleTick(p_152229_, p_152228_, p_152230_.getBlockPos(), Tilt.FULL, SoundEvents.BIG_DRIPLEAF_TILT_DOWN);
    }

    @Override
    protected FluidState getFluidState(BlockState p_152312_) {
        return p_152312_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_152312_);
    }

    @Override
    protected boolean canSurvive(BlockState p_152289_, LevelReader p_152290_, BlockPos p_152291_) {
        BlockPos blockpos = p_152291_.below();
        BlockState blockstate = p_152290_.getBlockState(blockpos);
        return blockstate.is(this) || blockstate.is(Blocks.BIG_DRIPLEAF_STEM) || blockstate.is(BlockTags.BIG_DRIPLEAF_PLACEABLE);
    }

    @Override
    protected BlockState updateShape(
        BlockState p_152293_,
        LevelReader p_363105_,
        ScheduledTickAccess p_360715_,
        BlockPos p_152297_,
        Direction p_152294_,
        BlockPos p_152298_,
        BlockState p_152295_,
        RandomSource p_361614_
    ) {
        if (p_152294_ == Direction.DOWN && !p_152293_.canSurvive(p_363105_, p_152297_)) {
            return Blocks.AIR.defaultBlockState();
        } else {
            if (p_152293_.getValue(WATERLOGGED)) {
                p_360715_.scheduleTick(p_152297_, Fluids.WATER, Fluids.WATER.getTickDelay(p_363105_));
            }

            return p_152294_ == Direction.UP && p_152295_.is(this)
                ? Blocks.BIG_DRIPLEAF_STEM.withPropertiesOf(p_152293_)
                : super.updateShape(p_152293_, p_363105_, p_360715_, p_152297_, p_152294_, p_152298_, p_152295_, p_361614_);
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader p_255698_, BlockPos p_256302_, BlockState p_255648_) {
        BlockState blockstate = p_255698_.getBlockState(p_256302_.above());
        return canReplace(blockstate);
    }

    @Override
    public boolean isBonemealSuccess(Level p_220788_, RandomSource p_220789_, BlockPos p_220790_, BlockState p_220791_) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel p_220783_, RandomSource p_220784_, BlockPos p_220785_, BlockState p_220786_) {
        BlockPos blockpos = p_220785_.above();
        BlockState blockstate = p_220783_.getBlockState(blockpos);
        if (canPlaceAt(p_220783_, blockpos, blockstate)) {
            Direction direction = p_220786_.getValue(FACING);
            BigDripleafStemBlock.place(p_220783_, p_220785_, p_220786_.getFluidState(), direction);
            place(p_220783_, blockpos, blockstate.getFluidState(), direction);
        }
    }

    @Override
    protected void entityInside(BlockState p_152266_, Level p_152267_, BlockPos p_152268_, Entity p_152269_, InsideBlockEffectApplier p_393000_) {
        if (!p_152267_.isClientSide) {
            if (p_152266_.getValue(TILT) == Tilt.NONE && canEntityTilt(p_152268_, p_152269_) && !p_152267_.hasNeighborSignal(p_152268_)) {
                this.setTiltAndScheduleTick(p_152266_, p_152267_, p_152268_, Tilt.UNSTABLE, null);
            }
        }
    }

    @Override
    protected void tick(BlockState p_220798_, ServerLevel p_220799_, BlockPos p_220800_, RandomSource p_220801_) {
        if (p_220799_.hasNeighborSignal(p_220800_)) {
            resetTilt(p_220798_, p_220799_, p_220800_);
        } else {
            Tilt tilt = p_220798_.getValue(TILT);
            if (tilt == Tilt.UNSTABLE) {
                this.setTiltAndScheduleTick(p_220798_, p_220799_, p_220800_, Tilt.PARTIAL, SoundEvents.BIG_DRIPLEAF_TILT_DOWN);
            } else if (tilt == Tilt.PARTIAL) {
                this.setTiltAndScheduleTick(p_220798_, p_220799_, p_220800_, Tilt.FULL, SoundEvents.BIG_DRIPLEAF_TILT_DOWN);
            } else if (tilt == Tilt.FULL) {
                resetTilt(p_220798_, p_220799_, p_220800_);
            }
        }
    }

    @Override
    protected void neighborChanged(BlockState p_152271_, Level p_152272_, BlockPos p_152273_, Block p_152274_, @Nullable Orientation p_364652_, boolean p_152276_) {
        if (p_152272_.hasNeighborSignal(p_152273_)) {
            resetTilt(p_152271_, p_152272_, p_152273_);
        }
    }

    private static void playTiltSound(Level pLevel, BlockPos pPos, SoundEvent pSound) {
        float f = Mth.randomBetween(pLevel.random, 0.8F, 1.2F);
        pLevel.playSound(null, pPos, pSound, SoundSource.BLOCKS, 1.0F, f);
    }

    private static boolean canEntityTilt(BlockPos pPos, Entity pEntity) {
        return pEntity.onGround() && pEntity.position().y > pPos.getY() + 0.6875F;
    }

    private void setTiltAndScheduleTick(BlockState pState, Level pLevel, BlockPos pPos, Tilt pTilt, @Nullable SoundEvent pSound) {
        setTilt(pState, pLevel, pPos, pTilt);
        if (pSound != null) {
            playTiltSound(pLevel, pPos, pSound);
        }

        int i = DELAY_UNTIL_NEXT_TILT_STATE.getInt(pTilt);
        if (i != -1) {
            pLevel.scheduleTick(pPos, this, i);
        }
    }

    private static void resetTilt(BlockState pState, Level pLevel, BlockPos pPos) {
        setTilt(pState, pLevel, pPos, Tilt.NONE);
        if (pState.getValue(TILT) != Tilt.NONE) {
            playTiltSound(pLevel, pPos, SoundEvents.BIG_DRIPLEAF_TILT_UP);
        }
    }

    private static void setTilt(BlockState pState, Level pLevel, BlockPos pPos, Tilt pTilt) {
        Tilt tilt = pState.getValue(TILT);
        pLevel.setBlock(pPos, pState.setValue(TILT, pTilt), 2);
        if (pTilt.causesVibration() && pTilt != tilt) {
            pLevel.gameEvent(null, GameEvent.BLOCK_CHANGE, pPos);
        }
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState p_152307_, BlockGetter p_152308_, BlockPos p_152309_, CollisionContext p_152310_) {
        return SHAPE_LEAF.get(p_152307_.getValue(TILT));
    }

    @Override
    protected VoxelShape getShape(BlockState p_152261_, BlockGetter p_152262_, BlockPos p_152263_, CollisionContext p_152264_) {
        return this.shapes.apply(p_152261_);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext p_152221_) {
        BlockState blockstate = p_152221_.getLevel().getBlockState(p_152221_.getClickedPos().below());
        FluidState fluidstate = p_152221_.getLevel().getFluidState(p_152221_.getClickedPos());
        boolean flag = blockstate.is(Blocks.BIG_DRIPLEAF) || blockstate.is(Blocks.BIG_DRIPLEAF_STEM);
        return this.defaultBlockState()
            .setValue(WATERLOGGED, fluidstate.isSourceOfType(Fluids.WATER))
            .setValue(FACING, flag ? blockstate.getValue(FACING) : p_152221_.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_152300_) {
        p_152300_.add(WATERLOGGED, FACING, TILT);
    }
}
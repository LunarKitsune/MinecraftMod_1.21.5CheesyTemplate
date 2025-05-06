package net.minecraft.world.level.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ButtonBlock extends FaceAttachedHorizontalDirectionalBlock {
    public static final MapCodec<ButtonBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_360402_ -> p_360402_.group(
                BlockSetType.CODEC.fieldOf("block_set_type").forGetter(p_312681_ -> p_312681_.type),
                Codec.intRange(1, 1024).fieldOf("ticks_to_stay_pressed").forGetter(p_312686_ -> p_312686_.ticksToStayPressed),
                propertiesCodec()
            )
            .apply(p_360402_, ButtonBlock::new)
    );
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private final BlockSetType type;
    private final int ticksToStayPressed;
    private final Function<BlockState, VoxelShape> shapes;

    @Override
    public MapCodec<ButtonBlock> codec() {
        return CODEC;
    }

    public ButtonBlock(BlockSetType pType, int pTicksToStayPressed, BlockBehaviour.Properties pProperties) {
        super(pProperties.sound(pType.soundType()));
        this.type = pType;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false).setValue(FACE, AttachFace.WALL));
        this.ticksToStayPressed = pTicksToStayPressed;
        this.shapes = this.makeShapes();
    }

    private Function<BlockState, VoxelShape> makeShapes() {
        VoxelShape voxelshape = Block.cube(14.0);
        VoxelShape voxelshape1 = Block.cube(12.0);
        Map<AttachFace, Map<Direction, VoxelShape>> map = Shapes.rotateAttachFace(Block.boxZ(6.0, 4.0, 8.0, 16.0));
        return this.getShapeForEachState(
            p_390924_ -> Shapes.join(
                map.get(p_390924_.getValue(FACE)).get(p_390924_.getValue(FACING)),
                p_390924_.getValue(POWERED) ? voxelshape : voxelshape1,
                BooleanOp.ONLY_FIRST
            )
        );
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return this.shapes.apply(pState);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState p_329418_, Level p_334611_, BlockPos p_332004_, Player p_330636_, BlockHitResult p_327724_) {
        if (p_329418_.getValue(POWERED)) {
            return InteractionResult.CONSUME;
        } else {
            this.press(p_329418_, p_334611_, p_332004_, p_330636_);
            return InteractionResult.SUCCESS;
        }
    }

    @Override
    protected void onExplosionHit(BlockState p_310762_, ServerLevel p_363623_, BlockPos p_312982_, Explosion p_311820_, BiConsumer<ItemStack, BlockPos> p_312672_) {
        if (p_311820_.canTriggerBlocks() && !p_310762_.getValue(POWERED)) {
            this.press(p_310762_, p_363623_, p_312982_, null);
        }

        super.onExplosionHit(p_310762_, p_363623_, p_312982_, p_311820_, p_312672_);
    }

    public void press(BlockState pState, Level pLevel, BlockPos pPos, @Nullable Player pPlayer) {
        pLevel.setBlock(pPos, pState.setValue(POWERED, true), 3);
        this.updateNeighbours(pState, pLevel, pPos);
        pLevel.scheduleTick(pPos, this, this.ticksToStayPressed);
        this.playSound(pPlayer, pLevel, pPos, true);
        pLevel.gameEvent(pPlayer, GameEvent.BLOCK_ACTIVATE, pPos);
    }

    protected void playSound(@Nullable Player pPlayer, LevelAccessor pLevel, BlockPos pPos, boolean pHitByArrow) {
        pLevel.playSound(pHitByArrow ? pPlayer : null, pPos, this.getSound(pHitByArrow), SoundSource.BLOCKS);
    }

    protected SoundEvent getSound(boolean pIsOn) {
        return pIsOn ? this.type.buttonClickOn() : this.type.buttonClickOff();
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState p_391932_, ServerLevel p_395947_, BlockPos p_394950_, boolean p_394747_) {
        if (!p_394747_ && p_391932_.getValue(POWERED)) {
            this.updateNeighbours(p_391932_, p_395947_, p_394950_);
        }
    }

    @Override
    protected int getSignal(BlockState pBlockState, BlockGetter pBlockAccess, BlockPos pPos, Direction pSide) {
        return pBlockState.getValue(POWERED) ? 15 : 0;
    }

    @Override
    protected int getDirectSignal(BlockState pBlockState, BlockGetter pBlockAccess, BlockPos pPos, Direction pSide) {
        return pBlockState.getValue(POWERED) && getConnectedDirection(pBlockState) == pSide ? 15 : 0;
    }

    @Override
    protected boolean isSignalSource(BlockState pState) {
        return true;
    }

    @Override
    protected void tick(BlockState p_220903_, ServerLevel p_220904_, BlockPos p_220905_, RandomSource p_220906_) {
        if (p_220903_.getValue(POWERED)) {
            this.checkPressed(p_220903_, p_220904_, p_220905_);
        }
    }

    @Override
    protected void entityInside(BlockState p_51083_, Level p_51084_, BlockPos p_51085_, Entity p_51086_, InsideBlockEffectApplier p_394334_) {
        if (!p_51084_.isClientSide && this.type.canButtonBeActivatedByArrows() && !p_51083_.getValue(POWERED)) {
            this.checkPressed(p_51083_, p_51084_, p_51085_);
        }
    }

    protected void checkPressed(BlockState pState, Level pLevel, BlockPos pPos) {
        AbstractArrow abstractarrow = this.type.canButtonBeActivatedByArrows()
            ? pLevel.getEntitiesOfClass(AbstractArrow.class, pState.getShape(pLevel, pPos).bounds().move(pPos)).stream().findFirst().orElse(null)
            : null;
        boolean flag = abstractarrow != null;
        boolean flag1 = pState.getValue(POWERED);
        if (flag != flag1) {
            pLevel.setBlock(pPos, pState.setValue(POWERED, flag), 3);
            this.updateNeighbours(pState, pLevel, pPos);
            this.playSound(null, pLevel, pPos, flag);
            pLevel.gameEvent(abstractarrow, flag ? GameEvent.BLOCK_ACTIVATE : GameEvent.BLOCK_DEACTIVATE, pPos);
        }

        if (flag) {
            pLevel.scheduleTick(new BlockPos(pPos), this, this.ticksToStayPressed);
        }
    }

    private void updateNeighbours(BlockState pState, Level pLevel, BlockPos pPos) {
        Direction direction = getConnectedDirection(pState).getOpposite();
        Orientation orientation = ExperimentalRedstoneUtils.initialOrientation(
            pLevel, direction, direction.getAxis().isHorizontal() ? Direction.UP : pState.getValue(FACING)
        );
        pLevel.updateNeighborsAt(pPos, this, orientation);
        pLevel.updateNeighborsAt(pPos.relative(direction), this, orientation);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING, POWERED, FACE);
    }
}
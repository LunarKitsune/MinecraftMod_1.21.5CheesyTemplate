package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.DaylightDetectorBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DaylightDetectorBlock extends BaseEntityBlock {
    public static final MapCodec<DaylightDetectorBlock> CODEC = simpleCodec(DaylightDetectorBlock::new);
    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    public static final BooleanProperty INVERTED = BlockStateProperties.INVERTED;
    private static final VoxelShape SHAPE = Block.column(16.0, 0.0, 6.0);

    @Override
    public MapCodec<DaylightDetectorBlock> codec() {
        return CODEC;
    }

    public DaylightDetectorBlock(BlockBehaviour.Properties p_52382_) {
        super(p_52382_);
        this.registerDefaultState(this.stateDefinition.any().setValue(POWER, 0).setValue(INVERTED, false));
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPE;
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState pState) {
        return true;
    }

    @Override
    protected int getSignal(BlockState pBlockState, BlockGetter pBlockAccess, BlockPos pPos, Direction pSide) {
        return pBlockState.getValue(POWER);
    }

    private static void updateSignalStrength(BlockState pState, Level pLevel, BlockPos pPos) {
        int i = pLevel.getBrightness(LightLayer.SKY, pPos) - pLevel.getSkyDarken();
        float f = pLevel.getSunAngle(1.0F);
        boolean flag = pState.getValue(INVERTED);
        if (flag) {
            i = 15 - i;
        } else if (i > 0) {
            float f1 = f < (float) Math.PI ? 0.0F : (float) (Math.PI * 2);
            f += (f1 - f) * 0.2F;
            i = Math.round(i * Mth.cos(f));
        }

        i = Mth.clamp(i, 0, 15);
        if (pState.getValue(POWER) != i) {
            pLevel.setBlock(pPos, pState.setValue(POWER, i), 3);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState p_52391_, Level p_52392_, BlockPos p_52393_, Player p_52394_, BlockHitResult p_52396_) {
        if (!p_52394_.mayBuild()) {
            return super.useWithoutItem(p_52391_, p_52392_, p_52393_, p_52394_, p_52396_);
        } else {
            if (!p_52392_.isClientSide) {
                BlockState blockstate = p_52391_.cycle(INVERTED);
                p_52392_.setBlock(p_52393_, blockstate, 2);
                p_52392_.gameEvent(GameEvent.BLOCK_CHANGE, p_52393_, GameEvent.Context.of(p_52394_, blockstate));
                updateSignalStrength(blockstate, p_52392_, p_52393_);
            }

            return InteractionResult.SUCCESS;
        }
    }

    @Override
    protected boolean isSignalSource(BlockState pState) {
        return true;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos p_153118_, BlockState p_153119_) {
        return new DaylightDetectorBlockEntity(p_153118_, p_153119_);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level p_153109_, BlockState p_153110_, BlockEntityType<T> p_153111_) {
        return !p_153109_.isClientSide && p_153109_.dimensionType().hasSkyLight() ? createTickerHelper(p_153111_, BlockEntityType.DAYLIGHT_DETECTOR, DaylightDetectorBlock::tickEntity) : null;
    }

    private static void tickEntity(Level pLevel, BlockPos pPos, BlockState pState, DaylightDetectorBlockEntity pBlockEntity) {
        if (pLevel.getGameTime() % 20L == 0L) {
            updateSignalStrength(pState, pLevel, pPos);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(POWER, INVERTED);
    }
}
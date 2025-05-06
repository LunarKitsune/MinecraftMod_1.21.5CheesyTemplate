package net.minecraft.world.level.block;

import com.google.common.collect.Lists;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.Orientation;

public class RedstoneTorchBlock extends BaseTorchBlock {
    public static final MapCodec<RedstoneTorchBlock> CODEC = simpleCodec(RedstoneTorchBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final Map<BlockGetter, List<RedstoneTorchBlock.Toggle>> RECENT_TOGGLES = new WeakHashMap<>();
    public static final int RECENT_TOGGLE_TIMER = 60;
    public static final int MAX_RECENT_TOGGLES = 8;
    public static final int RESTART_DELAY = 160;
    private static final int TOGGLE_DELAY = 2;

    @Override
    public MapCodec<? extends RedstoneTorchBlock> codec() {
        return CODEC;
    }

    public RedstoneTorchBlock(BlockBehaviour.Properties p_55678_) {
        super(p_55678_);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, true));
    }

    @Override
    protected void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        this.notifyNeighbors(pLevel, pPos, pState);
    }

    private void notifyNeighbors(Level pLevel, BlockPos pPos, BlockState pState) {
        Orientation orientation = this.randomOrientation(pLevel, pState);

        for (Direction direction : Direction.values()) {
            pLevel.updateNeighborsAt(pPos.relative(direction), this, ExperimentalRedstoneUtils.withFront(orientation, direction));
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState p_396570_, ServerLevel p_391552_, BlockPos p_392704_, boolean p_397552_) {
        if (!p_397552_) {
            this.notifyNeighbors(p_391552_, p_392704_, p_396570_);
        }
    }

    @Override
    protected int getSignal(BlockState pBlockState, BlockGetter pBlockAccess, BlockPos pPos, Direction pSide) {
        return pBlockState.getValue(LIT) && Direction.UP != pSide ? 15 : 0;
    }

    protected boolean hasNeighborSignal(Level pLevel, BlockPos pPos, BlockState pState) {
        return pLevel.hasSignal(pPos.below(), Direction.DOWN);
    }

    @Override
    protected void tick(BlockState p_221949_, ServerLevel p_221950_, BlockPos p_221951_, RandomSource p_221952_) {
        boolean flag = this.hasNeighborSignal(p_221950_, p_221951_, p_221949_);
        List<RedstoneTorchBlock.Toggle> list = RECENT_TOGGLES.get(p_221950_);

        while (list != null && !list.isEmpty() && p_221950_.getGameTime() - list.get(0).when > 60L) {
            list.remove(0);
        }

        if (p_221949_.getValue(LIT)) {
            if (flag) {
                p_221950_.setBlock(p_221951_, p_221949_.setValue(LIT, false), 3);
                if (isToggledTooFrequently(p_221950_, p_221951_, true)) {
                    p_221950_.levelEvent(1502, p_221951_, 0);
                    p_221950_.scheduleTick(p_221951_, p_221950_.getBlockState(p_221951_).getBlock(), 160);
                }
            }
        } else if (!flag && !isToggledTooFrequently(p_221950_, p_221951_, false)) {
            p_221950_.setBlock(p_221951_, p_221949_.setValue(LIT, true), 3);
        }
    }

    @Override
    protected void neighborChanged(BlockState p_55699_, Level p_55700_, BlockPos p_55701_, Block p_55702_, @Nullable Orientation p_368542_, boolean p_55704_) {
        if (p_55699_.getValue(LIT) == this.hasNeighborSignal(p_55700_, p_55701_, p_55699_) && !p_55700_.getBlockTicks().willTickThisTick(p_55701_, this)) {
            p_55700_.scheduleTick(p_55701_, this, 2);
        }
    }

    @Override
    protected int getDirectSignal(BlockState pBlockState, BlockGetter pBlockAccess, BlockPos pPos, Direction pSide) {
        return pSide == Direction.DOWN ? pBlockState.getSignal(pBlockAccess, pPos, pSide) : 0;
    }

    @Override
    protected boolean isSignalSource(BlockState pState) {
        return true;
    }

    @Override
    public void animateTick(BlockState p_221954_, Level p_221955_, BlockPos p_221956_, RandomSource p_221957_) {
        if (p_221954_.getValue(LIT)) {
            double d0 = p_221956_.getX() + 0.5 + (p_221957_.nextDouble() - 0.5) * 0.2;
            double d1 = p_221956_.getY() + 0.7 + (p_221957_.nextDouble() - 0.5) * 0.2;
            double d2 = p_221956_.getZ() + 0.5 + (p_221957_.nextDouble() - 0.5) * 0.2;
            p_221955_.addParticle(DustParticleOptions.REDSTONE, d0, d1, d2, 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(LIT);
    }

    private static boolean isToggledTooFrequently(Level pLevel, BlockPos pPos, boolean pLogToggle) {
        List<RedstoneTorchBlock.Toggle> list = RECENT_TOGGLES.computeIfAbsent(pLevel, p_55680_ -> Lists.newArrayList());
        if (pLogToggle) {
            list.add(new RedstoneTorchBlock.Toggle(pPos.immutable(), pLevel.getGameTime()));
        }

        int i = 0;

        for (RedstoneTorchBlock.Toggle redstonetorchblock$toggle : list) {
            if (redstonetorchblock$toggle.pos.equals(pPos)) {
                if (++i >= 8) {
                    return true;
                }
            }
        }

        return false;
    }

    @Nullable
    protected Orientation randomOrientation(Level pLevel, BlockState pState) {
        return ExperimentalRedstoneUtils.initialOrientation(pLevel, null, Direction.UP);
    }

    public static class Toggle {
        final BlockPos pos;
        final long when;

        public Toggle(BlockPos pPos, long pWhen) {
            this.pos = pPos;
            this.when = pWhen;
        }
    }
}
package net.minecraft.world.level.redstone;

import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.ArrayDeque;
import java.util.Deque;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.RedstoneSide;

public class ExperimentalRedstoneWireEvaluator extends RedstoneWireEvaluator {
    private final Deque<BlockPos> wiresToTurnOff = new ArrayDeque<>();
    private final Deque<BlockPos> wiresToTurnOn = new ArrayDeque<>();
    private final Object2IntMap<BlockPos> updatedWires = new Object2IntLinkedOpenHashMap<>();

    public ExperimentalRedstoneWireEvaluator(RedStoneWireBlock p_369306_) {
        super(p_369306_);
    }

    @Override
    public void updatePowerStrength(Level p_367453_, BlockPos p_363644_, BlockState p_363406_, @Nullable Orientation p_364106_, boolean p_364023_) {
        Orientation orientation = getInitialOrientation(p_367453_, p_364106_);
        this.calculateCurrentChanges(p_367453_, p_363644_, orientation);
        ObjectIterator<Entry<BlockPos>> objectiterator = this.updatedWires.object2IntEntrySet().iterator();

        for (boolean flag = true; objectiterator.hasNext(); flag = false) {
            Entry<BlockPos> entry = objectiterator.next();
            BlockPos blockpos = entry.getKey();
            int i = entry.getIntValue();
            int j = unpackPower(i);
            BlockState blockstate = p_367453_.getBlockState(blockpos);
            if (blockstate.is(this.wireBlock) && !blockstate.getValue(RedStoneWireBlock.POWER).equals(j)) {
                int k = 2;
                if (!p_364023_ || !flag) {
                    k |= 128;
                }

                p_367453_.setBlock(blockpos, blockstate.setValue(RedStoneWireBlock.POWER, j), k);
            } else {
                objectiterator.remove();
            }
        }

        this.causeNeighborUpdates(p_367453_);
    }

    private void causeNeighborUpdates(Level pLevel) {
        this.updatedWires.forEach((p_366674_, p_368942_) -> {
            Orientation orientation = unpackOrientation(p_368942_);
            BlockState blockstate = pLevel.getBlockState(p_366674_);

            for (Direction direction : orientation.getDirections()) {
                if (isConnected(blockstate, direction)) {
                    BlockPos blockpos = p_366674_.relative(direction);
                    BlockState blockstate1 = pLevel.getBlockState(blockpos);
                    Orientation orientation1 = orientation.withFrontPreserveUp(direction);
                    pLevel.neighborChanged(blockstate1, blockpos, this.wireBlock, orientation1, false);
                    if (blockstate1.isRedstoneConductor(pLevel, blockpos)) {
                        for (Direction direction1 : orientation1.getDirections()) {
                            if (direction1 != direction.getOpposite()) {
                                pLevel.neighborChanged(blockpos.relative(direction1), this.wireBlock, orientation1.withFrontPreserveUp(direction1));
                            }
                        }
                    }
                }
            }
        });
    }

    private static boolean isConnected(BlockState pState, Direction pDirection) {
        EnumProperty<RedstoneSide> enumproperty = RedStoneWireBlock.PROPERTY_BY_DIRECTION.get(pDirection);
        return enumproperty == null ? pDirection == Direction.DOWN : pState.getValue(enumproperty).isConnected();
    }

    private static Orientation getInitialOrientation(Level pLevel, @Nullable Orientation pOrientation) {
        Orientation orientation;
        if (pOrientation != null) {
            orientation = pOrientation;
        } else {
            orientation = Orientation.random(pLevel.random);
        }

        return orientation.withUp(Direction.UP).withSideBias(Orientation.SideBias.LEFT);
    }

    private void calculateCurrentChanges(Level pLevel, BlockPos pPos, Orientation pOrientation) {
        BlockState blockstate = pLevel.getBlockState(pPos);
        if (blockstate.is(this.wireBlock)) {
            this.setPower(pPos, blockstate.getValue(RedStoneWireBlock.POWER), pOrientation);
            this.wiresToTurnOff.add(pPos);
        } else {
            this.propagateChangeToNeighbors(pLevel, pPos, 0, pOrientation, true);
        }

        while (!this.wiresToTurnOff.isEmpty()) {
            BlockPos blockpos = this.wiresToTurnOff.removeFirst();
            int i = this.updatedWires.getInt(blockpos);
            Orientation orientation = unpackOrientation(i);
            int j = unpackPower(i);
            int k = this.getBlockSignal(pLevel, blockpos);
            int l = this.getIncomingWireSignal(pLevel, blockpos);
            int i1 = Math.max(k, l);
            int j1;
            if (i1 < j) {
                if (k > 0 && !this.wiresToTurnOn.contains(blockpos)) {
                    this.wiresToTurnOn.add(blockpos);
                }

                j1 = 0;
            } else {
                j1 = i1;
            }

            if (j1 != j) {
                this.setPower(blockpos, j1, orientation);
            }

            this.propagateChangeToNeighbors(pLevel, blockpos, j1, orientation, j > i1);
        }

        while (!this.wiresToTurnOn.isEmpty()) {
            BlockPos blockpos1 = this.wiresToTurnOn.removeFirst();
            int k1 = this.updatedWires.getInt(blockpos1);
            int l1 = unpackPower(k1);
            int i2 = this.getBlockSignal(pLevel, blockpos1);
            int j2 = this.getIncomingWireSignal(pLevel, blockpos1);
            int k2 = Math.max(i2, j2);
            Orientation orientation1 = unpackOrientation(k1);
            if (k2 > l1) {
                this.setPower(blockpos1, k2, orientation1);
            } else if (k2 < l1) {
                throw new IllegalStateException("Turning off wire while trying to turn it on. Should not happen.");
            }

            this.propagateChangeToNeighbors(pLevel, blockpos1, k2, orientation1, false);
        }
    }

    private static int packOrientationAndPower(Orientation pOrientation, int pPower) {
        return pOrientation.getIndex() << 4 | pPower;
    }

    private static Orientation unpackOrientation(int pData) {
        return Orientation.fromIndex(pData >> 4);
    }

    private static int unpackPower(int pData) {
        return pData & 15;
    }

    private void setPower(BlockPos pPos, int pPower, Orientation pOrientation) {
        this.updatedWires
            .compute(pPos, (p_367119_, p_364881_) -> p_364881_ == null ? packOrientationAndPower(pOrientation, pPower) : packOrientationAndPower(unpackOrientation(p_364881_), pPower));
    }

    private void propagateChangeToNeighbors(Level pLevel, BlockPos pPos, int pPower, Orientation pOrientation, boolean pCanTurnOff) {
        for (Direction direction : pOrientation.getHorizontalDirections()) {
            BlockPos blockpos = pPos.relative(direction);
            this.enqueueNeighborWire(pLevel, blockpos, pPower, pOrientation.withFront(direction), pCanTurnOff);
        }

        for (Direction direction2 : pOrientation.getVerticalDirections()) {
            BlockPos blockpos3 = pPos.relative(direction2);
            boolean flag = pLevel.getBlockState(blockpos3).isRedstoneConductor(pLevel, blockpos3);

            for (Direction direction1 : pOrientation.getHorizontalDirections()) {
                BlockPos blockpos1 = pPos.relative(direction1);
                if (direction2 == Direction.UP && !flag) {
                    BlockPos blockpos4 = blockpos3.relative(direction1);
                    this.enqueueNeighborWire(pLevel, blockpos4, pPower, pOrientation.withFront(direction1), pCanTurnOff);
                } else if (direction2 == Direction.DOWN && !pLevel.getBlockState(blockpos1).isRedstoneConductor(pLevel, blockpos1)) {
                    BlockPos blockpos2 = blockpos3.relative(direction1);
                    this.enqueueNeighborWire(pLevel, blockpos2, pPower, pOrientation.withFront(direction1), pCanTurnOff);
                }
            }
        }
    }

    private void enqueueNeighborWire(Level pLevel, BlockPos pPos, int pPower, Orientation pOrientation, boolean pCanTurnOff) {
        BlockState blockstate = pLevel.getBlockState(pPos);
        if (blockstate.is(this.wireBlock)) {
            int i = this.getWireSignal(pPos, blockstate);
            if (i < pPower - 1 && !this.wiresToTurnOn.contains(pPos)) {
                this.wiresToTurnOn.add(pPos);
                this.setPower(pPos, i, pOrientation);
            }

            if (pCanTurnOff && i > pPower && !this.wiresToTurnOff.contains(pPos)) {
                this.wiresToTurnOff.add(pPos);
                this.setPower(pPos, i, pOrientation);
            }
        }
    }

    @Override
    protected int getWireSignal(BlockPos p_368955_, BlockState p_368466_) {
        int i = this.updatedWires.getOrDefault(p_368955_, -1);
        return i != -1 ? unpackPower(i) : super.getWireSignal(p_368955_, p_368466_);
    }
}
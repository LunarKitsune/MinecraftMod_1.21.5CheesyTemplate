package net.minecraft.commands.arguments.blocks;

import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.properties.Property;

public class BlockInput implements Predicate<BlockInWorld> {
    private final BlockState state;
    private final Set<Property<?>> properties;
    @Nullable
    private final CompoundTag tag;

    public BlockInput(BlockState pState, Set<Property<?>> pProperties, @Nullable CompoundTag pTag) {
        this.state = pState;
        this.properties = pProperties;
        this.tag = pTag;
    }

    public BlockState getState() {
        return this.state;
    }

    public Set<Property<?>> getDefinedProperties() {
        return this.properties;
    }

    public boolean test(BlockInWorld pBlock) {
        BlockState blockstate = pBlock.getState();
        if (!blockstate.is(this.state.getBlock())) {
            return false;
        } else {
            for (Property<?> property : this.properties) {
                if (blockstate.getValue(property) != this.state.getValue(property)) {
                    return false;
                }
            }

            if (this.tag == null) {
                return true;
            } else {
                BlockEntity blockentity = pBlock.getEntity();
                return blockentity != null && NbtUtils.compareNbt(this.tag, blockentity.saveWithFullMetadata(pBlock.getLevel().registryAccess()), true);
            }
        }
    }

    public boolean test(ServerLevel pLevel, BlockPos pPos) {
        return this.test(new BlockInWorld(pLevel, pPos, false));
    }

    public boolean place(ServerLevel pLevel, BlockPos pPos, int pFlags) {
        BlockState blockstate = (pFlags & 16) != 0 ? this.state : Block.updateFromNeighbourShapes(this.state, pLevel, pPos);
        if (blockstate.isAir()) {
            blockstate = this.state;
        }

        blockstate = this.overwriteWithDefinedProperties(blockstate);
        boolean flag = false;
        if (pLevel.setBlock(pPos, blockstate, pFlags)) {
            flag = true;
        }

        if (this.tag != null) {
            BlockEntity blockentity = pLevel.getBlockEntity(pPos);
            if (blockentity != null) {
                CompoundTag compoundtag = blockentity.saveWithoutMetadata(pLevel.registryAccess());
                blockentity.loadWithComponents(this.tag, pLevel.registryAccess());
                CompoundTag compoundtag1 = blockentity.saveWithoutMetadata(pLevel.registryAccess());
                if (!compoundtag1.equals(compoundtag)) {
                    flag = true;
                    blockentity.setChanged();
                    pLevel.getChunkSource().blockChanged(pPos);
                }
            }
        }

        return flag;
    }

    private BlockState overwriteWithDefinedProperties(BlockState pState) {
        if (pState == this.state) {
            return pState;
        } else {
            for (Property<?> property : this.properties) {
                pState = copyProperty(pState, this.state, property);
            }

            return pState;
        }
    }

    private static <T extends Comparable<T>> BlockState copyProperty(BlockState pSource, BlockState pTarget, Property<T> pProperty) {
        return pSource.trySetValue(pProperty, pTarget.getValue(pProperty));
    }
}
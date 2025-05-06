package net.minecraft.world.level.block.entity;

import com.mojang.logging.LogUtils;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.TestBlockMode;
import org.slf4j.Logger;

public class TestBlockEntity extends BlockEntity {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DEFAULT_MESSAGE = "";
    private static final boolean DEFAULT_POWERED = false;
    private TestBlockMode mode;
    private String message = "";
    private boolean powered = false;
    private boolean triggered;

    public TestBlockEntity(BlockPos pPos, BlockState pState) {
        super(BlockEntityType.TEST_BLOCK, pPos, pState);
        this.mode = pState.getValue(TestBlock.MODE);
    }

    @Override
    public void saveAdditional(CompoundTag p_393366_, HolderLookup.Provider p_392768_) {
        p_393366_.store("mode", TestBlockMode.CODEC, this.mode);
        p_393366_.putString("message", this.message);
        p_393366_.putBoolean("powered", this.powered);
    }

    @Override
    public void loadAdditional(CompoundTag p_395727_, HolderLookup.Provider p_392427_) {
        this.mode = p_395727_.read("mode", TestBlockMode.CODEC).orElse(TestBlockMode.FAIL);
        this.message = p_395727_.getStringOr("message", "");
        this.powered = p_395727_.getBooleanOr("powered", false);
    }

    private void updateBlockState() {
        if (this.level != null) {
            BlockPos blockpos = this.getBlockPos();
            BlockState blockstate = this.level.getBlockState(blockpos);
            if (blockstate.is(Blocks.TEST_BLOCK)) {
                this.level.setBlock(blockpos, blockstate.setValue(TestBlock.MODE, this.mode), 2);
            }
        }
    }

    @Nullable
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider p_395613_) {
        return this.saveCustomOnly(p_395613_);
    }

    public boolean isPowered() {
        return this.powered;
    }

    public void setPowered(boolean pPowered) {
        this.powered = pPowered;
    }

    public TestBlockMode getMode() {
        return this.mode;
    }

    public void setMode(TestBlockMode pMode) {
        this.mode = pMode;
        this.updateBlockState();
    }

    private Block getBlockType() {
        return this.getBlockState().getBlock();
    }

    public void reset() {
        this.triggered = false;
        if (this.mode == TestBlockMode.START && this.level != null) {
            this.setPowered(false);
            this.level.updateNeighborsAt(this.getBlockPos(), this.getBlockType());
        }
    }

    public void trigger() {
        if (this.mode == TestBlockMode.START && this.level != null) {
            this.setPowered(true);
            BlockPos blockpos = this.getBlockPos();
            this.level.updateNeighborsAt(blockpos, this.getBlockType());
            this.level.getBlockTicks().willTickThisTick(blockpos, this.getBlockType());
            this.log();
        } else {
            if (this.mode == TestBlockMode.LOG) {
                this.log();
            }

            this.triggered = true;
        }
    }

    public void log() {
        if (!this.message.isBlank()) {
            LOGGER.info("Test {} (at {}): {}", this.mode.getSerializedName(), this.getBlockPos(), this.message);
        }
    }

    public boolean hasTriggered() {
        return this.triggered;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String pMessage) {
        this.message = pMessage;
    }
}
package net.minecraft.world.level.block.entity;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class BrushableBlockEntity extends BlockEntity {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String LOOT_TABLE_TAG = "LootTable";
    private static final String LOOT_TABLE_SEED_TAG = "LootTableSeed";
    private static final String HIT_DIRECTION_TAG = "hit_direction";
    private static final String ITEM_TAG = "item";
    private static final int BRUSH_COOLDOWN_TICKS = 10;
    private static final int BRUSH_RESET_TICKS = 40;
    private static final int REQUIRED_BRUSHES_TO_BREAK = 10;
    private int brushCount;
    private long brushCountResetsAtTick;
    private long coolDownEndsAtTick;
    private ItemStack item = ItemStack.EMPTY;
    @Nullable
    private Direction hitDirection;
    @Nullable
    private ResourceKey<LootTable> lootTable;
    private long lootTableSeed;

    public BrushableBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(BlockEntityType.BRUSHABLE_BLOCK, pPos, pBlockState);
    }

    public boolean brush(long pStartTick, ServerLevel pLevel, LivingEntity pBrusher, Direction pHitDirection, ItemStack pStack) {
        if (this.hitDirection == null) {
            this.hitDirection = pHitDirection;
        }

        this.brushCountResetsAtTick = pStartTick + 40L;
        if (pStartTick < this.coolDownEndsAtTick) {
            return false;
        } else {
            this.coolDownEndsAtTick = pStartTick + 10L;
            this.unpackLootTable(pLevel, pBrusher, pStack);
            int i = this.getCompletionState();
            if (++this.brushCount >= 10) {
                this.brushingCompleted(pLevel, pBrusher, pStack);
                return true;
            } else {
                pLevel.scheduleTick(this.getBlockPos(), this.getBlockState().getBlock(), 2);
                int j = this.getCompletionState();
                if (i != j) {
                    BlockState blockstate = this.getBlockState();
                    BlockState blockstate1 = blockstate.setValue(BlockStateProperties.DUSTED, j);
                    pLevel.setBlock(this.getBlockPos(), blockstate1, 3);
                }

                return false;
            }
        }
    }

    private void unpackLootTable(ServerLevel pLevel, LivingEntity pBrusher, ItemStack pStack) {
        if (this.lootTable != null) {
            LootTable loottable = pLevel.getServer().reloadableRegistries().getLootTable(this.lootTable);
            if (pBrusher instanceof ServerPlayer serverplayer) {
                CriteriaTriggers.GENERATE_LOOT.trigger(serverplayer, this.lootTable);
            }

            LootParams lootparams = new LootParams.Builder(pLevel)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(this.worldPosition))
                .withLuck(pBrusher.getLuck())
                .withParameter(LootContextParams.THIS_ENTITY, pBrusher)
                .withParameter(LootContextParams.TOOL, pStack)
                .create(LootContextParamSets.ARCHAEOLOGY);
            ObjectArrayList<ItemStack> objectarraylist = loottable.getRandomItems(lootparams, this.lootTableSeed);

            this.item = switch (objectarraylist.size()) {
                case 0 -> ItemStack.EMPTY;
                case 1 -> (ItemStack)objectarraylist.getFirst();
                default -> {
                    LOGGER.warn("Expected max 1 loot from loot table {}, but got {}", this.lootTable.location(), objectarraylist.size());
                    yield objectarraylist.getFirst();
                }
            };
            this.lootTable = null;
            this.setChanged();
        }
    }

    private void brushingCompleted(ServerLevel pLevel, LivingEntity pBrusher, ItemStack pStack) {
        this.dropContent(pLevel, pBrusher, pStack);
        BlockState blockstate = this.getBlockState();
        pLevel.levelEvent(3008, this.getBlockPos(), Block.getId(blockstate));
        Block block;
        if (this.getBlockState().getBlock() instanceof BrushableBlock brushableblock) {
            block = brushableblock.getTurnsInto();
        } else {
            block = Blocks.AIR;
        }

        pLevel.setBlock(this.worldPosition, block.defaultBlockState(), 3);
    }

    private void dropContent(ServerLevel pLevel, LivingEntity pBrusher, ItemStack pStack) {
        this.unpackLootTable(pLevel, pBrusher, pStack);
        if (!this.item.isEmpty()) {
            double d0 = EntityType.ITEM.getWidth();
            double d1 = 1.0 - d0;
            double d2 = d0 / 2.0;
            Direction direction = Objects.requireNonNullElse(this.hitDirection, Direction.UP);
            BlockPos blockpos = this.worldPosition.relative(direction, 1);
            double d3 = blockpos.getX() + 0.5 * d1 + d2;
            double d4 = blockpos.getY() + 0.5 + EntityType.ITEM.getHeight() / 2.0F;
            double d5 = blockpos.getZ() + 0.5 * d1 + d2;
            ItemEntity itementity = new ItemEntity(pLevel, d3, d4, d5, this.item.split(pLevel.random.nextInt(21) + 10));
            itementity.setDeltaMovement(Vec3.ZERO);
            pLevel.addFreshEntity(itementity);
            this.item = ItemStack.EMPTY;
        }
    }

    public void checkReset(ServerLevel pLevel) {
        if (this.brushCount != 0 && pLevel.getGameTime() >= this.brushCountResetsAtTick) {
            int i = this.getCompletionState();
            this.brushCount = Math.max(0, this.brushCount - 2);
            int j = this.getCompletionState();
            if (i != j) {
                pLevel.setBlock(this.getBlockPos(), this.getBlockState().setValue(BlockStateProperties.DUSTED, j), 3);
            }

            int k = 4;
            this.brushCountResetsAtTick = pLevel.getGameTime() + 4L;
        }

        if (this.brushCount == 0) {
            this.hitDirection = null;
            this.brushCountResetsAtTick = 0L;
            this.coolDownEndsAtTick = 0L;
        } else {
            pLevel.scheduleTick(this.getBlockPos(), this.getBlockState().getBlock(), 2);
        }
    }

    private boolean tryLoadLootTable(CompoundTag pTag) {
        this.lootTable = pTag.read("LootTable", LootTable.KEY_CODEC).orElse(null);
        this.lootTableSeed = pTag.getLongOr("LootTableSeed", 0L);
        return this.lootTable != null;
    }

    private boolean trySaveLootTable(CompoundTag pTag) {
        if (this.lootTable == null) {
            return false;
        } else {
            pTag.store("LootTable", LootTable.KEY_CODEC, this.lootTable);
            if (this.lootTableSeed != 0L) {
                pTag.putLong("LootTableSeed", this.lootTableSeed);
            }

            return true;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider p_329297_) {
        CompoundTag compoundtag = super.getUpdateTag(p_329297_);
        compoundtag.storeNullable("hit_direction", Direction.LEGACY_ID_CODEC, this.hitDirection);
        if (!this.item.isEmpty()) {
            RegistryOps<Tag> registryops = p_329297_.createSerializationContext(NbtOps.INSTANCE);
            compoundtag.store("item", ItemStack.CODEC, registryops, this.item);
        }

        return compoundtag;
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void loadAdditional(CompoundTag p_335386_, HolderLookup.Provider p_334199_) {
        super.loadAdditional(p_335386_, p_334199_);
        RegistryOps<Tag> registryops = p_334199_.createSerializationContext(NbtOps.INSTANCE);
        if (!this.tryLoadLootTable(p_335386_)) {
            this.item = p_335386_.read("item", ItemStack.CODEC, registryops).orElse(ItemStack.EMPTY);
        } else {
            this.item = ItemStack.EMPTY;
        }

        this.hitDirection = p_335386_.read("hit_direction", Direction.LEGACY_ID_CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(CompoundTag p_277339_, HolderLookup.Provider p_329463_) {
        super.saveAdditional(p_277339_, p_329463_);
        if (!this.trySaveLootTable(p_277339_) && !this.item.isEmpty()) {
            RegistryOps<Tag> registryops = p_329463_.createSerializationContext(NbtOps.INSTANCE);
            p_277339_.store("item", ItemStack.CODEC, registryops, this.item);
        }
    }

    public void setLootTable(ResourceKey<LootTable> pLootTable, long pSeed) {
        this.lootTable = pLootTable;
        this.lootTableSeed = pSeed;
    }

    private int getCompletionState() {
        if (this.brushCount == 0) {
            return 0;
        } else if (this.brushCount < 3) {
            return 1;
        } else {
            return this.brushCount < 6 ? 2 : 3;
        }
    }

    @Nullable
    public Direction getHitDirection() {
        return this.hitDirection;
    }

    public ItemStack getItem() {
        return this.item;
    }
}
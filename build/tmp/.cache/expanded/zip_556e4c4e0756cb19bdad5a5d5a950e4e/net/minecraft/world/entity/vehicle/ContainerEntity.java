package net.minecraft.world.entity.vehicle;

import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public interface ContainerEntity extends Container, MenuProvider {
    Vec3 position();

    AABB getBoundingBox();

    @Nullable
    ResourceKey<LootTable> getContainerLootTable();

    void setContainerLootTable(@Nullable ResourceKey<LootTable> pLootTable);

    long getContainerLootTableSeed();

    void setContainerLootTableSeed(long pLootTableSeed);

    NonNullList<ItemStack> getItemStacks();

    void clearItemStacks();

    Level level();

    boolean isRemoved();

    @Override
    default boolean isEmpty() {
        return this.isChestVehicleEmpty();
    }

    default void addChestVehicleSaveData(CompoundTag pTag, HolderLookup.Provider pLevelRegistry) {
        if (this.getContainerLootTable() != null) {
            pTag.putString("LootTable", this.getContainerLootTable().location().toString());
            if (this.getContainerLootTableSeed() != 0L) {
                pTag.putLong("LootTableSeed", this.getContainerLootTableSeed());
            }
        } else {
            ContainerHelper.saveAllItems(pTag, this.getItemStacks(), pLevelRegistry);
        }
    }

    default void readChestVehicleSaveData(CompoundTag pTag, HolderLookup.Provider pLevelRegistry) {
        this.clearItemStacks();
        ResourceKey<LootTable> resourcekey = pTag.read("LootTable", LootTable.KEY_CODEC).orElse(null);
        this.setContainerLootTable(resourcekey);
        this.setContainerLootTableSeed(pTag.getLongOr("LootTableSeed", 0L));
        if (resourcekey == null) {
            ContainerHelper.loadAllItems(pTag, this.getItemStacks(), pLevelRegistry);
        }
    }

    default void chestVehicleDestroyed(DamageSource pDamageSource, ServerLevel pLevel, Entity pEntity) {
        if (pLevel.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            Containers.dropContents(pLevel, pEntity, this);
            Entity entity = pDamageSource.getDirectEntity();
            if (entity != null && entity.getType() == EntityType.PLAYER) {
                PiglinAi.angerNearbyPiglins(pLevel, (Player)entity, true);
            }
        }
    }

    default InteractionResult interactWithContainerVehicle(Player pPlayer) {
        pPlayer.openMenu(this);
        return InteractionResult.SUCCESS;
    }

    default void unpackChestVehicleLootTable(@Nullable Player pPlayer) {
        MinecraftServer minecraftserver = this.level().getServer();
        if (this.getContainerLootTable() != null && minecraftserver != null) {
            LootTable loottable = minecraftserver.reloadableRegistries().getLootTable(this.getContainerLootTable());
            if (pPlayer != null) {
                CriteriaTriggers.GENERATE_LOOT.trigger((ServerPlayer)pPlayer, this.getContainerLootTable());
            }

            this.setContainerLootTable(null);
            LootParams.Builder lootparams$builder = new LootParams.Builder((ServerLevel)this.level()).withParameter(LootContextParams.ORIGIN, this.position());
            // Forge: set the chest to killer_entity for loot context.
            if (this instanceof AbstractMinecartContainer entityContainer)
                lootparams$builder.withParameter(LootContextParams.ATTACKING_ENTITY, entityContainer);
            if (pPlayer != null) {
                lootparams$builder.withLuck(pPlayer.getLuck()).withParameter(LootContextParams.THIS_ENTITY, pPlayer);
            }

            loottable.fill(this, lootparams$builder.create(LootContextParamSets.CHEST), this.getContainerLootTableSeed());
        }
    }

    default void clearChestVehicleContent() {
        this.unpackChestVehicleLootTable(null);
        this.getItemStacks().clear();
    }

    default boolean isChestVehicleEmpty() {
        for (ItemStack itemstack : this.getItemStacks()) {
            if (!itemstack.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    default ItemStack removeChestVehicleItemNoUpdate(int pSlot) {
        this.unpackChestVehicleLootTable(null);
        ItemStack itemstack = this.getItemStacks().get(pSlot);
        if (itemstack.isEmpty()) {
            return ItemStack.EMPTY;
        } else {
            this.getItemStacks().set(pSlot, ItemStack.EMPTY);
            return itemstack;
        }
    }

    default ItemStack getChestVehicleItem(int pSlot) {
        this.unpackChestVehicleLootTable(null);
        return this.getItemStacks().get(pSlot);
    }

    default ItemStack removeChestVehicleItem(int pSlot, int pAmount) {
        this.unpackChestVehicleLootTable(null);
        return ContainerHelper.removeItem(this.getItemStacks(), pSlot, pAmount);
    }

    default void setChestVehicleItem(int pSlot, ItemStack pStack) {
        this.unpackChestVehicleLootTable(null);
        this.getItemStacks().set(pSlot, pStack);
        pStack.limitSize(this.getMaxStackSize(pStack));
    }

    default SlotAccess getChestVehicleSlot(final int pIndex) {
        return pIndex >= 0 && pIndex < this.getContainerSize() ? new SlotAccess() {
            @Override
            public ItemStack get() {
                return ContainerEntity.this.getChestVehicleItem(pIndex);
            }

            @Override
            public boolean set(ItemStack p_219964_) {
                ContainerEntity.this.setChestVehicleItem(pIndex, p_219964_);
                return true;
            }
        } : SlotAccess.NULL;
    }

    default boolean isChestVehicleStillValid(Player pPlayer) {
        return !this.isRemoved() && pPlayer.canInteractWithEntity(this.getBoundingBox(), 4.0);
    }
}

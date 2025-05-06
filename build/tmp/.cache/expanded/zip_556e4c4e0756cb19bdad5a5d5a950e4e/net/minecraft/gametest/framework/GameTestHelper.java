package net.minecraft.gametest.framework;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.LongStream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.commands.FillBiomeCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class GameTestHelper implements net.minecraftforge.common.extensions.IForgeGameTestHelper {
    private final GameTestInfo testInfo;
    private boolean finalCheckAdded;

    public GameTestHelper(GameTestInfo pTestInfo) {
        this.testInfo = pTestInfo;
    }

    public GameTestAssertException assertionException(Component pMessage) {
        return new GameTestAssertException(pMessage, this.testInfo.getTick());
    }

    public GameTestAssertException assertionException(String pMessageKey, Object... pArgs) {
        return this.assertionException(Component.translatableEscape(pMessageKey, pArgs));
    }

    public GameTestAssertPosException assertionException(BlockPos pPos, Component pMessage) {
        return new GameTestAssertPosException(pMessage, this.absolutePos(pPos), pPos, this.testInfo.getTick());
    }

    public GameTestAssertPosException assertionException(BlockPos pPos, String pMessageKey, Object... pArgs) {
        return this.assertionException(pPos, Component.translatableEscape(pMessageKey, pArgs));
    }

    public ServerLevel getLevel() {
        return this.testInfo.getLevel();
    }

    public BlockState getBlockState(BlockPos pPos) {
        return this.getLevel().getBlockState(this.absolutePos(pPos));
    }

    public <T extends BlockEntity> T getBlockEntity(BlockPos pPos, Class<T> pClazz) {
        BlockEntity blockentity = this.getLevel().getBlockEntity(this.absolutePos(pPos));
        if (blockentity == null) {
            throw this.assertionException(pPos, "test.error.missing_block_entity");
        } else if (pClazz.isInstance(blockentity)) {
            return pClazz.cast(blockentity);
        } else {
            throw this.assertionException(pPos, "test.error.wrong_block_entity", blockentity.getType().builtInRegistryHolder().getRegisteredName());
        }
    }

    public void killAllEntities() {
        this.killAllEntitiesOfClass(Entity.class);
    }

    public void killAllEntitiesOfClass(Class<? extends Entity> pEntityClass) {
        AABB aabb = this.getBounds();
        List<? extends Entity> list = this.getLevel().getEntitiesOfClass(pEntityClass, aabb.inflate(1.0), p_177131_ -> !(p_177131_ instanceof Player));
        list.forEach(p_358470_ -> p_358470_.kill(this.getLevel()));
    }

    public ItemEntity spawnItem(Item pItem, Vec3 pPos) {
        ServerLevel serverlevel = this.getLevel();
        Vec3 vec3 = this.absoluteVec(pPos);
        ItemEntity itementity = new ItemEntity(serverlevel, vec3.x, vec3.y, vec3.z, new ItemStack(pItem, 1));
        itementity.setDeltaMovement(0.0, 0.0, 0.0);
        serverlevel.addFreshEntity(itementity);
        return itementity;
    }

    public ItemEntity spawnItem(Item pItem, float pX, float pY, float pZ) {
        return this.spawnItem(pItem, new Vec3(pX, pY, pZ));
    }

    public ItemEntity spawnItem(Item pItem, BlockPos pPos) {
        return this.spawnItem(pItem, pPos.getX(), pPos.getY(), pPos.getZ());
    }

    public <E extends Entity> E spawn(EntityType<E> pType, BlockPos pPos) {
        return this.spawn(pType, Vec3.atBottomCenterOf(pPos));
    }

    public <E extends Entity> E spawn(EntityType<E> pType, Vec3 pPos) {
        ServerLevel serverlevel = this.getLevel();
        E e = pType.create(serverlevel, EntitySpawnReason.STRUCTURE);
        if (e == null) {
            throw this.assertionException(BlockPos.containing(pPos), "test.error.spawn_failure", pType.builtInRegistryHolder().getRegisteredName());
        } else {
            if (e instanceof Mob mob) {
                mob.setPersistenceRequired();
            }

            Vec3 vec3 = this.absoluteVec(pPos);
            e.snapTo(vec3.x, vec3.y, vec3.z, e.getYRot(), e.getXRot());
            serverlevel.addFreshEntity(e);
            return e;
        }
    }

    public void hurt(Entity pEntity, DamageSource pDamageSource, float pAmount) {
        pEntity.hurtServer(this.getLevel(), pDamageSource, pAmount);
    }

    public void kill(Entity pEntity) {
        pEntity.kill(this.getLevel());
    }

    public <E extends Entity> E findOneEntity(EntityType<E> pType) {
        return this.findClosestEntity(pType, 0, 0, 0, 2.147483647E9);
    }

    public <E extends Entity> E findClosestEntity(EntityType<E> pType, int pX, int pY, int pZ, double pRadius) {
        List<E> list = this.findEntities(pType, pX, pY, pZ, pRadius);
        if (list.isEmpty()) {
            throw this.assertionException("test.error.expected_entity_around", pType.getDescription(), pX, pY, pZ);
        } else if (list.size() > 1) {
            throw this.assertionException("test.error.too_many_entities", pType.toShortString(), pX, pY, pZ, list.size());
        } else {
            Vec3 vec3 = this.absoluteVec(new Vec3(pX, pY, pZ));
            list.sort((p_325933_, p_325934_) -> {
                double d0 = p_325933_.position().distanceTo(vec3);
                double d1 = p_325934_.position().distanceTo(vec3);
                return Double.compare(d0, d1);
            });
            return list.get(0);
        }
    }

    public <E extends Entity> List<E> findEntities(EntityType<E> pType, int pX, int pY, int pZ, double pRadius) {
        return this.findEntities(pType, Vec3.atBottomCenterOf(new BlockPos(pX, pY, pZ)), pRadius);
    }

    public <E extends Entity> List<E> findEntities(EntityType<E> pType, Vec3 pPos, double pRadius) {
        ServerLevel serverlevel = this.getLevel();
        Vec3 vec3 = this.absoluteVec(pPos);
        AABB aabb = this.testInfo.getStructureBounds();
        AABB aabb1 = new AABB(vec3.add(-pRadius, -pRadius, -pRadius), vec3.add(pRadius, pRadius, pRadius));
        return serverlevel.getEntities(pType, aabb, p_325936_ -> p_325936_.getBoundingBox().intersects(aabb1) && p_325936_.isAlive());
    }

    public <E extends Entity> E spawn(EntityType<E> pType, int pX, int pY, int pZ) {
        return this.spawn(pType, new BlockPos(pX, pY, pZ));
    }

    public <E extends Entity> E spawn(EntityType<E> pType, float pX, float pY, float pZ) {
        return this.spawn(pType, new Vec3(pX, pY, pZ));
    }

    public <E extends Mob> E spawnWithNoFreeWill(EntityType<E> pType, BlockPos pPos) {
        E e = (E)this.spawn(pType, pPos);
        e.removeFreeWill();
        return e;
    }

    public <E extends Mob> E spawnWithNoFreeWill(EntityType<E> pType, int pX, int pY, int pZ) {
        return this.spawnWithNoFreeWill(pType, new BlockPos(pX, pY, pZ));
    }

    public <E extends Mob> E spawnWithNoFreeWill(EntityType<E> pType, Vec3 pPos) {
        E e = (E)this.spawn(pType, pPos);
        e.removeFreeWill();
        return e;
    }

    public <E extends Mob> E spawnWithNoFreeWill(EntityType<E> pType, float pX, float pY, float pZ) {
        return this.spawnWithNoFreeWill(pType, new Vec3(pX, pY, pZ));
    }

    public void moveTo(Mob pMob, float pX, float pY, float pZ) {
        Vec3 vec3 = this.absoluteVec(new Vec3(pX, pY, pZ));
        pMob.snapTo(vec3.x, vec3.y, vec3.z, pMob.getYRot(), pMob.getXRot());
    }

    public GameTestSequence walkTo(Mob pMob, BlockPos pPos, float pSpeed) {
        return this.startSequence().thenExecuteAfter(2, () -> {
            Path path = pMob.getNavigation().createPath(this.absolutePos(pPos), 0);
            pMob.getNavigation().moveTo(path, pSpeed);
        });
    }

    public void pressButton(int pX, int pY, int pZ) {
        this.pressButton(new BlockPos(pX, pY, pZ));
    }

    public void pressButton(BlockPos pPos) {
        this.assertBlockTag(BlockTags.BUTTONS, pPos);
        BlockPos blockpos = this.absolutePos(pPos);
        BlockState blockstate = this.getLevel().getBlockState(blockpos);
        ButtonBlock buttonblock = (ButtonBlock)blockstate.getBlock();
        buttonblock.press(blockstate, this.getLevel(), blockpos, null);
    }

    public void useBlock(BlockPos pPos) {
        this.useBlock(pPos, this.makeMockPlayer(GameType.CREATIVE));
    }

    public void useBlock(BlockPos pPos, Player pPlayer) {
        BlockPos blockpos = this.absolutePos(pPos);
        this.useBlock(pPos, pPlayer, new BlockHitResult(Vec3.atCenterOf(blockpos), Direction.NORTH, blockpos, true));
    }

    public void useBlock(BlockPos pPos, Player pPlayer, BlockHitResult pResult) {
        BlockPos blockpos = this.absolutePos(pPos);
        BlockState blockstate = this.getLevel().getBlockState(blockpos);
        InteractionHand interactionhand = InteractionHand.MAIN_HAND;
        InteractionResult interactionresult = blockstate.useItemOn(pPlayer.getItemInHand(interactionhand), this.getLevel(), pPlayer, interactionhand, pResult);
        if (!interactionresult.consumesAction()) {
            if (!(interactionresult instanceof InteractionResult.TryEmptyHandInteraction)
                || !blockstate.useWithoutItem(this.getLevel(), pPlayer, pResult).consumesAction()) {
                UseOnContext useoncontext = new UseOnContext(pPlayer, interactionhand, pResult);
                pPlayer.getItemInHand(interactionhand).useOn(useoncontext);
            }
        }
    }

    public LivingEntity makeAboutToDrown(LivingEntity pEntity) {
        pEntity.setAirSupply(0);
        pEntity.setHealth(0.25F);
        return pEntity;
    }

    public LivingEntity withLowHealth(LivingEntity pEntity) {
        pEntity.setHealth(0.25F);
        return pEntity;
    }

    public Player makeMockPlayer(final GameType pGameType) {
        return new Player(this.getLevel(), BlockPos.ZERO, 0.0F, new GameProfile(UUID.randomUUID(), "test-mock-player")) {
            @Nonnull
            @Override
            public GameType gameMode() {
                return pGameType;
            }

            @Override
            public boolean isClientAuthoritative() {
                return false;
            }
        };
    }

    @Deprecated(
        forRemoval = true
    )
    public ServerPlayer makeMockServerPlayerInLevel() {
        CommonListenerCookie commonlistenercookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "test-mock-player"), false);
        ServerPlayer serverplayer = new ServerPlayer(
            this.getLevel().getServer(), this.getLevel(), commonlistenercookie.gameProfile(), commonlistenercookie.clientInformation()
        ) {
            @Override
            public GameType gameMode() {
                return GameType.CREATIVE;
            }
        };
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        this.getLevel().getServer().getPlayerList().placeNewPlayer(connection, serverplayer, commonlistenercookie);
        return serverplayer;
    }

    public void pullLever(int pX, int pY, int pZ) {
        this.pullLever(new BlockPos(pX, pY, pZ));
    }

    public void pullLever(BlockPos pPos) {
        this.assertBlockPresent(Blocks.LEVER, pPos);
        BlockPos blockpos = this.absolutePos(pPos);
        BlockState blockstate = this.getLevel().getBlockState(blockpos);
        LeverBlock leverblock = (LeverBlock)blockstate.getBlock();
        leverblock.pull(blockstate, this.getLevel(), blockpos, null);
    }

    public void pulseRedstone(BlockPos pPos, long pDelay) {
        this.setBlock(pPos, Blocks.REDSTONE_BLOCK);
        this.runAfterDelay(pDelay, () -> this.setBlock(pPos, Blocks.AIR));
    }

    public void destroyBlock(BlockPos pPos) {
        this.getLevel().destroyBlock(this.absolutePos(pPos), false, null);
    }

    public void setBlock(int pX, int pY, int pZ, Block pBlock) {
        this.setBlock(new BlockPos(pX, pY, pZ), pBlock);
    }

    public void setBlock(int pX, int pY, int pZ, BlockState pState) {
        this.setBlock(new BlockPos(pX, pY, pZ), pState);
    }

    public void setBlock(BlockPos pPos, Block pBlock) {
        this.setBlock(pPos, pBlock.defaultBlockState());
    }

    public void setBlock(BlockPos pPos, BlockState pState) {
        this.getLevel().setBlock(this.absolutePos(pPos), pState, 3);
    }

    public void setNight() {
        this.setDayTime(13000);
    }

    public void setDayTime(int pTime) {
        this.getLevel().setDayTime(pTime);
    }

    public void assertBlockPresent(Block pBlock, int pX, int pY, int pZ) {
        this.assertBlockPresent(pBlock, new BlockPos(pX, pY, pZ));
    }

    public void assertBlockPresent(Block pBlock, BlockPos pPos) {
        BlockState blockstate = this.getBlockState(pPos);
        this.assertBlock(
            pPos,
            p_177216_ -> blockstate.is(pBlock),
            p_389750_ -> Component.translatable("test.error.expected_block", pBlock.getName(), p_389750_.getName())
        );
    }

    public void assertBlockNotPresent(Block pBlock, int pX, int pY, int pZ) {
        this.assertBlockNotPresent(pBlock, new BlockPos(pX, pY, pZ));
    }

    public void assertBlockNotPresent(Block pBlock, BlockPos pPos) {
        this.assertBlock(
            pPos,
            p_177251_ -> !this.getBlockState(pPos).is(pBlock),
            p_389752_ -> Component.translatable("test.error.unexpected_block", pBlock.getName())
        );
    }

    public void assertBlockTag(TagKey<Block> pTag, BlockPos pPos) {
        this.assertBlockState(
            pPos,
            p_389762_ -> p_389762_.is(pTag),
            p_389754_ -> Component.translatable("test.error.expected_block_tag", pTag.location(), p_389754_.getBlock().getName())
        );
    }

    public void succeedWhenBlockPresent(Block pBlock, int pX, int pY, int pZ) {
        this.succeedWhenBlockPresent(pBlock, new BlockPos(pX, pY, pZ));
    }

    public void succeedWhenBlockPresent(Block pBlock, BlockPos pPos) {
        this.succeedWhen(() -> this.assertBlockPresent(pBlock, pPos));
    }

    public void assertBlock(BlockPos pPos, Predicate<Block> pPredicate, Function<Block, Component> pMessage) {
        this.assertBlockState(pPos, p_177296_ -> pPredicate.test(p_177296_.getBlock()), p_389756_ -> pMessage.apply(p_389756_.getBlock()));
    }

    public <T extends Comparable<T>> void assertBlockProperty(BlockPos pPos, Property<T> pProperty, T pValue) {
        BlockState blockstate = this.getBlockState(pPos);
        boolean flag = blockstate.hasProperty(pProperty);
        if (!flag) {
            throw this.assertionException(pPos, "test.error.block_property_missing", pProperty.getName(), pValue);
        } else if (!blockstate.<T>getValue(pProperty).equals(pValue)) {
            throw this.assertionException(pPos, "test.error.block_property_mismatch", pProperty.getName(), pValue, blockstate.getValue(pProperty));
        }
    }

    public <T extends Comparable<T>> void assertBlockProperty(BlockPos pPos, Property<T> pProperty, Predicate<T> pPredicate, Component pMessage) {
        this.assertBlockState(pPos, p_277264_ -> {
            if (!p_277264_.hasProperty(pProperty)) {
                return false;
            } else {
                T t = p_277264_.getValue(pProperty);
                return pPredicate.test(t);
            }
        }, p_397227_ -> pMessage);
    }

    public void assertBlockState(BlockPos pPos, BlockState pState) {
        BlockState blockstate = this.getBlockState(pPos);
        if (!blockstate.equals(pState)) {
            throw this.assertionException(pPos, "test.error.state_not_equal", pState, blockstate);
        }
    }

    public void assertBlockState(BlockPos pPos, Predicate<BlockState> pPredicate, Function<BlockState, Component> pMessage) {
        BlockState blockstate = this.getBlockState(pPos);
        if (!pPredicate.test(blockstate)) {
            throw this.assertionException(pPos, pMessage.apply(blockstate));
        }
    }

    public <T extends BlockEntity> void assertBlockEntityData(BlockPos pPos, Class<T> pBlockEntityClass, Predicate<T> pPredicate, Supplier<Component> pMessage) {
        T t = this.getBlockEntity(pPos, pBlockEntityClass);
        if (!pPredicate.test(t)) {
            throw this.assertionException(pPos, pMessage.get());
        }
    }

    public void assertRedstoneSignal(BlockPos pPos, Direction pDirection, IntPredicate pSignalStrengthPredicate, Supplier<Component> pMessage) {
        BlockPos blockpos = this.absolutePos(pPos);
        ServerLevel serverlevel = this.getLevel();
        BlockState blockstate = serverlevel.getBlockState(blockpos);
        int i = blockstate.getSignal(serverlevel, blockpos, pDirection);
        if (!pSignalStrengthPredicate.test(i)) {
            throw this.assertionException(pPos, pMessage.get());
        }
    }

    public void assertEntityPresent(EntityType<?> pType) {
        List<? extends Entity> list = this.getLevel().getEntities(pType, this.getBounds(), Entity::isAlive);
        if (list.isEmpty()) {
            throw this.assertionException("test.error.expected_entity_in_test", pType.getDescription());
        }
    }

    public void assertEntityPresent(EntityType<?> pType, int pX, int pY, int pZ) {
        this.assertEntityPresent(pType, new BlockPos(pX, pY, pZ));
    }

    public void assertEntityPresent(EntityType<?> pType, BlockPos pPos) {
        BlockPos blockpos = this.absolutePos(pPos);
        List<? extends Entity> list = this.getLevel().getEntities(pType, new AABB(blockpos), Entity::isAlive);
        if (list.isEmpty()) {
            throw this.assertionException(pPos, "test.error.expected_entity", pType.getDescription());
        }
    }

    public void assertEntityPresent(EntityType<?> pType, AABB pBox) {
        AABB aabb = this.absoluteAABB(pBox);
        List<? extends Entity> list = this.getLevel().getEntities(pType, aabb, Entity::isAlive);
        if (list.isEmpty()) {
            throw this.assertionException(BlockPos.containing(pBox.getCenter()), "test.error.expected_entity", pType.getDescription());
        }
    }

    public void assertEntitiesPresent(EntityType<?> pEntityType, int pCount) {
        List<? extends Entity> list = this.getLevel().getEntities(pEntityType, this.getBounds(), Entity::isAlive);
        if (list.size() != pCount) {
            throw this.assertionException("test.error.expected_entity_count", pCount, pEntityType.getDescription(), list.size());
        }
    }

    public void assertEntitiesPresent(EntityType<?> pEntityType, BlockPos pPos, int pCount, double pRadius) {
        BlockPos blockpos = this.absolutePos(pPos);
        List<? extends Entity> list = this.getEntities((EntityType<? extends Entity>)pEntityType, pPos, pRadius);
        if (list.size() != pCount) {
            throw this.assertionException(pPos, "test.error.expected_entity_count", pCount, pEntityType.getDescription(), list.size());
        }
    }

    public void assertEntityPresent(EntityType<?> pType, BlockPos pPos, double pExpansionAmount) {
        List<? extends Entity> list = this.getEntities((EntityType<? extends Entity>)pType, pPos, pExpansionAmount);
        if (list.isEmpty()) {
            BlockPos blockpos = this.absolutePos(pPos);
            throw this.assertionException(pPos, "test.error.expected_entity", pType.getDescription());
        }
    }

    public <T extends Entity> List<T> getEntities(EntityType<T> pEntityType, BlockPos pPos, double pRadius) {
        BlockPos blockpos = this.absolutePos(pPos);
        return this.getLevel().getEntities(pEntityType, new AABB(blockpos).inflate(pRadius), Entity::isAlive);
    }

    public <T extends Entity> List<T> getEntities(EntityType<T> pEntityType) {
        return this.getLevel().getEntities(pEntityType, this.getBounds(), Entity::isAlive);
    }

    public void assertEntityInstancePresent(Entity pEntity, int pX, int pY, int pZ) {
        this.assertEntityInstancePresent(pEntity, new BlockPos(pX, pY, pZ));
    }

    public void assertEntityInstancePresent(Entity pEntity, BlockPos pPos) {
        BlockPos blockpos = this.absolutePos(pPos);
        List<? extends Entity> list = this.getLevel().getEntities(pEntity.getType(), new AABB(blockpos), Entity::isAlive);
        list.stream()
            .filter(p_177139_ -> p_177139_ == pEntity)
            .findFirst()
            .orElseThrow(() -> this.assertionException(pPos, "test.error.expected_entity", pEntity.getType().getDescription()));
    }

    public void assertItemEntityCountIs(Item pItem, BlockPos pPos, double pExpansionAmount, int pCount) {
        BlockPos blockpos = this.absolutePos(pPos);
        List<ItemEntity> list = this.getLevel().getEntities(EntityType.ITEM, new AABB(blockpos).inflate(pExpansionAmount), Entity::isAlive);
        int i = 0;

        for (ItemEntity itementity : list) {
            ItemStack itemstack = itementity.getItem();
            if (itemstack.is(pItem)) {
                i += itemstack.getCount();
            }
        }

        if (i != pCount) {
            throw this.assertionException(pPos, "test.error.expected_items_count", pCount, pItem.getName(), i);
        }
    }

    public void assertItemEntityPresent(Item pItem, BlockPos pPos, double pExpansionAmount) {
        BlockPos blockpos = this.absolutePos(pPos);

        for (Entity entity : this.getLevel().getEntities(EntityType.ITEM, new AABB(blockpos).inflate(pExpansionAmount), Entity::isAlive)) {
            ItemEntity itementity = (ItemEntity)entity;
            if (itementity.getItem().getItem().equals(pItem)) {
                return;
            }
        }

        throw this.assertionException(pPos, "test.error.expected_item", pItem.getName());
    }

    public void assertItemEntityNotPresent(Item pItem, BlockPos pPos, double pRadius) {
        BlockPos blockpos = this.absolutePos(pPos);

        for (Entity entity : this.getLevel().getEntities(EntityType.ITEM, new AABB(blockpos).inflate(pRadius), Entity::isAlive)) {
            ItemEntity itementity = (ItemEntity)entity;
            if (itementity.getItem().getItem().equals(pItem)) {
                throw this.assertionException(pPos, "test.error.unexpected_item", pItem.getName());
            }
        }
    }

    public void assertItemEntityPresent(Item pItem) {
        for (Entity entity : this.getLevel().getEntities(EntityType.ITEM, this.getBounds(), Entity::isAlive)) {
            ItemEntity itementity = (ItemEntity)entity;
            if (itementity.getItem().getItem().equals(pItem)) {
                return;
            }
        }

        throw this.assertionException("test.error.expected_item", pItem.getName());
    }

    public void assertItemEntityNotPresent(Item pItem) {
        for (Entity entity : this.getLevel().getEntities(EntityType.ITEM, this.getBounds(), Entity::isAlive)) {
            ItemEntity itementity = (ItemEntity)entity;
            if (itementity.getItem().getItem().equals(pItem)) {
                throw this.assertionException("test.error.unexpected_item", pItem.getName());
            }
        }
    }

    public void assertEntityNotPresent(EntityType<?> pType) {
        List<? extends Entity> list = this.getLevel().getEntities(pType, this.getBounds(), Entity::isAlive);
        if (!list.isEmpty()) {
            throw this.assertionException(list.getFirst().blockPosition(), "test.error.unexpected_entity", pType.getDescription());
        }
    }

    public void assertEntityNotPresent(EntityType<?> pType, int pX, int pY, int pZ) {
        this.assertEntityNotPresent(pType, new BlockPos(pX, pY, pZ));
    }

    public void assertEntityNotPresent(EntityType<?> pType, BlockPos pPos) {
        BlockPos blockpos = this.absolutePos(pPos);
        List<? extends Entity> list = this.getLevel().getEntities(pType, new AABB(blockpos), Entity::isAlive);
        if (!list.isEmpty()) {
            throw this.assertionException(pPos, "test.error.unexpected_entity", pType.getDescription());
        }
    }

    public void assertEntityNotPresent(EntityType<?> pType, AABB pBox) {
        AABB aabb = this.absoluteAABB(pBox);
        List<? extends Entity> list = this.getLevel().getEntities(pType, aabb, Entity::isAlive);
        if (!list.isEmpty()) {
            throw this.assertionException(list.getFirst().blockPosition(), "test.error.unexpected_entity", pType.getDescription());
        }
    }

    public void assertEntityTouching(EntityType<?> pType, double pX, double pY, double pZ) {
        Vec3 vec3 = new Vec3(pX, pY, pZ);
        Vec3 vec31 = this.absoluteVec(vec3);
        Predicate<? super Entity> predicate = p_177346_ -> p_177346_.getBoundingBox().intersects(vec31, vec31);
        List<? extends Entity> list = this.getLevel().getEntities(pType, this.getBounds(), predicate);
        if (list.isEmpty()) {
            throw this.assertionException(
                "test.error.expected_entity_touching", pType.getDescription(), vec31.x(), vec31.y(), vec31.z(), pX, pY, pZ
            );
        }
    }

    public void assertEntityNotTouching(EntityType<?> pType, double pX, double pY, double pZ) {
        Vec3 vec3 = new Vec3(pX, pY, pZ);
        Vec3 vec31 = this.absoluteVec(vec3);
        Predicate<? super Entity> predicate = p_177231_ -> !p_177231_.getBoundingBox().intersects(vec31, vec31);
        List<? extends Entity> list = this.getLevel().getEntities(pType, this.getBounds(), predicate);
        if (list.isEmpty()) {
            throw this.assertionException(
                "test.error.expected_entity_not_touching",
                pType.getDescription(),
                vec31.x(),
                vec31.y(),
                vec31.z(),
                pX,
                pY,
                pZ
            );
        }
    }

    public <E extends Entity, T> void assertEntityData(BlockPos pPos, EntityType<E> pType, Predicate<E> pPredicate) {
        BlockPos blockpos = this.absolutePos(pPos);
        List<E> list = this.getLevel().getEntities(pType, new AABB(blockpos), Entity::isAlive);
        if (list.isEmpty()) {
            throw this.assertionException(pPos, "test.error.expected_entity", pType.getDescription());
        } else {
            for (E e : list) {
                if (!pPredicate.test(e)) {
                    throw this.assertionException(e.blockPosition(), "test.error.expected_entity_data_predicate", e.getName());
                }
            }
        }
    }

    public <E extends Entity, T> void assertEntityData(BlockPos pPos, EntityType<E> pType, Function<? super E, T> pEntityDataGetter, @Nullable T pTestEntityData) {
        BlockPos blockpos = this.absolutePos(pPos);
        List<E> list = this.getLevel().getEntities(pType, new AABB(blockpos), Entity::isAlive);
        if (list.isEmpty()) {
            throw this.assertionException(pPos, "test.error.expected_entity", pType.getDescription());
        } else {
            for (E e : list) {
                T t = pEntityDataGetter.apply(e);
                if (!Objects.equals(t, pTestEntityData)) {
                    throw this.assertionException(pPos, "test.error.expected_entity_data", pTestEntityData, t);
                }
            }
        }
    }

    public <E extends LivingEntity> void assertEntityIsHolding(BlockPos pPos, EntityType<E> pEntityType, Item pItem) {
        BlockPos blockpos = this.absolutePos(pPos);
        List<E> list = this.getLevel().getEntities(pEntityType, new AABB(blockpos), Entity::isAlive);
        if (list.isEmpty()) {
            throw this.assertionException(pPos, "test.error.expected_entity", pEntityType.getDescription());
        } else {
            for (E e : list) {
                if (e.isHolding(pItem)) {
                    return;
                }
            }

            throw this.assertionException(pPos, "test.error.expected_entity_holding", pItem.getName());
        }
    }

    public <E extends Entity & InventoryCarrier> void assertEntityInventoryContains(BlockPos pPos, EntityType<E> pEntityType, Item pItem) {
        BlockPos blockpos = this.absolutePos(pPos);
        List<E> list = this.getLevel().getEntities(pEntityType, new AABB(blockpos), p_263479_ -> p_263479_.isAlive());
        if (list.isEmpty()) {
            throw this.assertionException(pPos, "test.error.expected_entity", pEntityType.getDescription());
        } else {
            for (E e : list) {
                if (e.getInventory().hasAnyMatching(p_263481_ -> p_263481_.is(pItem))) {
                    return;
                }
            }

            throw this.assertionException(pPos, "test.error.expected_entity_having", pItem.getName());
        }
    }

    public void assertContainerEmpty(BlockPos pPos) {
        BaseContainerBlockEntity basecontainerblockentity = this.getBlockEntity(pPos, BaseContainerBlockEntity.class);
        if (!basecontainerblockentity.isEmpty()) {
            throw this.assertionException(pPos, "test.error.expected_empty_container");
        }
    }

    public void assertContainerContainsSingle(BlockPos pPos, Item pItem) {
        BaseContainerBlockEntity basecontainerblockentity = this.getBlockEntity(pPos, BaseContainerBlockEntity.class);
        if (basecontainerblockentity.countItem(pItem) != 1) {
            throw this.assertionException(pPos, "test.error.expected_container_contents_single", pItem.getName());
        }
    }

    public void assertContainerContains(BlockPos pPos, Item pItem) {
        BaseContainerBlockEntity basecontainerblockentity = this.getBlockEntity(pPos, BaseContainerBlockEntity.class);
        if (basecontainerblockentity.countItem(pItem) == 0) {
            throw this.assertionException(pPos, "test.error.expected_container_contents", pItem.getName());
        }
    }

    public void assertSameBlockStates(BoundingBox pBoundingBox, BlockPos pPos) {
        BlockPos.betweenClosedStream(pBoundingBox)
            .forEach(
                p_177267_ -> {
                    BlockPos blockpos = pPos.offset(
                        p_177267_.getX() - pBoundingBox.minX(),
                        p_177267_.getY() - pBoundingBox.minY(),
                        p_177267_.getZ() - pBoundingBox.minZ()
                    );
                    this.assertSameBlockState(p_177267_, blockpos);
                }
            );
    }

    public void assertSameBlockState(BlockPos pTestPos, BlockPos pComparisonPos) {
        BlockState blockstate = this.getBlockState(pTestPos);
        BlockState blockstate1 = this.getBlockState(pComparisonPos);
        if (blockstate != blockstate1) {
            throw this.assertionException(pTestPos, "test.error.state_not_equal", blockstate1, blockstate);
        }
    }

    public void assertAtTickTimeContainerContains(long pTickTime, BlockPos pPos, Item pItem) {
        this.runAtTickTime(pTickTime, () -> this.assertContainerContainsSingle(pPos, pItem));
    }

    public void assertAtTickTimeContainerEmpty(long pTickTime, BlockPos pPos) {
        this.runAtTickTime(pTickTime, () -> this.assertContainerEmpty(pPos));
    }

    public <E extends Entity, T> void succeedWhenEntityData(BlockPos pPos, EntityType<E> pType, Function<E, T> pEntityDataGetter, T pTestEntityData) {
        this.succeedWhen(() -> this.assertEntityData(pPos, pType, pEntityDataGetter, pTestEntityData));
    }

    public void assertEntityPosition(Entity pEntity, AABB pBoundingBox, Component pMessage) {
        if (!pBoundingBox.contains(this.relativeVec(pEntity.position()))) {
            throw this.assertionException(pMessage);
        }
    }

    public <E extends Entity> void assertEntityProperty(E pEntity, Predicate<E> pPredicate, Component pMessage) {
        if (!pPredicate.test(pEntity)) {
            throw this.assertionException(pEntity.blockPosition(), "test.error.entity_property", pEntity.getName(), pMessage);
        }
    }

    public <E extends Entity, T> void assertEntityProperty(E pEntity, Function<E, T> pValueGetter, T pExpectedValue, Component pMessage) {
        T t = pValueGetter.apply(pEntity);
        if (!t.equals(pExpectedValue)) {
            throw this.assertionException(pEntity.blockPosition(), "test.error.entity_property_details", pEntity.getName(), pMessage, t, pExpectedValue);
        }
    }

    public void assertLivingEntityHasMobEffect(LivingEntity pEntity, Holder<MobEffect> pEffect, int pAmplifier) {
        MobEffectInstance mobeffectinstance = pEntity.getEffect(pEffect);
        if (mobeffectinstance == null || mobeffectinstance.getAmplifier() != pAmplifier) {
            throw this.assertionException("test.error.expected_entity_effect", pEntity.getName(), PotionContents.getPotionDescription(pEffect, pAmplifier));
        }
    }

    public void succeedWhenEntityPresent(EntityType<?> pType, int pX, int pY, int pZ) {
        this.succeedWhenEntityPresent(pType, new BlockPos(pX, pY, pZ));
    }

    public void succeedWhenEntityPresent(EntityType<?> pType, BlockPos pPos) {
        this.succeedWhen(() -> this.assertEntityPresent(pType, pPos));
    }

    public void succeedWhenEntityNotPresent(EntityType<?> pType, int pX, int pY, int pZ) {
        this.succeedWhenEntityNotPresent(pType, new BlockPos(pX, pY, pZ));
    }

    public void succeedWhenEntityNotPresent(EntityType<?> pType, BlockPos pPos) {
        this.succeedWhen(() -> this.assertEntityNotPresent(pType, pPos));
    }

    public void succeed() {
        this.testInfo.succeed();
    }

    private void ensureSingleFinalCheck() {
        if (this.finalCheckAdded) {
            throw new IllegalStateException("This test already has final clause");
        } else {
            this.finalCheckAdded = true;
        }
    }

    public void succeedIf(Runnable pCriterion) {
        this.ensureSingleFinalCheck();
        this.testInfo.createSequence().thenWaitUntil(0L, pCriterion).thenSucceed();
    }

    public void succeedWhen(Runnable pCriterion) {
        this.ensureSingleFinalCheck();
        this.testInfo.createSequence().thenWaitUntil(pCriterion).thenSucceed();
    }

    public void succeedOnTickWhen(int pTick, Runnable pCriterion) {
        this.ensureSingleFinalCheck();
        this.testInfo.createSequence().thenWaitUntil(pTick, pCriterion).thenSucceed();
    }

    public void runAtTickTime(long pTickTime, Runnable pTask) {
        this.testInfo.setRunAtTickTime(pTickTime, pTask);
    }

    public void runAfterDelay(long pDelay, Runnable pTask) {
        this.runAtTickTime(this.testInfo.getTick() + pDelay, pTask);
    }

    public void randomTick(BlockPos pPos) {
        BlockPos blockpos = this.absolutePos(pPos);
        ServerLevel serverlevel = this.getLevel();
        serverlevel.getBlockState(blockpos).randomTick(serverlevel, blockpos, serverlevel.random);
    }

    public void tickBlock(BlockPos pPos) {
        BlockPos blockpos = this.absolutePos(pPos);
        ServerLevel serverlevel = this.getLevel();
        serverlevel.getBlockState(blockpos).tick(serverlevel, blockpos, serverlevel.random);
    }

    public void tickPrecipitation(BlockPos pPos) {
        BlockPos blockpos = this.absolutePos(pPos);
        ServerLevel serverlevel = this.getLevel();
        serverlevel.tickPrecipitation(blockpos);
    }

    public void tickPrecipitation() {
        AABB aabb = this.getRelativeBounds();
        int i = (int)Math.floor(aabb.maxX);
        int j = (int)Math.floor(aabb.maxZ);
        int k = (int)Math.floor(aabb.maxY);

        for (int l = (int)Math.floor(aabb.minX); l < i; l++) {
            for (int i1 = (int)Math.floor(aabb.minZ); i1 < j; i1++) {
                this.tickPrecipitation(new BlockPos(l, k, i1));
            }
        }
    }

    public int getHeight(Heightmap.Types pHeightmapType, int pX, int pZ) {
        BlockPos blockpos = this.absolutePos(new BlockPos(pX, 0, pZ));
        return this.relativePos(this.getLevel().getHeightmapPos(pHeightmapType, blockpos)).getY();
    }

    public void fail(Component pMessage, BlockPos pPos) {
        throw this.assertionException(pPos, pMessage);
    }

    public void fail(Component pMessage, Entity pEntity) {
        throw this.assertionException(pEntity.blockPosition(), pMessage);
    }

    public void fail(Component pMessage) {
        throw this.assertionException(pMessage);
    }

    public void failIf(Runnable pCriterion) {
        this.testInfo.createSequence().thenWaitUntil(pCriterion).thenFail(() -> this.assertionException("test.error.fail"));
    }

    public void failIfEver(Runnable pCriterion) {
        LongStream.range(this.testInfo.getTick(), this.testInfo.getTimeoutTicks()).forEach(p_177365_ -> this.testInfo.setRunAtTickTime(p_177365_, pCriterion::run));
    }

    public GameTestSequence startSequence() {
        return this.testInfo.createSequence();
    }

    public BlockPos absolutePos(BlockPos pPos) {
        BlockPos blockpos = this.testInfo.getTestOrigin();
        BlockPos blockpos1 = blockpos.offset(pPos);
        return StructureTemplate.transform(blockpos1, Mirror.NONE, this.testInfo.getRotation(), blockpos);
    }

    public BlockPos relativePos(BlockPos pPos) {
        BlockPos blockpos = this.testInfo.getTestOrigin();
        Rotation rotation = this.testInfo.getRotation().getRotated(Rotation.CLOCKWISE_180);
        BlockPos blockpos1 = StructureTemplate.transform(pPos, Mirror.NONE, rotation, blockpos);
        return blockpos1.subtract(blockpos);
    }

    public AABB absoluteAABB(AABB pAabb) {
        Vec3 vec3 = this.absoluteVec(pAabb.getMinPosition());
        Vec3 vec31 = this.absoluteVec(pAabb.getMaxPosition());
        return new AABB(vec3, vec31);
    }

    public AABB relativeAABB(AABB pAabb) {
        Vec3 vec3 = this.relativeVec(pAabb.getMinPosition());
        Vec3 vec31 = this.relativeVec(pAabb.getMaxPosition());
        return new AABB(vec3, vec31);
    }

    public Vec3 absoluteVec(Vec3 pRelativeVec3) {
        Vec3 vec3 = Vec3.atLowerCornerOf(this.testInfo.getTestOrigin());
        return StructureTemplate.transform(vec3.add(pRelativeVec3), Mirror.NONE, this.testInfo.getRotation(), this.testInfo.getTestOrigin());
    }

    public Vec3 relativeVec(Vec3 pAbsoluteVec3) {
        Vec3 vec3 = Vec3.atLowerCornerOf(this.testInfo.getTestOrigin());
        return StructureTemplate.transform(pAbsoluteVec3.subtract(vec3), Mirror.NONE, this.testInfo.getRotation(), this.testInfo.getTestOrigin());
    }

    public Rotation getTestRotation() {
        return this.testInfo.getRotation();
    }

    public void assertTrue(boolean pCondition, Component pMessage) {
        if (!pCondition) {
            throw this.assertionException(pMessage);
        }
    }

    public <N> void assertValueEqual(N pExpected, N pActual, Component pName) {
        if (!pExpected.equals(pActual)) {
            throw this.assertionException("test.error.value_not_equal", pName, pExpected, pActual);
        }
    }

    public void assertFalse(boolean pCondition, Component pMessage) {
        this.assertTrue(!pCondition, pMessage);
    }

    public long getTick() {
        return this.testInfo.getTick();
    }

    /** Forge: same as {@link #getTick()} without implicit cast to long */
    @Override
    public int getTickAsInt() {
        return this.testInfo.getTick();
    }

    public AABB getBounds() {
        return this.testInfo.getStructureBounds();
    }

    private AABB getRelativeBounds() {
        AABB aabb = this.testInfo.getStructureBounds();
        Rotation rotation = this.testInfo.getRotation();
        switch (rotation) {
            case COUNTERCLOCKWISE_90:
            case CLOCKWISE_90:
                return new AABB(0.0, 0.0, 0.0, aabb.getZsize(), aabb.getYsize(), aabb.getXsize());
            default:
                return new AABB(0.0, 0.0, 0.0, aabb.getXsize(), aabb.getYsize(), aabb.getZsize());
        }
    }

    public void forEveryBlockInStructure(Consumer<BlockPos> pConsumer) {
        AABB aabb = this.getRelativeBounds().contract(1.0, 1.0, 1.0);
        BlockPos.MutableBlockPos.betweenClosedStream(aabb).forEach(pConsumer);
    }

    public void onEachTick(Runnable pTask) {
        LongStream.range(this.testInfo.getTick(), this.testInfo.getTimeoutTicks()).forEach(p_177283_ -> this.testInfo.setRunAtTickTime(p_177283_, pTask::run));
    }

    public void placeAt(Player pPlayer, ItemStack pStack, BlockPos pPos, Direction pDirection) {
        BlockPos blockpos = this.absolutePos(pPos.relative(pDirection));
        BlockHitResult blockhitresult = new BlockHitResult(Vec3.atCenterOf(blockpos), pDirection, blockpos, false);
        UseOnContext useoncontext = new UseOnContext(pPlayer, InteractionHand.MAIN_HAND, blockhitresult);
        pStack.useOn(useoncontext);
    }

    public void setBiome(ResourceKey<Biome> pBiome) {
        AABB aabb = this.getBounds();
        BlockPos blockpos = BlockPos.containing(aabb.minX, aabb.minY, aabb.minZ);
        BlockPos blockpos1 = BlockPos.containing(aabb.maxX, aabb.maxY, aabb.maxZ);
        Either<Integer, CommandSyntaxException> either = FillBiomeCommand.fill(
            this.getLevel(), blockpos, blockpos1, this.getLevel().registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(pBiome)
        );
        if (either.right().isPresent()) {
            throw this.assertionException("test.error.set_biome");
        }
    }

    /**
     * Adds a cleanup handler that will be called when the test is done, pass or fail.
     */
    public void addCleanup(Consumer<Boolean> handler) {
        this.testInfo.addListener(new GameTestListener() {
            @Override
            public void testPassed(GameTestInfo info, GameTestRunner runner) {
                handler.accept(true);
            }

            @Override
            public void testFailed(GameTestInfo info, GameTestRunner runner) {
                handler.accept(false);
            }

            @Override public void testStructureLoaded(GameTestInfo info) { }
            @Override public void testAddedForRerun(GameTestInfo oldInfo, GameTestInfo newInfo, GameTestRunner runner) { }
        });
    }
}

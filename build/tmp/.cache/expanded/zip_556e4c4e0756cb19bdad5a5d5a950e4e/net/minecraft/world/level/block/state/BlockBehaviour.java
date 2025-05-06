package net.minecraft.world.level.block.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.resources.DependantName;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.flag.FeatureElement;
import net.minecraft.world.flag.FeatureFlag;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class BlockBehaviour implements FeatureElement {
    protected static final Direction[] UPDATE_SHAPE_ORDER = new Direction[]{
        Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH, Direction.DOWN, Direction.UP
    };
    protected final boolean hasCollision;
    protected final float explosionResistance;
    protected final boolean isRandomlyTicking;
    protected final SoundType soundType;
    protected final float friction;
    protected final float speedFactor;
    protected final float jumpFactor;
    protected final boolean dynamicShape;
    protected final FeatureFlagSet requiredFeatures;
    protected final BlockBehaviour.Properties properties;
    protected final Optional<ResourceKey<LootTable>> drops;
    protected final String descriptionId;

    public BlockBehaviour(BlockBehaviour.Properties pProperties) {
        this.hasCollision = pProperties.hasCollision;
        this.drops = pProperties.effectiveDrops();
        this.descriptionId = pProperties.effectiveDescriptionId();
        this.explosionResistance = pProperties.explosionResistance;
        this.isRandomlyTicking = pProperties.isRandomlyTicking;
        this.soundType = pProperties.soundType;
        this.friction = pProperties.friction;
        this.speedFactor = pProperties.speedFactor;
        this.jumpFactor = pProperties.jumpFactor;
        this.dynamicShape = pProperties.dynamicShape;
        this.requiredFeatures = pProperties.requiredFeatures;
        this.properties = pProperties;
    }

    public BlockBehaviour.Properties properties() {
        return this.properties;
    }

    protected abstract MapCodec<? extends Block> codec();

    protected static <B extends Block> RecordCodecBuilder<B, BlockBehaviour.Properties> propertiesCodec() {
        return BlockBehaviour.Properties.CODEC.fieldOf("properties").forGetter(BlockBehaviour::properties);
    }

    public static <B extends Block> MapCodec<B> simpleCodec(Function<BlockBehaviour.Properties, B> pFactory) {
        return RecordCodecBuilder.mapCodec(p_309873_ -> p_309873_.group(propertiesCodec()).apply(p_309873_, pFactory));
    }

    protected void updateIndirectNeighbourShapes(BlockState pState, LevelAccessor pLevel, BlockPos pPos, int pFlags, int pRecursionLeft) {
    }

    protected boolean isPathfindable(BlockState pState, PathComputationType pPathComputationType) {
        switch (pPathComputationType) {
            case LAND:
                return !pState.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            case WATER:
                return pState.getFluidState().is(FluidTags.WATER);
            case AIR:
                return !pState.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            default:
                return false;
        }
    }

    protected BlockState updateShape(
        BlockState pState,
        LevelReader pLevel,
        ScheduledTickAccess pScheduledTickAccess,
        BlockPos pPos,
        Direction pDirection,
        BlockPos pNeighborPos,
        BlockState pNeighborState,
        RandomSource pRandom
    ) {
        return pState;
    }

    protected boolean skipRendering(BlockState pState, BlockState pAdjacentState, Direction pDirection) {
        return false;
    }

    protected void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pNeighborBlock, @Nullable Orientation pOrientation, boolean pMovedByPiston) {
    }

    protected void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pMovedByPiston) {
    }

    protected void affectNeighborsAfterRemoval(BlockState pState, ServerLevel pLevel, BlockPos pPos, boolean pMovedByPiston) {
    }

    protected void onExplosionHit(BlockState pState, ServerLevel pLevel, BlockPos pPos, Explosion pExplosion, BiConsumer<ItemStack, BlockPos> pDropConsumer) {
        if (!pState.isAir() && pExplosion.getBlockInteraction() != Explosion.BlockInteraction.TRIGGER_BLOCK) {
            Block block = pState.getBlock();
            boolean flag = pExplosion.getIndirectSourceEntity() instanceof Player;
            if (pState.canDropFromExplosion(pLevel, pPos, pExplosion)) {
                BlockEntity blockentity = pState.hasBlockEntity() ? pLevel.getBlockEntity(pPos) : null;
                LootParams.Builder lootparams$builder = new LootParams.Builder(pLevel)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pPos))
                    .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                    .withOptionalParameter(LootContextParams.BLOCK_ENTITY, blockentity)
                    .withOptionalParameter(LootContextParams.THIS_ENTITY, pExplosion.getDirectSourceEntity());
                if (pExplosion.getBlockInteraction() == Explosion.BlockInteraction.DESTROY_WITH_DECAY) {
                    lootparams$builder.withParameter(LootContextParams.EXPLOSION_RADIUS, pExplosion.radius());
                }

                pState.spawnAfterBreak(pLevel, pPos, ItemStack.EMPTY, flag);
                pState.getDrops(lootparams$builder).forEach(p_309419_ -> pDropConsumer.accept(p_309419_, pPos));
            }

            pState.onBlockExploded(pLevel, pPos, pExplosion);
        }
    }

    protected InteractionResult useWithoutItem(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, BlockHitResult pHitResult) {
        return InteractionResult.PASS;
    }

    protected InteractionResult useItemOn(
        ItemStack pStack, BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHitResult
    ) {
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    protected boolean triggerEvent(BlockState pState, Level pLevel, BlockPos pPos, int pId, int pParam) {
        return false;
    }

    protected RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    protected boolean useShapeForLightOcclusion(BlockState pState) {
        return false;
    }

    protected boolean isSignalSource(BlockState pState) {
        return false;
    }

    protected FluidState getFluidState(BlockState pState) {
        return Fluids.EMPTY.defaultFluidState();
    }

    protected boolean hasAnalogOutputSignal(BlockState pState) {
        return false;
    }

    protected float getMaxHorizontalOffset() {
        return 0.25F;
    }

    protected float getMaxVerticalOffset() {
        return 0.2F;
    }

    @Override
    public FeatureFlagSet requiredFeatures() {
        return this.requiredFeatures;
    }

    protected BlockState rotate(BlockState pState, Rotation pRotation) {
        return pState;
    }

    protected BlockState mirror(BlockState pState, Mirror pMirror) {
        return pState;
    }

    protected boolean canBeReplaced(BlockState pState, BlockPlaceContext pUseContext) {
        return pState.canBeReplaced() && (pUseContext.getItemInHand().isEmpty() || !pUseContext.getItemInHand().is(this.asItem()));
    }

    protected boolean canBeReplaced(BlockState pState, Fluid pFluid) {
        return pState.canBeReplaced() || !pState.isSolid();
    }

    protected List<ItemStack> getDrops(BlockState pState, LootParams.Builder pParams) {
        if (this.drops.isEmpty()) {
            return Collections.emptyList();
        } else {
            LootParams lootparams = pParams.withParameter(LootContextParams.BLOCK_STATE, pState).create(LootContextParamSets.BLOCK);
            ServerLevel serverlevel = lootparams.getLevel();
            LootTable loottable = serverlevel.getServer().reloadableRegistries().getLootTable(this.drops.get());
            return loottable.getRandomItems(lootparams);
        }
    }

    protected long getSeed(BlockState pState, BlockPos pPos) {
        return Mth.getSeed(pPos);
    }

    protected VoxelShape getOcclusionShape(BlockState pState) {
        return pState.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
    }

    protected VoxelShape getBlockSupportShape(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return this.getCollisionShape(pState, pLevel, pPos, CollisionContext.empty());
    }

    protected VoxelShape getInteractionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return Shapes.empty();
    }

    protected int getLightBlock(BlockState pState) {
        if (pState.isSolidRender()) {
            return 15;
        } else {
            return pState.propagatesSkylightDown() ? 0 : 1;
        }
    }

    @Nullable
    protected MenuProvider getMenuProvider(BlockState pState, Level pLevel, BlockPos pPos) {
        return null;
    }

    protected boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        return true;
    }

    protected float getShadeBrightness(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return pState.isCollisionShapeFullBlock(pLevel, pPos) ? 0.2F : 1.0F;
    }

    protected int getAnalogOutputSignal(BlockState pState, Level pLevel, BlockPos pPos) {
        return 0;
    }

    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return Shapes.block();
    }

    protected VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return this.hasCollision ? pState.getShape(pLevel, pPos) : Shapes.empty();
    }

    protected VoxelShape getEntityInsideCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, Entity pEntity) {
        return Shapes.block();
    }

    protected boolean isCollisionShapeFullBlock(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return Block.isShapeFullBlock(pState.getCollisionShape(pLevel, pPos));
    }

    protected VoxelShape getVisualShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return this.getCollisionShape(pState, pLevel, pPos, pContext);
    }

    protected void randomTick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
    }

    protected void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
    }

    protected float getDestroyProgress(BlockState pState, Player pPlayer, BlockGetter pLevel, BlockPos pPos) {
        float f = pState.getDestroySpeed(pLevel, pPos);
        if (f == -1.0F) {
            return 0.0F;
        } else {
            int i = net.minecraftforge.common.ForgeHooks.isCorrectToolForDrops(pState, pPlayer) ? 30 : 100;
            return pPlayer.getDestroySpeed(pState, pPos) / f / (float)i;
        }
    }

    protected void spawnAfterBreak(BlockState pState, ServerLevel pLevel, BlockPos pPos, ItemStack pStack, boolean pDropExperience) {
        if (pDropExperience) {
            net.minecraftforge.common.ForgeHooks.dropXpForBlock(pState, pLevel, pPos, pStack);
        }
    }

    protected void attack(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer) {
    }

    protected int getSignal(BlockState pState, BlockGetter pLevel, BlockPos pPos, Direction pDirection) {
        return 0;
    }

    protected void entityInside(BlockState pState, Level pLevel, BlockPos pPos, Entity pEntity, InsideBlockEffectApplier pEffectApplier) {
    }

    protected int getDirectSignal(BlockState pState, BlockGetter pLevel, BlockPos pPos, Direction pDirection) {
        return 0;
    }

    public final Optional<ResourceKey<LootTable>> getLootTable() {
        return this.drops;
    }

    public final String getDescriptionId() {
        return this.descriptionId;
    }

    protected void onProjectileHit(Level pLevel, BlockState pState, BlockHitResult pHit, Projectile pProjectile) {
    }

    protected boolean propagatesSkylightDown(BlockState pState) {
        return !Block.isShapeFullBlock(pState.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) && pState.getFluidState().isEmpty();
    }

    protected boolean isRandomlyTicking(BlockState pState) {
        return this.isRandomlyTicking;
    }

    /** @deprecated Forge: Use more sensitive version {@link net.minecraftforge.common.extensions.IForgeBlockState#getSoundType(LevelReader, BlockPos, Entity) } */
    @Deprecated
    protected SoundType getSoundType(BlockState pState) {
        return this.soundType;
    }

    protected ItemStack getCloneItemStack(LevelReader pLevel, BlockPos pPos, BlockState pState, boolean pIncludeData) {
        return new ItemStack(this.asItem());
    }

    public abstract Item asItem();

    protected abstract Block asBlock();

    public MapColor defaultMapColor() {
        return this.properties.mapColor.apply(this.asBlock().defaultBlockState());
    }

    public float defaultDestroyTime() {
        return this.properties.destroyTime;
    }

    protected boolean isAir(BlockState state) {
       return ((BlockStateBase)state).isAir;
    }

    public abstract static class BlockStateBase extends StateHolder<Block, BlockState> {
        private static final Direction[] DIRECTIONS = Direction.values();
        private static final VoxelShape[] EMPTY_OCCLUSION_SHAPES = Util.make(new VoxelShape[DIRECTIONS.length], p_368402_ -> Arrays.fill(p_368402_, Shapes.empty()));
        private static final VoxelShape[] FULL_BLOCK_OCCLUSION_SHAPES = Util.make(new VoxelShape[DIRECTIONS.length], p_362279_ -> Arrays.fill(p_362279_, Shapes.block()));
        private final int lightEmission;
        private final boolean useShapeForLightOcclusion;
        private final boolean isAir;
        private final boolean ignitedByLava;
        @Deprecated
        private final boolean liquid;
        @Deprecated
        private boolean legacySolid;
        private final PushReaction pushReaction;
        private final MapColor mapColor;
        private final float destroySpeed;
        private final boolean requiresCorrectToolForDrops;
        private final boolean canOcclude;
        private final BlockBehaviour.StatePredicate isRedstoneConductor;
        private final BlockBehaviour.StatePredicate isSuffocating;
        private final BlockBehaviour.StatePredicate isViewBlocking;
        private final BlockBehaviour.StatePredicate hasPostProcess;
        private final BlockBehaviour.StatePredicate emissiveRendering;
        @Nullable
        private final BlockBehaviour.OffsetFunction offsetFunction;
        private final boolean spawnTerrainParticles;
        private final NoteBlockInstrument instrument;
        private final boolean replaceable;
        @Nullable
        private BlockBehaviour.BlockStateBase.Cache cache;
        private FluidState fluidState = Fluids.EMPTY.defaultFluidState();
        private boolean isRandomlyTicking;
        private boolean solidRender;
        private VoxelShape occlusionShape;
        private VoxelShape[] occlusionShapesByFace;
        private boolean propagatesSkylightDown;
        private int lightBlock;

        protected BlockStateBase(Block pOwner, Reference2ObjectArrayMap<Property<?>, Comparable<?>> pValues, MapCodec<BlockState> pPropertiesCodec) {
            super(pOwner, pValues, pPropertiesCodec);
            BlockBehaviour.Properties blockbehaviour$properties = pOwner.properties;
            this.lightEmission = blockbehaviour$properties.lightEmission.applyAsInt(this.asState());
            this.useShapeForLightOcclusion = pOwner.useShapeForLightOcclusion(this.asState());
            this.isAir = blockbehaviour$properties.isAir;
            this.ignitedByLava = blockbehaviour$properties.ignitedByLava;
            this.liquid = blockbehaviour$properties.liquid;
            this.pushReaction = blockbehaviour$properties.pushReaction;
            this.mapColor = blockbehaviour$properties.mapColor.apply(this.asState());
            this.destroySpeed = blockbehaviour$properties.destroyTime;
            this.requiresCorrectToolForDrops = blockbehaviour$properties.requiresCorrectToolForDrops;
            this.canOcclude = blockbehaviour$properties.canOcclude;
            this.isRedstoneConductor = blockbehaviour$properties.isRedstoneConductor;
            this.isSuffocating = blockbehaviour$properties.isSuffocating;
            this.isViewBlocking = blockbehaviour$properties.isViewBlocking;
            this.hasPostProcess = blockbehaviour$properties.hasPostProcess;
            this.emissiveRendering = blockbehaviour$properties.emissiveRendering;
            this.offsetFunction = blockbehaviour$properties.offsetFunction;
            this.spawnTerrainParticles = blockbehaviour$properties.spawnTerrainParticles;
            this.instrument = blockbehaviour$properties.instrument;
            this.replaceable = blockbehaviour$properties.replaceable;
        }

        private boolean calculateSolid() {
            if (this.owner.properties.forceSolidOn) {
                return true;
            } else if (this.owner.properties.forceSolidOff) {
                return false;
            } else if (this.cache == null) {
                return false;
            } else {
                VoxelShape voxelshape = this.cache.collisionShape;
                if (voxelshape.isEmpty()) {
                    return false;
                } else {
                    AABB aabb = voxelshape.bounds();
                    return aabb.getSize() >= 0.7291666666666666 ? true : aabb.getYsize() >= 1.0;
                }
            }
        }

        public void initCache() {
            this.fluidState = this.owner.getFluidState(this.asState());
            this.isRandomlyTicking = this.owner.isRandomlyTicking(this.asState());
            if (!this.getBlock().hasDynamicShape()) {
                this.cache = new BlockBehaviour.BlockStateBase.Cache(this.asState());
            }

            this.legacySolid = this.calculateSolid();
            this.occlusionShape = this.canOcclude ? this.owner.getOcclusionShape(this.asState()) : Shapes.empty();
            this.solidRender = Block.isShapeFullBlock(this.occlusionShape);
            if (this.occlusionShape.isEmpty()) {
                this.occlusionShapesByFace = EMPTY_OCCLUSION_SHAPES;
            } else if (this.solidRender) {
                this.occlusionShapesByFace = FULL_BLOCK_OCCLUSION_SHAPES;
            } else {
                this.occlusionShapesByFace = new VoxelShape[DIRECTIONS.length];

                for (Direction direction : DIRECTIONS) {
                    this.occlusionShapesByFace[direction.ordinal()] = this.occlusionShape.getFaceShape(direction);
                }
            }

            this.propagatesSkylightDown = this.owner.propagatesSkylightDown(this.asState());
            this.lightBlock = this.owner.getLightBlock(this.asState());
        }

        public Block getBlock() {
            return this.owner;
        }

        public Holder<Block> getBlockHolder() {
            return this.owner.builtInRegistryHolder();
        }

        @Deprecated
        public boolean blocksMotion() {
            Block block = this.getBlock();
            return block != Blocks.COBWEB && block != Blocks.BAMBOO_SAPLING && this.isSolid();
        }

        @Deprecated
        public boolean isSolid() {
            return this.legacySolid;
        }

        public boolean isValidSpawn(BlockGetter pLevel, BlockPos pPos, EntityType<?> pEntityType) {
            return this.getBlock().properties.isValidSpawn.test(this.asState(), pLevel, pPos, pEntityType);
        }

        public boolean propagatesSkylightDown() {
            return this.propagatesSkylightDown;
        }

        public int getLightBlock() {
            return this.lightBlock;
        }

        public VoxelShape getFaceOcclusionShape(Direction pFace) {
            return this.occlusionShapesByFace[pFace.ordinal()];
        }

        public VoxelShape getOcclusionShape() {
            return this.occlusionShape;
        }

        public boolean hasLargeCollisionShape() {
            return this.cache == null || this.cache.largeCollisionShape;
        }

        public boolean useShapeForLightOcclusion() {
            return this.useShapeForLightOcclusion;
        }

        /** @deprecated Forge: Use {@link BlockState#getLightEmission(BlockGetter, BlockPos)} instead */
        @Deprecated
        public int getLightEmission() {
            return this.lightEmission;
        }

        public boolean isAir() {
            return this.getBlock().isAir((BlockState)this);
        }

        public boolean ignitedByLava() {
            return this.ignitedByLava;
        }

        @Deprecated
        public boolean liquid() {
            return this.liquid;
        }

        public MapColor getMapColor(BlockGetter pLevel, BlockPos pPos) {
            return getBlock().getMapColor(this.asState(), pLevel, pPos, this.mapColor);
        }

        /** @deprecated use {@link BlockState#rotate(LevelAccessor, BlockPos, Rotation)} */
        @Deprecated
        public BlockState rotate(Rotation pRotation) {
            return this.getBlock().rotate(this.asState(), pRotation);
        }

        public BlockState mirror(Mirror pMirror) {
            return this.getBlock().mirror(this.asState(), pMirror);
        }

        public RenderShape getRenderShape() {
            return this.getBlock().getRenderShape(this.asState());
        }

        public boolean emissiveRendering(BlockGetter pLevel, BlockPos pPos) {
            return this.emissiveRendering.test(this.asState(), pLevel, pPos);
        }

        public float getShadeBrightness(BlockGetter pLevel, BlockPos pPos) {
            return this.getBlock().getShadeBrightness(this.asState(), pLevel, pPos);
        }

        public boolean isRedstoneConductor(BlockGetter pLevel, BlockPos pPos) {
            return this.isRedstoneConductor.test(this.asState(), pLevel, pPos);
        }

        public boolean isSignalSource() {
            return this.getBlock().isSignalSource(this.asState());
        }

        public int getSignal(BlockGetter pLevel, BlockPos pPos, Direction pDirection) {
            return this.getBlock().getSignal(this.asState(), pLevel, pPos, pDirection);
        }

        public boolean hasAnalogOutputSignal() {
            return this.getBlock().hasAnalogOutputSignal(this.asState());
        }

        public int getAnalogOutputSignal(Level pLevel, BlockPos pPos) {
            return this.getBlock().getAnalogOutputSignal(this.asState(), pLevel, pPos);
        }

        public float getDestroySpeed(BlockGetter pLevel, BlockPos pPos) {
            return this.destroySpeed;
        }

        public float getDestroyProgress(Player pPlayer, BlockGetter pLevel, BlockPos pPos) {
            return this.getBlock().getDestroyProgress(this.asState(), pPlayer, pLevel, pPos);
        }

        public int getDirectSignal(BlockGetter pLevel, BlockPos pPos, Direction pDirection) {
            return this.getBlock().getDirectSignal(this.asState(), pLevel, pPos, pDirection);
        }

        public PushReaction getPistonPushReaction() {
            PushReaction reaction = getBlock().getPistonPushReaction(asState());
            if (reaction != null) return reaction;
            return this.pushReaction;
        }

        public boolean isSolidRender() {
            return this.solidRender;
        }

        public boolean canOcclude() {
            return this.canOcclude;
        }

        public boolean skipRendering(BlockState pState, Direction pFace) {
            return this.getBlock().skipRendering(this.asState(), pState, pFace);
        }

        public VoxelShape getShape(BlockGetter pLevel, BlockPos pPos) {
            return this.getShape(pLevel, pPos, CollisionContext.empty());
        }

        public VoxelShape getShape(BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
            return this.getBlock().getShape(this.asState(), pLevel, pPos, pContext);
        }

        public VoxelShape getCollisionShape(BlockGetter pLevel, BlockPos pPos) {
            return this.cache != null ? this.cache.collisionShape : this.getCollisionShape(pLevel, pPos, CollisionContext.empty());
        }

        public VoxelShape getCollisionShape(BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
            return this.getBlock().getCollisionShape(this.asState(), pLevel, pPos, pContext);
        }

        public VoxelShape getEntityInsideCollisionShape(BlockGetter pLevel, BlockPos pPos, Entity pEntity) {
            return this.getBlock().getEntityInsideCollisionShape(this.asState(), pLevel, pPos, pEntity);
        }

        public VoxelShape getBlockSupportShape(BlockGetter pLevel, BlockPos pPos) {
            return this.getBlock().getBlockSupportShape(this.asState(), pLevel, pPos);
        }

        public VoxelShape getVisualShape(BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
            return this.getBlock().getVisualShape(this.asState(), pLevel, pPos, pContext);
        }

        public VoxelShape getInteractionShape(BlockGetter pLevel, BlockPos pPos) {
            return this.getBlock().getInteractionShape(this.asState(), pLevel, pPos);
        }

        public final boolean entityCanStandOn(BlockGetter pLevel, BlockPos pPos, Entity pEntity) {
            return this.entityCanStandOnFace(pLevel, pPos, pEntity, Direction.UP);
        }

        public final boolean entityCanStandOnFace(BlockGetter pLevel, BlockPos pPos, Entity pEntity, Direction pFace) {
            return Block.isFaceFull(this.getCollisionShape(pLevel, pPos, CollisionContext.of(pEntity)), pFace);
        }

        public Vec3 getOffset(BlockPos pPos) {
            BlockBehaviour.OffsetFunction blockbehaviour$offsetfunction = this.offsetFunction;
            return blockbehaviour$offsetfunction != null ? blockbehaviour$offsetfunction.evaluate(this.asState(), pPos) : Vec3.ZERO;
        }

        public boolean hasOffsetFunction() {
            return this.offsetFunction != null;
        }

        public boolean triggerEvent(Level pLevel, BlockPos pPos, int pId, int pParam) {
            return this.getBlock().triggerEvent(this.asState(), pLevel, pPos, pId, pParam);
        }

        public void handleNeighborChanged(Level pLevel, BlockPos pPos, Block pNeighborBlock, @Nullable Orientation pOrientation, boolean pMovedByPiston) {
            DebugPackets.sendNeighborsUpdatePacket(pLevel, pPos);
            this.getBlock().neighborChanged(this.asState(), pLevel, pPos, pNeighborBlock, pOrientation, pMovedByPiston);
        }

        public final void updateNeighbourShapes(LevelAccessor pLevel, BlockPos pPos, int pFlags) {
            this.updateNeighbourShapes(pLevel, pPos, pFlags, 512);
        }

        public final void updateNeighbourShapes(LevelAccessor pLevel, BlockPos pPos, int pFlags, int pRecursionLeft) {
            BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

            for (Direction direction : BlockBehaviour.UPDATE_SHAPE_ORDER) {
                blockpos$mutableblockpos.setWithOffset(pPos, direction);
                pLevel.neighborShapeChanged(direction.getOpposite(), blockpos$mutableblockpos, pPos, this.asState(), pFlags, pRecursionLeft);
            }
        }

        public final void updateIndirectNeighbourShapes(LevelAccessor pLevel, BlockPos pPos, int pFlags) {
            this.updateIndirectNeighbourShapes(pLevel, pPos, pFlags, 512);
        }

        public void updateIndirectNeighbourShapes(LevelAccessor pLevel, BlockPos pPos, int pFlags, int pRecursionLeft) {
            this.getBlock().updateIndirectNeighbourShapes(this.asState(), pLevel, pPos, pFlags, pRecursionLeft);
        }

        public void onPlace(Level pLevel, BlockPos pPos, BlockState pOldState, boolean pMovedByPiston) {
            this.getBlock().onPlace(this.asState(), pLevel, pPos, pOldState, pMovedByPiston);
        }

        public void affectNeighborsAfterRemoval(ServerLevel pLevel, BlockPos pPos, boolean pMovedByPiston) {
            this.getBlock().affectNeighborsAfterRemoval(this.asState(), pLevel, pPos, pMovedByPiston);
        }

        public void onExplosionHit(ServerLevel pLevel, BlockPos pPos, Explosion pExplosion, BiConsumer<ItemStack, BlockPos> pDropConsumer) {
            this.getBlock().onExplosionHit(this.asState(), pLevel, pPos, pExplosion, pDropConsumer);
        }

        public void tick(ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
            this.getBlock().tick(this.asState(), pLevel, pPos, pRandom);
        }

        public void randomTick(ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
            this.getBlock().randomTick(this.asState(), pLevel, pPos, pRandom);
        }

        public void entityInside(Level pLevel, BlockPos pPos, Entity pEntity, InsideBlockEffectApplier pEffectApplier) {
            this.getBlock().entityInside(this.asState(), pLevel, pPos, pEntity, pEffectApplier);
        }

        public void spawnAfterBreak(ServerLevel pLevel, BlockPos pPos, ItemStack pStack, boolean pDropExperience) {
            this.getBlock().spawnAfterBreak(this.asState(), pLevel, pPos, pStack, pDropExperience);
        }

        public List<ItemStack> getDrops(LootParams.Builder pLootParams) {
            return this.getBlock().getDrops(this.asState(), pLootParams);
        }

        public InteractionResult useItemOn(ItemStack pStack, Level pLevel, Player pPlayer, InteractionHand pHand, BlockHitResult pHitResult) {
            return this.getBlock().useItemOn(pStack, this.asState(), pLevel, pHitResult.getBlockPos(), pPlayer, pHand, pHitResult);
        }

        public InteractionResult useWithoutItem(Level pLevel, Player pPlayer, BlockHitResult pHitResult) {
            return this.getBlock().useWithoutItem(this.asState(), pLevel, pHitResult.getBlockPos(), pPlayer, pHitResult);
        }

        public void attack(Level pLevel, BlockPos pPos, Player pPlayer) {
            this.getBlock().attack(this.asState(), pLevel, pPos, pPlayer);
        }

        public boolean isSuffocating(BlockGetter pLevel, BlockPos pPos) {
            return this.isSuffocating.test(this.asState(), pLevel, pPos);
        }

        public boolean isViewBlocking(BlockGetter pLevel, BlockPos pPos) {
            return this.isViewBlocking.test(this.asState(), pLevel, pPos);
        }

        public BlockState updateShape(
            LevelReader pLevel,
            ScheduledTickAccess pScheduledTickAccess,
            BlockPos pPos,
            Direction pDirection,
            BlockPos pNeighborPos,
            BlockState pNeighborState,
            RandomSource pRandom
        ) {
            return this.getBlock().updateShape(this.asState(), pLevel, pScheduledTickAccess, pPos, pDirection, pNeighborPos, pNeighborState, pRandom);
        }

        public boolean isPathfindable(PathComputationType pType) {
            return this.getBlock().isPathfindable(this.asState(), pType);
        }

        public boolean canBeReplaced(BlockPlaceContext pUseContext) {
            return this.getBlock().canBeReplaced(this.asState(), pUseContext);
        }

        public boolean canBeReplaced(Fluid pFluid) {
            return this.getBlock().canBeReplaced(this.asState(), pFluid);
        }

        public boolean canBeReplaced() {
            return this.replaceable;
        }

        public boolean canSurvive(LevelReader pLevel, BlockPos pPos) {
            return this.getBlock().canSurvive(this.asState(), pLevel, pPos);
        }

        public boolean hasPostProcess(BlockGetter pLevel, BlockPos pPos) {
            return this.hasPostProcess.test(this.asState(), pLevel, pPos);
        }

        @Nullable
        public MenuProvider getMenuProvider(Level pLevel, BlockPos pPos) {
            return this.getBlock().getMenuProvider(this.asState(), pLevel, pPos);
        }

        public boolean is(TagKey<Block> pTag) {
            return this.getBlock().builtInRegistryHolder().is(pTag);
        }

        public boolean is(TagKey<Block> pTag, Predicate<BlockBehaviour.BlockStateBase> pPredicate) {
            return this.is(pTag) && pPredicate.test(this);
        }

        public boolean is(HolderSet<Block> pHolder) {
            return pHolder.contains(this.getBlock().builtInRegistryHolder());
        }

        public boolean is(Holder<Block> pBlock) {
            return this.is(pBlock.value());
        }

        public Stream<TagKey<Block>> getTags() {
            return this.getBlock().builtInRegistryHolder().tags();
        }

        public boolean hasBlockEntity() {
            return this.getBlock() instanceof EntityBlock;
        }

        @Nullable
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockEntityType<T> pBlockEntityType) {
            return this.getBlock() instanceof EntityBlock ? ((EntityBlock)this.getBlock()).getTicker(pLevel, this.asState(), pBlockEntityType) : null;
        }

        public boolean is(Block pBlock) {
            return this.getBlock() == pBlock;
        }

        public boolean is(ResourceKey<Block> pBlock) {
            return this.getBlock().builtInRegistryHolder().is(pBlock);
        }

        public FluidState getFluidState() {
            return this.fluidState;
        }

        public boolean isRandomlyTicking() {
            return this.isRandomlyTicking;
        }

        public long getSeed(BlockPos pPos) {
            return this.getBlock().getSeed(this.asState(), pPos);
        }

        public SoundType getSoundType() {
            return this.getBlock().getSoundType(this.asState());
        }

        public void onProjectileHit(Level pLevel, BlockState pState, BlockHitResult pHit, Projectile pProjectile) {
            this.getBlock().onProjectileHit(pLevel, pState, pHit, pProjectile);
        }

        public boolean isFaceSturdy(BlockGetter pLevel, BlockPos pPos, Direction pDirection) {
            return this.isFaceSturdy(pLevel, pPos, pDirection, SupportType.FULL);
        }

        public boolean isFaceSturdy(BlockGetter pLevel, BlockPos pPos, Direction pFace, SupportType pSupportType) {
            return this.cache != null ? this.cache.isFaceSturdy(pFace, pSupportType) : pSupportType.isSupporting(this.asState(), pLevel, pPos, pFace);
        }

        public boolean isCollisionShapeFullBlock(BlockGetter pLevel, BlockPos pPos) {
            return this.cache != null ? this.cache.isCollisionShapeFullBlock : this.getBlock().isCollisionShapeFullBlock(this.asState(), pLevel, pPos);
        }

        public ItemStack getCloneItemStack(LevelReader pLevel, BlockPos pPos, boolean pIncludeData) {
            return this.getBlock().getCloneItemStack(pLevel, pPos, this.asState(), pIncludeData);
        }

        protected abstract BlockState asState();

        public boolean requiresCorrectToolForDrops() {
            return this.requiresCorrectToolForDrops;
        }

        public boolean shouldSpawnTerrainParticles() {
            return this.spawnTerrainParticles;
        }

        public NoteBlockInstrument instrument() {
            return this.instrument;
        }

        static final class Cache {
            private static final Direction[] DIRECTIONS = Direction.values();
            private static final int SUPPORT_TYPE_COUNT = SupportType.values().length;
            protected final VoxelShape collisionShape;
            protected final boolean largeCollisionShape;
            private final boolean[] faceSturdy;
            protected final boolean isCollisionShapeFullBlock;

            Cache(BlockState pState) {
                Block block = pState.getBlock();
                this.collisionShape = block.getCollisionShape(pState, EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty());
                if (!this.collisionShape.isEmpty() && pState.hasOffsetFunction()) {
                    throw new IllegalStateException(
                        String.format(
                            Locale.ROOT,
                            "%s has a collision shape and an offset type, but is not marked as dynamicShape in its properties.",
                            BuiltInRegistries.BLOCK.getKey(block)
                        )
                    );
                } else {
                    this.largeCollisionShape = Arrays.stream(Direction.Axis.values())
                        .anyMatch(p_60860_ -> this.collisionShape.min(p_60860_) < 0.0 || this.collisionShape.max(p_60860_) > 1.0);
                    this.faceSturdy = new boolean[DIRECTIONS.length * SUPPORT_TYPE_COUNT];

                    for (Direction direction : DIRECTIONS) {
                        for (SupportType supporttype : SupportType.values()) {
                            this.faceSturdy[getFaceSupportIndex(direction, supporttype)] = supporttype.isSupporting(
                                pState, EmptyBlockGetter.INSTANCE, BlockPos.ZERO, direction
                            );
                        }
                    }

                    this.isCollisionShapeFullBlock = Block.isShapeFullBlock(pState.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
                }
            }

            public boolean isFaceSturdy(Direction pDirection, SupportType pSupportType) {
                return this.faceSturdy[getFaceSupportIndex(pDirection, pSupportType)];
            }

            private static int getFaceSupportIndex(Direction pDirection, SupportType pSupportType) {
                return pDirection.ordinal() * SUPPORT_TYPE_COUNT + pSupportType.ordinal();
            }
        }
    }

    @FunctionalInterface
    public interface OffsetFunction {
        Vec3 evaluate(BlockState pState, BlockPos pPos);
    }

    public static enum OffsetType {
        NONE,
        XZ,
        XYZ;
    }

    public static class Properties {
        public static final Codec<BlockBehaviour.Properties> CODEC = Codec.unit(() -> of());
        Function<BlockState, MapColor> mapColor = p_284884_ -> MapColor.NONE;
        boolean hasCollision = true;
        SoundType soundType = SoundType.STONE;
        ToIntFunction<BlockState> lightEmission = p_60929_ -> 0;
        float explosionResistance;
        float destroyTime;
        boolean requiresCorrectToolForDrops;
        boolean isRandomlyTicking;
        float friction = 0.6F;
        float speedFactor = 1.0F;
        float jumpFactor = 1.0F;
        @Nullable
        private ResourceKey<Block> id;
        private DependantName<Block, Optional<ResourceKey<LootTable>>> drops = p_360538_ -> Optional.of(
            ResourceKey.create(Registries.LOOT_TABLE, p_360538_.location().withPrefix("blocks/"))
        );
        private DependantName<Block, String> descriptionId = p_360549_ -> Util.makeDescriptionId("block", p_360549_.location());
        boolean canOcclude = true;
        boolean isAir;
        boolean ignitedByLava;
        @Deprecated
        boolean liquid;
        @Deprecated
        boolean forceSolidOff;
        boolean forceSolidOn;
        PushReaction pushReaction = PushReaction.NORMAL;
        boolean spawnTerrainParticles = true;
        NoteBlockInstrument instrument = NoteBlockInstrument.HARP;
        boolean replaceable;
        BlockBehaviour.StateArgumentPredicate<EntityType<?>> isValidSpawn = (p_360542_, p_360543_, p_360544_, p_360545_) -> p_360542_.isFaceSturdy(
                p_360543_, p_360544_, Direction.UP
            )
            && p_360542_.getLightEmission(p_360543_, p_360544_) < 14;
        BlockBehaviour.StatePredicate isRedstoneConductor = (p_360546_, p_360547_, p_360548_) -> p_360546_.isCollisionShapeFullBlock(p_360547_, p_360548_);
        BlockBehaviour.StatePredicate isSuffocating = (p_360539_, p_360540_, p_360541_) -> p_360539_.blocksMotion() && p_360539_.isCollisionShapeFullBlock(p_360540_, p_360541_);
        BlockBehaviour.StatePredicate isViewBlocking = this.isSuffocating;
        BlockBehaviour.StatePredicate hasPostProcess = (p_60963_, p_60964_, p_60965_) -> false;
        BlockBehaviour.StatePredicate emissiveRendering = (p_60931_, p_60932_, p_60933_) -> false;
        boolean dynamicShape;
        FeatureFlagSet requiredFeatures = FeatureFlags.VANILLA_SET;
        @Nullable
        BlockBehaviour.OffsetFunction offsetFunction;

        private Properties() {
        }

        public static BlockBehaviour.Properties of() {
            return new BlockBehaviour.Properties();
        }

        public static BlockBehaviour.Properties ofFullCopy(BlockBehaviour pBlockBehaviour) {
            BlockBehaviour.Properties blockbehaviour$properties = ofLegacyCopy(pBlockBehaviour);
            BlockBehaviour.Properties blockbehaviour$properties1 = pBlockBehaviour.properties;
            blockbehaviour$properties.jumpFactor = blockbehaviour$properties1.jumpFactor;
            blockbehaviour$properties.isRedstoneConductor = blockbehaviour$properties1.isRedstoneConductor;
            blockbehaviour$properties.isValidSpawn = blockbehaviour$properties1.isValidSpawn;
            blockbehaviour$properties.hasPostProcess = blockbehaviour$properties1.hasPostProcess;
            blockbehaviour$properties.isSuffocating = blockbehaviour$properties1.isSuffocating;
            blockbehaviour$properties.isViewBlocking = blockbehaviour$properties1.isViewBlocking;
            blockbehaviour$properties.drops = blockbehaviour$properties1.drops;
            blockbehaviour$properties.descriptionId = blockbehaviour$properties1.descriptionId;
            return blockbehaviour$properties;
        }

        @Deprecated
        public static BlockBehaviour.Properties ofLegacyCopy(BlockBehaviour pBlockBehaviour) {
            BlockBehaviour.Properties blockbehaviour$properties = new BlockBehaviour.Properties();
            BlockBehaviour.Properties blockbehaviour$properties1 = pBlockBehaviour.properties;
            blockbehaviour$properties.destroyTime = blockbehaviour$properties1.destroyTime;
            blockbehaviour$properties.explosionResistance = blockbehaviour$properties1.explosionResistance;
            blockbehaviour$properties.hasCollision = blockbehaviour$properties1.hasCollision;
            blockbehaviour$properties.isRandomlyTicking = blockbehaviour$properties1.isRandomlyTicking;
            blockbehaviour$properties.lightEmission = blockbehaviour$properties1.lightEmission;
            blockbehaviour$properties.mapColor = blockbehaviour$properties1.mapColor;
            blockbehaviour$properties.soundType = blockbehaviour$properties1.soundType;
            blockbehaviour$properties.friction = blockbehaviour$properties1.friction;
            blockbehaviour$properties.speedFactor = blockbehaviour$properties1.speedFactor;
            blockbehaviour$properties.dynamicShape = blockbehaviour$properties1.dynamicShape;
            blockbehaviour$properties.canOcclude = blockbehaviour$properties1.canOcclude;
            blockbehaviour$properties.isAir = blockbehaviour$properties1.isAir;
            blockbehaviour$properties.ignitedByLava = blockbehaviour$properties1.ignitedByLava;
            blockbehaviour$properties.liquid = blockbehaviour$properties1.liquid;
            blockbehaviour$properties.forceSolidOff = blockbehaviour$properties1.forceSolidOff;
            blockbehaviour$properties.forceSolidOn = blockbehaviour$properties1.forceSolidOn;
            blockbehaviour$properties.pushReaction = blockbehaviour$properties1.pushReaction;
            blockbehaviour$properties.requiresCorrectToolForDrops = blockbehaviour$properties1.requiresCorrectToolForDrops;
            blockbehaviour$properties.offsetFunction = blockbehaviour$properties1.offsetFunction;
            blockbehaviour$properties.spawnTerrainParticles = blockbehaviour$properties1.spawnTerrainParticles;
            blockbehaviour$properties.requiredFeatures = blockbehaviour$properties1.requiredFeatures;
            blockbehaviour$properties.emissiveRendering = blockbehaviour$properties1.emissiveRendering;
            blockbehaviour$properties.instrument = blockbehaviour$properties1.instrument;
            blockbehaviour$properties.replaceable = blockbehaviour$properties1.replaceable;
            return blockbehaviour$properties;
        }

        public BlockBehaviour.Properties mapColor(DyeColor pMapColor) {
            this.mapColor = p_284892_ -> pMapColor.getMapColor();
            return this;
        }

        public BlockBehaviour.Properties mapColor(MapColor pMapColor) {
            this.mapColor = p_222988_ -> pMapColor;
            return this;
        }

        public BlockBehaviour.Properties mapColor(Function<BlockState, MapColor> pMapColor) {
            this.mapColor = pMapColor;
            return this;
        }

        public BlockBehaviour.Properties noCollission() {
            this.hasCollision = false;
            this.canOcclude = false;
            return this;
        }

        public BlockBehaviour.Properties noOcclusion() {
            this.canOcclude = false;
            return this;
        }

        public BlockBehaviour.Properties friction(float pFriction) {
            this.friction = pFriction;
            return this;
        }

        public BlockBehaviour.Properties speedFactor(float pSpeedFactor) {
            this.speedFactor = pSpeedFactor;
            return this;
        }

        public BlockBehaviour.Properties jumpFactor(float pJumpFactor) {
            this.jumpFactor = pJumpFactor;
            return this;
        }

        public BlockBehaviour.Properties sound(SoundType pSoundType) {
            this.soundType = pSoundType;
            return this;
        }

        public BlockBehaviour.Properties lightLevel(ToIntFunction<BlockState> pLightEmission) {
            this.lightEmission = pLightEmission;
            return this;
        }

        public BlockBehaviour.Properties strength(float pDestroyTime, float pExplosionResistance) {
            return this.destroyTime(pDestroyTime).explosionResistance(pExplosionResistance);
        }

        public BlockBehaviour.Properties instabreak() {
            return this.strength(0.0F);
        }

        public BlockBehaviour.Properties strength(float pStrength) {
            this.strength(pStrength, pStrength);
            return this;
        }

        public BlockBehaviour.Properties randomTicks() {
            this.isRandomlyTicking = true;
            return this;
        }

        public BlockBehaviour.Properties dynamicShape() {
            this.dynamicShape = true;
            return this;
        }

        public BlockBehaviour.Properties noLootTable() {
            this.drops = DependantName.fixed(Optional.empty());
            return this;
        }

        public BlockBehaviour.Properties overrideLootTable(Optional<ResourceKey<LootTable>> pLootTable) {
            this.drops = DependantName.fixed(pLootTable);
            return this;
        }

        protected Optional<ResourceKey<LootTable>> effectiveDrops() {
            return this.drops.get(Objects.requireNonNull(this.id, "Block id not set"));
        }

        public BlockBehaviour.Properties ignitedByLava() {
            this.ignitedByLava = true;
            return this;
        }

        public BlockBehaviour.Properties liquid() {
            this.liquid = true;
            return this;
        }

        public BlockBehaviour.Properties forceSolidOn() {
            this.forceSolidOn = true;
            return this;
        }

        @Deprecated
        public BlockBehaviour.Properties forceSolidOff() {
            this.forceSolidOff = true;
            return this;
        }

        public BlockBehaviour.Properties pushReaction(PushReaction pPushReaction) {
            this.pushReaction = pPushReaction;
            return this;
        }

        public BlockBehaviour.Properties air() {
            this.isAir = true;
            return this;
        }

        public BlockBehaviour.Properties isValidSpawn(BlockBehaviour.StateArgumentPredicate<EntityType<?>> pIsValidSpawn) {
            this.isValidSpawn = pIsValidSpawn;
            return this;
        }

        public BlockBehaviour.Properties isRedstoneConductor(BlockBehaviour.StatePredicate pIsRedstoneConductor) {
            this.isRedstoneConductor = pIsRedstoneConductor;
            return this;
        }

        public BlockBehaviour.Properties isSuffocating(BlockBehaviour.StatePredicate pIsSuffocating) {
            this.isSuffocating = pIsSuffocating;
            return this;
        }

        public BlockBehaviour.Properties isViewBlocking(BlockBehaviour.StatePredicate pIsViewBlocking) {
            this.isViewBlocking = pIsViewBlocking;
            return this;
        }

        public BlockBehaviour.Properties hasPostProcess(BlockBehaviour.StatePredicate pHasPostProcess) {
            this.hasPostProcess = pHasPostProcess;
            return this;
        }

        public BlockBehaviour.Properties emissiveRendering(BlockBehaviour.StatePredicate pEmissiveRendering) {
            this.emissiveRendering = pEmissiveRendering;
            return this;
        }

        public BlockBehaviour.Properties requiresCorrectToolForDrops() {
            this.requiresCorrectToolForDrops = true;
            return this;
        }

        public BlockBehaviour.Properties destroyTime(float pDestroyTime) {
            this.destroyTime = pDestroyTime;
            return this;
        }

        public BlockBehaviour.Properties explosionResistance(float pExplosionResistance) {
            this.explosionResistance = Math.max(0.0F, pExplosionResistance);
            return this;
        }

        public BlockBehaviour.Properties offsetType(BlockBehaviour.OffsetType pOffsetType) {
            this.offsetFunction = switch (pOffsetType) {
                case NONE -> null;
                case XZ -> (p_272565_, p_272567_) -> {
                    Block block = p_272565_.getBlock();
                    long i = Mth.getSeed(p_272567_.getX(), 0, p_272567_.getZ());
                    float f = block.getMaxHorizontalOffset();
                    double d0 = Mth.clamp(((float)(i & 15L) / 15.0F - 0.5) * 0.5, -f, f);
                    double d1 = Mth.clamp(((float)(i >> 8 & 15L) / 15.0F - 0.5) * 0.5, -f, f);
                    return new Vec3(d0, 0.0, d1);
                };
                case XYZ -> (p_272562_, p_272564_) -> {
                    Block block = p_272562_.getBlock();
                    long i = Mth.getSeed(p_272564_.getX(), 0, p_272564_.getZ());
                    double d0 = ((float)(i >> 4 & 15L) / 15.0F - 1.0) * block.getMaxVerticalOffset();
                    float f = block.getMaxHorizontalOffset();
                    double d1 = Mth.clamp(((float)(i & 15L) / 15.0F - 0.5) * 0.5, -f, f);
                    double d2 = Mth.clamp(((float)(i >> 8 & 15L) / 15.0F - 0.5) * 0.5, -f, f);
                    return new Vec3(d1, d0, d2);
                };
            };
            return this;
        }

        public BlockBehaviour.Properties noTerrainParticles() {
            this.spawnTerrainParticles = false;
            return this;
        }

        public BlockBehaviour.Properties requiredFeatures(FeatureFlag... pRequiredFeatures) {
            this.requiredFeatures = FeatureFlags.REGISTRY.subset(pRequiredFeatures);
            return this;
        }

        public BlockBehaviour.Properties instrument(NoteBlockInstrument pInstrument) {
            this.instrument = pInstrument;
            return this;
        }

        public BlockBehaviour.Properties replaceable() {
            this.replaceable = true;
            return this;
        }

        public BlockBehaviour.Properties setId(ResourceKey<Block> pId) {
            this.id = pId;
            return this;
        }

        public BlockBehaviour.Properties overrideDescription(String pDescription) {
            this.descriptionId = DependantName.fixed(pDescription);
            return this;
        }

        protected String effectiveDescriptionId() {
            return this.descriptionId.get(Objects.requireNonNull(this.id, "Block id not set"));
        }
    }

    @FunctionalInterface
    public interface StateArgumentPredicate<A> {
        boolean test(BlockState pState, BlockGetter pLevel, BlockPos pPos, A pValue);
    }

    @FunctionalInterface
    public interface StatePredicate {
        boolean test(BlockState pState, BlockGetter pLevel, BlockPos pPos);
    }
}

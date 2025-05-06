package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FireBlock extends BaseFireBlock {
    public static final MapCodec<FireBlock> CODEC = simpleCodec(FireBlock::new);
    public static final int MAX_AGE = 15;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_15;
    public static final BooleanProperty NORTH = PipeBlock.NORTH;
    public static final BooleanProperty EAST = PipeBlock.EAST;
    public static final BooleanProperty SOUTH = PipeBlock.SOUTH;
    public static final BooleanProperty WEST = PipeBlock.WEST;
    public static final BooleanProperty UP = PipeBlock.UP;
    public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = PipeBlock.PROPERTY_BY_DIRECTION
        .entrySet()
        .stream()
        .filter(p_53467_ -> p_53467_.getKey() != Direction.DOWN)
        .collect(Util.toMap());
    private final Function<BlockState, VoxelShape> shapes;
    private static final int IGNITE_INSTANT = 60;
    private static final int IGNITE_EASY = 30;
    private static final int IGNITE_MEDIUM = 15;
    private static final int IGNITE_HARD = 5;
    private static final int BURN_INSTANT = 100;
    private static final int BURN_EASY = 60;
    private static final int BURN_MEDIUM = 20;
    private static final int BURN_HARD = 5;
    private final Object2IntMap<Block> igniteOdds = new Object2IntOpenHashMap<>();
    private final Object2IntMap<Block> burnOdds = new Object2IntOpenHashMap<>();

    @Override
    public MapCodec<FireBlock> codec() {
        return CODEC;
    }

    public FireBlock(BlockBehaviour.Properties p_53425_) {
        super(p_53425_, 1.0F);
        this.registerDefaultState(
            this.stateDefinition
                .any()
                .setValue(AGE, 0)
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(UP, false)
        );
        this.shapes = this.makeShapes();
    }

    private Function<BlockState, VoxelShape> makeShapes() {
        Map<Direction, VoxelShape> map = Shapes.rotateAll(Block.boxZ(16.0, 0.0, 1.0));
        return this.getShapeForEachState(p_390938_ -> {
            VoxelShape voxelshape = Shapes.empty();

            for (Entry<Direction, BooleanProperty> entry : PROPERTY_BY_DIRECTION.entrySet()) {
                if (p_390938_.getValue(entry.getValue())) {
                    voxelshape = Shapes.or(voxelshape, map.get(entry.getKey()));
                }
            }

            return voxelshape.isEmpty() ? SHAPE : voxelshape;
        }, new Property[]{AGE});
    }

    @Override
    protected BlockState updateShape(
        BlockState p_53458_,
        LevelReader p_362645_,
        ScheduledTickAccess p_365166_,
        BlockPos p_53462_,
        Direction p_53459_,
        BlockPos p_53463_,
        BlockState p_53460_,
        RandomSource p_365603_
    ) {
        return this.canSurvive(p_53458_, p_362645_, p_53462_) ? this.getStateWithAge(p_362645_, p_53462_, p_53458_.getValue(AGE)) : Blocks.AIR.defaultBlockState();
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return this.shapes.apply(pState);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.getStateForPlacement(pContext.getLevel(), pContext.getClickedPos());
    }

    protected BlockState getStateForPlacement(BlockGetter pLevel, BlockPos pPos) {
        BlockPos blockpos = pPos.below();
        BlockState blockstate = pLevel.getBlockState(blockpos);
        if (!this.canCatchFire(pLevel, pPos, Direction.UP) && !blockstate.isFaceSturdy(pLevel, blockpos, Direction.UP)) {
            BlockState blockstate1 = this.defaultBlockState();

            for (Direction direction : Direction.values()) {
                BooleanProperty booleanproperty = PROPERTY_BY_DIRECTION.get(direction);
                if (booleanproperty != null) {
                    blockstate1 = blockstate1.setValue(booleanproperty, Boolean.valueOf(this.canCatchFire(pLevel, pPos.relative(direction), direction.getOpposite())));
                }
            }

            return blockstate1;
        } else {
            return this.defaultBlockState();
        }
    }

    @Override
    protected boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        BlockPos blockpos = pPos.below();
        return pLevel.getBlockState(blockpos).isFaceSturdy(pLevel, blockpos, Direction.UP) || this.isValidFireLocation(pLevel, pPos);
    }

    @Override
    protected void tick(BlockState p_221160_, ServerLevel p_221161_, BlockPos p_221162_, RandomSource p_221163_) {
        p_221161_.scheduleTick(p_221162_, this, getFireTickDelay(p_221161_.random));
        if (p_221161_.getGameRules().getBoolean(GameRules.RULE_DOFIRETICK)) {
            if (p_221161_.getGameRules().getBoolean(GameRules.RULE_ALLOWFIRETICKAWAYFROMPLAYERS) || p_221161_.anyPlayerCloseEnoughForSpawning(p_221162_)) {
                if (!p_221160_.canSurvive(p_221161_, p_221162_)) {
                    p_221161_.removeBlock(p_221162_, false);
                }

                BlockState blockstate = p_221161_.getBlockState(p_221162_.below());
                boolean flag = blockstate.isFireSource(p_221161_, p_221162_, Direction.UP);
                int i = p_221160_.getValue(AGE);
                if (!flag && p_221161_.isRaining() && this.isNearRain(p_221161_, p_221162_) && p_221163_.nextFloat() < 0.2F + i * 0.03F) {
                    p_221161_.removeBlock(p_221162_, false);
                } else {
                    int j = Math.min(15, i + p_221163_.nextInt(3) / 2);
                    if (i != j) {
                        p_221160_ = p_221160_.setValue(AGE, j);
                        p_221161_.setBlock(p_221162_, p_221160_, 260);
                    }

                    if (!flag) {
                        if (!this.isValidFireLocation(p_221161_, p_221162_)) {
                            BlockPos blockpos = p_221162_.below();
                            if (!p_221161_.getBlockState(blockpos).isFaceSturdy(p_221161_, blockpos, Direction.UP) || i > 3) {
                                p_221161_.removeBlock(p_221162_, false);
                            }

                            return;
                        }

                        if (i == 15 && p_221163_.nextInt(4) == 0 && !this.canCatchFire(p_221161_, p_221162_.below(), Direction.UP)) {
                            p_221161_.removeBlock(p_221162_, false);
                            return;
                        }
                    }

                    boolean flag1 = p_221161_.getBiome(p_221162_).is(BiomeTags.INCREASED_FIRE_BURNOUT);
                    int k = flag1 ? -50 : 0;
                    this.checkBurnOut(p_221161_, p_221162_.east(), 300 + k, p_221163_, i, Direction.WEST);
                    this.checkBurnOut(p_221161_, p_221162_.west(), 300 + k, p_221163_, i, Direction.EAST);
                    this.checkBurnOut(p_221161_, p_221162_.below(), 250 + k, p_221163_, i, Direction.UP);
                    this.checkBurnOut(p_221161_, p_221162_.above(), 250 + k, p_221163_, i, Direction.DOWN);
                    this.checkBurnOut(p_221161_, p_221162_.north(), 300 + k, p_221163_, i, Direction.SOUTH);
                    this.checkBurnOut(p_221161_, p_221162_.south(), 300 + k, p_221163_, i, Direction.NORTH);
                    BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

                    for (int l = -1; l <= 1; l++) {
                        for (int i1 = -1; i1 <= 1; i1++) {
                            for (int j1 = -1; j1 <= 4; j1++) {
                                if (l != 0 || j1 != 0 || i1 != 0) {
                                    int k1 = 100;
                                    if (j1 > 1) {
                                        k1 += (j1 - 1) * 100;
                                    }

                                    blockpos$mutableblockpos.setWithOffset(p_221162_, l, j1, i1);
                                    int l1 = this.getIgniteOdds(p_221161_, blockpos$mutableblockpos);
                                    if (l1 > 0) {
                                        int i2 = (l1 + 40 + p_221161_.getDifficulty().getId() * 7) / (i + 30);
                                        if (flag1) {
                                            i2 /= 2;
                                        }

                                        if (i2 > 0
                                            && p_221163_.nextInt(k1) <= i2
                                            && (!p_221161_.isRaining() || !this.isNearRain(p_221161_, blockpos$mutableblockpos))) {
                                            int j2 = Math.min(15, i + p_221163_.nextInt(5) / 4);
                                            p_221161_.setBlock(blockpos$mutableblockpos, this.getStateWithAge(p_221161_, blockpos$mutableblockpos, j2), 3);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    protected boolean isNearRain(Level pLevel, BlockPos pPos) {
        return pLevel.isRainingAt(pPos)
            || pLevel.isRainingAt(pPos.west())
            || pLevel.isRainingAt(pPos.east())
            || pLevel.isRainingAt(pPos.north())
            || pLevel.isRainingAt(pPos.south());
    }

    @Deprecated //Forge: Use IForgeBlockState.getFlammability, Public for default implementation only.
    public int getBurnOdds(BlockState pState) {
        return pState.hasProperty(BlockStateProperties.WATERLOGGED) && pState.getValue(BlockStateProperties.WATERLOGGED)
            ? 0
            : this.burnOdds.getInt(pState.getBlock());
    }

    @Deprecated //Forge: Use IForgeBlockState.getFireSpreadSpeed
    public int getIgniteOdds(BlockState pState) {
        return pState.hasProperty(BlockStateProperties.WATERLOGGED) && pState.getValue(BlockStateProperties.WATERLOGGED)
            ? 0
            : this.igniteOdds.getInt(pState.getBlock());
    }

    private void checkBurnOut(Level pLevel, BlockPos pPos, int pChance, RandomSource pRandom, int pAge, Direction face) {
        int i = pLevel.getBlockState(pPos).getFlammability(pLevel, pPos, face);
        if (pRandom.nextInt(pChance) < i) {
            BlockState blockstate = pLevel.getBlockState(pPos);
            blockstate.onCaughtFire(pLevel, pPos, face, null);
            if (pRandom.nextInt(pAge + 10) < 5 && !pLevel.isRainingAt(pPos)) {
                int j = Math.min(pAge + pRandom.nextInt(5) / 4, 15);
                pLevel.setBlock(pPos, this.getStateWithAge(pLevel, pPos, j), 3);
            } else {
                pLevel.removeBlock(pPos, false);
            }
        }
    }

    private BlockState getStateWithAge(LevelReader pLevel, BlockPos pPos, int pAge) {
        BlockState blockstate = getState(pLevel, pPos);
        return blockstate.is(Blocks.FIRE) ? blockstate.setValue(AGE, pAge) : blockstate;
    }

    private boolean isValidFireLocation(BlockGetter pLevel, BlockPos pPos) {
        for (Direction direction : Direction.values()) {
            if (this.canCatchFire(pLevel, pPos.relative(direction), direction.getOpposite())) {
                return true;
            }
        }

        return false;
    }

    private int getIgniteOdds(LevelReader pLevel, BlockPos pPos) {
        if (!pLevel.isEmptyBlock(pPos)) {
            return 0;
        } else {
            int i = 0;

            for (Direction direction : Direction.values()) {
                BlockState blockstate = pLevel.getBlockState(pPos.relative(direction));
                i = Math.max(blockstate.getFireSpreadSpeed(pLevel, pPos.relative(direction), direction.getOpposite()), i);
            }

            return i;
        }
    }

    @Deprecated //Forge: Use canCatchFire with more context
    @Override
    protected boolean canBurn(BlockState pState) {
        return this.getIgniteOdds(pState) > 0;
    }

    @Override
    protected void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        super.onPlace(pState, pLevel, pPos, pOldState, pIsMoving);
        pLevel.scheduleTick(pPos, this, getFireTickDelay(pLevel.random));
    }

    private static int getFireTickDelay(RandomSource pRandom) {
        return 30 + pRandom.nextInt(10);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(AGE, NORTH, EAST, SOUTH, WEST, UP);
    }

    public void setFlammable(Block pBlock, int pEncouragement, int pFlammability) {
        if (pBlock == Blocks.AIR) throw new IllegalArgumentException("Tried to set air on fire... This is bad.");
        this.igniteOdds.put(pBlock, pEncouragement);
        this.burnOdds.put(pBlock, pFlammability);
    }

    /**
     * Side sensitive version that calls the block function.
     *
     * @param world The current world
     * @param pos Block position
     * @param face The side the fire is coming from
     * @return True if the face can catch fire.
     */
    public boolean canCatchFire(BlockGetter world, BlockPos pos, Direction face) {
        return world.getBlockState(pos).isFlammable(world, pos, face);
    }

    public static void bootStrap() {
        FireBlock fireblock = (FireBlock)Blocks.FIRE;
        fireblock.setFlammable(Blocks.OAK_PLANKS, 5, 20);
        fireblock.setFlammable(Blocks.SPRUCE_PLANKS, 5, 20);
        fireblock.setFlammable(Blocks.BIRCH_PLANKS, 5, 20);
        fireblock.setFlammable(Blocks.JUNGLE_PLANKS, 5, 20);
        fireblock.setFlammable(Blocks.ACACIA_PLANKS, 5, 20);
        fireblock.setFlammable(Blocks.CHERRY_PLANKS, 5, 20);
        fireblock.setFlammable(Blocks.DARK_OAK_PLANKS, 5, 20);
        fireblock.setFlammable(Blocks.PALE_OAK_PLANKS, 5, 20);
        fireblock.setFlammable(Blocks.MANGROVE_PLANKS, 5, 20);
        fireblock.setFlammable(Blocks.BAMBOO_PLANKS, 5, 20);
        fireblock.setFlammable(Blocks.BAMBOO_MOSAIC, 5, 20);
        fireblock.setFlammable(Blocks.OAK_SLAB, 5, 20);
        fireblock.setFlammable(Blocks.SPRUCE_SLAB, 5, 20);
        fireblock.setFlammable(Blocks.BIRCH_SLAB, 5, 20);
        fireblock.setFlammable(Blocks.JUNGLE_SLAB, 5, 20);
        fireblock.setFlammable(Blocks.ACACIA_SLAB, 5, 20);
        fireblock.setFlammable(Blocks.CHERRY_SLAB, 5, 20);
        fireblock.setFlammable(Blocks.DARK_OAK_SLAB, 5, 20);
        fireblock.setFlammable(Blocks.PALE_OAK_SLAB, 5, 20);
        fireblock.setFlammable(Blocks.MANGROVE_SLAB, 5, 20);
        fireblock.setFlammable(Blocks.BAMBOO_SLAB, 5, 20);
        fireblock.setFlammable(Blocks.BAMBOO_MOSAIC_SLAB, 5, 20);
        fireblock.setFlammable(Blocks.OAK_FENCE_GATE, 5, 20);
        fireblock.setFlammable(Blocks.SPRUCE_FENCE_GATE, 5, 20);
        fireblock.setFlammable(Blocks.BIRCH_FENCE_GATE, 5, 20);
        fireblock.setFlammable(Blocks.JUNGLE_FENCE_GATE, 5, 20);
        fireblock.setFlammable(Blocks.ACACIA_FENCE_GATE, 5, 20);
        fireblock.setFlammable(Blocks.CHERRY_FENCE_GATE, 5, 20);
        fireblock.setFlammable(Blocks.DARK_OAK_FENCE_GATE, 5, 20);
        fireblock.setFlammable(Blocks.PALE_OAK_FENCE_GATE, 5, 20);
        fireblock.setFlammable(Blocks.MANGROVE_FENCE_GATE, 5, 20);
        fireblock.setFlammable(Blocks.BAMBOO_FENCE_GATE, 5, 20);
        fireblock.setFlammable(Blocks.OAK_FENCE, 5, 20);
        fireblock.setFlammable(Blocks.SPRUCE_FENCE, 5, 20);
        fireblock.setFlammable(Blocks.BIRCH_FENCE, 5, 20);
        fireblock.setFlammable(Blocks.JUNGLE_FENCE, 5, 20);
        fireblock.setFlammable(Blocks.ACACIA_FENCE, 5, 20);
        fireblock.setFlammable(Blocks.CHERRY_FENCE, 5, 20);
        fireblock.setFlammable(Blocks.DARK_OAK_FENCE, 5, 20);
        fireblock.setFlammable(Blocks.PALE_OAK_FENCE, 5, 20);
        fireblock.setFlammable(Blocks.MANGROVE_FENCE, 5, 20);
        fireblock.setFlammable(Blocks.BAMBOO_FENCE, 5, 20);
        fireblock.setFlammable(Blocks.OAK_STAIRS, 5, 20);
        fireblock.setFlammable(Blocks.BIRCH_STAIRS, 5, 20);
        fireblock.setFlammable(Blocks.SPRUCE_STAIRS, 5, 20);
        fireblock.setFlammable(Blocks.JUNGLE_STAIRS, 5, 20);
        fireblock.setFlammable(Blocks.ACACIA_STAIRS, 5, 20);
        fireblock.setFlammable(Blocks.CHERRY_STAIRS, 5, 20);
        fireblock.setFlammable(Blocks.DARK_OAK_STAIRS, 5, 20);
        fireblock.setFlammable(Blocks.PALE_OAK_STAIRS, 5, 20);
        fireblock.setFlammable(Blocks.MANGROVE_STAIRS, 5, 20);
        fireblock.setFlammable(Blocks.BAMBOO_STAIRS, 5, 20);
        fireblock.setFlammable(Blocks.BAMBOO_MOSAIC_STAIRS, 5, 20);
        fireblock.setFlammable(Blocks.OAK_LOG, 5, 5);
        fireblock.setFlammable(Blocks.SPRUCE_LOG, 5, 5);
        fireblock.setFlammable(Blocks.BIRCH_LOG, 5, 5);
        fireblock.setFlammable(Blocks.JUNGLE_LOG, 5, 5);
        fireblock.setFlammable(Blocks.ACACIA_LOG, 5, 5);
        fireblock.setFlammable(Blocks.CHERRY_LOG, 5, 5);
        fireblock.setFlammable(Blocks.PALE_OAK_LOG, 5, 5);
        fireblock.setFlammable(Blocks.DARK_OAK_LOG, 5, 5);
        fireblock.setFlammable(Blocks.MANGROVE_LOG, 5, 5);
        fireblock.setFlammable(Blocks.BAMBOO_BLOCK, 5, 5);
        fireblock.setFlammable(Blocks.STRIPPED_OAK_LOG, 5, 5);
        fireblock.setFlammable(Blocks.STRIPPED_SPRUCE_LOG, 5, 5);
        fireblock.setFlammable(Blocks.STRIPPED_BIRCH_LOG, 5, 5);
        fireblock.setFlammable(Blocks.STRIPPED_JUNGLE_LOG, 5, 5);
        fireblock.setFlammable(Blocks.STRIPPED_ACACIA_LOG, 5, 5);
        fireblock.setFlammable(Blocks.STRIPPED_CHERRY_LOG, 5, 5);
        fireblock.setFlammable(Blocks.STRIPPED_DARK_OAK_LOG, 5, 5);
        fireblock.setFlammable(Blocks.STRIPPED_PALE_OAK_LOG, 5, 5);
        fireblock.setFlammable(Blocks.STRIPPED_MANGROVE_LOG, 5, 5);
        fireblock.setFlammable(Blocks.STRIPPED_BAMBOO_BLOCK, 5, 5);
        fireblock.setFlammable(Blocks.STRIPPED_OAK_WOOD, 5, 5);
        fireblock.setFlammable(Blocks.STRIPPED_SPRUCE_WOOD, 5, 5);
        fireblock.setFlammable(Blocks.STRIPPED_BIRCH_WOOD, 5, 5);
        fireblock.setFlammable(Blocks.STRIPPED_JUNGLE_WOOD, 5, 5);
        fireblock.setFlammable(Blocks.STRIPPED_ACACIA_WOOD, 5, 5);
        fireblock.setFlammable(Blocks.STRIPPED_CHERRY_WOOD, 5, 5);
        fireblock.setFlammable(Blocks.STRIPPED_DARK_OAK_WOOD, 5, 5);
        fireblock.setFlammable(Blocks.STRIPPED_PALE_OAK_WOOD, 5, 5);
        fireblock.setFlammable(Blocks.STRIPPED_MANGROVE_WOOD, 5, 5);
        fireblock.setFlammable(Blocks.OAK_WOOD, 5, 5);
        fireblock.setFlammable(Blocks.SPRUCE_WOOD, 5, 5);
        fireblock.setFlammable(Blocks.BIRCH_WOOD, 5, 5);
        fireblock.setFlammable(Blocks.JUNGLE_WOOD, 5, 5);
        fireblock.setFlammable(Blocks.ACACIA_WOOD, 5, 5);
        fireblock.setFlammable(Blocks.CHERRY_WOOD, 5, 5);
        fireblock.setFlammable(Blocks.PALE_OAK_WOOD, 5, 5);
        fireblock.setFlammable(Blocks.DARK_OAK_WOOD, 5, 5);
        fireblock.setFlammable(Blocks.MANGROVE_WOOD, 5, 5);
        fireblock.setFlammable(Blocks.MANGROVE_ROOTS, 5, 20);
        fireblock.setFlammable(Blocks.OAK_LEAVES, 30, 60);
        fireblock.setFlammable(Blocks.SPRUCE_LEAVES, 30, 60);
        fireblock.setFlammable(Blocks.BIRCH_LEAVES, 30, 60);
        fireblock.setFlammable(Blocks.JUNGLE_LEAVES, 30, 60);
        fireblock.setFlammable(Blocks.ACACIA_LEAVES, 30, 60);
        fireblock.setFlammable(Blocks.CHERRY_LEAVES, 30, 60);
        fireblock.setFlammable(Blocks.DARK_OAK_LEAVES, 30, 60);
        fireblock.setFlammable(Blocks.PALE_OAK_LEAVES, 30, 60);
        fireblock.setFlammable(Blocks.MANGROVE_LEAVES, 30, 60);
        fireblock.setFlammable(Blocks.BOOKSHELF, 30, 20);
        fireblock.setFlammable(Blocks.TNT, 15, 100);
        fireblock.setFlammable(Blocks.SHORT_GRASS, 60, 100);
        fireblock.setFlammable(Blocks.FERN, 60, 100);
        fireblock.setFlammable(Blocks.DEAD_BUSH, 60, 100);
        fireblock.setFlammable(Blocks.SHORT_DRY_GRASS, 60, 100);
        fireblock.setFlammable(Blocks.TALL_DRY_GRASS, 60, 100);
        fireblock.setFlammable(Blocks.SUNFLOWER, 60, 100);
        fireblock.setFlammable(Blocks.LILAC, 60, 100);
        fireblock.setFlammable(Blocks.ROSE_BUSH, 60, 100);
        fireblock.setFlammable(Blocks.PEONY, 60, 100);
        fireblock.setFlammable(Blocks.TALL_GRASS, 60, 100);
        fireblock.setFlammable(Blocks.LARGE_FERN, 60, 100);
        fireblock.setFlammable(Blocks.DANDELION, 60, 100);
        fireblock.setFlammable(Blocks.POPPY, 60, 100);
        fireblock.setFlammable(Blocks.OPEN_EYEBLOSSOM, 60, 100);
        fireblock.setFlammable(Blocks.CLOSED_EYEBLOSSOM, 60, 100);
        fireblock.setFlammable(Blocks.BLUE_ORCHID, 60, 100);
        fireblock.setFlammable(Blocks.ALLIUM, 60, 100);
        fireblock.setFlammable(Blocks.AZURE_BLUET, 60, 100);
        fireblock.setFlammable(Blocks.RED_TULIP, 60, 100);
        fireblock.setFlammable(Blocks.ORANGE_TULIP, 60, 100);
        fireblock.setFlammable(Blocks.WHITE_TULIP, 60, 100);
        fireblock.setFlammable(Blocks.PINK_TULIP, 60, 100);
        fireblock.setFlammable(Blocks.OXEYE_DAISY, 60, 100);
        fireblock.setFlammable(Blocks.CORNFLOWER, 60, 100);
        fireblock.setFlammable(Blocks.LILY_OF_THE_VALLEY, 60, 100);
        fireblock.setFlammable(Blocks.TORCHFLOWER, 60, 100);
        fireblock.setFlammable(Blocks.PITCHER_PLANT, 60, 100);
        fireblock.setFlammable(Blocks.WITHER_ROSE, 60, 100);
        fireblock.setFlammable(Blocks.PINK_PETALS, 60, 100);
        fireblock.setFlammable(Blocks.WILDFLOWERS, 60, 100);
        fireblock.setFlammable(Blocks.LEAF_LITTER, 60, 100);
        fireblock.setFlammable(Blocks.CACTUS_FLOWER, 60, 100);
        fireblock.setFlammable(Blocks.WHITE_WOOL, 30, 60);
        fireblock.setFlammable(Blocks.ORANGE_WOOL, 30, 60);
        fireblock.setFlammable(Blocks.MAGENTA_WOOL, 30, 60);
        fireblock.setFlammable(Blocks.LIGHT_BLUE_WOOL, 30, 60);
        fireblock.setFlammable(Blocks.YELLOW_WOOL, 30, 60);
        fireblock.setFlammable(Blocks.LIME_WOOL, 30, 60);
        fireblock.setFlammable(Blocks.PINK_WOOL, 30, 60);
        fireblock.setFlammable(Blocks.GRAY_WOOL, 30, 60);
        fireblock.setFlammable(Blocks.LIGHT_GRAY_WOOL, 30, 60);
        fireblock.setFlammable(Blocks.CYAN_WOOL, 30, 60);
        fireblock.setFlammable(Blocks.PURPLE_WOOL, 30, 60);
        fireblock.setFlammable(Blocks.BLUE_WOOL, 30, 60);
        fireblock.setFlammable(Blocks.BROWN_WOOL, 30, 60);
        fireblock.setFlammable(Blocks.GREEN_WOOL, 30, 60);
        fireblock.setFlammable(Blocks.RED_WOOL, 30, 60);
        fireblock.setFlammable(Blocks.BLACK_WOOL, 30, 60);
        fireblock.setFlammable(Blocks.VINE, 15, 100);
        fireblock.setFlammable(Blocks.COAL_BLOCK, 5, 5);
        fireblock.setFlammable(Blocks.HAY_BLOCK, 60, 20);
        fireblock.setFlammable(Blocks.TARGET, 15, 20);
        fireblock.setFlammable(Blocks.WHITE_CARPET, 60, 20);
        fireblock.setFlammable(Blocks.ORANGE_CARPET, 60, 20);
        fireblock.setFlammable(Blocks.MAGENTA_CARPET, 60, 20);
        fireblock.setFlammable(Blocks.LIGHT_BLUE_CARPET, 60, 20);
        fireblock.setFlammable(Blocks.YELLOW_CARPET, 60, 20);
        fireblock.setFlammable(Blocks.LIME_CARPET, 60, 20);
        fireblock.setFlammable(Blocks.PINK_CARPET, 60, 20);
        fireblock.setFlammable(Blocks.GRAY_CARPET, 60, 20);
        fireblock.setFlammable(Blocks.LIGHT_GRAY_CARPET, 60, 20);
        fireblock.setFlammable(Blocks.CYAN_CARPET, 60, 20);
        fireblock.setFlammable(Blocks.PURPLE_CARPET, 60, 20);
        fireblock.setFlammable(Blocks.BLUE_CARPET, 60, 20);
        fireblock.setFlammable(Blocks.BROWN_CARPET, 60, 20);
        fireblock.setFlammable(Blocks.GREEN_CARPET, 60, 20);
        fireblock.setFlammable(Blocks.RED_CARPET, 60, 20);
        fireblock.setFlammable(Blocks.BLACK_CARPET, 60, 20);
        fireblock.setFlammable(Blocks.PALE_MOSS_BLOCK, 5, 100);
        fireblock.setFlammable(Blocks.PALE_MOSS_CARPET, 5, 100);
        fireblock.setFlammable(Blocks.PALE_HANGING_MOSS, 5, 100);
        fireblock.setFlammable(Blocks.DRIED_KELP_BLOCK, 30, 60);
        fireblock.setFlammable(Blocks.BAMBOO, 60, 60);
        fireblock.setFlammable(Blocks.SCAFFOLDING, 60, 60);
        fireblock.setFlammable(Blocks.LECTERN, 30, 20);
        fireblock.setFlammable(Blocks.COMPOSTER, 5, 20);
        fireblock.setFlammable(Blocks.SWEET_BERRY_BUSH, 60, 100);
        fireblock.setFlammable(Blocks.BEEHIVE, 5, 20);
        fireblock.setFlammable(Blocks.BEE_NEST, 30, 20);
        fireblock.setFlammable(Blocks.AZALEA_LEAVES, 30, 60);
        fireblock.setFlammable(Blocks.FLOWERING_AZALEA_LEAVES, 30, 60);
        fireblock.setFlammable(Blocks.CAVE_VINES, 15, 60);
        fireblock.setFlammable(Blocks.CAVE_VINES_PLANT, 15, 60);
        fireblock.setFlammable(Blocks.SPORE_BLOSSOM, 60, 100);
        fireblock.setFlammable(Blocks.AZALEA, 30, 60);
        fireblock.setFlammable(Blocks.FLOWERING_AZALEA, 30, 60);
        fireblock.setFlammable(Blocks.BIG_DRIPLEAF, 60, 100);
        fireblock.setFlammable(Blocks.BIG_DRIPLEAF_STEM, 60, 100);
        fireblock.setFlammable(Blocks.SMALL_DRIPLEAF, 60, 100);
        fireblock.setFlammable(Blocks.HANGING_ROOTS, 30, 60);
        fireblock.setFlammable(Blocks.GLOW_LICHEN, 15, 100);
        fireblock.setFlammable(Blocks.FIREFLY_BUSH, 60, 100);
        fireblock.setFlammable(Blocks.BUSH, 60, 100);
    }
}

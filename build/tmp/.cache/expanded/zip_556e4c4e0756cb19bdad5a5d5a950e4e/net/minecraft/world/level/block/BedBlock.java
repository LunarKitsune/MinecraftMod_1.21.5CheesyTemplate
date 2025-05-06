package net.minecraft.world.level.block;

import com.mojang.math.OctahedralGroup;
import com.mojang.math.Quadrant;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.entity.BedBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.ArrayUtils;

public class BedBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<BedBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_359966_ -> p_359966_.group(DyeColor.CODEC.fieldOf("color").forGetter(BedBlock::getColor), propertiesCodec()).apply(p_359966_, BedBlock::new)
    );
    public static final EnumProperty<BedPart> PART = BlockStateProperties.BED_PART;
    public static final BooleanProperty OCCUPIED = BlockStateProperties.OCCUPIED;
    private static final Map<Direction, VoxelShape> SHAPES = Util.make(() -> {
        VoxelShape voxelshape = Block.box(0.0, 0.0, 0.0, 3.0, 3.0, 3.0);
        VoxelShape voxelshape1 = Shapes.rotate(voxelshape, OctahedralGroup.fromXYAngles(Quadrant.R0, Quadrant.R90));
        return Shapes.rotateHorizontal(Shapes.or(Block.column(16.0, 3.0, 9.0), voxelshape, voxelshape1));
    });
    private final DyeColor color;

    @Override
    public MapCodec<BedBlock> codec() {
        return CODEC;
    }

    public BedBlock(DyeColor pColor, BlockBehaviour.Properties pProperties) {
        super(pProperties);
        this.color = pColor;
        this.registerDefaultState(this.stateDefinition.any().setValue(PART, BedPart.FOOT).setValue(OCCUPIED, false));
    }

    @Nullable
    public static Direction getBedOrientation(BlockGetter pLevel, BlockPos pPos) {
        BlockState blockstate = pLevel.getBlockState(pPos);
        return blockstate.getBlock() instanceof BedBlock ? blockstate.getValue(FACING) : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState p_49515_, Level p_49516_, BlockPos p_49517_, Player p_49518_, BlockHitResult p_49520_) {
        if (p_49516_.isClientSide) {
            return InteractionResult.SUCCESS_SERVER;
        } else {
            if (p_49515_.getValue(PART) != BedPart.HEAD) {
                p_49517_ = p_49517_.relative(p_49515_.getValue(FACING));
                p_49515_ = p_49516_.getBlockState(p_49517_);
                if (!p_49515_.is(this)) {
                    return InteractionResult.CONSUME;
                }
            }

            if (!canSetSpawn(p_49516_)) {
                p_49516_.removeBlock(p_49517_, false);
                BlockPos blockpos = p_49517_.relative(p_49515_.getValue(FACING).getOpposite());
                if (p_49516_.getBlockState(blockpos).is(this)) {
                    p_49516_.removeBlock(blockpos, false);
                }

                Vec3 vec3 = p_49517_.getCenter();
                p_49516_.explode(null, p_49516_.damageSources().badRespawnPointExplosion(vec3), null, vec3, 5.0F, true, Level.ExplosionInteraction.BLOCK);
                return InteractionResult.SUCCESS_SERVER;
            } else if (p_49515_.getValue(OCCUPIED)) {
                if (!this.kickVillagerOutOfBed(p_49516_, p_49517_)) {
                    p_49518_.displayClientMessage(Component.translatable("block.minecraft.bed.occupied"), true);
                }

                return InteractionResult.SUCCESS_SERVER;
            } else {
                p_49518_.startSleepInBed(p_49517_).ifLeft(p_49477_ -> {
                    if (p_49477_.getMessage() != null) {
                        p_49518_.displayClientMessage(p_49477_.getMessage(), true);
                    }
                });
                return InteractionResult.SUCCESS_SERVER;
            }
        }
    }

    public static boolean canSetSpawn(Level pLevel) {
        return pLevel.dimensionType().bedWorks();
    }

    private boolean kickVillagerOutOfBed(Level pLevel, BlockPos pPos) {
        List<Villager> list = pLevel.getEntitiesOfClass(Villager.class, new AABB(pPos), LivingEntity::isSleeping);
        if (list.isEmpty()) {
            return false;
        } else {
            list.get(0).stopSleeping();
            return true;
        }
    }

    @Override
    public void fallOn(Level p_152169_, BlockState p_152170_, BlockPos p_152171_, Entity p_152172_, double p_396743_) {
        super.fallOn(p_152169_, p_152170_, p_152171_, p_152172_, p_396743_ * 0.5);
    }

    @Override
    public void updateEntityMovementAfterFallOn(BlockGetter p_49483_, Entity p_49484_) {
        if (p_49484_.isSuppressingBounce()) {
            super.updateEntityMovementAfterFallOn(p_49483_, p_49484_);
        } else {
            this.bounceUp(p_49484_);
        }
    }

    private void bounceUp(Entity pEntity) {
        Vec3 vec3 = pEntity.getDeltaMovement();
        if (vec3.y < 0.0) {
            double d0 = pEntity instanceof LivingEntity ? 1.0 : 0.8;
            pEntity.setDeltaMovement(vec3.x, -vec3.y * 0.66F * d0, vec3.z);
        }
    }

    @Override
    protected BlockState updateShape(
        BlockState p_49525_,
        LevelReader p_367181_,
        ScheduledTickAccess p_361759_,
        BlockPos p_49529_,
        Direction p_49526_,
        BlockPos p_49530_,
        BlockState p_49527_,
        RandomSource p_361707_
    ) {
        if (p_49526_ == getNeighbourDirection(p_49525_.getValue(PART), p_49525_.getValue(FACING))) {
            return p_49527_.is(this) && p_49527_.getValue(PART) != p_49525_.getValue(PART)
                ? p_49525_.setValue(OCCUPIED, p_49527_.getValue(OCCUPIED))
                : Blocks.AIR.defaultBlockState();
        } else {
            return super.updateShape(p_49525_, p_367181_, p_361759_, p_49529_, p_49526_, p_49530_, p_49527_, p_361707_);
        }
    }

    private static Direction getNeighbourDirection(BedPart pPart, Direction pDirection) {
        return pPart == BedPart.FOOT ? pDirection : pDirection.getOpposite();
    }

    @Override
    public BlockState playerWillDestroy(Level p_49505_, BlockPos p_49506_, BlockState p_49507_, Player p_49508_) {
        if (!p_49505_.isClientSide && p_49508_.preventsBlockDrops()) {
            BedPart bedpart = p_49507_.getValue(PART);
            if (bedpart == BedPart.FOOT) {
                BlockPos blockpos = p_49506_.relative(getNeighbourDirection(bedpart, p_49507_.getValue(FACING)));
                BlockState blockstate = p_49505_.getBlockState(blockpos);
                if (blockstate.is(this) && blockstate.getValue(PART) == BedPart.HEAD) {
                    p_49505_.setBlock(blockpos, Blocks.AIR.defaultBlockState(), 35);
                    p_49505_.levelEvent(p_49508_, 2001, blockpos, Block.getId(blockstate));
                }
            }
        }

        return super.playerWillDestroy(p_49505_, p_49506_, p_49507_, p_49508_);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        Direction direction = pContext.getHorizontalDirection();
        BlockPos blockpos = pContext.getClickedPos();
        BlockPos blockpos1 = blockpos.relative(direction);
        Level level = pContext.getLevel();
        return level.getBlockState(blockpos1).canBeReplaced(pContext) && level.getWorldBorder().isWithinBounds(blockpos1) ? this.defaultBlockState().setValue(FACING, direction) : null;
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPES.get(getConnectedDirection(pState).getOpposite());
    }

    public static Direction getConnectedDirection(BlockState pState) {
        Direction direction = pState.getValue(FACING);
        return pState.getValue(PART) == BedPart.HEAD ? direction.getOpposite() : direction;
    }

    public static DoubleBlockCombiner.BlockType getBlockType(BlockState pState) {
        BedPart bedpart = pState.getValue(PART);
        return bedpart == BedPart.HEAD ? DoubleBlockCombiner.BlockType.FIRST : DoubleBlockCombiner.BlockType.SECOND;
    }

    private static boolean isBunkBed(BlockGetter pLevel, BlockPos pPos) {
        return pLevel.getBlockState(pPos.below()).getBlock() instanceof BedBlock;
    }

    public static Optional<Vec3> findStandUpPosition(EntityType<?> pEntityType, CollisionGetter pCollisionGetter, BlockPos pPos, Direction pDirection, float pYRot) {
        Direction direction = pDirection.getClockWise();
        Direction direction1 = direction.isFacingAngle(pYRot) ? direction.getOpposite() : direction;
        if (isBunkBed(pCollisionGetter, pPos)) {
            return findBunkBedStandUpPosition(pEntityType, pCollisionGetter, pPos, pDirection, direction1);
        } else {
            int[][] aint = bedStandUpOffsets(pDirection, direction1);
            Optional<Vec3> optional = findStandUpPositionAtOffset(pEntityType, pCollisionGetter, pPos, aint, true);
            return optional.isPresent() ? optional : findStandUpPositionAtOffset(pEntityType, pCollisionGetter, pPos, aint, false);
        }
    }

    private static Optional<Vec3> findBunkBedStandUpPosition(EntityType<?> pEntityType, CollisionGetter pCollisionGetter, BlockPos pPos, Direction pStateFacing, Direction pEntityFacing) {
        int[][] aint = bedSurroundStandUpOffsets(pStateFacing, pEntityFacing);
        Optional<Vec3> optional = findStandUpPositionAtOffset(pEntityType, pCollisionGetter, pPos, aint, true);
        if (optional.isPresent()) {
            return optional;
        } else {
            BlockPos blockpos = pPos.below();
            Optional<Vec3> optional1 = findStandUpPositionAtOffset(pEntityType, pCollisionGetter, blockpos, aint, true);
            if (optional1.isPresent()) {
                return optional1;
            } else {
                int[][] aint1 = bedAboveStandUpOffsets(pStateFacing);
                Optional<Vec3> optional2 = findStandUpPositionAtOffset(pEntityType, pCollisionGetter, pPos, aint1, true);
                if (optional2.isPresent()) {
                    return optional2;
                } else {
                    Optional<Vec3> optional3 = findStandUpPositionAtOffset(pEntityType, pCollisionGetter, pPos, aint, false);
                    if (optional3.isPresent()) {
                        return optional3;
                    } else {
                        Optional<Vec3> optional4 = findStandUpPositionAtOffset(pEntityType, pCollisionGetter, blockpos, aint, false);
                        return optional4.isPresent() ? optional4 : findStandUpPositionAtOffset(pEntityType, pCollisionGetter, pPos, aint1, false);
                    }
                }
            }
        }
    }

    private static Optional<Vec3> findStandUpPositionAtOffset(EntityType<?> pEntityType, CollisionGetter pCollisionGetter, BlockPos pPos, int[][] pOffsets, boolean pSimulate) {
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for (int[] aint : pOffsets) {
            blockpos$mutableblockpos.set(pPos.getX() + aint[0], pPos.getY(), pPos.getZ() + aint[1]);
            Vec3 vec3 = DismountHelper.findSafeDismountLocation(pEntityType, pCollisionGetter, blockpos$mutableblockpos, pSimulate);
            if (vec3 != null) {
                return Optional.of(vec3);
            }
        }

        return Optional.empty();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING, PART, OCCUPIED);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos p_152175_, BlockState p_152176_) {
        return new BedBlockEntity(p_152175_, p_152176_, this.color);
    }

    @Override
    public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, @Nullable LivingEntity pPlacer, ItemStack pStack) {
        super.setPlacedBy(pLevel, pPos, pState, pPlacer, pStack);
        if (!pLevel.isClientSide) {
            BlockPos blockpos = pPos.relative(pState.getValue(FACING));
            pLevel.setBlock(blockpos, pState.setValue(PART, BedPart.HEAD), 3);
            pLevel.updateNeighborsAt(pPos, Blocks.AIR);
            pState.updateNeighbourShapes(pLevel, pPos, 3);
        }
    }

    public DyeColor getColor() {
        return this.color;
    }

    @Override
    protected long getSeed(BlockState pState, BlockPos pPos) {
        BlockPos blockpos = pPos.relative(pState.getValue(FACING), pState.getValue(PART) == BedPart.HEAD ? 0 : 1);
        return Mth.getSeed(blockpos.getX(), pPos.getY(), blockpos.getZ());
    }

    @Override
    protected boolean isPathfindable(BlockState p_49510_, PathComputationType p_49513_) {
        return false;
    }

    private static int[][] bedStandUpOffsets(Direction pFirstDir, Direction pSecondDir) {
        return ArrayUtils.addAll((int[][])bedSurroundStandUpOffsets(pFirstDir, pSecondDir), (int[][])bedAboveStandUpOffsets(pFirstDir));
    }

    private static int[][] bedSurroundStandUpOffsets(Direction pFirstDir, Direction pSecondDir) {
        return new int[][]{
            {pSecondDir.getStepX(), pSecondDir.getStepZ()},
            {pSecondDir.getStepX() - pFirstDir.getStepX(), pSecondDir.getStepZ() - pFirstDir.getStepZ()},
            {pSecondDir.getStepX() - pFirstDir.getStepX() * 2, pSecondDir.getStepZ() - pFirstDir.getStepZ() * 2},
            {-pFirstDir.getStepX() * 2, -pFirstDir.getStepZ() * 2},
            {-pSecondDir.getStepX() - pFirstDir.getStepX() * 2, -pSecondDir.getStepZ() - pFirstDir.getStepZ() * 2},
            {-pSecondDir.getStepX() - pFirstDir.getStepX(), -pSecondDir.getStepZ() - pFirstDir.getStepZ()},
            {-pSecondDir.getStepX(), -pSecondDir.getStepZ()},
            {-pSecondDir.getStepX() + pFirstDir.getStepX(), -pSecondDir.getStepZ() + pFirstDir.getStepZ()},
            {pFirstDir.getStepX(), pFirstDir.getStepZ()},
            {pSecondDir.getStepX() + pFirstDir.getStepX(), pSecondDir.getStepZ() + pFirstDir.getStepZ()}
        };
    }

    private static int[][] bedAboveStandUpOffsets(Direction pDir) {
        return new int[][]{{0, 0}, {-pDir.getStepX(), -pDir.getStepZ()}};
    }
}
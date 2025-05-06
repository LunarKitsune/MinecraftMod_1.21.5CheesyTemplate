package net.minecraft.world.level.block.piston;

import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PistonMovingBlockEntity extends BlockEntity {
    private static final int TICKS_TO_EXTEND = 2;
    private static final double PUSH_OFFSET = 0.01;
    public static final double TICK_MOVEMENT = 0.51;
    private static final BlockState DEFAULT_BLOCK_STATE = Blocks.AIR.defaultBlockState();
    private static final float DEFAULT_PROGRESS = 0.0F;
    private static final boolean DEFAULT_EXTENDING = false;
    private static final boolean DEFAULT_SOURCE = false;
    private BlockState movedState = DEFAULT_BLOCK_STATE;
    private Direction direction;
    private boolean extending = false;
    private boolean isSourcePiston = false;
    private static final ThreadLocal<Direction> NOCLIP = ThreadLocal.withInitial(() -> null);
    private float progress = 0.0F;
    private float progressO = 0.0F;
    private long lastTicked;
    private int deathTicks;

    public PistonMovingBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(BlockEntityType.PISTON, pPos, pBlockState);
    }

    public PistonMovingBlockEntity(BlockPos pPos, BlockState pBlockState, BlockState pMovedState, Direction pDirection, boolean pExtending, boolean pIsSourcePiston) {
        this(pPos, pBlockState);
        this.movedState = pMovedState;
        this.direction = pDirection;
        this.extending = pExtending;
        this.isSourcePiston = pIsSourcePiston;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider p_335610_) {
        return this.saveCustomOnly(p_335610_);
    }

    public boolean isExtending() {
        return this.extending;
    }

    public Direction getDirection() {
        return this.direction;
    }

    public boolean isSourcePiston() {
        return this.isSourcePiston;
    }

    public float getProgress(float pPartialTicks) {
        if (pPartialTicks > 1.0F) {
            pPartialTicks = 1.0F;
        }

        return Mth.lerp(pPartialTicks, this.progressO, this.progress);
    }

    public float getXOff(float pPartialTicks) {
        return this.direction.getStepX() * this.getExtendedProgress(this.getProgress(pPartialTicks));
    }

    public float getYOff(float pPartialTicks) {
        return this.direction.getStepY() * this.getExtendedProgress(this.getProgress(pPartialTicks));
    }

    public float getZOff(float pPartialTicks) {
        return this.direction.getStepZ() * this.getExtendedProgress(this.getProgress(pPartialTicks));
    }

    private float getExtendedProgress(float pProgress) {
        return this.extending ? pProgress - 1.0F : 1.0F - pProgress;
    }

    private BlockState getCollisionRelatedBlockState() {
        return !this.isExtending() && this.isSourcePiston() && this.movedState.getBlock() instanceof PistonBaseBlock
            ? Blocks.PISTON_HEAD
                .defaultBlockState()
                .setValue(PistonHeadBlock.SHORT, this.progress > 0.25F)
                .setValue(PistonHeadBlock.TYPE, this.movedState.is(Blocks.STICKY_PISTON) ? PistonType.STICKY : PistonType.DEFAULT)
                .setValue(PistonHeadBlock.FACING, this.movedState.getValue(PistonBaseBlock.FACING))
            : this.movedState;
    }

    private static void moveCollidedEntities(Level pLevel, BlockPos pPos, float pPartialTick, PistonMovingBlockEntity pPiston) {
        Direction direction = pPiston.getMovementDirection();
        double d0 = pPartialTick - pPiston.progress;
        VoxelShape voxelshape = pPiston.getCollisionRelatedBlockState().getCollisionShape(pLevel, pPos);
        if (!voxelshape.isEmpty()) {
            AABB aabb = moveByPositionAndProgress(pPos, voxelshape.bounds(), pPiston);
            List<Entity> list = pLevel.getEntities(null, PistonMath.getMovementArea(aabb, direction, d0).minmax(aabb));
            if (!list.isEmpty()) {
                List<AABB> list1 = voxelshape.toAabbs();
                boolean flag = pPiston.movedState.isSlimeBlock(); //TODO: is this patch really needed the logic of the original seems sound revisit later
                Iterator iterator = list.iterator();

                while (true) {
                    Entity entity;
                    while (true) {
                        if (!iterator.hasNext()) {
                            return;
                        }

                        entity = (Entity)iterator.next();
                        if (entity.getPistonPushReaction() != PushReaction.IGNORE) {
                            if (!flag) {
                                break;
                            }

                            if (!(entity instanceof ServerPlayer)) {
                                Vec3 vec3 = entity.getDeltaMovement();
                                double d1 = vec3.x;
                                double d2 = vec3.y;
                                double d3 = vec3.z;
                                switch (direction.getAxis()) {
                                    case X:
                                        d1 = direction.getStepX();
                                        break;
                                    case Y:
                                        d2 = direction.getStepY();
                                        break;
                                    case Z:
                                        d3 = direction.getStepZ();
                                }

                                entity.setDeltaMovement(d1, d2, d3);
                                break;
                            }
                        }
                    }

                    double d4 = 0.0;

                    for (AABB aabb2 : list1) {
                        AABB aabb1 = PistonMath.getMovementArea(moveByPositionAndProgress(pPos, aabb2, pPiston), direction, d0);
                        AABB aabb3 = entity.getBoundingBox();
                        if (aabb1.intersects(aabb3)) {
                            d4 = Math.max(d4, getMovement(aabb1, direction, aabb3));
                            if (d4 >= d0) {
                                break;
                            }
                        }
                    }

                    if (!(d4 <= 0.0)) {
                        d4 = Math.min(d4, d0) + 0.01;
                        moveEntityByPiston(direction, entity, d4, direction);
                        if (!pPiston.extending && pPiston.isSourcePiston) {
                            fixEntityWithinPistonBase(pPos, entity, direction, d0);
                        }
                    }
                }
            }
        }
    }

    private static void moveEntityByPiston(Direction pNoClipDirection, Entity pEntity, double pProgress, Direction pDirection) {
        NOCLIP.set(pNoClipDirection);
        Vec3 vec3 = pEntity.position();
        pEntity.move(MoverType.PISTON, new Vec3(pProgress * pDirection.getStepX(), pProgress * pDirection.getStepY(), pProgress * pDirection.getStepZ()));
        pEntity.applyEffectsFromBlocks(vec3, pEntity.position());
        pEntity.removeLatestMovementRecordingBatch();
        NOCLIP.set(null);
    }

    private static void moveStuckEntities(Level pLevel, BlockPos pPos, float pPartialTick, PistonMovingBlockEntity pPiston) {
        if (pPiston.isStickyForEntities()) {
            Direction direction = pPiston.getMovementDirection();
            if (direction.getAxis().isHorizontal()) {
                double d0 = pPiston.movedState.getCollisionShape(pLevel, pPos).max(Direction.Axis.Y);
                AABB aabb = moveByPositionAndProgress(pPos, new AABB(0.0, d0, 0.0, 1.0, 1.5000010000000001, 1.0), pPiston);
                double d1 = pPartialTick - pPiston.progress;

                for (Entity entity : pLevel.getEntities((Entity)null, aabb, p_287552_ -> matchesStickyCritera(aabb, p_287552_, pPos))) {
                    moveEntityByPiston(direction, entity, d1, direction);
                }
            }
        }
    }

    private static boolean matchesStickyCritera(AABB pBox, Entity pEntity, BlockPos pPos) {
        return pEntity.getPistonPushReaction() == PushReaction.NORMAL
            && pEntity.onGround()
            && (
                pEntity.isSupportedBy(pPos)
                    || pEntity.getX() >= pBox.minX
                        && pEntity.getX() <= pBox.maxX
                        && pEntity.getZ() >= pBox.minZ
                        && pEntity.getZ() <= pBox.maxZ
            );
    }

    private boolean isStickyForEntities() {
        return this.movedState.is(Blocks.HONEY_BLOCK);
    }

    public Direction getMovementDirection() {
        return this.extending ? this.direction : this.direction.getOpposite();
    }

    private static double getMovement(AABB pHeadShape, Direction pDirection, AABB pFacing) {
        switch (pDirection) {
            case EAST:
                return pHeadShape.maxX - pFacing.minX;
            case WEST:
                return pFacing.maxX - pHeadShape.minX;
            case UP:
            default:
                return pHeadShape.maxY - pFacing.minY;
            case DOWN:
                return pFacing.maxY - pHeadShape.minY;
            case SOUTH:
                return pHeadShape.maxZ - pFacing.minZ;
            case NORTH:
                return pFacing.maxZ - pHeadShape.minZ;
        }
    }

    private static AABB moveByPositionAndProgress(BlockPos pPos, AABB pAabb, PistonMovingBlockEntity pPistonMovingBlockEntity) {
        double d0 = pPistonMovingBlockEntity.getExtendedProgress(pPistonMovingBlockEntity.progress);
        return pAabb.move(
            pPos.getX() + d0 * pPistonMovingBlockEntity.direction.getStepX(),
            pPos.getY() + d0 * pPistonMovingBlockEntity.direction.getStepY(),
            pPos.getZ() + d0 * pPistonMovingBlockEntity.direction.getStepZ()
        );
    }

    private static void fixEntityWithinPistonBase(BlockPos pPos, Entity pEntity, Direction pDir, double pProgress) {
        AABB aabb = pEntity.getBoundingBox();
        AABB aabb1 = Shapes.block().bounds().move(pPos);
        if (aabb.intersects(aabb1)) {
            Direction direction = pDir.getOpposite();
            double d0 = getMovement(aabb1, direction, aabb) + 0.01;
            double d1 = getMovement(aabb1, direction, aabb.intersect(aabb1)) + 0.01;
            if (Math.abs(d0 - d1) < 0.01) {
                d0 = Math.min(d0, pProgress) + 0.01;
                moveEntityByPiston(pDir, pEntity, d0, direction);
            }
        }
    }

    public BlockState getMovedState() {
        return this.movedState;
    }

    public void finalTick() {
        if (this.level != null && (this.progressO < 1.0F || this.level.isClientSide)) {
            this.progress = 1.0F;
            this.progressO = this.progress;
            this.level.removeBlockEntity(this.worldPosition);
            this.setRemoved();
            if (this.level.getBlockState(this.worldPosition).is(Blocks.MOVING_PISTON)) {
                BlockState blockstate;
                if (this.isSourcePiston) {
                    blockstate = Blocks.AIR.defaultBlockState();
                } else {
                    blockstate = Block.updateFromNeighbourShapes(this.movedState, this.level, this.worldPosition);
                }

                this.level.setBlock(this.worldPosition, blockstate, 3);
                this.level.neighborChanged(this.worldPosition, blockstate.getBlock(), ExperimentalRedstoneUtils.initialOrientation(this.level, this.getPushDirection(), null));
            }
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos p_397541_, BlockState p_393563_) {
        this.finalTick();
    }

    public Direction getPushDirection() {
        return this.extending ? this.direction : this.direction.getOpposite();
    }

    public static void tick(Level pLevel, BlockPos pPos, BlockState pState, PistonMovingBlockEntity pBlockEntity) {
        pBlockEntity.lastTicked = pLevel.getGameTime();
        pBlockEntity.progressO = pBlockEntity.progress;
        if (pBlockEntity.progressO >= 1.0F) {
            if (pLevel.isClientSide && pBlockEntity.deathTicks < 5) {
                pBlockEntity.deathTicks++;
            } else {
                pLevel.removeBlockEntity(pPos);
                pBlockEntity.setRemoved();
                if (pLevel.getBlockState(pPos).is(Blocks.MOVING_PISTON)) {
                    BlockState blockstate = Block.updateFromNeighbourShapes(pBlockEntity.movedState, pLevel, pPos);
                    if (blockstate.isAir()) {
                        pLevel.setBlock(pPos, pBlockEntity.movedState, 340);
                        Block.updateOrDestroy(pBlockEntity.movedState, blockstate, pLevel, pPos, 3);
                    } else {
                        if (blockstate.hasProperty(BlockStateProperties.WATERLOGGED) && blockstate.getValue(BlockStateProperties.WATERLOGGED)) {
                            blockstate = blockstate.setValue(BlockStateProperties.WATERLOGGED, false);
                        }

                        pLevel.setBlock(pPos, blockstate, 67);
                        pLevel.neighborChanged(pPos, blockstate.getBlock(), ExperimentalRedstoneUtils.initialOrientation(pLevel, pBlockEntity.getPushDirection(), null));
                    }
                }
            }
        } else {
            float f = pBlockEntity.progress + 0.5F;
            moveCollidedEntities(pLevel, pPos, f, pBlockEntity);
            moveStuckEntities(pLevel, pPos, f, pBlockEntity);
            pBlockEntity.progress = f;
            if (pBlockEntity.progress >= 1.0F) {
                pBlockEntity.progress = 1.0F;
            }
        }
    }

    @Override
    protected void loadAdditional(CompoundTag p_333735_, HolderLookup.Provider p_332716_) {
        super.loadAdditional(p_333735_, p_332716_);
        RegistryOps<Tag> registryops = p_332716_.createSerializationContext(NbtOps.INSTANCE);
        this.movedState = p_333735_.read("blockState", BlockState.CODEC, registryops).orElse(DEFAULT_BLOCK_STATE);
        this.direction = p_333735_.read("facing", Direction.LEGACY_ID_CODEC).orElse(Direction.DOWN);
        this.progress = p_333735_.getFloatOr("progress", 0.0F);
        this.progressO = this.progress;
        this.extending = p_333735_.getBooleanOr("extending", false);
        this.isSourcePiston = p_333735_.getBooleanOr("source", false);
    }

    @Override
    protected void saveAdditional(CompoundTag p_187530_, HolderLookup.Provider p_331280_) {
        super.saveAdditional(p_187530_, p_331280_);
        RegistryOps<Tag> registryops = p_331280_.createSerializationContext(NbtOps.INSTANCE);
        p_187530_.store("blockState", BlockState.CODEC, registryops, this.movedState);
        p_187530_.store("facing", Direction.LEGACY_ID_CODEC, this.direction);
        p_187530_.putFloat("progress", this.progressO);
        p_187530_.putBoolean("extending", this.extending);
        p_187530_.putBoolean("source", this.isSourcePiston);
    }

    public VoxelShape getCollisionShape(BlockGetter pLevel, BlockPos pPos) {
        VoxelShape voxelshape;
        if (!this.extending && this.isSourcePiston && this.movedState.getBlock() instanceof PistonBaseBlock) {
            voxelshape = this.movedState.setValue(PistonBaseBlock.EXTENDED, true).getCollisionShape(pLevel, pPos);
        } else {
            voxelshape = Shapes.empty();
        }

        Direction direction = NOCLIP.get();
        if (this.progress < 1.0 && direction == this.getMovementDirection()) {
            return voxelshape;
        } else {
            BlockState blockstate;
            if (this.isSourcePiston()) {
                blockstate = Blocks.PISTON_HEAD
                    .defaultBlockState()
                    .setValue(PistonHeadBlock.FACING, this.direction)
                    .setValue(PistonHeadBlock.SHORT, this.extending != 1.0F - this.progress < 0.25F);
            } else {
                blockstate = this.movedState;
            }

            float f = this.getExtendedProgress(this.progress);
            double d0 = this.direction.getStepX() * f;
            double d1 = this.direction.getStepY() * f;
            double d2 = this.direction.getStepZ() * f;
            return Shapes.or(voxelshape, blockstate.getCollisionShape(pLevel, pPos).move(d0, d1, d2));
        }
    }

    public long getLastTicked() {
        return this.lastTicked;
    }

    @Override
    public void setLevel(Level p_250671_) {
        super.setLevel(p_250671_);
        if (p_250671_.holderLookup(Registries.BLOCK).get(this.movedState.getBlock().builtInRegistryHolder().key()).isEmpty()) {
            this.movedState = Blocks.AIR.defaultBlockState();
        }
    }
}

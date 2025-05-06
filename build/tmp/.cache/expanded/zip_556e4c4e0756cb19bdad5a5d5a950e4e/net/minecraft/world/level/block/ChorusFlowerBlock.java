package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ChorusFlowerBlock extends Block {
    public static final MapCodec<ChorusFlowerBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_360419_ -> p_360419_.group(BuiltInRegistries.BLOCK.byNameCodec().fieldOf("plant").forGetter(p_312628_ -> p_312628_.plant), propertiesCodec())
            .apply(p_360419_, ChorusFlowerBlock::new)
    );
    public static final int DEAD_AGE = 5;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_5;
    private static final VoxelShape SHAPE_BLOCK_SUPPORT = Block.column(14.0, 0.0, 15.0);
    private final Block plant;

    @Override
    public MapCodec<ChorusFlowerBlock> codec() {
        return CODEC;
    }

    public ChorusFlowerBlock(Block pPlant, BlockBehaviour.Properties pProperties) {
        super(pProperties);
        this.plant = pPlant;
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected void tick(BlockState p_220975_, ServerLevel p_220976_, BlockPos p_220977_, RandomSource p_220978_) {
        if (!p_220975_.canSurvive(p_220976_, p_220977_)) {
            p_220976_.destroyBlock(p_220977_, true);
        }
    }

    @Override
    protected boolean isRandomlyTicking(BlockState pState) {
        return pState.getValue(AGE) < 5;
    }

    @Override
    public VoxelShape getBlockSupportShape(BlockState p_298376_, BlockGetter p_300068_, BlockPos p_300404_) {
        return SHAPE_BLOCK_SUPPORT;
    }

    @Override
    protected void randomTick(BlockState p_220980_, ServerLevel p_220981_, BlockPos p_220982_, RandomSource p_220983_) {
        BlockPos blockpos = p_220982_.above();
        if (p_220981_.isEmptyBlock(blockpos) && blockpos.getY() <= p_220981_.getMaxY()) {
            int i = p_220980_.getValue(AGE);
            if (i < 5 && net.minecraftforge.common.ForgeHooks.onCropsGrowPre(p_220981_, blockpos, p_220980_, true)) {
                boolean flag = false;
                boolean flag1 = false;
                BlockState blockstate = p_220981_.getBlockState(p_220982_.below());
                if (blockstate.is(Blocks.END_STONE) || blockstate.is(net.minecraftforge.common.Tags.Blocks.CHORUS_ADDITIONALLY_GROWS_ON)) {
                    flag = true;
                } else if (blockstate.is(this.plant)) {
                    int j = 1;

                    for (int k = 0; k < 4; k++) {
                        BlockState blockstate1 = p_220981_.getBlockState(p_220982_.below(j + 1));
                        if (!blockstate1.is(this.plant)) {
                            if (blockstate1.is(Blocks.END_STONE) || blockstate1.is(net.minecraftforge.common.Tags.Blocks.CHORUS_ADDITIONALLY_GROWS_ON)) {
                                flag1 = true;
                            }
                            break;
                        }

                        j++;
                    }

                    if (j < 2 || j <= p_220983_.nextInt(flag1 ? 5 : 4)) {
                        flag = true;
                    }
                } else if (blockstate.isAir()) {
                    flag = true;
                }

                if (flag && allNeighborsEmpty(p_220981_, blockpos, null) && p_220981_.isEmptyBlock(p_220982_.above(2))) {
                    p_220981_.setBlock(p_220982_, ChorusPlantBlock.getStateWithConnections(p_220981_, p_220982_, this.plant.defaultBlockState()), 2);
                    this.placeGrownFlower(p_220981_, blockpos, i);
                } else if (i < 4) {
                    int l = p_220983_.nextInt(4);
                    if (flag1) {
                        l++;
                    }

                    boolean flag2 = false;

                    for (int i1 = 0; i1 < l; i1++) {
                        Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(p_220983_);
                        BlockPos blockpos1 = p_220982_.relative(direction);
                        if (p_220981_.isEmptyBlock(blockpos1) && p_220981_.isEmptyBlock(blockpos1.below()) && allNeighborsEmpty(p_220981_, blockpos1, direction.getOpposite())) {
                            this.placeGrownFlower(p_220981_, blockpos1, i + 1);
                            flag2 = true;
                        }
                    }

                    if (flag2) {
                        p_220981_.setBlock(p_220982_, ChorusPlantBlock.getStateWithConnections(p_220981_, p_220982_, this.plant.defaultBlockState()), 2);
                    } else {
                        this.placeDeadFlower(p_220981_, p_220982_);
                    }
                } else {
                    this.placeDeadFlower(p_220981_, p_220982_);
                }
                net.minecraftforge.common.ForgeHooks.onCropsGrowPost(p_220981_, p_220982_, p_220980_);
            }
        }
    }

    private void placeGrownFlower(Level pLevel, BlockPos pPos, int pAge) {
        pLevel.setBlock(pPos, this.defaultBlockState().setValue(AGE, pAge), 2);
        pLevel.levelEvent(1033, pPos, 0);
    }

    private void placeDeadFlower(Level pLevel, BlockPos pPos) {
        pLevel.setBlock(pPos, this.defaultBlockState().setValue(AGE, 5), 2);
        pLevel.levelEvent(1034, pPos, 0);
    }

    private static boolean allNeighborsEmpty(LevelReader pLevel, BlockPos pPos, @Nullable Direction pExcludingSide) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (direction != pExcludingSide && !pLevel.isEmptyBlock(pPos.relative(direction))) {
                return false;
            }
        }

        return true;
    }

    @Override
    protected BlockState updateShape(
        BlockState p_51687_,
        LevelReader p_364413_,
        ScheduledTickAccess p_360794_,
        BlockPos p_51691_,
        Direction p_51688_,
        BlockPos p_51692_,
        BlockState p_51689_,
        RandomSource p_368740_
    ) {
        if (p_51688_ != Direction.UP && !p_51687_.canSurvive(p_364413_, p_51691_)) {
            p_360794_.scheduleTick(p_51691_, this, 1);
        }

        return super.updateShape(p_51687_, p_364413_, p_360794_, p_51691_, p_51688_, p_51692_, p_51689_, p_368740_);
    }

    @Override
    protected boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        BlockState blockstate = pLevel.getBlockState(pPos.below());
        if (!blockstate.is(this.plant) && !blockstate.is(Blocks.END_STONE) && !blockstate.is(net.minecraftforge.common.Tags.Blocks.CHORUS_ADDITIONALLY_GROWS_ON)) {
            if (!blockstate.isAir()) {
                return false;
            } else {
                boolean flag = false;

                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    BlockState blockstate1 = pLevel.getBlockState(pPos.relative(direction));
                    if (blockstate1.is(this.plant)) {
                        if (flag) {
                            return false;
                        }

                        flag = true;
                    } else if (!blockstate1.isAir()) {
                        return false;
                    }
                }

                return flag;
            }
        } else {
            return true;
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(AGE);
    }

    public static void generatePlant(LevelAccessor pLevel, BlockPos pPos, RandomSource pRandom, int pMaxHorizontalDistance) {
        pLevel.setBlock(pPos, ChorusPlantBlock.getStateWithConnections(pLevel, pPos, Blocks.CHORUS_PLANT.defaultBlockState()), 2);
        growTreeRecursive(pLevel, pPos, pRandom, pPos, pMaxHorizontalDistance, 0);
    }

    private static void growTreeRecursive(LevelAccessor pLevel, BlockPos pBranchPos, RandomSource pRandom, BlockPos pOriginalBranchPos, int pMaxHorizontalDistance, int pIterations) {
        Block block = Blocks.CHORUS_PLANT;
        int i = pRandom.nextInt(4) + 1;
        if (pIterations == 0) {
            i++;
        }

        for (int j = 0; j < i; j++) {
            BlockPos blockpos = pBranchPos.above(j + 1);
            if (!allNeighborsEmpty(pLevel, blockpos, null)) {
                return;
            }

            pLevel.setBlock(blockpos, ChorusPlantBlock.getStateWithConnections(pLevel, blockpos, block.defaultBlockState()), 2);
            pLevel.setBlock(blockpos.below(), ChorusPlantBlock.getStateWithConnections(pLevel, blockpos.below(), block.defaultBlockState()), 2);
        }

        boolean flag = false;
        if (pIterations < 4) {
            int l = pRandom.nextInt(4);
            if (pIterations == 0) {
                l++;
            }

            for (int k = 0; k < l; k++) {
                Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(pRandom);
                BlockPos blockpos1 = pBranchPos.above(i).relative(direction);
                if (Math.abs(blockpos1.getX() - pOriginalBranchPos.getX()) < pMaxHorizontalDistance
                    && Math.abs(blockpos1.getZ() - pOriginalBranchPos.getZ()) < pMaxHorizontalDistance
                    && pLevel.isEmptyBlock(blockpos1)
                    && pLevel.isEmptyBlock(blockpos1.below())
                    && allNeighborsEmpty(pLevel, blockpos1, direction.getOpposite())) {
                    flag = true;
                    pLevel.setBlock(blockpos1, ChorusPlantBlock.getStateWithConnections(pLevel, blockpos1, block.defaultBlockState()), 2);
                    pLevel.setBlock(
                        blockpos1.relative(direction.getOpposite()),
                        ChorusPlantBlock.getStateWithConnections(pLevel, blockpos1.relative(direction.getOpposite()), block.defaultBlockState()),
                        2
                    );
                    growTreeRecursive(pLevel, blockpos1, pRandom, pOriginalBranchPos, pMaxHorizontalDistance, pIterations + 1);
                }
            }
        }

        if (!flag) {
            pLevel.setBlock(pBranchPos.above(i), Blocks.CHORUS_FLOWER.defaultBlockState().setValue(AGE, 5), 2);
        }
    }

    @Override
    protected void onProjectileHit(Level pLevel, BlockState pState, BlockHitResult pHit, Projectile pProjectile) {
        BlockPos blockpos = pHit.getBlockPos();
        if (pLevel instanceof ServerLevel serverlevel && pProjectile.mayInteract(serverlevel, blockpos) && pProjectile.mayBreak(serverlevel)) {
            pLevel.destroyBlock(blockpos, true, pProjectile);
        }
    }
}

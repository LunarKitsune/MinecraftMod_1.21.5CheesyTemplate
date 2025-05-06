package net.minecraft.world.level.block;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class StemBlock extends VegetationBlock implements BonemealableBlock {
    public static final MapCodec<StemBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_360456_ -> p_360456_.group(
                ResourceKey.codec(Registries.BLOCK).fieldOf("fruit").forGetter(p_312514_ -> p_312514_.fruit),
                ResourceKey.codec(Registries.BLOCK).fieldOf("attached_stem").forGetter(p_309847_ -> p_309847_.attachedStem),
                ResourceKey.codec(Registries.ITEM).fieldOf("seed").forGetter(p_311480_ -> p_311480_.seed),
                propertiesCodec()
            )
            .apply(p_360456_, StemBlock::new)
    );
    public static final int MAX_AGE = 7;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_7;
    private static final VoxelShape[] SHAPES = Block.boxes(7, p_390954_ -> Block.column(2.0, 0.0, 2 + p_390954_ * 2));
    private final ResourceKey<Block> fruit;
    private final ResourceKey<Block> attachedStem;
    private final ResourceKey<Item> seed;

    @Override
    public MapCodec<StemBlock> codec() {
        return CODEC;
    }

    public StemBlock(ResourceKey<Block> pFruit, ResourceKey<Block> pAttachedStem, ResourceKey<Item> pSeed, BlockBehaviour.Properties pProperties) {
        super(pProperties);
        this.fruit = pFruit;
        this.attachedStem = pAttachedStem;
        this.seed = pSeed;
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPES[pState.getValue(AGE)];
    }

    @Override
    protected boolean mayPlaceOn(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return pState.is(Blocks.FARMLAND);
    }

    @Override
    protected void randomTick(BlockState p_222538_, ServerLevel p_222539_, BlockPos p_222540_, RandomSource p_222541_) {
        if (!p_222539_.isAreaLoaded(p_222540_, 1)) return; // Forge: prevent loading unloaded chunks when checking neighbor's light
        if (p_222539_.getRawBrightness(p_222540_, 0) >= 9) {
            float f = CropBlock.getGrowthSpeed(this, p_222539_, p_222540_);
            var vanilla = p_222541_.nextInt((int)(25.0F / f) + 1) == 0;
            if (net.minecraftforge.common.ForgeHooks.onCropsGrowPre(p_222539_, p_222540_, p_222538_, vanilla)) {
                int i = p_222538_.getValue(AGE);
                if (i < 7) {
                    p_222538_ = p_222538_.setValue(AGE, i + 1);
                    p_222539_.setBlock(p_222540_, p_222538_, 2);
                } else {
                    Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(p_222541_);
                    BlockPos blockpos = p_222540_.relative(direction);
                    BlockState blockstate = p_222539_.getBlockState(blockpos.below());
                    if (p_222539_.getBlockState(blockpos).isAir() && (blockstate.is(Blocks.FARMLAND) || blockstate.is(BlockTags.DIRT))) {
                        Registry<Block> registry = p_222539_.registryAccess().lookupOrThrow(Registries.BLOCK);
                        Optional<Block> optional = registry.getOptional(this.fruit);
                        Optional<Block> optional1 = registry.getOptional(this.attachedStem);
                        if (optional.isPresent() && optional1.isPresent()) {
                            p_222539_.setBlockAndUpdate(blockpos, optional.get().defaultBlockState());
                            p_222539_.setBlockAndUpdate(p_222540_, optional1.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, direction));
                        }
                    }
                }
                net.minecraftforge.common.ForgeHooks.onCropsGrowPost(p_222539_, p_222540_, p_222538_);
            }
        }
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader p_312829_, BlockPos p_57027_, BlockState p_57028_, boolean p_375751_) {
        return new ItemStack(DataFixUtils.orElse(p_312829_.registryAccess().lookupOrThrow(Registries.ITEM).getOptional(this.seed), this));
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader p_255699_, BlockPos p_57031_, BlockState p_57032_) {
        return p_57032_.getValue(AGE) != 7;
    }

    @Override
    public boolean isBonemealSuccess(Level p_222533_, RandomSource p_222534_, BlockPos p_222535_, BlockState p_222536_) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel p_222528_, RandomSource p_222529_, BlockPos p_222530_, BlockState p_222531_) {
        int i = Math.min(7, p_222531_.getValue(AGE) + Mth.nextInt(p_222528_.random, 2, 5));
        BlockState blockstate = p_222531_.setValue(AGE, i);
        p_222528_.setBlock(p_222530_, blockstate, 2);
        if (i == 7) {
            blockstate.randomTick(p_222528_, p_222530_, p_222528_.random);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(AGE);
    }

    @Override
    public net.minecraftforge.common.PlantType getPlantType(BlockGetter world, BlockPos pos) {
        return net.minecraftforge.common.PlantType.CROP;
    }
}

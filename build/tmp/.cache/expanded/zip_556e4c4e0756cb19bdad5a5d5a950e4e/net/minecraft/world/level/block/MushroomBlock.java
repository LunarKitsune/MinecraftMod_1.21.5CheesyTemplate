package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MushroomBlock extends VegetationBlock implements BonemealableBlock {
    public static final MapCodec<MushroomBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_360440_ -> p_360440_.group(ResourceKey.codec(Registries.CONFIGURED_FEATURE).fieldOf("feature").forGetter(p_310622_ -> p_310622_.feature), propertiesCodec())
            .apply(p_360440_, MushroomBlock::new)
    );
    private static final VoxelShape SHAPE = Block.column(6.0, 0.0, 6.0);
    private final ResourceKey<ConfiguredFeature<?, ?>> feature;

    @Override
    public MapCodec<MushroomBlock> codec() {
        return CODEC;
    }

    public MushroomBlock(ResourceKey<ConfiguredFeature<?, ?>> pFeature, BlockBehaviour.Properties pProperties) {
        super(pProperties);
        this.feature = pFeature;
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPE;
    }

    @Override
    protected void randomTick(BlockState p_221784_, ServerLevel p_221785_, BlockPos p_221786_, RandomSource p_221787_) {
        if (p_221787_.nextInt(25) == 0) {
            int i = 5;
            int j = 4;

            for (BlockPos blockpos : BlockPos.betweenClosed(p_221786_.offset(-4, -1, -4), p_221786_.offset(4, 1, 4))) {
                if (p_221785_.getBlockState(blockpos).is(this)) {
                    if (--i <= 0) {
                        return;
                    }
                }
            }

            BlockPos blockpos1 = p_221786_.offset(p_221787_.nextInt(3) - 1, p_221787_.nextInt(2) - p_221787_.nextInt(2), p_221787_.nextInt(3) - 1);

            for (int k = 0; k < 4; k++) {
                if (p_221785_.isEmptyBlock(blockpos1) && p_221784_.canSurvive(p_221785_, blockpos1)) {
                    p_221786_ = blockpos1;
                }

                blockpos1 = p_221786_.offset(p_221787_.nextInt(3) - 1, p_221787_.nextInt(2) - p_221787_.nextInt(2), p_221787_.nextInt(3) - 1);
            }

            if (p_221785_.isEmptyBlock(blockpos1) && p_221784_.canSurvive(p_221785_, blockpos1)) {
                p_221785_.setBlock(blockpos1, p_221784_, 2);
            }
        }
    }

    @Override
    protected boolean mayPlaceOn(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return pState.isSolidRender();
    }

    @Override
    protected boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        BlockPos blockpos = pPos.below();
        BlockState blockstate = pLevel.getBlockState(blockpos);
        return blockstate.is(BlockTags.MUSHROOM_GROW_BLOCK) ? true : pLevel.getRawBrightness(pPos, 0) < 13 && blockstate.canSustainPlant(pLevel, blockpos, net.minecraft.core.Direction.UP, this);
    }

    public boolean growMushroom(ServerLevel pLevel, BlockPos pPos, BlockState pState, RandomSource pRandom) {
        Optional<? extends Holder<ConfiguredFeature<?, ?>>> optional = pLevel.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).get(this.feature);
        if (optional.isEmpty()) {
            return false;
        } else {
            var event = net.minecraftforge.event.ForgeEventFactory.blockGrowFeature(pLevel, pRandom, pPos, optional.get());
            if (event.getResult().isDenied()) return false;
            pLevel.removeBlock(pPos, false);
            if (event.getFeature().value().place(pLevel, pLevel.getChunkSource().getGenerator(), pRandom, pPos)) {
                return true;
            } else {
                pLevel.setBlock(pPos, pState, 3);
                return false;
            }
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader p_255904_, BlockPos p_54871_, BlockState p_54872_) {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(Level p_221779_, RandomSource p_221780_, BlockPos p_221781_, BlockState p_221782_) {
        return p_221780_.nextFloat() < 0.4;
    }

    @Override
    public void performBonemeal(ServerLevel p_221769_, RandomSource p_221770_, BlockPos p_221771_, BlockState p_221772_) {
        this.growMushroom(p_221769_, p_221771_, p_221772_, p_221770_);
    }
}

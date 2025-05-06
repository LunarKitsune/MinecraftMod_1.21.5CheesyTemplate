package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
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

public class FungusBlock extends VegetationBlock implements BonemealableBlock {
    public static final MapCodec<FungusBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_360434_ -> p_360434_.group(
                ResourceKey.codec(Registries.CONFIGURED_FEATURE).fieldOf("feature").forGetter(p_309283_ -> p_309283_.feature),
                BuiltInRegistries.BLOCK.byNameCodec().fieldOf("grows_on").forGetter(p_309285_ -> p_309285_.requiredBlock),
                propertiesCodec()
            )
            .apply(p_360434_, FungusBlock::new)
    );
    private static final double BONEMEAL_SUCCESS_PROBABILITY = 0.4;
    private static final VoxelShape SHAPE = Block.column(8.0, 0.0, 9.0);
    private final Block requiredBlock;
    private final ResourceKey<ConfiguredFeature<?, ?>> feature;

    @Override
    public MapCodec<FungusBlock> codec() {
        return CODEC;
    }

    public FungusBlock(ResourceKey<ConfiguredFeature<?, ?>> pFeature, Block pRequiredBlock, BlockBehaviour.Properties pProperties) {
        super(pProperties);
        this.feature = pFeature;
        this.requiredBlock = pRequiredBlock;
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return pState.is(BlockTags.NYLIUM)
            || pState.is(Blocks.MYCELIUM)
            || pState.is(Blocks.SOUL_SOIL)
            || super.mayPlaceOn(pState, pLevel, pPos);
    }

    private Optional<? extends Holder<ConfiguredFeature<?, ?>>> getFeature(LevelReader pLevel) {
        return pLevel.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).get(this.feature);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader p_256655_, BlockPos p_256553_, BlockState p_256213_) {
        BlockState blockstate = p_256655_.getBlockState(p_256553_.below());
        return blockstate.is(this.requiredBlock);
    }

    @Override
    public boolean isBonemealSuccess(Level p_221248_, RandomSource p_221249_, BlockPos p_221250_, BlockState p_221251_) {
        return p_221249_.nextFloat() < 0.4;
    }

    @Override
    public void performBonemeal(ServerLevel p_221243_, RandomSource p_221244_, BlockPos p_221245_, BlockState p_221246_) {
        this.getFeature(p_221243_).ifPresent(p_256352_ -> {
            var event = net.minecraftforge.event.ForgeEventFactory.blockGrowFeature(p_221243_, p_221244_, p_221245_, p_256352_);
            if (event.getResult().isDenied()) return;
            event.getFeature().value().place(p_221243_, p_221243_.getChunkSource().getGenerator(), p_221244_, p_221245_);
        });
    }
}

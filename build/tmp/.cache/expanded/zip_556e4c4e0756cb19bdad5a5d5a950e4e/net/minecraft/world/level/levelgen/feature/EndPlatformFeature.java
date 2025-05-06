package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class EndPlatformFeature extends Feature<NoneFeatureConfiguration> {
    public EndPlatformFeature(Codec<NoneFeatureConfiguration> p_345504_) {
        super(p_345504_);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> p_345440_) {
        createEndPlatform(p_345440_.level(), p_345440_.origin(), false);
        return true;
    }

    public static void createEndPlatform(ServerLevelAccessor pLevel, BlockPos pPos, boolean pDropBlocks) {
        BlockPos.MutableBlockPos blockpos$mutableblockpos = pPos.mutable();

        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                for (int k = -1; k < 3; k++) {
                    BlockPos blockpos = blockpos$mutableblockpos.set(pPos).move(j, k, i);
                    Block block = k == -1 ? Blocks.OBSIDIAN : Blocks.AIR;
                    if (!pLevel.getBlockState(blockpos).is(block)) {
                        if (pDropBlocks) {
                            pLevel.destroyBlock(blockpos, true, null);
                        }

                        pLevel.setBlock(blockpos, block.defaultBlockState(), 3);
                    }
                }
            }
        }
    }
}
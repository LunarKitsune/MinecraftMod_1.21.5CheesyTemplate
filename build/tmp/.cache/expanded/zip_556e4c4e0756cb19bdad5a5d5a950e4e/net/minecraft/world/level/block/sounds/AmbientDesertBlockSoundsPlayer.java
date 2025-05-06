package net.minecraft.world.level.block.sounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

public class AmbientDesertBlockSoundsPlayer {
    private static final int IDLE_SOUND_CHANCE = 1600;
    private static final int WIND_SOUND_CHANCE = 10000;
    private static final int SURROUNDING_BLOCKS_PLAY_SOUND_THRESHOLD = 3;
    private static final int SURROUNDING_BLOCKS_DISTANCE_CHECK = 8;

    public static void playAmbientBlockSounds(BlockState pState, Level pLevel, BlockPos pPos, RandomSource pRandom) {
        if (pState.is(BlockTags.PLAYS_AMBIENT_DESERT_BLOCK_SOUNDS) && pLevel.canSeeSky(pPos.above())) {
            if (pRandom.nextInt(1600) == 0 && shouldPlayAmbientSound(pLevel, pPos)) {
                pLevel.playLocalSound(
                    pPos.getX(), pPos.getY(), pPos.getZ(), SoundEvents.SAND_IDLE, SoundSource.AMBIENT, 1.0F, 1.0F, false
                );
            }

            if (pRandom.nextInt(10000) == 0 && isInAmbientSoundBiome(pLevel.getBiome(pPos)) && shouldPlayAmbientSound(pLevel, pPos)) {
                pLevel.playPlayerSound(SoundEvents.SAND_WIND, SoundSource.AMBIENT, 1.0F, 1.0F);
            }
        }
    }

    private static boolean isInAmbientSoundBiome(Holder<Biome> pBiome) {
        return pBiome.is(Biomes.DESERT) || pBiome.is(BiomeTags.IS_BADLANDS);
    }

    private static boolean shouldPlayAmbientSound(Level pLevel, BlockPos pPos) {
        int i = 0;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos blockpos = pPos.relative(direction, 8);
            BlockState blockstate = pLevel.getBlockState(blockpos.atY(pLevel.getHeight(Heightmap.Types.WORLD_SURFACE, blockpos) - 1));
            if (blockstate.is(BlockTags.PLAYS_AMBIENT_DESERT_BLOCK_SOUNDS)) {
                if (++i >= 3) {
                    return true;
                }
            }
        }

        return false;
    }
}
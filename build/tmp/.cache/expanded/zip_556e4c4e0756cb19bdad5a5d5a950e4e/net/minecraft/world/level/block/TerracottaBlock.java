package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.sounds.AmbientDesertBlockSoundsPlayer;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class TerracottaBlock extends Block {
    public static final MapCodec<TerracottaBlock> CODEC = simpleCodec(TerracottaBlock::new);

    @Override
    public MapCodec<TerracottaBlock> codec() {
        return CODEC;
    }

    public TerracottaBlock(BlockBehaviour.Properties p_395892_) {
        super(p_395892_);
    }

    @Override
    public void animateTick(BlockState p_392075_, Level p_391650_, BlockPos p_397680_, RandomSource p_391886_) {
        AmbientDesertBlockSoundsPlayer.playAmbientBlockSounds(p_392075_, p_391650_, p_397680_, p_391886_);
    }
}
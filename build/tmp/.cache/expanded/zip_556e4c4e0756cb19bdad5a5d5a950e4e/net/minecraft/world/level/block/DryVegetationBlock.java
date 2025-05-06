package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DryVegetationBlock extends VegetationBlock implements net.minecraftforge.common.IForgeShearable {
    public static final MapCodec<DryVegetationBlock> CODEC = simpleCodec(DryVegetationBlock::new);
    private static final VoxelShape SHAPE = Block.column(12.0, 0.0, 13.0);
    private static final int IDLE_SOUND_CHANCE = 150;
    private static final int IDLE_SOUND_BADLANDS_DECREASED_CHANCE = 5;

    @Override
    public MapCodec<? extends DryVegetationBlock> codec() {
        return CODEC;
    }

    public DryVegetationBlock(BlockBehaviour.Properties p_397903_) {
        super(p_397903_);
    }

    @Override
    protected VoxelShape getShape(BlockState p_392262_, BlockGetter p_392875_, BlockPos p_394920_, CollisionContext p_395697_) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(BlockState p_392399_, BlockGetter p_393872_, BlockPos p_395819_) {
        return p_392399_.is(BlockTags.DRY_VEGETATION_MAY_PLACE_ON);
    }

    @Override
    public void animateTick(BlockState p_392005_, Level p_391577_, BlockPos p_396013_, RandomSource p_394858_) {
        if (p_394858_.nextInt(150) == 0) {
            BlockState blockstate = p_391577_.getBlockState(p_396013_.below());
            if ((blockstate.is(Blocks.RED_SAND) || blockstate.is(BlockTags.TERRACOTTA)) && p_394858_.nextInt(5) != 0) {
                return;
            }

            BlockState blockstate1 = p_391577_.getBlockState(p_396013_.below(2));
            if (blockstate.is(BlockTags.PLAYS_AMBIENT_DESERT_BLOCK_SOUNDS) && blockstate1.is(BlockTags.PLAYS_AMBIENT_DESERT_BLOCK_SOUNDS)) {
                p_391577_.playLocalSound(
                    p_396013_.getX(), p_396013_.getY(), p_396013_.getZ(), SoundEvents.DEAD_BUSH_IDLE, SoundSource.AMBIENT, 1.0F, 1.0F, false
                );
            }
        }
    }
}

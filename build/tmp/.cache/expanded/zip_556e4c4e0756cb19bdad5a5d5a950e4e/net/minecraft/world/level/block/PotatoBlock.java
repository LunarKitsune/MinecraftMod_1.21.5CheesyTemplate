package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PotatoBlock extends CropBlock {
    public static final MapCodec<PotatoBlock> CODEC = simpleCodec(PotatoBlock::new);
    private static final VoxelShape[] SHAPES = Block.boxes(7, p_396923_ -> Block.column(16.0, 0.0, 2 + p_396923_));

    @Override
    public MapCodec<PotatoBlock> codec() {
        return CODEC;
    }

    public PotatoBlock(BlockBehaviour.Properties p_55198_) {
        super(p_55198_);
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return Items.POTATO;
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPES[this.getAge(pState)];
    }
}
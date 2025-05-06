package net.minecraft.world.level.block;

import com.mojang.math.OctahedralGroup;
import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class GrindstoneBlock extends FaceAttachedHorizontalDirectionalBlock {
    public static final MapCodec<GrindstoneBlock> CODEC = simpleCodec(GrindstoneBlock::new);
    private static final Component CONTAINER_TITLE = Component.translatable("container.grindstone_title");
    private final Function<BlockState, VoxelShape> shapes;

    @Override
    public MapCodec<GrindstoneBlock> codec() {
        return CODEC;
    }

    public GrindstoneBlock(BlockBehaviour.Properties p_53808_) {
        super(p_53808_);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FACE, AttachFace.WALL));
        this.shapes = this.makeShapes();
    }

    private Function<BlockState, VoxelShape> makeShapes() {
        VoxelShape voxelshape = Shapes.or(Block.box(2.0, 6.0, 7.0, 4.0, 10.0, 16.0), Block.box(2.0, 5.0, 3.0, 4.0, 11.0, 9.0));
        VoxelShape voxelshape1 = Shapes.rotate(voxelshape, OctahedralGroup.INVERT_X);
        VoxelShape voxelshape2 = Shapes.or(Block.boxZ(8.0, 2.0, 14.0, 0.0, 12.0), voxelshape, voxelshape1);
        Map<AttachFace, Map<Direction, VoxelShape>> map = Shapes.rotateAttachFace(voxelshape2);
        return this.getShapeForEachState(p_390940_ -> map.get(p_390940_.getValue(FACE)).get(p_390940_.getValue(FACING)));
    }

    private VoxelShape getVoxelShape(BlockState pState) {
        return this.shapes.apply(pState);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return this.getVoxelShape(pState);
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return this.getVoxelShape(pState);
    }

    @Override
    protected boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        return true;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState p_53821_, Level p_53822_, BlockPos p_53823_, Player p_53824_, BlockHitResult p_53826_) {
        if (!p_53822_.isClientSide) {
            p_53824_.openMenu(p_53821_.getMenuProvider(p_53822_, p_53823_));
            p_53824_.awardStat(Stats.INTERACT_WITH_GRINDSTONE);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    protected MenuProvider getMenuProvider(BlockState pState, Level pLevel, BlockPos pPos) {
        return new SimpleMenuProvider(
            (p_53812_, p_53813_, p_53814_) -> new GrindstoneMenu(p_53812_, p_53813_, ContainerLevelAccess.create(pLevel, pPos)), CONTAINER_TITLE
        );
    }

    @Override
    protected BlockState rotate(BlockState pState, Rotation pRotation) {
        return pState.setValue(FACING, pRotation.rotate(pState.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState pState, Mirror pMirror) {
        return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING, FACE);
    }

    @Override
    protected boolean isPathfindable(BlockState p_53816_, PathComputationType p_53819_) {
        return false;
    }
}
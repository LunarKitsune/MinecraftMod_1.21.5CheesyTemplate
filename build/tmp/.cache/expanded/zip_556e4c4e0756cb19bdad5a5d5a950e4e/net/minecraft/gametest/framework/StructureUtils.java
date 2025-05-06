package net.minecraft.gametest.framework;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class StructureUtils {
    public static final int DEFAULT_Y_SEARCH_RADIUS = 10;
    public static final String DEFAULT_TEST_STRUCTURES_DIR = "Minecraft.Server/src/test/convertables/data";
    public static Path testStructuresDir = Paths.get("Minecraft.Server/src/test/convertables/data");

    public static Rotation getRotationForRotationSteps(int pRotationSteps) {
        switch (pRotationSteps) {
            case 0:
                return Rotation.NONE;
            case 1:
                return Rotation.CLOCKWISE_90;
            case 2:
                return Rotation.CLOCKWISE_180;
            case 3:
                return Rotation.COUNTERCLOCKWISE_90;
            default:
                throw new IllegalArgumentException("rotationSteps must be a value from 0-3. Got value " + pRotationSteps);
        }
    }

    public static int getRotationStepsForRotation(Rotation pRotation) {
        switch (pRotation) {
            case NONE:
                return 0;
            case CLOCKWISE_90:
                return 1;
            case CLOCKWISE_180:
                return 2;
            case COUNTERCLOCKWISE_90:
                return 3;
            default:
                throw new IllegalArgumentException("Unknown rotation value, don't know how many steps it represents: " + pRotation);
        }
    }

    public static TestInstanceBlockEntity createNewEmptyTest(ResourceLocation pId, BlockPos pPos, Vec3i pSize, Rotation pRotation, ServerLevel pLevel) {
        BoundingBox boundingbox = getStructureBoundingBox(TestInstanceBlockEntity.getStructurePos(pPos), pSize, pRotation);
        clearSpaceForStructure(boundingbox, pLevel);
        pLevel.setBlockAndUpdate(pPos, Blocks.TEST_INSTANCE_BLOCK.defaultBlockState());
        TestInstanceBlockEntity testinstanceblockentity = (TestInstanceBlockEntity)pLevel.getBlockEntity(pPos);
        ResourceKey<GameTestInstance> resourcekey = ResourceKey.create(Registries.TEST_INSTANCE, pId);
        testinstanceblockentity.set(
            new TestInstanceBlockEntity.Data(Optional.of(resourcekey), pSize, pRotation, false, TestInstanceBlockEntity.Status.CLEARED, Optional.empty())
        );
        return testinstanceblockentity;
    }

    public static void clearSpaceForStructure(BoundingBox pBoundingBox, ServerLevel pLevel) {
        int i = pBoundingBox.minY() - 1;
        BoundingBox boundingbox = new BoundingBox(
            pBoundingBox.minX() - 2,
            pBoundingBox.minY() - 3,
            pBoundingBox.minZ() - 3,
            pBoundingBox.maxX() + 3,
            pBoundingBox.maxY() + 20,
            pBoundingBox.maxZ() + 3
        );
        BlockPos.betweenClosedStream(boundingbox).forEach(p_177748_ -> clearBlock(i, p_177748_, pLevel));
        pLevel.getBlockTicks().clearArea(boundingbox);
        pLevel.clearBlockEvents(boundingbox);
        AABB aabb = AABB.of(boundingbox);
        List<Entity> list = pLevel.getEntitiesOfClass(Entity.class, aabb, p_177750_ -> !(p_177750_ instanceof Player));
        list.forEach(Entity::discard);
    }

    public static BlockPos getTransformedFarCorner(BlockPos pPos, Vec3i pOffset, Rotation pRotation) {
        BlockPos blockpos = pPos.offset(pOffset).offset(-1, -1, -1);
        return StructureTemplate.transform(blockpos, Mirror.NONE, pRotation, pPos);
    }

    public static BoundingBox getStructureBoundingBox(BlockPos pPos, Vec3i pOffset, Rotation pRotation) {
        BlockPos blockpos = getTransformedFarCorner(pPos, pOffset, pRotation);
        BoundingBox boundingbox = BoundingBox.fromCorners(pPos, blockpos);
        int i = Math.min(boundingbox.minX(), boundingbox.maxX());
        int j = Math.min(boundingbox.minZ(), boundingbox.maxZ());
        return boundingbox.move(pPos.getX() - i, 0, pPos.getZ() - j);
    }

    public static Optional<BlockPos> findTestContainingPos(BlockPos pPos, int pRadius, ServerLevel pLevel) {
        return findTestBlocks(pPos, pRadius, pLevel).filter(p_177756_ -> doesStructureContain(p_177756_, pPos, pLevel)).findFirst();
    }

    public static Optional<BlockPos> findNearestTest(BlockPos pPos, int pRadius, ServerLevel pLevel) {
        Comparator<BlockPos> comparator = Comparator.comparingInt(p_177759_ -> p_177759_.distManhattan(pPos));
        return findTestBlocks(pPos, pRadius, pLevel).min(comparator);
    }

    public static Stream<BlockPos> findTestBlocks(BlockPos pPos, int pRadius, ServerLevel pLevel) {
        BoundingBox boundingbox = getBoundingBoxAtGround(pPos, pRadius, pLevel);
        return BlockPos.betweenClosedStream(boundingbox).filter(p_389789_ -> pLevel.getBlockState(p_389789_).is(Blocks.TEST_INSTANCE_BLOCK)).map(BlockPos::immutable);
    }

    private static BoundingBox getBoundingBoxAtGround(BlockPos pPos, int pRadius, ServerLevel pLevel) {
        BlockPos blockpos = BlockPos.containing(
            pPos.getX(), pLevel.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, pPos).getY(), pPos.getZ()
        );
        return new BoundingBox(blockpos).inflatedBy(pRadius, 10, pRadius);
    }

    public static Stream<BlockPos> lookedAtTestPos(BlockPos pPos, Entity pEntity, ServerLevel pLevel) {
        int i = 200;
        Vec3 vec3 = pEntity.getEyePosition();
        Vec3 vec31 = vec3.add(pEntity.getLookAngle().scale(200.0));
        return findTestBlocks(pPos, 200, pLevel)
            .map(p_389787_ -> pLevel.getBlockEntity(p_389787_, BlockEntityType.TEST_INSTANCE_BLOCK))
            .flatMap(Optional::stream)
            .filter(p_389792_ -> p_389792_.getStructureBounds().clip(vec3, vec31).isPresent())
            .map(BlockEntity::getBlockPos)
            .sorted(Comparator.comparing(pPos::distSqr))
            .limit(1L);
    }

    private static void clearBlock(int pStructureBlockY, BlockPos pPos, ServerLevel pServerLevel) {
        BlockState blockstate;
        if (pPos.getY() < pStructureBlockY) {
            blockstate = Blocks.STONE.defaultBlockState();
        } else {
            blockstate = Blocks.AIR.defaultBlockState();
        }

        BlockInput blockinput = new BlockInput(blockstate, Collections.emptySet(), null);
        blockinput.place(pServerLevel, pPos, 818);
        pServerLevel.updateNeighborsAt(pPos, blockstate.getBlock());
    }

    private static boolean doesStructureContain(BlockPos pStructureBlockPos, BlockPos pPosToTest, ServerLevel pServerLevel) {
        return pServerLevel.getBlockEntity(pStructureBlockPos) instanceof TestInstanceBlockEntity testinstanceblockentity
            ? testinstanceblockentity.getStructureBoundingBox().isInside(pPosToTest)
            : false;
    }
}
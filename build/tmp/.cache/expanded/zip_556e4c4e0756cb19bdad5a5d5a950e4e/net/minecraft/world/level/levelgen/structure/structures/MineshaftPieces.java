package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.MinecartChest;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;

public class MineshaftPieces {
    private static final int DEFAULT_SHAFT_WIDTH = 3;
    private static final int DEFAULT_SHAFT_HEIGHT = 3;
    private static final int DEFAULT_SHAFT_LENGTH = 5;
    private static final int MAX_PILLAR_HEIGHT = 20;
    private static final int MAX_CHAIN_HEIGHT = 50;
    private static final int MAX_DEPTH = 8;
    public static final int MAGIC_START_Y = 50;

    private static MineshaftPieces.MineShaftPiece createRandomShaftPiece(
        StructurePieceAccessor pPieces,
        RandomSource pRandom,
        int pX,
        int pY,
        int pZ,
        @Nullable Direction pOrientation,
        int pGenDepth,
        MineshaftStructure.Type pType
    ) {
        int i = pRandom.nextInt(100);
        if (i >= 80) {
            BoundingBox boundingbox = MineshaftPieces.MineShaftCrossing.findCrossing(pPieces, pRandom, pX, pY, pZ, pOrientation);
            if (boundingbox != null) {
                return new MineshaftPieces.MineShaftCrossing(pGenDepth, boundingbox, pOrientation, pType);
            }
        } else if (i >= 70) {
            BoundingBox boundingbox1 = MineshaftPieces.MineShaftStairs.findStairs(pPieces, pRandom, pX, pY, pZ, pOrientation);
            if (boundingbox1 != null) {
                return new MineshaftPieces.MineShaftStairs(pGenDepth, boundingbox1, pOrientation, pType);
            }
        } else {
            BoundingBox boundingbox2 = MineshaftPieces.MineShaftCorridor.findCorridorSize(pPieces, pRandom, pX, pY, pZ, pOrientation);
            if (boundingbox2 != null) {
                return new MineshaftPieces.MineShaftCorridor(pGenDepth, pRandom, boundingbox2, pOrientation, pType);
            }
        }

        return null;
    }

    static MineshaftPieces.MineShaftPiece generateAndAddPiece(
        StructurePiece pPiece,
        StructurePieceAccessor pPieces,
        RandomSource pRandom,
        int pX,
        int pY,
        int pZ,
        Direction pDirection,
        int pGenDepth
    ) {
        if (pGenDepth > 8) {
            return null;
        } else if (Math.abs(pX - pPiece.getBoundingBox().minX()) <= 80 && Math.abs(pZ - pPiece.getBoundingBox().minZ()) <= 80) {
            MineshaftStructure.Type mineshaftstructure$type = ((MineshaftPieces.MineShaftPiece)pPiece).type;
            MineshaftPieces.MineShaftPiece mineshaftpieces$mineshaftpiece = createRandomShaftPiece(
                pPieces, pRandom, pX, pY, pZ, pDirection, pGenDepth + 1, mineshaftstructure$type
            );
            if (mineshaftpieces$mineshaftpiece != null) {
                pPieces.addPiece(mineshaftpieces$mineshaftpiece);
                mineshaftpieces$mineshaftpiece.addChildren(pPiece, pPieces, pRandom);
            }

            return mineshaftpieces$mineshaftpiece;
        } else {
            return null;
        }
    }

    public static class MineShaftCorridor extends MineshaftPieces.MineShaftPiece {
        private final boolean hasRails;
        private final boolean spiderCorridor;
        private boolean hasPlacedSpider;
        private final int numSections;

        public MineShaftCorridor(CompoundTag pTag) {
            super(StructurePieceType.MINE_SHAFT_CORRIDOR, pTag);
            this.hasRails = pTag.getBooleanOr("hr", false);
            this.spiderCorridor = pTag.getBooleanOr("sc", false);
            this.hasPlacedSpider = pTag.getBooleanOr("hps", false);
            this.numSections = pTag.getIntOr("Num", 0);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext p_227806_, CompoundTag p_227807_) {
            super.addAdditionalSaveData(p_227806_, p_227807_);
            p_227807_.putBoolean("hr", this.hasRails);
            p_227807_.putBoolean("sc", this.spiderCorridor);
            p_227807_.putBoolean("hps", this.hasPlacedSpider);
            p_227807_.putInt("Num", this.numSections);
        }

        public MineShaftCorridor(int pGenDepth, RandomSource pRandom, BoundingBox pBoundingBox, Direction pOrientation, MineshaftStructure.Type pType) {
            super(StructurePieceType.MINE_SHAFT_CORRIDOR, pGenDepth, pType, pBoundingBox);
            this.setOrientation(pOrientation);
            this.hasRails = pRandom.nextInt(3) == 0;
            this.spiderCorridor = !this.hasRails && pRandom.nextInt(23) == 0;
            if (this.getOrientation().getAxis() == Direction.Axis.Z) {
                this.numSections = pBoundingBox.getZSpan() / 5;
            } else {
                this.numSections = pBoundingBox.getXSpan() / 5;
            }
        }

        @Nullable
        public static BoundingBox findCorridorSize(
            StructurePieceAccessor pPieces, RandomSource pRandom, int pX, int pY, int pZ, Direction pDirection
        ) {
            for (int i = pRandom.nextInt(3) + 2; i > 0; i--) {
                int j = i * 5;

                BoundingBox $$11 = switch (pDirection) {
                    default -> new BoundingBox(0, 0, -(j - 1), 2, 2, 0);
                    case SOUTH -> new BoundingBox(0, 0, 0, 2, 2, j - 1);
                    case WEST -> new BoundingBox(-(j - 1), 0, 0, 0, 2, 2);
                    case EAST -> new BoundingBox(0, 0, 0, j - 1, 2, 2);
                };
                $$11.move(pX, pY, pZ);
                if (pPieces.findCollisionPiece($$11) == null) {
                    return $$11;
                }
            }

            return null;
        }

        @Override
        public void addChildren(StructurePiece p_227795_, StructurePieceAccessor p_227796_, RandomSource p_227797_) {
            int i = this.getGenDepth();
            int j = p_227797_.nextInt(4);
            Direction direction = this.getOrientation();
            if (direction != null) {
                switch (direction) {
                    case NORTH:
                    default:
                        if (j <= 1) {
                            MineshaftPieces.generateAndAddPiece(
                                p_227795_,
                                p_227796_,
                                p_227797_,
                                this.boundingBox.minX(),
                                this.boundingBox.minY() - 1 + p_227797_.nextInt(3),
                                this.boundingBox.minZ() - 1,
                                direction,
                                i
                            );
                        } else if (j == 2) {
                            MineshaftPieces.generateAndAddPiece(
                                p_227795_,
                                p_227796_,
                                p_227797_,
                                this.boundingBox.minX() - 1,
                                this.boundingBox.minY() - 1 + p_227797_.nextInt(3),
                                this.boundingBox.minZ(),
                                Direction.WEST,
                                i
                            );
                        } else {
                            MineshaftPieces.generateAndAddPiece(
                                p_227795_,
                                p_227796_,
                                p_227797_,
                                this.boundingBox.maxX() + 1,
                                this.boundingBox.minY() - 1 + p_227797_.nextInt(3),
                                this.boundingBox.minZ(),
                                Direction.EAST,
                                i
                            );
                        }
                        break;
                    case SOUTH:
                        if (j <= 1) {
                            MineshaftPieces.generateAndAddPiece(
                                p_227795_,
                                p_227796_,
                                p_227797_,
                                this.boundingBox.minX(),
                                this.boundingBox.minY() - 1 + p_227797_.nextInt(3),
                                this.boundingBox.maxZ() + 1,
                                direction,
                                i
                            );
                        } else if (j == 2) {
                            MineshaftPieces.generateAndAddPiece(
                                p_227795_,
                                p_227796_,
                                p_227797_,
                                this.boundingBox.minX() - 1,
                                this.boundingBox.minY() - 1 + p_227797_.nextInt(3),
                                this.boundingBox.maxZ() - 3,
                                Direction.WEST,
                                i
                            );
                        } else {
                            MineshaftPieces.generateAndAddPiece(
                                p_227795_,
                                p_227796_,
                                p_227797_,
                                this.boundingBox.maxX() + 1,
                                this.boundingBox.minY() - 1 + p_227797_.nextInt(3),
                                this.boundingBox.maxZ() - 3,
                                Direction.EAST,
                                i
                            );
                        }
                        break;
                    case WEST:
                        if (j <= 1) {
                            MineshaftPieces.generateAndAddPiece(
                                p_227795_,
                                p_227796_,
                                p_227797_,
                                this.boundingBox.minX() - 1,
                                this.boundingBox.minY() - 1 + p_227797_.nextInt(3),
                                this.boundingBox.minZ(),
                                direction,
                                i
                            );
                        } else if (j == 2) {
                            MineshaftPieces.generateAndAddPiece(
                                p_227795_,
                                p_227796_,
                                p_227797_,
                                this.boundingBox.minX(),
                                this.boundingBox.minY() - 1 + p_227797_.nextInt(3),
                                this.boundingBox.minZ() - 1,
                                Direction.NORTH,
                                i
                            );
                        } else {
                            MineshaftPieces.generateAndAddPiece(
                                p_227795_,
                                p_227796_,
                                p_227797_,
                                this.boundingBox.minX(),
                                this.boundingBox.minY() - 1 + p_227797_.nextInt(3),
                                this.boundingBox.maxZ() + 1,
                                Direction.SOUTH,
                                i
                            );
                        }
                        break;
                    case EAST:
                        if (j <= 1) {
                            MineshaftPieces.generateAndAddPiece(
                                p_227795_,
                                p_227796_,
                                p_227797_,
                                this.boundingBox.maxX() + 1,
                                this.boundingBox.minY() - 1 + p_227797_.nextInt(3),
                                this.boundingBox.minZ(),
                                direction,
                                i
                            );
                        } else if (j == 2) {
                            MineshaftPieces.generateAndAddPiece(
                                p_227795_,
                                p_227796_,
                                p_227797_,
                                this.boundingBox.maxX() - 3,
                                this.boundingBox.minY() - 1 + p_227797_.nextInt(3),
                                this.boundingBox.minZ() - 1,
                                Direction.NORTH,
                                i
                            );
                        } else {
                            MineshaftPieces.generateAndAddPiece(
                                p_227795_,
                                p_227796_,
                                p_227797_,
                                this.boundingBox.maxX() - 3,
                                this.boundingBox.minY() - 1 + p_227797_.nextInt(3),
                                this.boundingBox.maxZ() + 1,
                                Direction.SOUTH,
                                i
                            );
                        }
                }
            }

            if (i < 8) {
                if (direction != Direction.NORTH && direction != Direction.SOUTH) {
                    for (int i1 = this.boundingBox.minX() + 3; i1 + 3 <= this.boundingBox.maxX(); i1 += 5) {
                        int j1 = p_227797_.nextInt(5);
                        if (j1 == 0) {
                            MineshaftPieces.generateAndAddPiece(
                                p_227795_, p_227796_, p_227797_, i1, this.boundingBox.minY(), this.boundingBox.minZ() - 1, Direction.NORTH, i + 1
                            );
                        } else if (j1 == 1) {
                            MineshaftPieces.generateAndAddPiece(
                                p_227795_, p_227796_, p_227797_, i1, this.boundingBox.minY(), this.boundingBox.maxZ() + 1, Direction.SOUTH, i + 1
                            );
                        }
                    }
                } else {
                    for (int k = this.boundingBox.minZ() + 3; k + 3 <= this.boundingBox.maxZ(); k += 5) {
                        int l = p_227797_.nextInt(5);
                        if (l == 0) {
                            MineshaftPieces.generateAndAddPiece(
                                p_227795_, p_227796_, p_227797_, this.boundingBox.minX() - 1, this.boundingBox.minY(), k, Direction.WEST, i + 1
                            );
                        } else if (l == 1) {
                            MineshaftPieces.generateAndAddPiece(
                                p_227795_, p_227796_, p_227797_, this.boundingBox.maxX() + 1, this.boundingBox.minY(), k, Direction.EAST, i + 1
                            );
                        }
                    }
                }
            }
        }

        @Override
        protected boolean createChest(
            WorldGenLevel p_227787_,
            BoundingBox p_227788_,
            RandomSource p_227789_,
            int p_227790_,
            int p_227791_,
            int p_227792_,
            ResourceKey<LootTable> p_336306_
        ) {
            BlockPos blockpos = this.getWorldPos(p_227790_, p_227791_, p_227792_);
            if (p_227788_.isInside(blockpos) && p_227787_.getBlockState(blockpos).isAir() && !p_227787_.getBlockState(blockpos.below()).isAir()) {
                BlockState blockstate = Blocks.RAIL
                    .defaultBlockState()
                    .setValue(RailBlock.SHAPE, p_227789_.nextBoolean() ? RailShape.NORTH_SOUTH : RailShape.EAST_WEST);
                this.placeBlock(p_227787_, blockstate, p_227790_, p_227791_, p_227792_, p_227788_);
                MinecartChest minecartchest = EntityType.CHEST_MINECART.create(p_227787_.getLevel(), EntitySpawnReason.CHUNK_GENERATION);
                if (minecartchest != null) {
                    minecartchest.setInitialPos(blockpos.getX() + 0.5, blockpos.getY() + 0.5, blockpos.getZ() + 0.5);
                    minecartchest.setLootTable(p_336306_, p_227789_.nextLong());
                    p_227787_.addFreshEntity(minecartchest);
                }

                return true;
            } else {
                return false;
            }
        }

        @Override
        public void postProcess(
            WorldGenLevel p_227743_,
            StructureManager p_227744_,
            ChunkGenerator p_227745_,
            RandomSource p_227746_,
            BoundingBox p_227747_,
            ChunkPos p_227748_,
            BlockPos p_227749_
        ) {
            if (!this.isInInvalidLocation(p_227743_, p_227747_)) {
                int i = 0;
                int j = 2;
                int k = 0;
                int l = 2;
                int i1 = this.numSections * 5 - 1;
                BlockState blockstate = this.type.getPlanksState();
                this.generateBox(p_227743_, p_227747_, 0, 0, 0, 2, 1, i1, CAVE_AIR, CAVE_AIR, false);
                this.generateMaybeBox(p_227743_, p_227747_, p_227746_, 0.8F, 0, 2, 0, 2, 2, i1, CAVE_AIR, CAVE_AIR, false, false);
                if (this.spiderCorridor) {
                    this.generateMaybeBox(p_227743_, p_227747_, p_227746_, 0.6F, 0, 0, 0, 2, 1, i1, Blocks.COBWEB.defaultBlockState(), CAVE_AIR, false, true);
                }

                for (int j1 = 0; j1 < this.numSections; j1++) {
                    int k1 = 2 + j1 * 5;
                    this.placeSupport(p_227743_, p_227747_, 0, 0, k1, 2, 2, p_227746_);
                    this.maybePlaceCobWeb(p_227743_, p_227747_, p_227746_, 0.1F, 0, 2, k1 - 1);
                    this.maybePlaceCobWeb(p_227743_, p_227747_, p_227746_, 0.1F, 2, 2, k1 - 1);
                    this.maybePlaceCobWeb(p_227743_, p_227747_, p_227746_, 0.1F, 0, 2, k1 + 1);
                    this.maybePlaceCobWeb(p_227743_, p_227747_, p_227746_, 0.1F, 2, 2, k1 + 1);
                    this.maybePlaceCobWeb(p_227743_, p_227747_, p_227746_, 0.05F, 0, 2, k1 - 2);
                    this.maybePlaceCobWeb(p_227743_, p_227747_, p_227746_, 0.05F, 2, 2, k1 - 2);
                    this.maybePlaceCobWeb(p_227743_, p_227747_, p_227746_, 0.05F, 0, 2, k1 + 2);
                    this.maybePlaceCobWeb(p_227743_, p_227747_, p_227746_, 0.05F, 2, 2, k1 + 2);
                    if (p_227746_.nextInt(100) == 0) {
                        this.createChest(p_227743_, p_227747_, p_227746_, 2, 0, k1 - 1, BuiltInLootTables.ABANDONED_MINESHAFT);
                    }

                    if (p_227746_.nextInt(100) == 0) {
                        this.createChest(p_227743_, p_227747_, p_227746_, 0, 0, k1 + 1, BuiltInLootTables.ABANDONED_MINESHAFT);
                    }

                    if (this.spiderCorridor && !this.hasPlacedSpider) {
                        int l1 = 1;
                        int i2 = k1 - 1 + p_227746_.nextInt(3);
                        BlockPos blockpos = this.getWorldPos(1, 0, i2);
                        if (p_227747_.isInside(blockpos) && this.isInterior(p_227743_, 1, 0, i2, p_227747_)) {
                            this.hasPlacedSpider = true;
                            p_227743_.setBlock(blockpos, Blocks.SPAWNER.defaultBlockState(), 2);
                            if (p_227743_.getBlockEntity(blockpos) instanceof SpawnerBlockEntity spawnerblockentity) {
                                spawnerblockentity.setEntityId(EntityType.CAVE_SPIDER, p_227746_);
                            }
                        }
                    }
                }

                for (int j2 = 0; j2 <= 2; j2++) {
                    for (int l2 = 0; l2 <= i1; l2++) {
                        this.setPlanksBlock(p_227743_, p_227747_, blockstate, j2, -1, l2);
                    }
                }

                int k2 = 2;
                this.placeDoubleLowerOrUpperSupport(p_227743_, p_227747_, 0, -1, 2);
                if (this.numSections > 1) {
                    int i3 = i1 - 2;
                    this.placeDoubleLowerOrUpperSupport(p_227743_, p_227747_, 0, -1, i3);
                }

                if (this.hasRails) {
                    BlockState blockstate1 = Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.NORTH_SOUTH);

                    for (int j3 = 0; j3 <= i1; j3++) {
                        BlockState blockstate2 = this.getBlock(p_227743_, 1, -1, j3, p_227747_);
                        if (!blockstate2.isAir() && blockstate2.isSolidRender()) {
                            float f = this.isInterior(p_227743_, 1, 0, j3, p_227747_) ? 0.7F : 0.9F;
                            this.maybeGenerateBlock(p_227743_, p_227747_, p_227746_, f, 1, 0, j3, blockstate1);
                        }
                    }
                }
            }
        }

        private void placeDoubleLowerOrUpperSupport(WorldGenLevel pLevel, BoundingBox pBox, int pX, int pY, int pZ) {
            BlockState blockstate = this.type.getWoodState();
            BlockState blockstate1 = this.type.getPlanksState();
            if (this.getBlock(pLevel, pX, pY, pZ, pBox).is(blockstate1.getBlock())) {
                this.fillPillarDownOrChainUp(pLevel, blockstate, pX, pY, pZ, pBox);
            }

            if (this.getBlock(pLevel, pX + 2, pY, pZ, pBox).is(blockstate1.getBlock())) {
                this.fillPillarDownOrChainUp(pLevel, blockstate, pX + 2, pY, pZ, pBox);
            }
        }

        @Override
        protected void fillColumnDown(WorldGenLevel p_227813_, BlockState p_227814_, int p_227815_, int p_227816_, int p_227817_, BoundingBox p_227818_) {
            BlockPos.MutableBlockPos blockpos$mutableblockpos = this.getWorldPos(p_227815_, p_227816_, p_227817_);
            if (p_227818_.isInside(blockpos$mutableblockpos)) {
                int i = blockpos$mutableblockpos.getY();

                while (this.isReplaceableByStructures(p_227813_.getBlockState(blockpos$mutableblockpos)) && blockpos$mutableblockpos.getY() > p_227813_.getMinY() + 1) {
                    blockpos$mutableblockpos.move(Direction.DOWN);
                }

                if (this.canPlaceColumnOnTopOf(p_227813_, blockpos$mutableblockpos, p_227813_.getBlockState(blockpos$mutableblockpos))) {
                    while (blockpos$mutableblockpos.getY() < i) {
                        blockpos$mutableblockpos.move(Direction.UP);
                        p_227813_.setBlock(blockpos$mutableblockpos, p_227814_, 2);
                    }
                }
            }
        }

        protected void fillPillarDownOrChainUp(WorldGenLevel pLevel, BlockState pState, int pX, int pY, int pZ, BoundingBox pBox) {
            BlockPos.MutableBlockPos blockpos$mutableblockpos = this.getWorldPos(pX, pY, pZ);
            if (pBox.isInside(blockpos$mutableblockpos)) {
                int i = blockpos$mutableblockpos.getY();
                int j = 1;
                boolean flag = true;

                for (boolean flag1 = true; flag || flag1; j++) {
                    if (flag) {
                        blockpos$mutableblockpos.setY(i - j);
                        BlockState blockstate = pLevel.getBlockState(blockpos$mutableblockpos);
                        boolean flag2 = this.isReplaceableByStructures(blockstate) && !blockstate.is(Blocks.LAVA);
                        if (!flag2 && this.canPlaceColumnOnTopOf(pLevel, blockpos$mutableblockpos, blockstate)) {
                            fillColumnBetween(pLevel, pState, blockpos$mutableblockpos, i - j + 1, i);
                            return;
                        }

                        flag = j <= 20 && flag2 && blockpos$mutableblockpos.getY() > pLevel.getMinY() + 1;
                    }

                    if (flag1) {
                        blockpos$mutableblockpos.setY(i + j);
                        BlockState blockstate1 = pLevel.getBlockState(blockpos$mutableblockpos);
                        boolean flag3 = this.isReplaceableByStructures(blockstate1);
                        if (!flag3 && this.canHangChainBelow(pLevel, blockpos$mutableblockpos, blockstate1)) {
                            pLevel.setBlock(blockpos$mutableblockpos.setY(i + 1), this.type.getFenceState(), 2);
                            fillColumnBetween(pLevel, Blocks.CHAIN.defaultBlockState(), blockpos$mutableblockpos, i + 2, i + j);
                            return;
                        }

                        flag1 = j <= 50 && flag3 && blockpos$mutableblockpos.getY() < pLevel.getMaxY();
                    }
                }
            }
        }

        private static void fillColumnBetween(WorldGenLevel pLevel, BlockState pState, BlockPos.MutableBlockPos pPos, int pMinY, int pMaxY) {
            for (int i = pMinY; i < pMaxY; i++) {
                pLevel.setBlock(pPos.setY(i), pState, 2);
            }
        }

        private boolean canPlaceColumnOnTopOf(LevelReader pLevel, BlockPos pPos, BlockState pState) {
            return pState.isFaceSturdy(pLevel, pPos, Direction.UP);
        }

        private boolean canHangChainBelow(LevelReader pLevel, BlockPos pPos, BlockState pState) {
            return Block.canSupportCenter(pLevel, pPos, Direction.DOWN) && !(pState.getBlock() instanceof FallingBlock);
        }

        private void placeSupport(
            WorldGenLevel pLevel, BoundingBox pBox, int pMinX, int pMinY, int pZ, int pMaxY, int pMaxX, RandomSource pRandom
        ) {
            if (this.isSupportingBox(pLevel, pBox, pMinX, pMaxX, pMaxY, pZ)) {
                BlockState blockstate = this.type.getPlanksState();
                BlockState blockstate1 = this.type.getFenceState();
                this.generateBox(
                    pLevel,
                    pBox,
                    pMinX,
                    pMinY,
                    pZ,
                    pMinX,
                    pMaxY - 1,
                    pZ,
                    blockstate1.setValue(FenceBlock.WEST, true),
                    CAVE_AIR,
                    false
                );
                this.generateBox(
                    pLevel,
                    pBox,
                    pMaxX,
                    pMinY,
                    pZ,
                    pMaxX,
                    pMaxY - 1,
                    pZ,
                    blockstate1.setValue(FenceBlock.EAST, true),
                    CAVE_AIR,
                    false
                );
                if (pRandom.nextInt(4) == 0) {
                    this.generateBox(pLevel, pBox, pMinX, pMaxY, pZ, pMinX, pMaxY, pZ, blockstate, CAVE_AIR, false);
                    this.generateBox(pLevel, pBox, pMaxX, pMaxY, pZ, pMaxX, pMaxY, pZ, blockstate, CAVE_AIR, false);
                } else {
                    this.generateBox(pLevel, pBox, pMinX, pMaxY, pZ, pMaxX, pMaxY, pZ, blockstate, CAVE_AIR, false);
                    this.maybeGenerateBlock(
                        pLevel,
                        pBox,
                        pRandom,
                        0.05F,
                        pMinX + 1,
                        pMaxY,
                        pZ - 1,
                        Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.SOUTH)
                    );
                    this.maybeGenerateBlock(
                        pLevel,
                        pBox,
                        pRandom,
                        0.05F,
                        pMinX + 1,
                        pMaxY,
                        pZ + 1,
                        Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.NORTH)
                    );
                }
            }
        }

        private void maybePlaceCobWeb(
            WorldGenLevel pLevel, BoundingBox pBox, RandomSource pRandom, float pChance, int pX, int pY, int pZ
        ) {
            if (this.isInterior(pLevel, pX, pY, pZ, pBox)
                && pRandom.nextFloat() < pChance
                && this.hasSturdyNeighbours(pLevel, pBox, pX, pY, pZ, 2)) {
                this.placeBlock(pLevel, Blocks.COBWEB.defaultBlockState(), pX, pY, pZ, pBox);
            }
        }

        private boolean hasSturdyNeighbours(WorldGenLevel pLevel, BoundingBox pBox, int pX, int pY, int pZ, int pRequired) {
            BlockPos.MutableBlockPos blockpos$mutableblockpos = this.getWorldPos(pX, pY, pZ);
            int i = 0;

            for (Direction direction : Direction.values()) {
                blockpos$mutableblockpos.move(direction);
                if (pBox.isInside(blockpos$mutableblockpos)
                    && pLevel.getBlockState(blockpos$mutableblockpos).isFaceSturdy(pLevel, blockpos$mutableblockpos, direction.getOpposite())) {
                    if (++i >= pRequired) {
                        return true;
                    }
                }

                blockpos$mutableblockpos.move(direction.getOpposite());
            }

            return false;
        }
    }

    public static class MineShaftCrossing extends MineshaftPieces.MineShaftPiece {
        private final Direction direction;
        private final boolean isTwoFloored;

        public MineShaftCrossing(CompoundTag pTag) {
            super(StructurePieceType.MINE_SHAFT_CROSSING, pTag);
            this.isTwoFloored = pTag.getBooleanOr("tf", false);
            this.direction = pTag.read("D", Direction.LEGACY_ID_CODEC_2D).orElse(Direction.SOUTH);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext p_227862_, CompoundTag p_227863_) {
            super.addAdditionalSaveData(p_227862_, p_227863_);
            p_227863_.putBoolean("tf", this.isTwoFloored);
            p_227863_.store("D", Direction.LEGACY_ID_CODEC_2D, this.direction);
        }

        public MineShaftCrossing(int pGenDepth, BoundingBox pBoundingBox, @Nullable Direction pDirection, MineshaftStructure.Type pType) {
            super(StructurePieceType.MINE_SHAFT_CROSSING, pGenDepth, pType, pBoundingBox);
            this.direction = pDirection;
            this.isTwoFloored = pBoundingBox.getYSpan() > 3;
        }

        @Nullable
        public static BoundingBox findCrossing(
            StructurePieceAccessor pPieces, RandomSource pRandom, int pX, int pY, int pZ, Direction pDirection
        ) {
            int i;
            if (pRandom.nextInt(4) == 0) {
                i = 6;
            } else {
                i = 2;
            }
            BoundingBox $$11 = switch (pDirection) {
                default -> new BoundingBox(-1, 0, -4, 3, i, 0);
                case SOUTH -> new BoundingBox(-1, 0, 0, 3, i, 4);
                case WEST -> new BoundingBox(-4, 0, -1, 0, i, 3);
                case EAST -> new BoundingBox(0, 0, -1, 4, i, 3);
            };
            $$11.move(pX, pY, pZ);
            return pPieces.findCollisionPiece($$11) != null ? null : $$11;
        }

        @Override
        public void addChildren(StructurePiece p_227851_, StructurePieceAccessor p_227852_, RandomSource p_227853_) {
            int i = this.getGenDepth();
            switch (this.direction) {
                case NORTH:
                default:
                    MineshaftPieces.generateAndAddPiece(
                        p_227851_,
                        p_227852_,
                        p_227853_,
                        this.boundingBox.minX() + 1,
                        this.boundingBox.minY(),
                        this.boundingBox.minZ() - 1,
                        Direction.NORTH,
                        i
                    );
                    MineshaftPieces.generateAndAddPiece(
                        p_227851_,
                        p_227852_,
                        p_227853_,
                        this.boundingBox.minX() - 1,
                        this.boundingBox.minY(),
                        this.boundingBox.minZ() + 1,
                        Direction.WEST,
                        i
                    );
                    MineshaftPieces.generateAndAddPiece(
                        p_227851_,
                        p_227852_,
                        p_227853_,
                        this.boundingBox.maxX() + 1,
                        this.boundingBox.minY(),
                        this.boundingBox.minZ() + 1,
                        Direction.EAST,
                        i
                    );
                    break;
                case SOUTH:
                    MineshaftPieces.generateAndAddPiece(
                        p_227851_,
                        p_227852_,
                        p_227853_,
                        this.boundingBox.minX() + 1,
                        this.boundingBox.minY(),
                        this.boundingBox.maxZ() + 1,
                        Direction.SOUTH,
                        i
                    );
                    MineshaftPieces.generateAndAddPiece(
                        p_227851_,
                        p_227852_,
                        p_227853_,
                        this.boundingBox.minX() - 1,
                        this.boundingBox.minY(),
                        this.boundingBox.minZ() + 1,
                        Direction.WEST,
                        i
                    );
                    MineshaftPieces.generateAndAddPiece(
                        p_227851_,
                        p_227852_,
                        p_227853_,
                        this.boundingBox.maxX() + 1,
                        this.boundingBox.minY(),
                        this.boundingBox.minZ() + 1,
                        Direction.EAST,
                        i
                    );
                    break;
                case WEST:
                    MineshaftPieces.generateAndAddPiece(
                        p_227851_,
                        p_227852_,
                        p_227853_,
                        this.boundingBox.minX() + 1,
                        this.boundingBox.minY(),
                        this.boundingBox.minZ() - 1,
                        Direction.NORTH,
                        i
                    );
                    MineshaftPieces.generateAndAddPiece(
                        p_227851_,
                        p_227852_,
                        p_227853_,
                        this.boundingBox.minX() + 1,
                        this.boundingBox.minY(),
                        this.boundingBox.maxZ() + 1,
                        Direction.SOUTH,
                        i
                    );
                    MineshaftPieces.generateAndAddPiece(
                        p_227851_,
                        p_227852_,
                        p_227853_,
                        this.boundingBox.minX() - 1,
                        this.boundingBox.minY(),
                        this.boundingBox.minZ() + 1,
                        Direction.WEST,
                        i
                    );
                    break;
                case EAST:
                    MineshaftPieces.generateAndAddPiece(
                        p_227851_,
                        p_227852_,
                        p_227853_,
                        this.boundingBox.minX() + 1,
                        this.boundingBox.minY(),
                        this.boundingBox.minZ() - 1,
                        Direction.NORTH,
                        i
                    );
                    MineshaftPieces.generateAndAddPiece(
                        p_227851_,
                        p_227852_,
                        p_227853_,
                        this.boundingBox.minX() + 1,
                        this.boundingBox.minY(),
                        this.boundingBox.maxZ() + 1,
                        Direction.SOUTH,
                        i
                    );
                    MineshaftPieces.generateAndAddPiece(
                        p_227851_,
                        p_227852_,
                        p_227853_,
                        this.boundingBox.maxX() + 1,
                        this.boundingBox.minY(),
                        this.boundingBox.minZ() + 1,
                        Direction.EAST,
                        i
                    );
            }

            if (this.isTwoFloored) {
                if (p_227853_.nextBoolean()) {
                    MineshaftPieces.generateAndAddPiece(
                        p_227851_,
                        p_227852_,
                        p_227853_,
                        this.boundingBox.minX() + 1,
                        this.boundingBox.minY() + 3 + 1,
                        this.boundingBox.minZ() - 1,
                        Direction.NORTH,
                        i
                    );
                }

                if (p_227853_.nextBoolean()) {
                    MineshaftPieces.generateAndAddPiece(
                        p_227851_,
                        p_227852_,
                        p_227853_,
                        this.boundingBox.minX() - 1,
                        this.boundingBox.minY() + 3 + 1,
                        this.boundingBox.minZ() + 1,
                        Direction.WEST,
                        i
                    );
                }

                if (p_227853_.nextBoolean()) {
                    MineshaftPieces.generateAndAddPiece(
                        p_227851_,
                        p_227852_,
                        p_227853_,
                        this.boundingBox.maxX() + 1,
                        this.boundingBox.minY() + 3 + 1,
                        this.boundingBox.minZ() + 1,
                        Direction.EAST,
                        i
                    );
                }

                if (p_227853_.nextBoolean()) {
                    MineshaftPieces.generateAndAddPiece(
                        p_227851_,
                        p_227852_,
                        p_227853_,
                        this.boundingBox.minX() + 1,
                        this.boundingBox.minY() + 3 + 1,
                        this.boundingBox.maxZ() + 1,
                        Direction.SOUTH,
                        i
                    );
                }
            }
        }

        @Override
        public void postProcess(
            WorldGenLevel p_227836_,
            StructureManager p_227837_,
            ChunkGenerator p_227838_,
            RandomSource p_227839_,
            BoundingBox p_227840_,
            ChunkPos p_227841_,
            BlockPos p_227842_
        ) {
            if (!this.isInInvalidLocation(p_227836_, p_227840_)) {
                BlockState blockstate = this.type.getPlanksState();
                if (this.isTwoFloored) {
                    this.generateBox(
                        p_227836_,
                        p_227840_,
                        this.boundingBox.minX() + 1,
                        this.boundingBox.minY(),
                        this.boundingBox.minZ(),
                        this.boundingBox.maxX() - 1,
                        this.boundingBox.minY() + 3 - 1,
                        this.boundingBox.maxZ(),
                        CAVE_AIR,
                        CAVE_AIR,
                        false
                    );
                    this.generateBox(
                        p_227836_,
                        p_227840_,
                        this.boundingBox.minX(),
                        this.boundingBox.minY(),
                        this.boundingBox.minZ() + 1,
                        this.boundingBox.maxX(),
                        this.boundingBox.minY() + 3 - 1,
                        this.boundingBox.maxZ() - 1,
                        CAVE_AIR,
                        CAVE_AIR,
                        false
                    );
                    this.generateBox(
                        p_227836_,
                        p_227840_,
                        this.boundingBox.minX() + 1,
                        this.boundingBox.maxY() - 2,
                        this.boundingBox.minZ(),
                        this.boundingBox.maxX() - 1,
                        this.boundingBox.maxY(),
                        this.boundingBox.maxZ(),
                        CAVE_AIR,
                        CAVE_AIR,
                        false
                    );
                    this.generateBox(
                        p_227836_,
                        p_227840_,
                        this.boundingBox.minX(),
                        this.boundingBox.maxY() - 2,
                        this.boundingBox.minZ() + 1,
                        this.boundingBox.maxX(),
                        this.boundingBox.maxY(),
                        this.boundingBox.maxZ() - 1,
                        CAVE_AIR,
                        CAVE_AIR,
                        false
                    );
                    this.generateBox(
                        p_227836_,
                        p_227840_,
                        this.boundingBox.minX() + 1,
                        this.boundingBox.minY() + 3,
                        this.boundingBox.minZ() + 1,
                        this.boundingBox.maxX() - 1,
                        this.boundingBox.minY() + 3,
                        this.boundingBox.maxZ() - 1,
                        CAVE_AIR,
                        CAVE_AIR,
                        false
                    );
                } else {
                    this.generateBox(
                        p_227836_,
                        p_227840_,
                        this.boundingBox.minX() + 1,
                        this.boundingBox.minY(),
                        this.boundingBox.minZ(),
                        this.boundingBox.maxX() - 1,
                        this.boundingBox.maxY(),
                        this.boundingBox.maxZ(),
                        CAVE_AIR,
                        CAVE_AIR,
                        false
                    );
                    this.generateBox(
                        p_227836_,
                        p_227840_,
                        this.boundingBox.minX(),
                        this.boundingBox.minY(),
                        this.boundingBox.minZ() + 1,
                        this.boundingBox.maxX(),
                        this.boundingBox.maxY(),
                        this.boundingBox.maxZ() - 1,
                        CAVE_AIR,
                        CAVE_AIR,
                        false
                    );
                }

                this.placeSupportPillar(
                    p_227836_, p_227840_, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, this.boundingBox.maxY()
                );
                this.placeSupportPillar(
                    p_227836_, p_227840_, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.maxZ() - 1, this.boundingBox.maxY()
                );
                this.placeSupportPillar(
                    p_227836_, p_227840_, this.boundingBox.maxX() - 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, this.boundingBox.maxY()
                );
                this.placeSupportPillar(
                    p_227836_, p_227840_, this.boundingBox.maxX() - 1, this.boundingBox.minY(), this.boundingBox.maxZ() - 1, this.boundingBox.maxY()
                );
                int i = this.boundingBox.minY() - 1;

                for (int j = this.boundingBox.minX(); j <= this.boundingBox.maxX(); j++) {
                    for (int k = this.boundingBox.minZ(); k <= this.boundingBox.maxZ(); k++) {
                        this.setPlanksBlock(p_227836_, p_227840_, blockstate, j, i, k);
                    }
                }
            }
        }

        private void placeSupportPillar(WorldGenLevel pLevel, BoundingBox pBox, int pX, int pY, int pZ, int pMaxY) {
            if (!this.getBlock(pLevel, pX, pMaxY + 1, pZ, pBox).isAir()) {
                this.generateBox(
                    pLevel, pBox, pX, pY, pZ, pX, pMaxY, pZ, this.type.getPlanksState(), CAVE_AIR, false
                );
            }
        }
    }

    abstract static class MineShaftPiece extends StructurePiece {
        protected MineshaftStructure.Type type;

        public MineShaftPiece(StructurePieceType pStructurePieceType, int pGenDepth, MineshaftStructure.Type pType, BoundingBox pBoundingBox) {
            super(pStructurePieceType, pGenDepth, pBoundingBox);
            this.type = pType;
        }

        public MineShaftPiece(StructurePieceType p_227872_, CompoundTag p_227873_) {
            super(p_227872_, p_227873_);
            this.type = MineshaftStructure.Type.byId(p_227873_.getIntOr("MST", 0));
        }

        @Override
        protected boolean canBeReplaced(LevelReader p_227885_, int p_227886_, int p_227887_, int p_227888_, BoundingBox p_227889_) {
            BlockState blockstate = this.getBlock(p_227885_, p_227886_, p_227887_, p_227888_, p_227889_);
            return !blockstate.is(this.type.getPlanksState().getBlock())
                && !blockstate.is(this.type.getWoodState().getBlock())
                && !blockstate.is(this.type.getFenceState().getBlock())
                && !blockstate.is(Blocks.CHAIN);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext p_227898_, CompoundTag p_227899_) {
            p_227899_.putInt("MST", this.type.ordinal());
        }

        protected boolean isSupportingBox(BlockGetter pLevel, BoundingBox pBox, int pXStart, int pXEnd, int pY, int pZ) {
            for (int i = pXStart; i <= pXEnd; i++) {
                if (this.getBlock(pLevel, i, pY + 1, pZ, pBox).isAir()) {
                    return false;
                }
            }

            return true;
        }

        protected boolean isInInvalidLocation(LevelAccessor pLevel, BoundingBox pBoundingBox) {
            int i = Math.max(this.boundingBox.minX() - 1, pBoundingBox.minX());
            int j = Math.max(this.boundingBox.minY() - 1, pBoundingBox.minY());
            int k = Math.max(this.boundingBox.minZ() - 1, pBoundingBox.minZ());
            int l = Math.min(this.boundingBox.maxX() + 1, pBoundingBox.maxX());
            int i1 = Math.min(this.boundingBox.maxY() + 1, pBoundingBox.maxY());
            int j1 = Math.min(this.boundingBox.maxZ() + 1, pBoundingBox.maxZ());
            BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos((i + l) / 2, (j + i1) / 2, (k + j1) / 2);
            if (pLevel.getBiome(blockpos$mutableblockpos).is(BiomeTags.MINESHAFT_BLOCKING)) {
                return true;
            } else {
                for (int k1 = i; k1 <= l; k1++) {
                    for (int l1 = k; l1 <= j1; l1++) {
                        if (pLevel.getBlockState(blockpos$mutableblockpos.set(k1, j, l1)).liquid()) {
                            return true;
                        }

                        if (pLevel.getBlockState(blockpos$mutableblockpos.set(k1, i1, l1)).liquid()) {
                            return true;
                        }
                    }
                }

                for (int i2 = i; i2 <= l; i2++) {
                    for (int k2 = j; k2 <= i1; k2++) {
                        if (pLevel.getBlockState(blockpos$mutableblockpos.set(i2, k2, k)).liquid()) {
                            return true;
                        }

                        if (pLevel.getBlockState(blockpos$mutableblockpos.set(i2, k2, j1)).liquid()) {
                            return true;
                        }
                    }
                }

                for (int j2 = k; j2 <= j1; j2++) {
                    for (int l2 = j; l2 <= i1; l2++) {
                        if (pLevel.getBlockState(blockpos$mutableblockpos.set(i, l2, j2)).liquid()) {
                            return true;
                        }

                        if (pLevel.getBlockState(blockpos$mutableblockpos.set(l, l2, j2)).liquid()) {
                            return true;
                        }
                    }
                }

                return false;
            }
        }

        protected void setPlanksBlock(WorldGenLevel pLevel, BoundingBox pBox, BlockState pPlankState, int pX, int pY, int pZ) {
            if (this.isInterior(pLevel, pX, pY, pZ, pBox)) {
                BlockPos blockpos = this.getWorldPos(pX, pY, pZ);
                BlockState blockstate = pLevel.getBlockState(blockpos);
                if (!blockstate.isFaceSturdy(pLevel, blockpos, Direction.UP)) {
                    pLevel.setBlock(blockpos, pPlankState, 2);
                }
            }
        }
    }

    public static class MineShaftRoom extends MineshaftPieces.MineShaftPiece {
        private final List<BoundingBox> childEntranceBoxes = Lists.newLinkedList();

        public MineShaftRoom(int pGenDepth, RandomSource pRandom, int pX, int pZ, MineshaftStructure.Type pType) {
            super(
                StructurePieceType.MINE_SHAFT_ROOM,
                pGenDepth,
                pType,
                new BoundingBox(
                    pX, 50, pZ, pX + 7 + pRandom.nextInt(6), 54 + pRandom.nextInt(6), pZ + 7 + pRandom.nextInt(6)
                )
            );
            this.type = pType;
        }

        public MineShaftRoom(CompoundTag pTag) {
            super(StructurePieceType.MINE_SHAFT_ROOM, pTag);
            this.childEntranceBoxes.addAll(pTag.read("Entrances", BoundingBox.CODEC.listOf()).orElse(List.of()));
        }

        @Override
        public void addChildren(StructurePiece p_227922_, StructurePieceAccessor p_227923_, RandomSource p_227924_) {
            int i = this.getGenDepth();
            int k = this.boundingBox.getYSpan() - 3 - 1;
            if (k <= 0) {
                k = 1;
            }

            int j = 0;

            while (j < this.boundingBox.getXSpan()) {
                j += p_227924_.nextInt(this.boundingBox.getXSpan());
                if (j + 3 > this.boundingBox.getXSpan()) {
                    break;
                }

                MineshaftPieces.MineShaftPiece mineshaftpieces$mineshaftpiece = MineshaftPieces.generateAndAddPiece(
                    p_227922_,
                    p_227923_,
                    p_227924_,
                    this.boundingBox.minX() + j,
                    this.boundingBox.minY() + p_227924_.nextInt(k) + 1,
                    this.boundingBox.minZ() - 1,
                    Direction.NORTH,
                    i
                );
                if (mineshaftpieces$mineshaftpiece != null) {
                    BoundingBox boundingbox = mineshaftpieces$mineshaftpiece.getBoundingBox();
                    this.childEntranceBoxes
                        .add(
                            new BoundingBox(
                                boundingbox.minX(),
                                boundingbox.minY(),
                                this.boundingBox.minZ(),
                                boundingbox.maxX(),
                                boundingbox.maxY(),
                                this.boundingBox.minZ() + 1
                            )
                        );
                }

                j += 4;
            }

            j = 0;

            while (j < this.boundingBox.getXSpan()) {
                j += p_227924_.nextInt(this.boundingBox.getXSpan());
                if (j + 3 > this.boundingBox.getXSpan()) {
                    break;
                }

                MineshaftPieces.MineShaftPiece mineshaftpieces$mineshaftpiece1 = MineshaftPieces.generateAndAddPiece(
                    p_227922_,
                    p_227923_,
                    p_227924_,
                    this.boundingBox.minX() + j,
                    this.boundingBox.minY() + p_227924_.nextInt(k) + 1,
                    this.boundingBox.maxZ() + 1,
                    Direction.SOUTH,
                    i
                );
                if (mineshaftpieces$mineshaftpiece1 != null) {
                    BoundingBox boundingbox1 = mineshaftpieces$mineshaftpiece1.getBoundingBox();
                    this.childEntranceBoxes
                        .add(
                            new BoundingBox(
                                boundingbox1.minX(),
                                boundingbox1.minY(),
                                this.boundingBox.maxZ() - 1,
                                boundingbox1.maxX(),
                                boundingbox1.maxY(),
                                this.boundingBox.maxZ()
                            )
                        );
                }

                j += 4;
            }

            j = 0;

            while (j < this.boundingBox.getZSpan()) {
                j += p_227924_.nextInt(this.boundingBox.getZSpan());
                if (j + 3 > this.boundingBox.getZSpan()) {
                    break;
                }

                MineshaftPieces.MineShaftPiece mineshaftpieces$mineshaftpiece2 = MineshaftPieces.generateAndAddPiece(
                    p_227922_,
                    p_227923_,
                    p_227924_,
                    this.boundingBox.minX() - 1,
                    this.boundingBox.minY() + p_227924_.nextInt(k) + 1,
                    this.boundingBox.minZ() + j,
                    Direction.WEST,
                    i
                );
                if (mineshaftpieces$mineshaftpiece2 != null) {
                    BoundingBox boundingbox2 = mineshaftpieces$mineshaftpiece2.getBoundingBox();
                    this.childEntranceBoxes
                        .add(
                            new BoundingBox(
                                this.boundingBox.minX(),
                                boundingbox2.minY(),
                                boundingbox2.minZ(),
                                this.boundingBox.minX() + 1,
                                boundingbox2.maxY(),
                                boundingbox2.maxZ()
                            )
                        );
                }

                j += 4;
            }

            j = 0;

            while (j < this.boundingBox.getZSpan()) {
                j += p_227924_.nextInt(this.boundingBox.getZSpan());
                if (j + 3 > this.boundingBox.getZSpan()) {
                    break;
                }

                StructurePiece structurepiece = MineshaftPieces.generateAndAddPiece(
                    p_227922_,
                    p_227923_,
                    p_227924_,
                    this.boundingBox.maxX() + 1,
                    this.boundingBox.minY() + p_227924_.nextInt(k) + 1,
                    this.boundingBox.minZ() + j,
                    Direction.EAST,
                    i
                );
                if (structurepiece != null) {
                    BoundingBox boundingbox3 = structurepiece.getBoundingBox();
                    this.childEntranceBoxes
                        .add(
                            new BoundingBox(
                                this.boundingBox.maxX() - 1,
                                boundingbox3.minY(),
                                boundingbox3.minZ(),
                                this.boundingBox.maxX(),
                                boundingbox3.maxY(),
                                boundingbox3.maxZ()
                            )
                        );
                }

                j += 4;
            }
        }

        @Override
        public void postProcess(
            WorldGenLevel p_227914_,
            StructureManager p_227915_,
            ChunkGenerator p_227916_,
            RandomSource p_227917_,
            BoundingBox p_227918_,
            ChunkPos p_227919_,
            BlockPos p_227920_
        ) {
            if (!this.isInInvalidLocation(p_227914_, p_227918_)) {
                this.generateBox(
                    p_227914_,
                    p_227918_,
                    this.boundingBox.minX(),
                    this.boundingBox.minY() + 1,
                    this.boundingBox.minZ(),
                    this.boundingBox.maxX(),
                    Math.min(this.boundingBox.minY() + 3, this.boundingBox.maxY()),
                    this.boundingBox.maxZ(),
                    CAVE_AIR,
                    CAVE_AIR,
                    false
                );

                for (BoundingBox boundingbox : this.childEntranceBoxes) {
                    this.generateBox(
                        p_227914_,
                        p_227918_,
                        boundingbox.minX(),
                        boundingbox.maxY() - 2,
                        boundingbox.minZ(),
                        boundingbox.maxX(),
                        boundingbox.maxY(),
                        boundingbox.maxZ(),
                        CAVE_AIR,
                        CAVE_AIR,
                        false
                    );
                }

                this.generateUpperHalfSphere(
                    p_227914_,
                    p_227918_,
                    this.boundingBox.minX(),
                    this.boundingBox.minY() + 4,
                    this.boundingBox.minZ(),
                    this.boundingBox.maxX(),
                    this.boundingBox.maxY(),
                    this.boundingBox.maxZ(),
                    CAVE_AIR,
                    false
                );
            }
        }

        @Override
        public void move(int p_227910_, int p_227911_, int p_227912_) {
            super.move(p_227910_, p_227911_, p_227912_);

            for (BoundingBox boundingbox : this.childEntranceBoxes) {
                boundingbox.move(p_227910_, p_227911_, p_227912_);
            }
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext p_227926_, CompoundTag p_227927_) {
            super.addAdditionalSaveData(p_227926_, p_227927_);
            p_227927_.store("Entrances", BoundingBox.CODEC.listOf(), this.childEntranceBoxes);
        }
    }

    public static class MineShaftStairs extends MineshaftPieces.MineShaftPiece {
        public MineShaftStairs(int pGenDepth, BoundingBox pBoundingBox, Direction pOrientation, MineshaftStructure.Type pType) {
            super(StructurePieceType.MINE_SHAFT_STAIRS, pGenDepth, pType, pBoundingBox);
            this.setOrientation(pOrientation);
        }

        public MineShaftStairs(CompoundTag pTag) {
            super(StructurePieceType.MINE_SHAFT_STAIRS, pTag);
        }

        @Nullable
        public static BoundingBox findStairs(
            StructurePieceAccessor pPieces, RandomSource pRandom, int pX, int pY, int pZ, Direction pDirection
        ) {
            BoundingBox $$9 = switch (pDirection) {
                default -> new BoundingBox(0, -5, -8, 2, 2, 0);
                case SOUTH -> new BoundingBox(0, -5, 0, 2, 2, 8);
                case WEST -> new BoundingBox(-8, -5, 0, 0, 2, 2);
                case EAST -> new BoundingBox(0, -5, 0, 8, 2, 2);
            };
            $$9.move(pX, pY, pZ);
            return pPieces.findCollisionPiece($$9) != null ? null : $$9;
        }

        @Override
        public void addChildren(StructurePiece p_227947_, StructurePieceAccessor p_227948_, RandomSource p_227949_) {
            int i = this.getGenDepth();
            Direction direction = this.getOrientation();
            if (direction != null) {
                switch (direction) {
                    case NORTH:
                    default:
                        MineshaftPieces.generateAndAddPiece(
                            p_227947_,
                            p_227948_,
                            p_227949_,
                            this.boundingBox.minX(),
                            this.boundingBox.minY(),
                            this.boundingBox.minZ() - 1,
                            Direction.NORTH,
                            i
                        );
                        break;
                    case SOUTH:
                        MineshaftPieces.generateAndAddPiece(
                            p_227947_,
                            p_227948_,
                            p_227949_,
                            this.boundingBox.minX(),
                            this.boundingBox.minY(),
                            this.boundingBox.maxZ() + 1,
                            Direction.SOUTH,
                            i
                        );
                        break;
                    case WEST:
                        MineshaftPieces.generateAndAddPiece(
                            p_227947_,
                            p_227948_,
                            p_227949_,
                            this.boundingBox.minX() - 1,
                            this.boundingBox.minY(),
                            this.boundingBox.minZ(),
                            Direction.WEST,
                            i
                        );
                        break;
                    case EAST:
                        MineshaftPieces.generateAndAddPiece(
                            p_227947_,
                            p_227948_,
                            p_227949_,
                            this.boundingBox.maxX() + 1,
                            this.boundingBox.minY(),
                            this.boundingBox.minZ(),
                            Direction.EAST,
                            i
                        );
                }
            }
        }

        @Override
        public void postProcess(
            WorldGenLevel p_227939_,
            StructureManager p_227940_,
            ChunkGenerator p_227941_,
            RandomSource p_227942_,
            BoundingBox p_227943_,
            ChunkPos p_227944_,
            BlockPos p_227945_
        ) {
            if (!this.isInInvalidLocation(p_227939_, p_227943_)) {
                this.generateBox(p_227939_, p_227943_, 0, 5, 0, 2, 7, 1, CAVE_AIR, CAVE_AIR, false);
                this.generateBox(p_227939_, p_227943_, 0, 0, 7, 2, 2, 8, CAVE_AIR, CAVE_AIR, false);

                for (int i = 0; i < 5; i++) {
                    this.generateBox(p_227939_, p_227943_, 0, 5 - i - (i < 4 ? 1 : 0), 2 + i, 2, 7 - i, 2 + i, CAVE_AIR, CAVE_AIR, false);
                }
            }
        }
    }
}
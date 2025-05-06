package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class StrongholdPieces {
    private static final int SMALL_DOOR_WIDTH = 3;
    private static final int SMALL_DOOR_HEIGHT = 3;
    private static final int MAX_DEPTH = 50;
    private static final int LOWEST_Y_POSITION = 10;
    private static final boolean CHECK_AIR = true;
    public static final int MAGIC_START_Y = 64;
    private static final StrongholdPieces.PieceWeight[] STRONGHOLD_PIECE_WEIGHTS = new StrongholdPieces.PieceWeight[]{
        new StrongholdPieces.PieceWeight(StrongholdPieces.Straight.class, 40, 0),
        new StrongholdPieces.PieceWeight(StrongholdPieces.PrisonHall.class, 5, 5),
        new StrongholdPieces.PieceWeight(StrongholdPieces.LeftTurn.class, 20, 0),
        new StrongholdPieces.PieceWeight(StrongholdPieces.RightTurn.class, 20, 0),
        new StrongholdPieces.PieceWeight(StrongholdPieces.RoomCrossing.class, 10, 6),
        new StrongholdPieces.PieceWeight(StrongholdPieces.StraightStairsDown.class, 5, 5),
        new StrongholdPieces.PieceWeight(StrongholdPieces.StairsDown.class, 5, 5),
        new StrongholdPieces.PieceWeight(StrongholdPieces.FiveCrossing.class, 5, 4),
        new StrongholdPieces.PieceWeight(StrongholdPieces.ChestCorridor.class, 5, 4),
        new StrongholdPieces.PieceWeight(StrongholdPieces.Library.class, 10, 2) {
            @Override
            public boolean doPlace(int p_229450_) {
                return super.doPlace(p_229450_) && p_229450_ > 4;
            }
        },
        new StrongholdPieces.PieceWeight(StrongholdPieces.PortalRoom.class, 20, 1) {
            @Override
            public boolean doPlace(int p_229456_) {
                return super.doPlace(p_229456_) && p_229456_ > 5;
            }
        }
    };
    private static List<StrongholdPieces.PieceWeight> currentPieces;
    static Class<? extends StrongholdPieces.StrongholdPiece> imposedPiece;
    private static int totalWeight;
    static final StrongholdPieces.SmoothStoneSelector SMOOTH_STONE_SELECTOR = new StrongholdPieces.SmoothStoneSelector();

    public static void resetPieces() {
        currentPieces = Lists.newArrayList();

        for (StrongholdPieces.PieceWeight strongholdpieces$pieceweight : STRONGHOLD_PIECE_WEIGHTS) {
            strongholdpieces$pieceweight.placeCount = 0;
            currentPieces.add(strongholdpieces$pieceweight);
        }

        imposedPiece = null;
    }

    private static boolean updatePieceWeight() {
        boolean flag = false;
        totalWeight = 0;

        for (StrongholdPieces.PieceWeight strongholdpieces$pieceweight : currentPieces) {
            if (strongholdpieces$pieceweight.maxPlaceCount > 0 && strongholdpieces$pieceweight.placeCount < strongholdpieces$pieceweight.maxPlaceCount) {
                flag = true;
            }

            totalWeight = totalWeight + strongholdpieces$pieceweight.weight;
        }

        return flag;
    }

    private static StrongholdPieces.StrongholdPiece findAndCreatePieceFactory(
        Class<? extends StrongholdPieces.StrongholdPiece> pPieceClass,
        StructurePieceAccessor pPieces,
        RandomSource pRandom,
        int pX,
        int pY,
        int pZ,
        @Nullable Direction pDirection,
        int pGenDepth
    ) {
        StrongholdPieces.StrongholdPiece strongholdpieces$strongholdpiece = null;
        if (pPieceClass == StrongholdPieces.Straight.class) {
            strongholdpieces$strongholdpiece = StrongholdPieces.Straight.createPiece(pPieces, pRandom, pX, pY, pZ, pDirection, pGenDepth);
        } else if (pPieceClass == StrongholdPieces.PrisonHall.class) {
            strongholdpieces$strongholdpiece = StrongholdPieces.PrisonHall.createPiece(
                pPieces, pRandom, pX, pY, pZ, pDirection, pGenDepth
            );
        } else if (pPieceClass == StrongholdPieces.LeftTurn.class) {
            strongholdpieces$strongholdpiece = StrongholdPieces.LeftTurn.createPiece(pPieces, pRandom, pX, pY, pZ, pDirection, pGenDepth);
        } else if (pPieceClass == StrongholdPieces.RightTurn.class) {
            strongholdpieces$strongholdpiece = StrongholdPieces.RightTurn.createPiece(pPieces, pRandom, pX, pY, pZ, pDirection, pGenDepth);
        } else if (pPieceClass == StrongholdPieces.RoomCrossing.class) {
            strongholdpieces$strongholdpiece = StrongholdPieces.RoomCrossing.createPiece(
                pPieces, pRandom, pX, pY, pZ, pDirection, pGenDepth
            );
        } else if (pPieceClass == StrongholdPieces.StraightStairsDown.class) {
            strongholdpieces$strongholdpiece = StrongholdPieces.StraightStairsDown.createPiece(
                pPieces, pRandom, pX, pY, pZ, pDirection, pGenDepth
            );
        } else if (pPieceClass == StrongholdPieces.StairsDown.class) {
            strongholdpieces$strongholdpiece = StrongholdPieces.StairsDown.createPiece(
                pPieces, pRandom, pX, pY, pZ, pDirection, pGenDepth
            );
        } else if (pPieceClass == StrongholdPieces.FiveCrossing.class) {
            strongholdpieces$strongholdpiece = StrongholdPieces.FiveCrossing.createPiece(
                pPieces, pRandom, pX, pY, pZ, pDirection, pGenDepth
            );
        } else if (pPieceClass == StrongholdPieces.ChestCorridor.class) {
            strongholdpieces$strongholdpiece = StrongholdPieces.ChestCorridor.createPiece(
                pPieces, pRandom, pX, pY, pZ, pDirection, pGenDepth
            );
        } else if (pPieceClass == StrongholdPieces.Library.class) {
            strongholdpieces$strongholdpiece = StrongholdPieces.Library.createPiece(pPieces, pRandom, pX, pY, pZ, pDirection, pGenDepth);
        } else if (pPieceClass == StrongholdPieces.PortalRoom.class) {
            strongholdpieces$strongholdpiece = StrongholdPieces.PortalRoom.createPiece(pPieces, pX, pY, pZ, pDirection, pGenDepth);
        }

        return strongholdpieces$strongholdpiece;
    }

    private static StrongholdPieces.StrongholdPiece generatePieceFromSmallDoor(
        StrongholdPieces.StartPiece pPiece,
        StructurePieceAccessor pPieces,
        RandomSource pRandom,
        int pX,
        int pY,
        int pZ,
        Direction pDirection,
        int pGenDepth
    ) {
        if (!updatePieceWeight()) {
            return null;
        } else {
            if (imposedPiece != null) {
                StrongholdPieces.StrongholdPiece strongholdpieces$strongholdpiece = findAndCreatePieceFactory(
                    imposedPiece, pPieces, pRandom, pX, pY, pZ, pDirection, pGenDepth
                );
                imposedPiece = null;
                if (strongholdpieces$strongholdpiece != null) {
                    return strongholdpieces$strongholdpiece;
                }
            }

            int j = 0;

            while (j < 5) {
                j++;
                int i = pRandom.nextInt(totalWeight);

                for (StrongholdPieces.PieceWeight strongholdpieces$pieceweight : currentPieces) {
                    i -= strongholdpieces$pieceweight.weight;
                    if (i < 0) {
                        if (!strongholdpieces$pieceweight.doPlace(pGenDepth) || strongholdpieces$pieceweight == pPiece.previousPiece) {
                            break;
                        }

                        StrongholdPieces.StrongholdPiece strongholdpieces$strongholdpiece1 = findAndCreatePieceFactory(
                            strongholdpieces$pieceweight.pieceClass, pPieces, pRandom, pX, pY, pZ, pDirection, pGenDepth
                        );
                        if (strongholdpieces$strongholdpiece1 != null) {
                            strongholdpieces$pieceweight.placeCount++;
                            pPiece.previousPiece = strongholdpieces$pieceweight;
                            if (!strongholdpieces$pieceweight.isValid()) {
                                currentPieces.remove(strongholdpieces$pieceweight);
                            }

                            return strongholdpieces$strongholdpiece1;
                        }
                    }
                }
            }

            BoundingBox boundingbox = StrongholdPieces.FillerCorridor.findPieceBox(pPieces, pRandom, pX, pY, pZ, pDirection);
            return boundingbox != null && boundingbox.minY() > 1 ? new StrongholdPieces.FillerCorridor(pGenDepth, boundingbox, pDirection) : null;
        }
    }

    static StructurePiece generateAndAddPiece(
        StrongholdPieces.StartPiece pPiece,
        StructurePieceAccessor pPieces,
        RandomSource pRandom,
        int pX,
        int pY,
        int pZ,
        @Nullable Direction pDirection,
        int pGenDepth
    ) {
        if (pGenDepth > 50) {
            return null;
        } else if (Math.abs(pX - pPiece.getBoundingBox().minX()) <= 112 && Math.abs(pZ - pPiece.getBoundingBox().minZ()) <= 112) {
            StructurePiece structurepiece = generatePieceFromSmallDoor(pPiece, pPieces, pRandom, pX, pY, pZ, pDirection, pGenDepth + 1);
            if (structurepiece != null) {
                pPieces.addPiece(structurepiece);
                pPiece.pendingChildren.add(structurepiece);
            }

            return structurepiece;
        } else {
            return null;
        }
    }

    public static class ChestCorridor extends StrongholdPieces.StrongholdPiece {
        private static final int WIDTH = 5;
        private static final int HEIGHT = 5;
        private static final int DEPTH = 7;
        private boolean hasPlacedChest;

        public ChestCorridor(int pGenDepth, RandomSource pRandom, BoundingBox pBox, Direction pOrientation) {
            super(StructurePieceType.STRONGHOLD_CHEST_CORRIDOR, pGenDepth, pBox);
            this.setOrientation(pOrientation);
            this.entryDoor = this.randomSmallDoor(pRandom);
        }

        public ChestCorridor(CompoundTag pTag) {
            super(StructurePieceType.STRONGHOLD_CHEST_CORRIDOR, pTag);
            this.hasPlacedChest = pTag.getBooleanOr("Chest", false);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext p_229492_, CompoundTag p_229493_) {
            super.addAdditionalSaveData(p_229492_, p_229493_);
            p_229493_.putBoolean("Chest", this.hasPlacedChest);
        }

        @Override
        public void addChildren(StructurePiece p_229480_, StructurePieceAccessor p_229481_, RandomSource p_229482_) {
            this.generateSmallDoorChildForward((StrongholdPieces.StartPiece)p_229480_, p_229481_, p_229482_, 1, 1);
        }

        public static StrongholdPieces.ChestCorridor createPiece(
            StructurePieceAccessor pPieces, RandomSource pRandom, int pX, int pY, int pZ, Direction pOrientation, int pGenDepth
        ) {
            BoundingBox boundingbox = BoundingBox.orientBox(pX, pY, pZ, -1, -1, 0, 5, 5, 7, pOrientation);
            return isOkBox(boundingbox) && pPieces.findCollisionPiece(boundingbox) == null
                ? new StrongholdPieces.ChestCorridor(pGenDepth, pRandom, boundingbox, pOrientation)
                : null;
        }

        @Override
        public void postProcess(
            WorldGenLevel p_229472_,
            StructureManager p_229473_,
            ChunkGenerator p_229474_,
            RandomSource p_229475_,
            BoundingBox p_229476_,
            ChunkPos p_229477_,
            BlockPos p_229478_
        ) {
            this.generateBox(p_229472_, p_229476_, 0, 0, 0, 4, 4, 6, true, p_229475_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateSmallDoor(p_229472_, p_229475_, p_229476_, this.entryDoor, 1, 1, 0);
            this.generateSmallDoor(p_229472_, p_229475_, p_229476_, StrongholdPieces.StrongholdPiece.SmallDoorType.OPENING, 1, 1, 6);
            this.generateBox(p_229472_, p_229476_, 3, 1, 2, 3, 1, 4, Blocks.STONE_BRICKS.defaultBlockState(), Blocks.STONE_BRICKS.defaultBlockState(), false);
            this.placeBlock(p_229472_, Blocks.STONE_BRICK_SLAB.defaultBlockState(), 3, 1, 1, p_229476_);
            this.placeBlock(p_229472_, Blocks.STONE_BRICK_SLAB.defaultBlockState(), 3, 1, 5, p_229476_);
            this.placeBlock(p_229472_, Blocks.STONE_BRICK_SLAB.defaultBlockState(), 3, 2, 2, p_229476_);
            this.placeBlock(p_229472_, Blocks.STONE_BRICK_SLAB.defaultBlockState(), 3, 2, 4, p_229476_);

            for (int i = 2; i <= 4; i++) {
                this.placeBlock(p_229472_, Blocks.STONE_BRICK_SLAB.defaultBlockState(), 2, 1, i, p_229476_);
            }

            if (!this.hasPlacedChest && p_229476_.isInside(this.getWorldPos(3, 2, 3))) {
                this.hasPlacedChest = true;
                this.createChest(p_229472_, p_229476_, p_229475_, 3, 2, 3, BuiltInLootTables.STRONGHOLD_CORRIDOR);
            }
        }
    }

    public static class FillerCorridor extends StrongholdPieces.StrongholdPiece {
        private final int steps;

        public FillerCorridor(int pGenDepth, BoundingBox pBox, Direction pOrientation) {
            super(StructurePieceType.STRONGHOLD_FILLER_CORRIDOR, pGenDepth, pBox);
            this.setOrientation(pOrientation);
            this.steps = pOrientation != Direction.NORTH && pOrientation != Direction.SOUTH ? pBox.getXSpan() : pBox.getZSpan();
        }

        public FillerCorridor(CompoundTag pTag) {
            super(StructurePieceType.STRONGHOLD_FILLER_CORRIDOR, pTag);
            this.steps = pTag.getIntOr("Steps", 0);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext p_229517_, CompoundTag p_229518_) {
            super.addAdditionalSaveData(p_229517_, p_229518_);
            p_229518_.putInt("Steps", this.steps);
        }

        public static BoundingBox findPieceBox(
            StructurePieceAccessor pPieces, RandomSource pRandom, int pX, int pY, int pZ, Direction pOrientation
        ) {
            int i = 3;
            BoundingBox boundingbox = BoundingBox.orientBox(pX, pY, pZ, -1, -1, 0, 5, 5, 4, pOrientation);
            StructurePiece structurepiece = pPieces.findCollisionPiece(boundingbox);
            if (structurepiece == null) {
                return null;
            } else {
                if (structurepiece.getBoundingBox().minY() == boundingbox.minY()) {
                    for (int j = 2; j >= 1; j--) {
                        boundingbox = BoundingBox.orientBox(pX, pY, pZ, -1, -1, 0, 5, 5, j, pOrientation);
                        if (!structurepiece.getBoundingBox().intersects(boundingbox)) {
                            return BoundingBox.orientBox(pX, pY, pZ, -1, -1, 0, 5, 5, j + 1, pOrientation);
                        }
                    }
                }

                return null;
            }
        }

        @Override
        public void postProcess(
            WorldGenLevel p_229502_,
            StructureManager p_229503_,
            ChunkGenerator p_229504_,
            RandomSource p_229505_,
            BoundingBox p_229506_,
            ChunkPos p_229507_,
            BlockPos p_229508_
        ) {
            for (int i = 0; i < this.steps; i++) {
                this.placeBlock(p_229502_, Blocks.STONE_BRICKS.defaultBlockState(), 0, 0, i, p_229506_);
                this.placeBlock(p_229502_, Blocks.STONE_BRICKS.defaultBlockState(), 1, 0, i, p_229506_);
                this.placeBlock(p_229502_, Blocks.STONE_BRICKS.defaultBlockState(), 2, 0, i, p_229506_);
                this.placeBlock(p_229502_, Blocks.STONE_BRICKS.defaultBlockState(), 3, 0, i, p_229506_);
                this.placeBlock(p_229502_, Blocks.STONE_BRICKS.defaultBlockState(), 4, 0, i, p_229506_);

                for (int j = 1; j <= 3; j++) {
                    this.placeBlock(p_229502_, Blocks.STONE_BRICKS.defaultBlockState(), 0, j, i, p_229506_);
                    this.placeBlock(p_229502_, Blocks.CAVE_AIR.defaultBlockState(), 1, j, i, p_229506_);
                    this.placeBlock(p_229502_, Blocks.CAVE_AIR.defaultBlockState(), 2, j, i, p_229506_);
                    this.placeBlock(p_229502_, Blocks.CAVE_AIR.defaultBlockState(), 3, j, i, p_229506_);
                    this.placeBlock(p_229502_, Blocks.STONE_BRICKS.defaultBlockState(), 4, j, i, p_229506_);
                }

                this.placeBlock(p_229502_, Blocks.STONE_BRICKS.defaultBlockState(), 0, 4, i, p_229506_);
                this.placeBlock(p_229502_, Blocks.STONE_BRICKS.defaultBlockState(), 1, 4, i, p_229506_);
                this.placeBlock(p_229502_, Blocks.STONE_BRICKS.defaultBlockState(), 2, 4, i, p_229506_);
                this.placeBlock(p_229502_, Blocks.STONE_BRICKS.defaultBlockState(), 3, 4, i, p_229506_);
                this.placeBlock(p_229502_, Blocks.STONE_BRICKS.defaultBlockState(), 4, 4, i, p_229506_);
            }
        }
    }

    public static class FiveCrossing extends StrongholdPieces.StrongholdPiece {
        protected static final int WIDTH = 10;
        protected static final int HEIGHT = 9;
        protected static final int DEPTH = 11;
        private final boolean leftLow;
        private final boolean leftHigh;
        private final boolean rightLow;
        private final boolean rightHigh;

        public FiveCrossing(int pGenDepth, RandomSource pRandom, BoundingBox pBox, Direction pOrientation) {
            super(StructurePieceType.STRONGHOLD_FIVE_CROSSING, pGenDepth, pBox);
            this.setOrientation(pOrientation);
            this.entryDoor = this.randomSmallDoor(pRandom);
            this.leftLow = pRandom.nextBoolean();
            this.leftHigh = pRandom.nextBoolean();
            this.rightLow = pRandom.nextBoolean();
            this.rightHigh = pRandom.nextInt(3) > 0;
        }

        public FiveCrossing(CompoundTag pTag) {
            super(StructurePieceType.STRONGHOLD_FIVE_CROSSING, pTag);
            this.leftLow = pTag.getBooleanOr("leftLow", false);
            this.leftHigh = pTag.getBooleanOr("leftHigh", false);
            this.rightLow = pTag.getBooleanOr("rightLow", false);
            this.rightHigh = pTag.getBooleanOr("rightHigh", false);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext p_229554_, CompoundTag p_229555_) {
            super.addAdditionalSaveData(p_229554_, p_229555_);
            p_229555_.putBoolean("leftLow", this.leftLow);
            p_229555_.putBoolean("leftHigh", this.leftHigh);
            p_229555_.putBoolean("rightLow", this.rightLow);
            p_229555_.putBoolean("rightHigh", this.rightHigh);
        }

        @Override
        public void addChildren(StructurePiece p_229542_, StructurePieceAccessor p_229543_, RandomSource p_229544_) {
            int i = 3;
            int j = 5;
            Direction direction = this.getOrientation();
            if (direction == Direction.WEST || direction == Direction.NORTH) {
                i = 8 - i;
                j = 8 - j;
            }

            this.generateSmallDoorChildForward((StrongholdPieces.StartPiece)p_229542_, p_229543_, p_229544_, 5, 1);
            if (this.leftLow) {
                this.generateSmallDoorChildLeft((StrongholdPieces.StartPiece)p_229542_, p_229543_, p_229544_, i, 1);
            }

            if (this.leftHigh) {
                this.generateSmallDoorChildLeft((StrongholdPieces.StartPiece)p_229542_, p_229543_, p_229544_, j, 7);
            }

            if (this.rightLow) {
                this.generateSmallDoorChildRight((StrongholdPieces.StartPiece)p_229542_, p_229543_, p_229544_, i, 1);
            }

            if (this.rightHigh) {
                this.generateSmallDoorChildRight((StrongholdPieces.StartPiece)p_229542_, p_229543_, p_229544_, j, 7);
            }
        }

        public static StrongholdPieces.FiveCrossing createPiece(
            StructurePieceAccessor pPieces, RandomSource pRandom, int pX, int pY, int pZ, Direction pOrientation, int pGenDepth
        ) {
            BoundingBox boundingbox = BoundingBox.orientBox(pX, pY, pZ, -4, -3, 0, 10, 9, 11, pOrientation);
            return isOkBox(boundingbox) && pPieces.findCollisionPiece(boundingbox) == null
                ? new StrongholdPieces.FiveCrossing(pGenDepth, pRandom, boundingbox, pOrientation)
                : null;
        }

        @Override
        public void postProcess(
            WorldGenLevel p_229534_,
            StructureManager p_229535_,
            ChunkGenerator p_229536_,
            RandomSource p_229537_,
            BoundingBox p_229538_,
            ChunkPos p_229539_,
            BlockPos p_229540_
        ) {
            this.generateBox(p_229534_, p_229538_, 0, 0, 0, 9, 8, 10, true, p_229537_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateSmallDoor(p_229534_, p_229537_, p_229538_, this.entryDoor, 4, 3, 0);
            if (this.leftLow) {
                this.generateBox(p_229534_, p_229538_, 0, 3, 1, 0, 5, 3, CAVE_AIR, CAVE_AIR, false);
            }

            if (this.rightLow) {
                this.generateBox(p_229534_, p_229538_, 9, 3, 1, 9, 5, 3, CAVE_AIR, CAVE_AIR, false);
            }

            if (this.leftHigh) {
                this.generateBox(p_229534_, p_229538_, 0, 5, 7, 0, 7, 9, CAVE_AIR, CAVE_AIR, false);
            }

            if (this.rightHigh) {
                this.generateBox(p_229534_, p_229538_, 9, 5, 7, 9, 7, 9, CAVE_AIR, CAVE_AIR, false);
            }

            this.generateBox(p_229534_, p_229538_, 5, 1, 10, 7, 3, 10, CAVE_AIR, CAVE_AIR, false);
            this.generateBox(p_229534_, p_229538_, 1, 2, 1, 8, 2, 6, false, p_229537_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(p_229534_, p_229538_, 4, 1, 5, 4, 4, 9, false, p_229537_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(p_229534_, p_229538_, 8, 1, 5, 8, 4, 9, false, p_229537_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(p_229534_, p_229538_, 1, 4, 7, 3, 4, 9, false, p_229537_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(p_229534_, p_229538_, 1, 3, 5, 3, 3, 6, false, p_229537_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(p_229534_, p_229538_, 1, 3, 4, 3, 3, 4, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), false);
            this.generateBox(p_229534_, p_229538_, 1, 4, 6, 3, 4, 6, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), false);
            this.generateBox(p_229534_, p_229538_, 5, 1, 7, 7, 1, 8, false, p_229537_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(p_229534_, p_229538_, 5, 1, 9, 7, 1, 9, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), false);
            this.generateBox(p_229534_, p_229538_, 5, 2, 7, 7, 2, 7, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), false);
            this.generateBox(p_229534_, p_229538_, 4, 5, 7, 4, 5, 9, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), false);
            this.generateBox(p_229534_, p_229538_, 8, 5, 7, 8, 5, 9, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), false);
            this.generateBox(
                p_229534_,
                p_229538_,
                5,
                5,
                7,
                7,
                5,
                9,
                Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE),
                Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE),
                false
            );
            this.placeBlock(p_229534_, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.SOUTH), 6, 5, 6, p_229538_);
        }
    }

    public static class LeftTurn extends StrongholdPieces.Turn {
        public LeftTurn(int pGenDepth, RandomSource pRandom, BoundingBox pBox, Direction pOrientation) {
            super(StructurePieceType.STRONGHOLD_LEFT_TURN, pGenDepth, pBox);
            this.setOrientation(pOrientation);
            this.entryDoor = this.randomSmallDoor(pRandom);
        }

        public LeftTurn(CompoundTag pTag) {
            super(StructurePieceType.STRONGHOLD_LEFT_TURN, pTag);
        }

        @Override
        public void addChildren(StructurePiece p_229572_, StructurePieceAccessor p_229573_, RandomSource p_229574_) {
            Direction direction = this.getOrientation();
            if (direction != Direction.NORTH && direction != Direction.EAST) {
                this.generateSmallDoorChildRight((StrongholdPieces.StartPiece)p_229572_, p_229573_, p_229574_, 1, 1);
            } else {
                this.generateSmallDoorChildLeft((StrongholdPieces.StartPiece)p_229572_, p_229573_, p_229574_, 1, 1);
            }
        }

        public static StrongholdPieces.LeftTurn createPiece(
            StructurePieceAccessor pPieces, RandomSource pRandom, int pX, int pY, int pZ, Direction pOrientation, int pGenDepth
        ) {
            BoundingBox boundingbox = BoundingBox.orientBox(pX, pY, pZ, -1, -1, 0, 5, 5, 5, pOrientation);
            return isOkBox(boundingbox) && pPieces.findCollisionPiece(boundingbox) == null
                ? new StrongholdPieces.LeftTurn(pGenDepth, pRandom, boundingbox, pOrientation)
                : null;
        }

        @Override
        public void postProcess(
            WorldGenLevel p_229564_,
            StructureManager p_229565_,
            ChunkGenerator p_229566_,
            RandomSource p_229567_,
            BoundingBox p_229568_,
            ChunkPos p_229569_,
            BlockPos p_229570_
        ) {
            this.generateBox(p_229564_, p_229568_, 0, 0, 0, 4, 4, 4, true, p_229567_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateSmallDoor(p_229564_, p_229567_, p_229568_, this.entryDoor, 1, 1, 0);
            Direction direction = this.getOrientation();
            if (direction != Direction.NORTH && direction != Direction.EAST) {
                this.generateBox(p_229564_, p_229568_, 4, 1, 1, 4, 3, 3, CAVE_AIR, CAVE_AIR, false);
            } else {
                this.generateBox(p_229564_, p_229568_, 0, 1, 1, 0, 3, 3, CAVE_AIR, CAVE_AIR, false);
            }
        }
    }

    public static class Library extends StrongholdPieces.StrongholdPiece {
        protected static final int WIDTH = 14;
        protected static final int HEIGHT = 6;
        protected static final int TALL_HEIGHT = 11;
        protected static final int DEPTH = 15;
        private final boolean isTall;

        public Library(int pGenDepth, RandomSource pRandom, BoundingBox pBox, Direction pOrientation) {
            super(StructurePieceType.STRONGHOLD_LIBRARY, pGenDepth, pBox);
            this.setOrientation(pOrientation);
            this.entryDoor = this.randomSmallDoor(pRandom);
            this.isTall = pBox.getYSpan() > 6;
        }

        public Library(CompoundTag pTag) {
            super(StructurePieceType.STRONGHOLD_LIBRARY, pTag);
            this.isTall = pTag.getBooleanOr("Tall", false);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext p_229612_, CompoundTag p_229613_) {
            super.addAdditionalSaveData(p_229612_, p_229613_);
            p_229613_.putBoolean("Tall", this.isTall);
        }

        public static StrongholdPieces.Library createPiece(
            StructurePieceAccessor pPieces, RandomSource pRandom, int pX, int pY, int pZ, Direction pOrientation, int pGenDepth
        ) {
            BoundingBox boundingbox = BoundingBox.orientBox(pX, pY, pZ, -4, -1, 0, 14, 11, 15, pOrientation);
            if (!isOkBox(boundingbox) || pPieces.findCollisionPiece(boundingbox) != null) {
                boundingbox = BoundingBox.orientBox(pX, pY, pZ, -4, -1, 0, 14, 6, 15, pOrientation);
                if (!isOkBox(boundingbox) || pPieces.findCollisionPiece(boundingbox) != null) {
                    return null;
                }
            }

            return new StrongholdPieces.Library(pGenDepth, pRandom, boundingbox, pOrientation);
        }

        @Override
        public void postProcess(
            WorldGenLevel p_229596_,
            StructureManager p_229597_,
            ChunkGenerator p_229598_,
            RandomSource p_229599_,
            BoundingBox p_229600_,
            ChunkPos p_229601_,
            BlockPos p_229602_
        ) {
            int i = 11;
            if (!this.isTall) {
                i = 6;
            }

            this.generateBox(p_229596_, p_229600_, 0, 0, 0, 13, i - 1, 14, true, p_229599_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateSmallDoor(p_229596_, p_229599_, p_229600_, this.entryDoor, 4, 1, 0);
            this.generateMaybeBox(p_229596_, p_229600_, p_229599_, 0.07F, 2, 1, 1, 11, 4, 13, Blocks.COBWEB.defaultBlockState(), Blocks.COBWEB.defaultBlockState(), false, false);
            int j = 1;
            int k = 12;

            for (int l = 1; l <= 13; l++) {
                if ((l - 1) % 4 == 0) {
                    this.generateBox(p_229596_, p_229600_, 1, 1, l, 1, 4, l, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
                    this.generateBox(p_229596_, p_229600_, 12, 1, l, 12, 4, l, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
                    this.placeBlock(p_229596_, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.EAST), 2, 3, l, p_229600_);
                    this.placeBlock(p_229596_, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.WEST), 11, 3, l, p_229600_);
                    if (this.isTall) {
                        this.generateBox(p_229596_, p_229600_, 1, 6, l, 1, 9, l, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
                        this.generateBox(p_229596_, p_229600_, 12, 6, l, 12, 9, l, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
                    }
                } else {
                    this.generateBox(p_229596_, p_229600_, 1, 1, l, 1, 4, l, Blocks.BOOKSHELF.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(), false);
                    this.generateBox(p_229596_, p_229600_, 12, 1, l, 12, 4, l, Blocks.BOOKSHELF.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(), false);
                    if (this.isTall) {
                        this.generateBox(p_229596_, p_229600_, 1, 6, l, 1, 9, l, Blocks.BOOKSHELF.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(), false);
                        this.generateBox(p_229596_, p_229600_, 12, 6, l, 12, 9, l, Blocks.BOOKSHELF.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(), false);
                    }
                }
            }

            for (int l1 = 3; l1 < 12; l1 += 2) {
                this.generateBox(p_229596_, p_229600_, 3, 1, l1, 4, 3, l1, Blocks.BOOKSHELF.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(), false);
                this.generateBox(p_229596_, p_229600_, 6, 1, l1, 7, 3, l1, Blocks.BOOKSHELF.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(), false);
                this.generateBox(p_229596_, p_229600_, 9, 1, l1, 10, 3, l1, Blocks.BOOKSHELF.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(), false);
            }

            if (this.isTall) {
                this.generateBox(p_229596_, p_229600_, 1, 5, 1, 3, 5, 13, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
                this.generateBox(p_229596_, p_229600_, 10, 5, 1, 12, 5, 13, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
                this.generateBox(p_229596_, p_229600_, 4, 5, 1, 9, 5, 2, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
                this.generateBox(p_229596_, p_229600_, 4, 5, 12, 9, 5, 13, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
                this.placeBlock(p_229596_, Blocks.OAK_PLANKS.defaultBlockState(), 9, 5, 11, p_229600_);
                this.placeBlock(p_229596_, Blocks.OAK_PLANKS.defaultBlockState(), 8, 5, 11, p_229600_);
                this.placeBlock(p_229596_, Blocks.OAK_PLANKS.defaultBlockState(), 9, 5, 10, p_229600_);
                BlockState blockstate5 = Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true).setValue(FenceBlock.EAST, true);
                BlockState blockstate = Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.SOUTH, true);
                this.generateBox(p_229596_, p_229600_, 3, 6, 3, 3, 6, 11, blockstate, blockstate, false);
                this.generateBox(p_229596_, p_229600_, 10, 6, 3, 10, 6, 9, blockstate, blockstate, false);
                this.generateBox(p_229596_, p_229600_, 4, 6, 2, 9, 6, 2, blockstate5, blockstate5, false);
                this.generateBox(p_229596_, p_229600_, 4, 6, 12, 7, 6, 12, blockstate5, blockstate5, false);
                this.placeBlock(p_229596_, Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.EAST, true), 3, 6, 2, p_229600_);
                this.placeBlock(
                    p_229596_, Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.SOUTH, true).setValue(FenceBlock.EAST, true), 3, 6, 12, p_229600_
                );
                this.placeBlock(
                    p_229596_, Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.WEST, true), 10, 6, 2, p_229600_
                );

                for (int i1 = 0; i1 <= 2; i1++) {
                    this.placeBlock(
                        p_229596_,
                        Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.SOUTH, true).setValue(FenceBlock.WEST, true),
                        8 + i1,
                        6,
                        12 - i1,
                        p_229600_
                    );
                    if (i1 != 2) {
                        this.placeBlock(
                            p_229596_,
                            Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.EAST, true),
                            8 + i1,
                            6,
                            11 - i1,
                            p_229600_
                        );
                    }
                }

                BlockState blockstate6 = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH);
                this.placeBlock(p_229596_, blockstate6, 10, 1, 13, p_229600_);
                this.placeBlock(p_229596_, blockstate6, 10, 2, 13, p_229600_);
                this.placeBlock(p_229596_, blockstate6, 10, 3, 13, p_229600_);
                this.placeBlock(p_229596_, blockstate6, 10, 4, 13, p_229600_);
                this.placeBlock(p_229596_, blockstate6, 10, 5, 13, p_229600_);
                this.placeBlock(p_229596_, blockstate6, 10, 6, 13, p_229600_);
                this.placeBlock(p_229596_, blockstate6, 10, 7, 13, p_229600_);
                int j1 = 7;
                int k1 = 7;
                BlockState blockstate1 = Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.EAST, true);
                this.placeBlock(p_229596_, blockstate1, 6, 9, 7, p_229600_);
                BlockState blockstate2 = Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true);
                this.placeBlock(p_229596_, blockstate2, 7, 9, 7, p_229600_);
                this.placeBlock(p_229596_, blockstate1, 6, 8, 7, p_229600_);
                this.placeBlock(p_229596_, blockstate2, 7, 8, 7, p_229600_);
                BlockState blockstate3 = blockstate.setValue(FenceBlock.WEST, true).setValue(FenceBlock.EAST, true);
                this.placeBlock(p_229596_, blockstate3, 6, 7, 7, p_229600_);
                this.placeBlock(p_229596_, blockstate3, 7, 7, 7, p_229600_);
                this.placeBlock(p_229596_, blockstate1, 5, 7, 7, p_229600_);
                this.placeBlock(p_229596_, blockstate2, 8, 7, 7, p_229600_);
                this.placeBlock(p_229596_, blockstate1.setValue(FenceBlock.NORTH, true), 6, 7, 6, p_229600_);
                this.placeBlock(p_229596_, blockstate1.setValue(FenceBlock.SOUTH, true), 6, 7, 8, p_229600_);
                this.placeBlock(p_229596_, blockstate2.setValue(FenceBlock.NORTH, true), 7, 7, 6, p_229600_);
                this.placeBlock(p_229596_, blockstate2.setValue(FenceBlock.SOUTH, true), 7, 7, 8, p_229600_);
                BlockState blockstate4 = Blocks.TORCH.defaultBlockState();
                this.placeBlock(p_229596_, blockstate4, 5, 8, 7, p_229600_);
                this.placeBlock(p_229596_, blockstate4, 8, 8, 7, p_229600_);
                this.placeBlock(p_229596_, blockstate4, 6, 8, 6, p_229600_);
                this.placeBlock(p_229596_, blockstate4, 6, 8, 8, p_229600_);
                this.placeBlock(p_229596_, blockstate4, 7, 8, 6, p_229600_);
                this.placeBlock(p_229596_, blockstate4, 7, 8, 8, p_229600_);
            }

            this.createChest(p_229596_, p_229600_, p_229599_, 3, 3, 5, BuiltInLootTables.STRONGHOLD_LIBRARY);
            if (this.isTall) {
                this.placeBlock(p_229596_, CAVE_AIR, 12, 9, 1, p_229600_);
                this.createChest(p_229596_, p_229600_, p_229599_, 12, 8, 1, BuiltInLootTables.STRONGHOLD_LIBRARY);
            }
        }
    }

    static class PieceWeight {
        public final Class<? extends StrongholdPieces.StrongholdPiece> pieceClass;
        public final int weight;
        public int placeCount;
        public final int maxPlaceCount;

        public PieceWeight(Class<? extends StrongholdPieces.StrongholdPiece> pPieceClass, int pWeight, int pMaxPlaceCount) {
            this.pieceClass = pPieceClass;
            this.weight = pWeight;
            this.maxPlaceCount = pMaxPlaceCount;
        }

        public boolean doPlace(int pGenDepth) {
            return this.maxPlaceCount == 0 || this.placeCount < this.maxPlaceCount;
        }

        public boolean isValid() {
            return this.maxPlaceCount == 0 || this.placeCount < this.maxPlaceCount;
        }
    }

    public static class PortalRoom extends StrongholdPieces.StrongholdPiece {
        protected static final int WIDTH = 11;
        protected static final int HEIGHT = 8;
        protected static final int DEPTH = 16;
        private boolean hasPlacedSpawner;

        public PortalRoom(int pGenDepth, BoundingBox pBox, Direction pOrientation) {
            super(StructurePieceType.STRONGHOLD_PORTAL_ROOM, pGenDepth, pBox);
            this.setOrientation(pOrientation);
        }

        public PortalRoom(CompoundTag pTag) {
            super(StructurePieceType.STRONGHOLD_PORTAL_ROOM, pTag);
            this.hasPlacedSpawner = pTag.getBooleanOr("Mob", false);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext p_229654_, CompoundTag p_229655_) {
            super.addAdditionalSaveData(p_229654_, p_229655_);
            p_229655_.putBoolean("Mob", this.hasPlacedSpawner);
        }

        @Override
        public void addChildren(StructurePiece p_229643_, StructurePieceAccessor p_229644_, RandomSource p_229645_) {
            if (p_229643_ != null) {
                ((StrongholdPieces.StartPiece)p_229643_).portalRoomPiece = this;
            }
        }

        public static StrongholdPieces.PortalRoom createPiece(
            StructurePieceAccessor pPieces, int pX, int pY, int pZ, Direction pOrientation, int pGenDepth
        ) {
            BoundingBox boundingbox = BoundingBox.orientBox(pX, pY, pZ, -4, -1, 0, 11, 8, 16, pOrientation);
            return isOkBox(boundingbox) && pPieces.findCollisionPiece(boundingbox) == null
                ? new StrongholdPieces.PortalRoom(pGenDepth, boundingbox, pOrientation)
                : null;
        }

        @Override
        public void postProcess(
            WorldGenLevel p_229635_,
            StructureManager p_229636_,
            ChunkGenerator p_229637_,
            RandomSource p_229638_,
            BoundingBox p_229639_,
            ChunkPos p_229640_,
            BlockPos p_229641_
        ) {
            this.generateBox(p_229635_, p_229639_, 0, 0, 0, 10, 7, 15, false, p_229638_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateSmallDoor(p_229635_, p_229638_, p_229639_, StrongholdPieces.StrongholdPiece.SmallDoorType.GRATES, 4, 1, 0);
            int i = 6;
            this.generateBox(p_229635_, p_229639_, 1, 6, 1, 1, 6, 14, false, p_229638_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(p_229635_, p_229639_, 9, 6, 1, 9, 6, 14, false, p_229638_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(p_229635_, p_229639_, 2, 6, 1, 8, 6, 2, false, p_229638_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(p_229635_, p_229639_, 2, 6, 14, 8, 6, 14, false, p_229638_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(p_229635_, p_229639_, 1, 1, 1, 2, 1, 4, false, p_229638_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(p_229635_, p_229639_, 8, 1, 1, 9, 1, 4, false, p_229638_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(p_229635_, p_229639_, 1, 1, 1, 1, 1, 3, Blocks.LAVA.defaultBlockState(), Blocks.LAVA.defaultBlockState(), false);
            this.generateBox(p_229635_, p_229639_, 9, 1, 1, 9, 1, 3, Blocks.LAVA.defaultBlockState(), Blocks.LAVA.defaultBlockState(), false);
            this.generateBox(p_229635_, p_229639_, 3, 1, 8, 7, 1, 12, false, p_229638_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(p_229635_, p_229639_, 4, 1, 9, 6, 1, 11, Blocks.LAVA.defaultBlockState(), Blocks.LAVA.defaultBlockState(), false);
            BlockState blockstate = Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true);
            BlockState blockstate1 = Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.WEST, true).setValue(IronBarsBlock.EAST, true);

            for (int j = 3; j < 14; j += 2) {
                this.generateBox(p_229635_, p_229639_, 0, 3, j, 0, 4, j, blockstate, blockstate, false);
                this.generateBox(p_229635_, p_229639_, 10, 3, j, 10, 4, j, blockstate, blockstate, false);
            }

            for (int i1 = 2; i1 < 9; i1 += 2) {
                this.generateBox(p_229635_, p_229639_, i1, 3, 15, i1, 4, 15, blockstate1, blockstate1, false);
            }

            BlockState blockstate5 = Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
            this.generateBox(p_229635_, p_229639_, 4, 1, 5, 6, 1, 7, false, p_229638_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(p_229635_, p_229639_, 4, 2, 6, 6, 2, 7, false, p_229638_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(p_229635_, p_229639_, 4, 3, 7, 6, 3, 7, false, p_229638_, StrongholdPieces.SMOOTH_STONE_SELECTOR);

            for (int k = 4; k <= 6; k++) {
                this.placeBlock(p_229635_, blockstate5, k, 1, 4, p_229639_);
                this.placeBlock(p_229635_, blockstate5, k, 2, 5, p_229639_);
                this.placeBlock(p_229635_, blockstate5, k, 3, 6, p_229639_);
            }

            BlockState blockstate6 = Blocks.END_PORTAL_FRAME.defaultBlockState().setValue(EndPortalFrameBlock.FACING, Direction.NORTH);
            BlockState blockstate2 = Blocks.END_PORTAL_FRAME.defaultBlockState().setValue(EndPortalFrameBlock.FACING, Direction.SOUTH);
            BlockState blockstate3 = Blocks.END_PORTAL_FRAME.defaultBlockState().setValue(EndPortalFrameBlock.FACING, Direction.EAST);
            BlockState blockstate4 = Blocks.END_PORTAL_FRAME.defaultBlockState().setValue(EndPortalFrameBlock.FACING, Direction.WEST);
            boolean flag = true;
            boolean[] aboolean = new boolean[12];

            for (int l = 0; l < aboolean.length; l++) {
                aboolean[l] = p_229638_.nextFloat() > 0.9F;
                flag &= aboolean[l];
            }

            this.placeBlock(p_229635_, blockstate6.setValue(EndPortalFrameBlock.HAS_EYE, aboolean[0]), 4, 3, 8, p_229639_);
            this.placeBlock(p_229635_, blockstate6.setValue(EndPortalFrameBlock.HAS_EYE, aboolean[1]), 5, 3, 8, p_229639_);
            this.placeBlock(p_229635_, blockstate6.setValue(EndPortalFrameBlock.HAS_EYE, aboolean[2]), 6, 3, 8, p_229639_);
            this.placeBlock(p_229635_, blockstate2.setValue(EndPortalFrameBlock.HAS_EYE, aboolean[3]), 4, 3, 12, p_229639_);
            this.placeBlock(p_229635_, blockstate2.setValue(EndPortalFrameBlock.HAS_EYE, aboolean[4]), 5, 3, 12, p_229639_);
            this.placeBlock(p_229635_, blockstate2.setValue(EndPortalFrameBlock.HAS_EYE, aboolean[5]), 6, 3, 12, p_229639_);
            this.placeBlock(p_229635_, blockstate3.setValue(EndPortalFrameBlock.HAS_EYE, aboolean[6]), 3, 3, 9, p_229639_);
            this.placeBlock(p_229635_, blockstate3.setValue(EndPortalFrameBlock.HAS_EYE, aboolean[7]), 3, 3, 10, p_229639_);
            this.placeBlock(p_229635_, blockstate3.setValue(EndPortalFrameBlock.HAS_EYE, aboolean[8]), 3, 3, 11, p_229639_);
            this.placeBlock(p_229635_, blockstate4.setValue(EndPortalFrameBlock.HAS_EYE, aboolean[9]), 7, 3, 9, p_229639_);
            this.placeBlock(p_229635_, blockstate4.setValue(EndPortalFrameBlock.HAS_EYE, aboolean[10]), 7, 3, 10, p_229639_);
            this.placeBlock(p_229635_, blockstate4.setValue(EndPortalFrameBlock.HAS_EYE, aboolean[11]), 7, 3, 11, p_229639_);
            if (flag) {
                BlockState blockstate7 = Blocks.END_PORTAL.defaultBlockState();
                this.placeBlock(p_229635_, blockstate7, 4, 3, 9, p_229639_);
                this.placeBlock(p_229635_, blockstate7, 5, 3, 9, p_229639_);
                this.placeBlock(p_229635_, blockstate7, 6, 3, 9, p_229639_);
                this.placeBlock(p_229635_, blockstate7, 4, 3, 10, p_229639_);
                this.placeBlock(p_229635_, blockstate7, 5, 3, 10, p_229639_);
                this.placeBlock(p_229635_, blockstate7, 6, 3, 10, p_229639_);
                this.placeBlock(p_229635_, blockstate7, 4, 3, 11, p_229639_);
                this.placeBlock(p_229635_, blockstate7, 5, 3, 11, p_229639_);
                this.placeBlock(p_229635_, blockstate7, 6, 3, 11, p_229639_);
            }

            if (!this.hasPlacedSpawner) {
                BlockPos blockpos = this.getWorldPos(5, 3, 6);
                if (p_229639_.isInside(blockpos)) {
                    this.hasPlacedSpawner = true;
                    p_229635_.setBlock(blockpos, Blocks.SPAWNER.defaultBlockState(), 2);
                    if (p_229635_.getBlockEntity(blockpos) instanceof SpawnerBlockEntity spawnerblockentity) {
                        spawnerblockentity.setEntityId(EntityType.SILVERFISH, p_229638_);
                    }
                }
            }
        }
    }

    public static class PrisonHall extends StrongholdPieces.StrongholdPiece {
        protected static final int WIDTH = 9;
        protected static final int HEIGHT = 5;
        protected static final int DEPTH = 11;

        public PrisonHall(int pGenDepth, RandomSource pRandom, BoundingBox pBox, Direction pOrientation) {
            super(StructurePieceType.STRONGHOLD_PRISON_HALL, pGenDepth, pBox);
            this.setOrientation(pOrientation);
            this.entryDoor = this.randomSmallDoor(pRandom);
        }

        public PrisonHall(CompoundTag pTag) {
            super(StructurePieceType.STRONGHOLD_PRISON_HALL, pTag);
        }

        @Override
        public void addChildren(StructurePiece p_229675_, StructurePieceAccessor p_229676_, RandomSource p_229677_) {
            this.generateSmallDoorChildForward((StrongholdPieces.StartPiece)p_229675_, p_229676_, p_229677_, 1, 1);
        }

        public static StrongholdPieces.PrisonHall createPiece(
            StructurePieceAccessor pPieces, RandomSource pRandom, int pX, int pY, int pZ, Direction pOrientation, int pGenDepth
        ) {
            BoundingBox boundingbox = BoundingBox.orientBox(pX, pY, pZ, -1, -1, 0, 9, 5, 11, pOrientation);
            return isOkBox(boundingbox) && pPieces.findCollisionPiece(boundingbox) == null
                ? new StrongholdPieces.PrisonHall(pGenDepth, pRandom, boundingbox, pOrientation)
                : null;
        }

        @Override
        public void postProcess(
            WorldGenLevel p_229667_,
            StructureManager p_229668_,
            ChunkGenerator p_229669_,
            RandomSource p_229670_,
            BoundingBox p_229671_,
            ChunkPos p_229672_,
            BlockPos p_229673_
        ) {
            this.generateBox(p_229667_, p_229671_, 0, 0, 0, 8, 4, 10, true, p_229670_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateSmallDoor(p_229667_, p_229670_, p_229671_, this.entryDoor, 1, 1, 0);
            this.generateBox(p_229667_, p_229671_, 1, 1, 10, 3, 3, 10, CAVE_AIR, CAVE_AIR, false);
            this.generateBox(p_229667_, p_229671_, 4, 1, 1, 4, 3, 1, false, p_229670_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(p_229667_, p_229671_, 4, 1, 3, 4, 3, 3, false, p_229670_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(p_229667_, p_229671_, 4, 1, 7, 4, 3, 7, false, p_229670_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(p_229667_, p_229671_, 4, 1, 9, 4, 3, 9, false, p_229670_, StrongholdPieces.SMOOTH_STONE_SELECTOR);

            for (int i = 1; i <= 3; i++) {
                this.placeBlock(
                    p_229667_, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true), 4, i, 4, p_229671_
                );
                this.placeBlock(
                    p_229667_,
                    Blocks.IRON_BARS
                        .defaultBlockState()
                        .setValue(IronBarsBlock.NORTH, true)
                        .setValue(IronBarsBlock.SOUTH, true)
                        .setValue(IronBarsBlock.EAST, true),
                    4,
                    i,
                    5,
                    p_229671_
                );
                this.placeBlock(
                    p_229667_, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true), 4, i, 6, p_229671_
                );
                this.placeBlock(
                    p_229667_, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.WEST, true).setValue(IronBarsBlock.EAST, true), 5, i, 5, p_229671_
                );
                this.placeBlock(
                    p_229667_, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.WEST, true).setValue(IronBarsBlock.EAST, true), 6, i, 5, p_229671_
                );
                this.placeBlock(
                    p_229667_, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.WEST, true).setValue(IronBarsBlock.EAST, true), 7, i, 5, p_229671_
                );
            }

            this.placeBlock(
                p_229667_, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true), 4, 3, 2, p_229671_
            );
            this.placeBlock(
                p_229667_, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true), 4, 3, 8, p_229671_
            );
            BlockState blockstate1 = Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.WEST);
            BlockState blockstate = Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.WEST).setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
            this.placeBlock(p_229667_, blockstate1, 4, 1, 2, p_229671_);
            this.placeBlock(p_229667_, blockstate, 4, 2, 2, p_229671_);
            this.placeBlock(p_229667_, blockstate1, 4, 1, 8, p_229671_);
            this.placeBlock(p_229667_, blockstate, 4, 2, 8, p_229671_);
        }
    }

    public static class RightTurn extends StrongholdPieces.Turn {
        public RightTurn(int pGenDepth, RandomSource pRandom, BoundingBox pBox, Direction pOrientation) {
            super(StructurePieceType.STRONGHOLD_RIGHT_TURN, pGenDepth, pBox);
            this.setOrientation(pOrientation);
            this.entryDoor = this.randomSmallDoor(pRandom);
        }

        public RightTurn(CompoundTag pTag) {
            super(StructurePieceType.STRONGHOLD_RIGHT_TURN, pTag);
        }

        @Override
        public void addChildren(StructurePiece p_229702_, StructurePieceAccessor p_229703_, RandomSource p_229704_) {
            Direction direction = this.getOrientation();
            if (direction != Direction.NORTH && direction != Direction.EAST) {
                this.generateSmallDoorChildLeft((StrongholdPieces.StartPiece)p_229702_, p_229703_, p_229704_, 1, 1);
            } else {
                this.generateSmallDoorChildRight((StrongholdPieces.StartPiece)p_229702_, p_229703_, p_229704_, 1, 1);
            }
        }

        public static StrongholdPieces.RightTurn createPiece(
            StructurePieceAccessor pPieces, RandomSource pRandom, int pX, int pY, int pZ, Direction pOrientation, int pGenDepth
        ) {
            BoundingBox boundingbox = BoundingBox.orientBox(pX, pY, pZ, -1, -1, 0, 5, 5, 5, pOrientation);
            return isOkBox(boundingbox) && pPieces.findCollisionPiece(boundingbox) == null
                ? new StrongholdPieces.RightTurn(pGenDepth, pRandom, boundingbox, pOrientation)
                : null;
        }

        @Override
        public void postProcess(
            WorldGenLevel p_229694_,
            StructureManager p_229695_,
            ChunkGenerator p_229696_,
            RandomSource p_229697_,
            BoundingBox p_229698_,
            ChunkPos p_229699_,
            BlockPos p_229700_
        ) {
            this.generateBox(p_229694_, p_229698_, 0, 0, 0, 4, 4, 4, true, p_229697_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateSmallDoor(p_229694_, p_229697_, p_229698_, this.entryDoor, 1, 1, 0);
            Direction direction = this.getOrientation();
            if (direction != Direction.NORTH && direction != Direction.EAST) {
                this.generateBox(p_229694_, p_229698_, 0, 1, 1, 0, 3, 3, CAVE_AIR, CAVE_AIR, false);
            } else {
                this.generateBox(p_229694_, p_229698_, 4, 1, 1, 4, 3, 3, CAVE_AIR, CAVE_AIR, false);
            }
        }
    }

    public static class RoomCrossing extends StrongholdPieces.StrongholdPiece {
        protected static final int WIDTH = 11;
        protected static final int HEIGHT = 7;
        protected static final int DEPTH = 11;
        protected final int type;

        public RoomCrossing(int pGenDepth, RandomSource pRandom, BoundingBox pBox, Direction pOrientation) {
            super(StructurePieceType.STRONGHOLD_ROOM_CROSSING, pGenDepth, pBox);
            this.setOrientation(pOrientation);
            this.entryDoor = this.randomSmallDoor(pRandom);
            this.type = pRandom.nextInt(5);
        }

        public RoomCrossing(CompoundTag pTag) {
            super(StructurePieceType.STRONGHOLD_ROOM_CROSSING, pTag);
            this.type = pTag.getIntOr("Type", 0);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext p_229745_, CompoundTag p_229746_) {
            super.addAdditionalSaveData(p_229745_, p_229746_);
            p_229746_.putInt("Type", this.type);
        }

        @Override
        public void addChildren(StructurePiece p_229733_, StructurePieceAccessor p_229734_, RandomSource p_229735_) {
            this.generateSmallDoorChildForward((StrongholdPieces.StartPiece)p_229733_, p_229734_, p_229735_, 4, 1);
            this.generateSmallDoorChildLeft((StrongholdPieces.StartPiece)p_229733_, p_229734_, p_229735_, 1, 4);
            this.generateSmallDoorChildRight((StrongholdPieces.StartPiece)p_229733_, p_229734_, p_229735_, 1, 4);
        }

        public static StrongholdPieces.RoomCrossing createPiece(
            StructurePieceAccessor pPieces, RandomSource pRandom, int pX, int pY, int pZ, Direction pOrientation, int pGenDepth
        ) {
            BoundingBox boundingbox = BoundingBox.orientBox(pX, pY, pZ, -4, -1, 0, 11, 7, 11, pOrientation);
            return isOkBox(boundingbox) && pPieces.findCollisionPiece(boundingbox) == null
                ? new StrongholdPieces.RoomCrossing(pGenDepth, pRandom, boundingbox, pOrientation)
                : null;
        }

        @Override
        public void postProcess(
            WorldGenLevel p_229725_,
            StructureManager p_229726_,
            ChunkGenerator p_229727_,
            RandomSource p_229728_,
            BoundingBox p_229729_,
            ChunkPos p_229730_,
            BlockPos p_229731_
        ) {
            this.generateBox(p_229725_, p_229729_, 0, 0, 0, 10, 6, 10, true, p_229728_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateSmallDoor(p_229725_, p_229728_, p_229729_, this.entryDoor, 4, 1, 0);
            this.generateBox(p_229725_, p_229729_, 4, 1, 10, 6, 3, 10, CAVE_AIR, CAVE_AIR, false);
            this.generateBox(p_229725_, p_229729_, 0, 1, 4, 0, 3, 6, CAVE_AIR, CAVE_AIR, false);
            this.generateBox(p_229725_, p_229729_, 10, 1, 4, 10, 3, 6, CAVE_AIR, CAVE_AIR, false);
            switch (this.type) {
                case 0:
                    this.placeBlock(p_229725_, Blocks.STONE_BRICKS.defaultBlockState(), 5, 1, 5, p_229729_);
                    this.placeBlock(p_229725_, Blocks.STONE_BRICKS.defaultBlockState(), 5, 2, 5, p_229729_);
                    this.placeBlock(p_229725_, Blocks.STONE_BRICKS.defaultBlockState(), 5, 3, 5, p_229729_);
                    this.placeBlock(p_229725_, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.WEST), 4, 3, 5, p_229729_);
                    this.placeBlock(p_229725_, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.EAST), 6, 3, 5, p_229729_);
                    this.placeBlock(p_229725_, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.SOUTH), 5, 3, 4, p_229729_);
                    this.placeBlock(p_229725_, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.NORTH), 5, 3, 6, p_229729_);
                    this.placeBlock(p_229725_, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 4, 1, 4, p_229729_);
                    this.placeBlock(p_229725_, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 4, 1, 5, p_229729_);
                    this.placeBlock(p_229725_, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 4, 1, 6, p_229729_);
                    this.placeBlock(p_229725_, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 6, 1, 4, p_229729_);
                    this.placeBlock(p_229725_, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 6, 1, 5, p_229729_);
                    this.placeBlock(p_229725_, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 6, 1, 6, p_229729_);
                    this.placeBlock(p_229725_, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 5, 1, 4, p_229729_);
                    this.placeBlock(p_229725_, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 5, 1, 6, p_229729_);
                    break;
                case 1:
                    for (int i1 = 0; i1 < 5; i1++) {
                        this.placeBlock(p_229725_, Blocks.STONE_BRICKS.defaultBlockState(), 3, 1, 3 + i1, p_229729_);
                        this.placeBlock(p_229725_, Blocks.STONE_BRICKS.defaultBlockState(), 7, 1, 3 + i1, p_229729_);
                        this.placeBlock(p_229725_, Blocks.STONE_BRICKS.defaultBlockState(), 3 + i1, 1, 3, p_229729_);
                        this.placeBlock(p_229725_, Blocks.STONE_BRICKS.defaultBlockState(), 3 + i1, 1, 7, p_229729_);
                    }

                    this.placeBlock(p_229725_, Blocks.STONE_BRICKS.defaultBlockState(), 5, 1, 5, p_229729_);
                    this.placeBlock(p_229725_, Blocks.STONE_BRICKS.defaultBlockState(), 5, 2, 5, p_229729_);
                    this.placeBlock(p_229725_, Blocks.STONE_BRICKS.defaultBlockState(), 5, 3, 5, p_229729_);
                    this.placeBlock(p_229725_, Blocks.WATER.defaultBlockState(), 5, 4, 5, p_229729_);
                    break;
                case 2:
                    for (int i = 1; i <= 9; i++) {
                        this.placeBlock(p_229725_, Blocks.COBBLESTONE.defaultBlockState(), 1, 3, i, p_229729_);
                        this.placeBlock(p_229725_, Blocks.COBBLESTONE.defaultBlockState(), 9, 3, i, p_229729_);
                    }

                    for (int j = 1; j <= 9; j++) {
                        this.placeBlock(p_229725_, Blocks.COBBLESTONE.defaultBlockState(), j, 3, 1, p_229729_);
                        this.placeBlock(p_229725_, Blocks.COBBLESTONE.defaultBlockState(), j, 3, 9, p_229729_);
                    }

                    this.placeBlock(p_229725_, Blocks.COBBLESTONE.defaultBlockState(), 5, 1, 4, p_229729_);
                    this.placeBlock(p_229725_, Blocks.COBBLESTONE.defaultBlockState(), 5, 1, 6, p_229729_);
                    this.placeBlock(p_229725_, Blocks.COBBLESTONE.defaultBlockState(), 5, 3, 4, p_229729_);
                    this.placeBlock(p_229725_, Blocks.COBBLESTONE.defaultBlockState(), 5, 3, 6, p_229729_);
                    this.placeBlock(p_229725_, Blocks.COBBLESTONE.defaultBlockState(), 4, 1, 5, p_229729_);
                    this.placeBlock(p_229725_, Blocks.COBBLESTONE.defaultBlockState(), 6, 1, 5, p_229729_);
                    this.placeBlock(p_229725_, Blocks.COBBLESTONE.defaultBlockState(), 4, 3, 5, p_229729_);
                    this.placeBlock(p_229725_, Blocks.COBBLESTONE.defaultBlockState(), 6, 3, 5, p_229729_);

                    for (int k = 1; k <= 3; k++) {
                        this.placeBlock(p_229725_, Blocks.COBBLESTONE.defaultBlockState(), 4, k, 4, p_229729_);
                        this.placeBlock(p_229725_, Blocks.COBBLESTONE.defaultBlockState(), 6, k, 4, p_229729_);
                        this.placeBlock(p_229725_, Blocks.COBBLESTONE.defaultBlockState(), 4, k, 6, p_229729_);
                        this.placeBlock(p_229725_, Blocks.COBBLESTONE.defaultBlockState(), 6, k, 6, p_229729_);
                    }

                    this.placeBlock(p_229725_, Blocks.WALL_TORCH.defaultBlockState(), 5, 3, 5, p_229729_);

                    for (int l = 2; l <= 8; l++) {
                        this.placeBlock(p_229725_, Blocks.OAK_PLANKS.defaultBlockState(), 2, 3, l, p_229729_);
                        this.placeBlock(p_229725_, Blocks.OAK_PLANKS.defaultBlockState(), 3, 3, l, p_229729_);
                        if (l <= 3 || l >= 7) {
                            this.placeBlock(p_229725_, Blocks.OAK_PLANKS.defaultBlockState(), 4, 3, l, p_229729_);
                            this.placeBlock(p_229725_, Blocks.OAK_PLANKS.defaultBlockState(), 5, 3, l, p_229729_);
                            this.placeBlock(p_229725_, Blocks.OAK_PLANKS.defaultBlockState(), 6, 3, l, p_229729_);
                        }

                        this.placeBlock(p_229725_, Blocks.OAK_PLANKS.defaultBlockState(), 7, 3, l, p_229729_);
                        this.placeBlock(p_229725_, Blocks.OAK_PLANKS.defaultBlockState(), 8, 3, l, p_229729_);
                    }

                    BlockState blockstate = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.WEST);
                    this.placeBlock(p_229725_, blockstate, 9, 1, 3, p_229729_);
                    this.placeBlock(p_229725_, blockstate, 9, 2, 3, p_229729_);
                    this.placeBlock(p_229725_, blockstate, 9, 3, 3, p_229729_);
                    this.createChest(p_229725_, p_229729_, p_229728_, 3, 4, 8, BuiltInLootTables.STRONGHOLD_CROSSING);
            }
        }
    }

    static class SmoothStoneSelector extends StructurePiece.BlockSelector {
        @Override
        public void next(RandomSource p_229749_, int p_229750_, int p_229751_, int p_229752_, boolean p_229753_) {
            if (p_229753_) {
                float f = p_229749_.nextFloat();
                if (f < 0.2F) {
                    this.next = Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
                } else if (f < 0.5F) {
                    this.next = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
                } else if (f < 0.55F) {
                    this.next = Blocks.INFESTED_STONE_BRICKS.defaultBlockState();
                } else {
                    this.next = Blocks.STONE_BRICKS.defaultBlockState();
                }
            } else {
                this.next = Blocks.CAVE_AIR.defaultBlockState();
            }
        }
    }

    public static class StairsDown extends StrongholdPieces.StrongholdPiece {
        private static final int WIDTH = 5;
        private static final int HEIGHT = 11;
        private static final int DEPTH = 5;
        private final boolean isSource;

        public StairsDown(StructurePieceType pType, int pGenDepth, int pX, int pZ, Direction pOrientation) {
            super(pType, pGenDepth, makeBoundingBox(pX, 64, pZ, pOrientation, 5, 11, 5));
            this.isSource = true;
            this.setOrientation(pOrientation);
            this.entryDoor = StrongholdPieces.StrongholdPiece.SmallDoorType.OPENING;
        }

        public StairsDown(int pGenDepth, RandomSource pRandom, BoundingBox pBox, Direction pOrientation) {
            super(StructurePieceType.STRONGHOLD_STAIRS_DOWN, pGenDepth, pBox);
            this.isSource = false;
            this.setOrientation(pOrientation);
            this.entryDoor = this.randomSmallDoor(pRandom);
        }

        public StairsDown(StructurePieceType p_229770_, CompoundTag p_229771_) {
            super(p_229770_, p_229771_);
            this.isSource = p_229771_.getBooleanOr("Source", false);
        }

        public StairsDown(CompoundTag pTag) {
            this(StructurePieceType.STRONGHOLD_STAIRS_DOWN, pTag);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext p_229795_, CompoundTag p_229796_) {
            super.addAdditionalSaveData(p_229795_, p_229796_);
            p_229796_.putBoolean("Source", this.isSource);
        }

        @Override
        public void addChildren(StructurePiece p_229783_, StructurePieceAccessor p_229784_, RandomSource p_229785_) {
            if (this.isSource) {
                StrongholdPieces.imposedPiece = StrongholdPieces.FiveCrossing.class;
            }

            this.generateSmallDoorChildForward((StrongholdPieces.StartPiece)p_229783_, p_229784_, p_229785_, 1, 1);
        }

        public static StrongholdPieces.StairsDown createPiece(
            StructurePieceAccessor pPieces, RandomSource pRandom, int pX, int pY, int pZ, Direction pOrientation, int pGenDepth
        ) {
            BoundingBox boundingbox = BoundingBox.orientBox(pX, pY, pZ, -1, -7, 0, 5, 11, 5, pOrientation);
            return isOkBox(boundingbox) && pPieces.findCollisionPiece(boundingbox) == null
                ? new StrongholdPieces.StairsDown(pGenDepth, pRandom, boundingbox, pOrientation)
                : null;
        }

        @Override
        public void postProcess(
            WorldGenLevel p_229775_,
            StructureManager p_229776_,
            ChunkGenerator p_229777_,
            RandomSource p_229778_,
            BoundingBox p_229779_,
            ChunkPos p_229780_,
            BlockPos p_229781_
        ) {
            this.generateBox(p_229775_, p_229779_, 0, 0, 0, 4, 10, 4, true, p_229778_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateSmallDoor(p_229775_, p_229778_, p_229779_, this.entryDoor, 1, 7, 0);
            this.generateSmallDoor(p_229775_, p_229778_, p_229779_, StrongholdPieces.StrongholdPiece.SmallDoorType.OPENING, 1, 1, 4);
            this.placeBlock(p_229775_, Blocks.STONE_BRICKS.defaultBlockState(), 2, 6, 1, p_229779_);
            this.placeBlock(p_229775_, Blocks.STONE_BRICKS.defaultBlockState(), 1, 5, 1, p_229779_);
            this.placeBlock(p_229775_, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 1, 6, 1, p_229779_);
            this.placeBlock(p_229775_, Blocks.STONE_BRICKS.defaultBlockState(), 1, 5, 2, p_229779_);
            this.placeBlock(p_229775_, Blocks.STONE_BRICKS.defaultBlockState(), 1, 4, 3, p_229779_);
            this.placeBlock(p_229775_, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 1, 5, 3, p_229779_);
            this.placeBlock(p_229775_, Blocks.STONE_BRICKS.defaultBlockState(), 2, 4, 3, p_229779_);
            this.placeBlock(p_229775_, Blocks.STONE_BRICKS.defaultBlockState(), 3, 3, 3, p_229779_);
            this.placeBlock(p_229775_, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 3, 4, 3, p_229779_);
            this.placeBlock(p_229775_, Blocks.STONE_BRICKS.defaultBlockState(), 3, 3, 2, p_229779_);
            this.placeBlock(p_229775_, Blocks.STONE_BRICKS.defaultBlockState(), 3, 2, 1, p_229779_);
            this.placeBlock(p_229775_, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 3, 3, 1, p_229779_);
            this.placeBlock(p_229775_, Blocks.STONE_BRICKS.defaultBlockState(), 2, 2, 1, p_229779_);
            this.placeBlock(p_229775_, Blocks.STONE_BRICKS.defaultBlockState(), 1, 1, 1, p_229779_);
            this.placeBlock(p_229775_, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 1, 2, 1, p_229779_);
            this.placeBlock(p_229775_, Blocks.STONE_BRICKS.defaultBlockState(), 1, 1, 2, p_229779_);
            this.placeBlock(p_229775_, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 1, 1, 3, p_229779_);
        }
    }

    public static class StartPiece extends StrongholdPieces.StairsDown {
        public StrongholdPieces.PieceWeight previousPiece;
        @Nullable
        public StrongholdPieces.PortalRoom portalRoomPiece;
        public final List<StructurePiece> pendingChildren = Lists.newArrayList();

        public StartPiece(RandomSource pRandom, int pX, int pZ) {
            super(StructurePieceType.STRONGHOLD_START, 0, pX, pZ, getRandomHorizontalDirection(pRandom));
        }

        public StartPiece(CompoundTag p_229805_) {
            super(StructurePieceType.STRONGHOLD_START, p_229805_);
        }

        @Override
        public BlockPos getLocatorPosition() {
            return this.portalRoomPiece != null ? this.portalRoomPiece.getLocatorPosition() : super.getLocatorPosition();
        }
    }

    public static class Straight extends StrongholdPieces.StrongholdPiece {
        private static final int WIDTH = 5;
        private static final int HEIGHT = 5;
        private static final int DEPTH = 7;
        private final boolean leftChild;
        private final boolean rightChild;

        public Straight(int pGenDepth, RandomSource pRandom, BoundingBox pBox, Direction pOrientation) {
            super(StructurePieceType.STRONGHOLD_STRAIGHT, pGenDepth, pBox);
            this.setOrientation(pOrientation);
            this.entryDoor = this.randomSmallDoor(pRandom);
            this.leftChild = pRandom.nextInt(2) == 0;
            this.rightChild = pRandom.nextInt(2) == 0;
        }

        public Straight(CompoundTag pTag) {
            super(StructurePieceType.STRONGHOLD_STRAIGHT, pTag);
            this.leftChild = pTag.getBooleanOr("Left", false);
            this.rightChild = pTag.getBooleanOr("Right", false);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext p_229840_, CompoundTag p_229841_) {
            super.addAdditionalSaveData(p_229840_, p_229841_);
            p_229841_.putBoolean("Left", this.leftChild);
            p_229841_.putBoolean("Right", this.rightChild);
        }

        @Override
        public void addChildren(StructurePiece p_229828_, StructurePieceAccessor p_229829_, RandomSource p_229830_) {
            this.generateSmallDoorChildForward((StrongholdPieces.StartPiece)p_229828_, p_229829_, p_229830_, 1, 1);
            if (this.leftChild) {
                this.generateSmallDoorChildLeft((StrongholdPieces.StartPiece)p_229828_, p_229829_, p_229830_, 1, 2);
            }

            if (this.rightChild) {
                this.generateSmallDoorChildRight((StrongholdPieces.StartPiece)p_229828_, p_229829_, p_229830_, 1, 2);
            }
        }

        public static StrongholdPieces.Straight createPiece(
            StructurePieceAccessor pPieces, RandomSource pRandom, int pX, int pY, int pZ, Direction pDirection, int pGenDepth
        ) {
            BoundingBox boundingbox = BoundingBox.orientBox(pX, pY, pZ, -1, -1, 0, 5, 5, 7, pDirection);
            return isOkBox(boundingbox) && pPieces.findCollisionPiece(boundingbox) == null
                ? new StrongholdPieces.Straight(pGenDepth, pRandom, boundingbox, pDirection)
                : null;
        }

        @Override
        public void postProcess(
            WorldGenLevel p_229820_,
            StructureManager p_229821_,
            ChunkGenerator p_229822_,
            RandomSource p_229823_,
            BoundingBox p_229824_,
            ChunkPos p_229825_,
            BlockPos p_229826_
        ) {
            this.generateBox(p_229820_, p_229824_, 0, 0, 0, 4, 4, 6, true, p_229823_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateSmallDoor(p_229820_, p_229823_, p_229824_, this.entryDoor, 1, 1, 0);
            this.generateSmallDoor(p_229820_, p_229823_, p_229824_, StrongholdPieces.StrongholdPiece.SmallDoorType.OPENING, 1, 1, 6);
            BlockState blockstate = Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.EAST);
            BlockState blockstate1 = Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.WEST);
            this.maybeGenerateBlock(p_229820_, p_229824_, p_229823_, 0.1F, 1, 2, 1, blockstate);
            this.maybeGenerateBlock(p_229820_, p_229824_, p_229823_, 0.1F, 3, 2, 1, blockstate1);
            this.maybeGenerateBlock(p_229820_, p_229824_, p_229823_, 0.1F, 1, 2, 5, blockstate);
            this.maybeGenerateBlock(p_229820_, p_229824_, p_229823_, 0.1F, 3, 2, 5, blockstate1);
            if (this.leftChild) {
                this.generateBox(p_229820_, p_229824_, 0, 1, 2, 0, 3, 4, CAVE_AIR, CAVE_AIR, false);
            }

            if (this.rightChild) {
                this.generateBox(p_229820_, p_229824_, 4, 1, 2, 4, 3, 4, CAVE_AIR, CAVE_AIR, false);
            }
        }
    }

    public static class StraightStairsDown extends StrongholdPieces.StrongholdPiece {
        private static final int WIDTH = 5;
        private static final int HEIGHT = 11;
        private static final int DEPTH = 8;

        public StraightStairsDown(int pGenDepth, RandomSource pRandom, BoundingBox pBox, Direction pOrientation) {
            super(StructurePieceType.STRONGHOLD_STRAIGHT_STAIRS_DOWN, pGenDepth, pBox);
            this.setOrientation(pOrientation);
            this.entryDoor = this.randomSmallDoor(pRandom);
        }

        public StraightStairsDown(CompoundTag pTag) {
            super(StructurePieceType.STRONGHOLD_STRAIGHT_STAIRS_DOWN, pTag);
        }

        @Override
        public void addChildren(StructurePiece p_229861_, StructurePieceAccessor p_229862_, RandomSource p_229863_) {
            this.generateSmallDoorChildForward((StrongholdPieces.StartPiece)p_229861_, p_229862_, p_229863_, 1, 1);
        }

        public static StrongholdPieces.StraightStairsDown createPiece(
            StructurePieceAccessor pPieces, RandomSource pRandom, int pX, int pY, int pZ, Direction pOrientation, int pGenDepth
        ) {
            BoundingBox boundingbox = BoundingBox.orientBox(pX, pY, pZ, -1, -7, 0, 5, 11, 8, pOrientation);
            return isOkBox(boundingbox) && pPieces.findCollisionPiece(boundingbox) == null
                ? new StrongholdPieces.StraightStairsDown(pGenDepth, pRandom, boundingbox, pOrientation)
                : null;
        }

        @Override
        public void postProcess(
            WorldGenLevel p_229853_,
            StructureManager p_229854_,
            ChunkGenerator p_229855_,
            RandomSource p_229856_,
            BoundingBox p_229857_,
            ChunkPos p_229858_,
            BlockPos p_229859_
        ) {
            this.generateBox(p_229853_, p_229857_, 0, 0, 0, 4, 10, 7, true, p_229856_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateSmallDoor(p_229853_, p_229856_, p_229857_, this.entryDoor, 1, 7, 0);
            this.generateSmallDoor(p_229853_, p_229856_, p_229857_, StrongholdPieces.StrongholdPiece.SmallDoorType.OPENING, 1, 1, 7);
            BlockState blockstate = Blocks.COBBLESTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH);

            for (int i = 0; i < 6; i++) {
                this.placeBlock(p_229853_, blockstate, 1, 6 - i, 1 + i, p_229857_);
                this.placeBlock(p_229853_, blockstate, 2, 6 - i, 1 + i, p_229857_);
                this.placeBlock(p_229853_, blockstate, 3, 6 - i, 1 + i, p_229857_);
                if (i < 5) {
                    this.placeBlock(p_229853_, Blocks.STONE_BRICKS.defaultBlockState(), 1, 5 - i, 1 + i, p_229857_);
                    this.placeBlock(p_229853_, Blocks.STONE_BRICKS.defaultBlockState(), 2, 5 - i, 1 + i, p_229857_);
                    this.placeBlock(p_229853_, Blocks.STONE_BRICKS.defaultBlockState(), 3, 5 - i, 1 + i, p_229857_);
                }
            }
        }
    }

    abstract static class StrongholdPiece extends StructurePiece {
        protected StrongholdPieces.StrongholdPiece.SmallDoorType entryDoor = StrongholdPieces.StrongholdPiece.SmallDoorType.OPENING;

        protected StrongholdPiece(StructurePieceType p_229874_, int p_229875_, BoundingBox p_229876_) {
            super(p_229874_, p_229875_, p_229876_);
        }

        public StrongholdPiece(StructurePieceType p_229878_, CompoundTag p_229879_) {
            super(p_229878_, p_229879_);
            this.entryDoor = p_229879_.read("EntryDoor", StrongholdPieces.StrongholdPiece.SmallDoorType.LEGACY_CODEC).orElseThrow();
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext p_229891_, CompoundTag p_229892_) {
            p_229892_.store("EntryDoor", StrongholdPieces.StrongholdPiece.SmallDoorType.LEGACY_CODEC, this.entryDoor);
        }

        protected void generateSmallDoor(
            WorldGenLevel pLevel,
            RandomSource pRandom,
            BoundingBox pBox,
            StrongholdPieces.StrongholdPiece.SmallDoorType pType,
            int pX,
            int pY,
            int pZ
        ) {
            switch (pType) {
                case OPENING:
                    this.generateBox(
                        pLevel, pBox, pX, pY, pZ, pX + 3 - 1, pY + 3 - 1, pZ, CAVE_AIR, CAVE_AIR, false
                    );
                    break;
                case WOOD_DOOR:
                    this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), pX, pY, pZ, pBox);
                    this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), pX, pY + 1, pZ, pBox);
                    this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), pX, pY + 2, pZ, pBox);
                    this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), pX + 1, pY + 2, pZ, pBox);
                    this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), pX + 2, pY + 2, pZ, pBox);
                    this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), pX + 2, pY + 1, pZ, pBox);
                    this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), pX + 2, pY, pZ, pBox);
                    this.placeBlock(pLevel, Blocks.OAK_DOOR.defaultBlockState(), pX + 1, pY, pZ, pBox);
                    this.placeBlock(
                        pLevel,
                        Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER),
                        pX + 1,
                        pY + 1,
                        pZ,
                        pBox
                    );
                    break;
                case GRATES:
                    this.placeBlock(pLevel, Blocks.CAVE_AIR.defaultBlockState(), pX + 1, pY, pZ, pBox);
                    this.placeBlock(pLevel, Blocks.CAVE_AIR.defaultBlockState(), pX + 1, pY + 1, pZ, pBox);
                    this.placeBlock(pLevel, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.WEST, true), pX, pY, pZ, pBox);
                    this.placeBlock(pLevel, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.WEST, true), pX, pY + 1, pZ, pBox);
                    this.placeBlock(
                        pLevel,
                        Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.EAST, true).setValue(IronBarsBlock.WEST, true),
                        pX,
                        pY + 2,
                        pZ,
                        pBox
                    );
                    this.placeBlock(
                        pLevel,
                        Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.EAST, true).setValue(IronBarsBlock.WEST, true),
                        pX + 1,
                        pY + 2,
                        pZ,
                        pBox
                    );
                    this.placeBlock(
                        pLevel,
                        Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.EAST, true).setValue(IronBarsBlock.WEST, true),
                        pX + 2,
                        pY + 2,
                        pZ,
                        pBox
                    );
                    this.placeBlock(
                        pLevel, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.EAST, true), pX + 2, pY + 1, pZ, pBox
                    );
                    this.placeBlock(pLevel, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.EAST, true), pX + 2, pY, pZ, pBox);
                    break;
                case IRON_DOOR:
                    this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), pX, pY, pZ, pBox);
                    this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), pX, pY + 1, pZ, pBox);
                    this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), pX, pY + 2, pZ, pBox);
                    this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), pX + 1, pY + 2, pZ, pBox);
                    this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), pX + 2, pY + 2, pZ, pBox);
                    this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), pX + 2, pY + 1, pZ, pBox);
                    this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), pX + 2, pY, pZ, pBox);
                    this.placeBlock(pLevel, Blocks.IRON_DOOR.defaultBlockState(), pX + 1, pY, pZ, pBox);
                    this.placeBlock(
                        pLevel,
                        Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER),
                        pX + 1,
                        pY + 1,
                        pZ,
                        pBox
                    );
                    this.placeBlock(
                        pLevel,
                        Blocks.STONE_BUTTON.defaultBlockState().setValue(ButtonBlock.FACING, Direction.NORTH),
                        pX + 2,
                        pY + 1,
                        pZ + 1,
                        pBox
                    );
                    this.placeBlock(
                        pLevel,
                        Blocks.STONE_BUTTON.defaultBlockState().setValue(ButtonBlock.FACING, Direction.SOUTH),
                        pX + 2,
                        pY + 1,
                        pZ - 1,
                        pBox
                    );
            }
        }

        protected StrongholdPieces.StrongholdPiece.SmallDoorType randomSmallDoor(RandomSource pRandom) {
            int i = pRandom.nextInt(5);
            switch (i) {
                case 0:
                case 1:
                default:
                    return StrongholdPieces.StrongholdPiece.SmallDoorType.OPENING;
                case 2:
                    return StrongholdPieces.StrongholdPiece.SmallDoorType.WOOD_DOOR;
                case 3:
                    return StrongholdPieces.StrongholdPiece.SmallDoorType.GRATES;
                case 4:
                    return StrongholdPieces.StrongholdPiece.SmallDoorType.IRON_DOOR;
            }
        }

        @Nullable
        protected StructurePiece generateSmallDoorChildForward(
            StrongholdPieces.StartPiece pStartPiece, StructurePieceAccessor pPieces, RandomSource pRandom, int pOffsetX, int pOffsetY
        ) {
            Direction direction = this.getOrientation();
            if (direction != null) {
                switch (direction) {
                    case NORTH:
                        return StrongholdPieces.generateAndAddPiece(
                            pStartPiece,
                            pPieces,
                            pRandom,
                            this.boundingBox.minX() + pOffsetX,
                            this.boundingBox.minY() + pOffsetY,
                            this.boundingBox.minZ() - 1,
                            direction,
                            this.getGenDepth()
                        );
                    case SOUTH:
                        return StrongholdPieces.generateAndAddPiece(
                            pStartPiece,
                            pPieces,
                            pRandom,
                            this.boundingBox.minX() + pOffsetX,
                            this.boundingBox.minY() + pOffsetY,
                            this.boundingBox.maxZ() + 1,
                            direction,
                            this.getGenDepth()
                        );
                    case WEST:
                        return StrongholdPieces.generateAndAddPiece(
                            pStartPiece,
                            pPieces,
                            pRandom,
                            this.boundingBox.minX() - 1,
                            this.boundingBox.minY() + pOffsetY,
                            this.boundingBox.minZ() + pOffsetX,
                            direction,
                            this.getGenDepth()
                        );
                    case EAST:
                        return StrongholdPieces.generateAndAddPiece(
                            pStartPiece,
                            pPieces,
                            pRandom,
                            this.boundingBox.maxX() + 1,
                            this.boundingBox.minY() + pOffsetY,
                            this.boundingBox.minZ() + pOffsetX,
                            direction,
                            this.getGenDepth()
                        );
                }
            }

            return null;
        }

        @Nullable
        protected StructurePiece generateSmallDoorChildLeft(
            StrongholdPieces.StartPiece pStartPiece, StructurePieceAccessor pPieces, RandomSource pRandom, int pOffsetY, int pOffsetX
        ) {
            Direction direction = this.getOrientation();
            if (direction != null) {
                switch (direction) {
                    case NORTH:
                        return StrongholdPieces.generateAndAddPiece(
                            pStartPiece,
                            pPieces,
                            pRandom,
                            this.boundingBox.minX() - 1,
                            this.boundingBox.minY() + pOffsetY,
                            this.boundingBox.minZ() + pOffsetX,
                            Direction.WEST,
                            this.getGenDepth()
                        );
                    case SOUTH:
                        return StrongholdPieces.generateAndAddPiece(
                            pStartPiece,
                            pPieces,
                            pRandom,
                            this.boundingBox.minX() - 1,
                            this.boundingBox.minY() + pOffsetY,
                            this.boundingBox.minZ() + pOffsetX,
                            Direction.WEST,
                            this.getGenDepth()
                        );
                    case WEST:
                        return StrongholdPieces.generateAndAddPiece(
                            pStartPiece,
                            pPieces,
                            pRandom,
                            this.boundingBox.minX() + pOffsetX,
                            this.boundingBox.minY() + pOffsetY,
                            this.boundingBox.minZ() - 1,
                            Direction.NORTH,
                            this.getGenDepth()
                        );
                    case EAST:
                        return StrongholdPieces.generateAndAddPiece(
                            pStartPiece,
                            pPieces,
                            pRandom,
                            this.boundingBox.minX() + pOffsetX,
                            this.boundingBox.minY() + pOffsetY,
                            this.boundingBox.minZ() - 1,
                            Direction.NORTH,
                            this.getGenDepth()
                        );
                }
            }

            return null;
        }

        @Nullable
        protected StructurePiece generateSmallDoorChildRight(
            StrongholdPieces.StartPiece pStartPiece, StructurePieceAccessor pPieces, RandomSource pRandom, int pOffsetY, int pOffsetX
        ) {
            Direction direction = this.getOrientation();
            if (direction != null) {
                switch (direction) {
                    case NORTH:
                        return StrongholdPieces.generateAndAddPiece(
                            pStartPiece,
                            pPieces,
                            pRandom,
                            this.boundingBox.maxX() + 1,
                            this.boundingBox.minY() + pOffsetY,
                            this.boundingBox.minZ() + pOffsetX,
                            Direction.EAST,
                            this.getGenDepth()
                        );
                    case SOUTH:
                        return StrongholdPieces.generateAndAddPiece(
                            pStartPiece,
                            pPieces,
                            pRandom,
                            this.boundingBox.maxX() + 1,
                            this.boundingBox.minY() + pOffsetY,
                            this.boundingBox.minZ() + pOffsetX,
                            Direction.EAST,
                            this.getGenDepth()
                        );
                    case WEST:
                        return StrongholdPieces.generateAndAddPiece(
                            pStartPiece,
                            pPieces,
                            pRandom,
                            this.boundingBox.minX() + pOffsetX,
                            this.boundingBox.minY() + pOffsetY,
                            this.boundingBox.maxZ() + 1,
                            Direction.SOUTH,
                            this.getGenDepth()
                        );
                    case EAST:
                        return StrongholdPieces.generateAndAddPiece(
                            pStartPiece,
                            pPieces,
                            pRandom,
                            this.boundingBox.minX() + pOffsetX,
                            this.boundingBox.minY() + pOffsetY,
                            this.boundingBox.maxZ() + 1,
                            Direction.SOUTH,
                            this.getGenDepth()
                        );
                }
            }

            return null;
        }

        protected static boolean isOkBox(BoundingBox pBox) {
            return pBox != null && pBox.minY() > 10;
        }

        protected static enum SmallDoorType {
            OPENING,
            WOOD_DOOR,
            GRATES,
            IRON_DOOR;

            @Deprecated
            public static final Codec<StrongholdPieces.StrongholdPiece.SmallDoorType> LEGACY_CODEC = ExtraCodecs.legacyEnum(
                StrongholdPieces.StrongholdPiece.SmallDoorType::valueOf
            );
        }
    }

    public abstract static class Turn extends StrongholdPieces.StrongholdPiece {
        protected static final int WIDTH = 5;
        protected static final int HEIGHT = 5;
        protected static final int DEPTH = 5;

        protected Turn(StructurePieceType p_229930_, int p_229931_, BoundingBox p_229932_) {
            super(p_229930_, p_229931_, p_229932_);
        }

        public Turn(StructurePieceType p_229934_, CompoundTag p_229935_) {
            super(p_229934_, p_229935_);
        }
    }
}
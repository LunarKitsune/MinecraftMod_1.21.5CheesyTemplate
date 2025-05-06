package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class NetherFortressPieces {
    private static final int MAX_DEPTH = 30;
    private static final int LOWEST_Y_POSITION = 10;
    public static final int MAGIC_START_Y = 64;
    static final NetherFortressPieces.PieceWeight[] BRIDGE_PIECE_WEIGHTS = new NetherFortressPieces.PieceWeight[]{
        new NetherFortressPieces.PieceWeight(NetherFortressPieces.BridgeStraight.class, 30, 0, true),
        new NetherFortressPieces.PieceWeight(NetherFortressPieces.BridgeCrossing.class, 10, 4),
        new NetherFortressPieces.PieceWeight(NetherFortressPieces.RoomCrossing.class, 10, 4),
        new NetherFortressPieces.PieceWeight(NetherFortressPieces.StairsRoom.class, 10, 3),
        new NetherFortressPieces.PieceWeight(NetherFortressPieces.MonsterThrone.class, 5, 2),
        new NetherFortressPieces.PieceWeight(NetherFortressPieces.CastleEntrance.class, 5, 1)
    };
    static final NetherFortressPieces.PieceWeight[] CASTLE_PIECE_WEIGHTS = new NetherFortressPieces.PieceWeight[]{
        new NetherFortressPieces.PieceWeight(NetherFortressPieces.CastleSmallCorridorPiece.class, 25, 0, true),
        new NetherFortressPieces.PieceWeight(NetherFortressPieces.CastleSmallCorridorCrossingPiece.class, 15, 5),
        new NetherFortressPieces.PieceWeight(NetherFortressPieces.CastleSmallCorridorRightTurnPiece.class, 5, 10),
        new NetherFortressPieces.PieceWeight(NetherFortressPieces.CastleSmallCorridorLeftTurnPiece.class, 5, 10),
        new NetherFortressPieces.PieceWeight(NetherFortressPieces.CastleCorridorStairsPiece.class, 10, 3, true),
        new NetherFortressPieces.PieceWeight(NetherFortressPieces.CastleCorridorTBalconyPiece.class, 7, 2),
        new NetherFortressPieces.PieceWeight(NetherFortressPieces.CastleStalkRoom.class, 5, 2)
    };

    static NetherFortressPieces.NetherBridgePiece findAndCreateBridgePieceFactory(
        NetherFortressPieces.PieceWeight pWeight,
        StructurePieceAccessor pPieces,
        RandomSource pRandom,
        int pX,
        int pY,
        int pZ,
        Direction pOrientation,
        int pGenDepth
    ) {
        Class<? extends NetherFortressPieces.NetherBridgePiece> oclass = pWeight.pieceClass;
        NetherFortressPieces.NetherBridgePiece netherfortresspieces$netherbridgepiece = null;
        if (oclass == NetherFortressPieces.BridgeStraight.class) {
            netherfortresspieces$netherbridgepiece = NetherFortressPieces.BridgeStraight.createPiece(
                pPieces, pRandom, pX, pY, pZ, pOrientation, pGenDepth
            );
        } else if (oclass == NetherFortressPieces.BridgeCrossing.class) {
            netherfortresspieces$netherbridgepiece = NetherFortressPieces.BridgeCrossing.createPiece(
                pPieces, pX, pY, pZ, pOrientation, pGenDepth
            );
        } else if (oclass == NetherFortressPieces.RoomCrossing.class) {
            netherfortresspieces$netherbridgepiece = NetherFortressPieces.RoomCrossing.createPiece(
                pPieces, pX, pY, pZ, pOrientation, pGenDepth
            );
        } else if (oclass == NetherFortressPieces.StairsRoom.class) {
            netherfortresspieces$netherbridgepiece = NetherFortressPieces.StairsRoom.createPiece(pPieces, pX, pY, pZ, pGenDepth, pOrientation);
        } else if (oclass == NetherFortressPieces.MonsterThrone.class) {
            netherfortresspieces$netherbridgepiece = NetherFortressPieces.MonsterThrone.createPiece(
                pPieces, pX, pY, pZ, pGenDepth, pOrientation
            );
        } else if (oclass == NetherFortressPieces.CastleEntrance.class) {
            netherfortresspieces$netherbridgepiece = NetherFortressPieces.CastleEntrance.createPiece(
                pPieces, pRandom, pX, pY, pZ, pOrientation, pGenDepth
            );
        } else if (oclass == NetherFortressPieces.CastleSmallCorridorPiece.class) {
            netherfortresspieces$netherbridgepiece = NetherFortressPieces.CastleSmallCorridorPiece.createPiece(
                pPieces, pX, pY, pZ, pOrientation, pGenDepth
            );
        } else if (oclass == NetherFortressPieces.CastleSmallCorridorRightTurnPiece.class) {
            netherfortresspieces$netherbridgepiece = NetherFortressPieces.CastleSmallCorridorRightTurnPiece.createPiece(
                pPieces, pRandom, pX, pY, pZ, pOrientation, pGenDepth
            );
        } else if (oclass == NetherFortressPieces.CastleSmallCorridorLeftTurnPiece.class) {
            netherfortresspieces$netherbridgepiece = NetherFortressPieces.CastleSmallCorridorLeftTurnPiece.createPiece(
                pPieces, pRandom, pX, pY, pZ, pOrientation, pGenDepth
            );
        } else if (oclass == NetherFortressPieces.CastleCorridorStairsPiece.class) {
            netherfortresspieces$netherbridgepiece = NetherFortressPieces.CastleCorridorStairsPiece.createPiece(
                pPieces, pX, pY, pZ, pOrientation, pGenDepth
            );
        } else if (oclass == NetherFortressPieces.CastleCorridorTBalconyPiece.class) {
            netherfortresspieces$netherbridgepiece = NetherFortressPieces.CastleCorridorTBalconyPiece.createPiece(
                pPieces, pX, pY, pZ, pOrientation, pGenDepth
            );
        } else if (oclass == NetherFortressPieces.CastleSmallCorridorCrossingPiece.class) {
            netherfortresspieces$netherbridgepiece = NetherFortressPieces.CastleSmallCorridorCrossingPiece.createPiece(
                pPieces, pX, pY, pZ, pOrientation, pGenDepth
            );
        } else if (oclass == NetherFortressPieces.CastleStalkRoom.class) {
            netherfortresspieces$netherbridgepiece = NetherFortressPieces.CastleStalkRoom.createPiece(
                pPieces, pX, pY, pZ, pOrientation, pGenDepth
            );
        }

        return netherfortresspieces$netherbridgepiece;
    }

    public static class BridgeCrossing extends NetherFortressPieces.NetherBridgePiece {
        private static final int WIDTH = 19;
        private static final int HEIGHT = 10;
        private static final int DEPTH = 19;

        public BridgeCrossing(int pGenDepth, BoundingBox pBox, Direction pOrientation) {
            super(StructurePieceType.NETHER_FORTRESS_BRIDGE_CROSSING, pGenDepth, pBox);
            this.setOrientation(pOrientation);
        }

        protected BridgeCrossing(int pX, int pZ, Direction pOrientation) {
            super(StructurePieceType.NETHER_FORTRESS_BRIDGE_CROSSING, 0, StructurePiece.makeBoundingBox(pX, 64, pZ, pOrientation, 19, 10, 19));
            this.setOrientation(pOrientation);
        }

        protected BridgeCrossing(StructurePieceType p_228030_, CompoundTag p_228031_) {
            super(p_228030_, p_228031_);
        }

        public BridgeCrossing(CompoundTag pTag) {
            this(StructurePieceType.NETHER_FORTRESS_BRIDGE_CROSSING, pTag);
        }

        @Override
        public void addChildren(StructurePiece p_228043_, StructurePieceAccessor p_228044_, RandomSource p_228045_) {
            this.generateChildForward((NetherFortressPieces.StartPiece)p_228043_, p_228044_, p_228045_, 8, 3, false);
            this.generateChildLeft((NetherFortressPieces.StartPiece)p_228043_, p_228044_, p_228045_, 3, 8, false);
            this.generateChildRight((NetherFortressPieces.StartPiece)p_228043_, p_228044_, p_228045_, 3, 8, false);
        }

        public static NetherFortressPieces.BridgeCrossing createPiece(
            StructurePieceAccessor pPieces, int pX, int pY, int pZ, Direction pOrientation, int pGenDepth
        ) {
            BoundingBox boundingbox = BoundingBox.orientBox(pX, pY, pZ, -8, -3, 0, 19, 10, 19, pOrientation);
            return isOkBox(boundingbox) && pPieces.findCollisionPiece(boundingbox) == null
                ? new NetherFortressPieces.BridgeCrossing(pGenDepth, boundingbox, pOrientation)
                : null;
        }

        @Override
        public void postProcess(
            WorldGenLevel p_228035_,
            StructureManager p_228036_,
            ChunkGenerator p_228037_,
            RandomSource p_228038_,
            BoundingBox p_228039_,
            ChunkPos p_228040_,
            BlockPos p_228041_
        ) {
            this.generateBox(p_228035_, p_228039_, 7, 3, 0, 11, 4, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228035_, p_228039_, 0, 3, 7, 18, 4, 11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228035_, p_228039_, 8, 5, 0, 10, 7, 18, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(p_228035_, p_228039_, 0, 5, 8, 18, 7, 10, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(p_228035_, p_228039_, 7, 5, 0, 7, 5, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228035_, p_228039_, 7, 5, 11, 7, 5, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228035_, p_228039_, 11, 5, 0, 11, 5, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228035_, p_228039_, 11, 5, 11, 11, 5, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228035_, p_228039_, 0, 5, 7, 7, 5, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228035_, p_228039_, 11, 5, 7, 18, 5, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228035_, p_228039_, 0, 5, 11, 7, 5, 11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228035_, p_228039_, 11, 5, 11, 18, 5, 11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228035_, p_228039_, 7, 2, 0, 11, 2, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228035_, p_228039_, 7, 2, 13, 11, 2, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228035_, p_228039_, 7, 0, 0, 11, 1, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228035_, p_228039_, 7, 0, 15, 11, 1, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

            for (int i = 7; i <= 11; i++) {
                for (int j = 0; j <= 2; j++) {
                    this.fillColumnDown(p_228035_, Blocks.NETHER_BRICKS.defaultBlockState(), i, -1, j, p_228039_);
                    this.fillColumnDown(p_228035_, Blocks.NETHER_BRICKS.defaultBlockState(), i, -1, 18 - j, p_228039_);
                }
            }

            this.generateBox(p_228035_, p_228039_, 0, 2, 7, 5, 2, 11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228035_, p_228039_, 13, 2, 7, 18, 2, 11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228035_, p_228039_, 0, 0, 7, 3, 1, 11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228035_, p_228039_, 15, 0, 7, 18, 1, 11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

            for (int k = 0; k <= 2; k++) {
                for (int l = 7; l <= 11; l++) {
                    this.fillColumnDown(p_228035_, Blocks.NETHER_BRICKS.defaultBlockState(), k, -1, l, p_228039_);
                    this.fillColumnDown(p_228035_, Blocks.NETHER_BRICKS.defaultBlockState(), 18 - k, -1, l, p_228039_);
                }
            }
        }
    }

    public static class BridgeEndFiller extends NetherFortressPieces.NetherBridgePiece {
        private static final int WIDTH = 5;
        private static final int HEIGHT = 10;
        private static final int DEPTH = 8;
        private final int selfSeed;

        public BridgeEndFiller(int pGenDepth, RandomSource pRandom, BoundingBox pBox, Direction pOrientation) {
            super(StructurePieceType.NETHER_FORTRESS_BRIDGE_END_FILLER, pGenDepth, pBox);
            this.setOrientation(pOrientation);
            this.selfSeed = pRandom.nextInt();
        }

        public BridgeEndFiller(CompoundTag pTag) {
            super(StructurePieceType.NETHER_FORTRESS_BRIDGE_END_FILLER, pTag);
            this.selfSeed = pTag.getIntOr("Seed", 0);
        }

        public static NetherFortressPieces.BridgeEndFiller createPiece(
            StructurePieceAccessor pPieces, RandomSource pRandom, int pX, int pY, int pZ, Direction pOrientation, int pGenDepth
        ) {
            BoundingBox boundingbox = BoundingBox.orientBox(pX, pY, pZ, -1, -3, 0, 5, 10, 8, pOrientation);
            return isOkBox(boundingbox) && pPieces.findCollisionPiece(boundingbox) == null
                ? new NetherFortressPieces.BridgeEndFiller(pGenDepth, pRandom, boundingbox, pOrientation)
                : null;
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext p_228081_, CompoundTag p_228082_) {
            super.addAdditionalSaveData(p_228081_, p_228082_);
            p_228082_.putInt("Seed", this.selfSeed);
        }

        @Override
        public void postProcess(
            WorldGenLevel p_228065_,
            StructureManager p_228066_,
            ChunkGenerator p_228067_,
            RandomSource p_228068_,
            BoundingBox p_228069_,
            ChunkPos p_228070_,
            BlockPos p_228071_
        ) {
            RandomSource randomsource = RandomSource.create(this.selfSeed);

            for (int i = 0; i <= 4; i++) {
                for (int j = 3; j <= 4; j++) {
                    int k = randomsource.nextInt(8);
                    this.generateBox(p_228065_, p_228069_, i, j, 0, i, j, k, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
                }
            }

            int l = randomsource.nextInt(8);
            this.generateBox(p_228065_, p_228069_, 0, 5, 0, 0, 5, l, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            l = randomsource.nextInt(8);
            this.generateBox(p_228065_, p_228069_, 4, 5, 0, 4, 5, l, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

            for (int i1 = 0; i1 <= 4; i1++) {
                int k1 = randomsource.nextInt(5);
                this.generateBox(p_228065_, p_228069_, i1, 2, 0, i1, 2, k1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            }

            for (int j1 = 0; j1 <= 4; j1++) {
                for (int l1 = 0; l1 <= 1; l1++) {
                    int i2 = randomsource.nextInt(3);
                    this.generateBox(p_228065_, p_228069_, j1, l1, 0, j1, l1, i2, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
                }
            }
        }
    }

    public static class BridgeStraight extends NetherFortressPieces.NetherBridgePiece {
        private static final int WIDTH = 5;
        private static final int HEIGHT = 10;
        private static final int DEPTH = 19;

        public BridgeStraight(int pGenDepth, RandomSource pRandom, BoundingBox pBox, Direction pOrientation) {
            super(StructurePieceType.NETHER_FORTRESS_BRIDGE_STRAIGHT, pGenDepth, pBox);
            this.setOrientation(pOrientation);
        }

        public BridgeStraight(CompoundTag pTag) {
            super(StructurePieceType.NETHER_FORTRESS_BRIDGE_STRAIGHT, pTag);
        }

        @Override
        public void addChildren(StructurePiece p_228102_, StructurePieceAccessor p_228103_, RandomSource p_228104_) {
            this.generateChildForward((NetherFortressPieces.StartPiece)p_228102_, p_228103_, p_228104_, 1, 3, false);
        }

        public static NetherFortressPieces.BridgeStraight createPiece(
            StructurePieceAccessor pPieces, RandomSource pRandom, int pX, int pY, int pZ, Direction pOrientation, int pGenDepth
        ) {
            BoundingBox boundingbox = BoundingBox.orientBox(pX, pY, pZ, -1, -3, 0, 5, 10, 19, pOrientation);
            return isOkBox(boundingbox) && pPieces.findCollisionPiece(boundingbox) == null
                ? new NetherFortressPieces.BridgeStraight(pGenDepth, pRandom, boundingbox, pOrientation)
                : null;
        }

        @Override
        public void postProcess(
            WorldGenLevel p_228094_,
            StructureManager p_228095_,
            ChunkGenerator p_228096_,
            RandomSource p_228097_,
            BoundingBox p_228098_,
            ChunkPos p_228099_,
            BlockPos p_228100_
        ) {
            this.generateBox(p_228094_, p_228098_, 0, 3, 0, 4, 4, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228094_, p_228098_, 1, 5, 0, 3, 7, 18, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(p_228094_, p_228098_, 0, 5, 0, 0, 5, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228094_, p_228098_, 4, 5, 0, 4, 5, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228094_, p_228098_, 0, 2, 0, 4, 2, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228094_, p_228098_, 0, 2, 13, 4, 2, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228094_, p_228098_, 0, 0, 0, 4, 1, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228094_, p_228098_, 0, 0, 15, 4, 1, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

            for (int i = 0; i <= 4; i++) {
                for (int j = 0; j <= 2; j++) {
                    this.fillColumnDown(p_228094_, Blocks.NETHER_BRICKS.defaultBlockState(), i, -1, j, p_228098_);
                    this.fillColumnDown(p_228094_, Blocks.NETHER_BRICKS.defaultBlockState(), i, -1, 18 - j, p_228098_);
                }
            }

            BlockState blockstate1 = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.SOUTH, true);
            BlockState blockstate2 = blockstate1.setValue(FenceBlock.EAST, true);
            BlockState blockstate = blockstate1.setValue(FenceBlock.WEST, true);
            this.generateBox(p_228094_, p_228098_, 0, 1, 1, 0, 4, 1, blockstate2, blockstate2, false);
            this.generateBox(p_228094_, p_228098_, 0, 3, 4, 0, 4, 4, blockstate2, blockstate2, false);
            this.generateBox(p_228094_, p_228098_, 0, 3, 14, 0, 4, 14, blockstate2, blockstate2, false);
            this.generateBox(p_228094_, p_228098_, 0, 1, 17, 0, 4, 17, blockstate2, blockstate2, false);
            this.generateBox(p_228094_, p_228098_, 4, 1, 1, 4, 4, 1, blockstate, blockstate, false);
            this.generateBox(p_228094_, p_228098_, 4, 3, 4, 4, 4, 4, blockstate, blockstate, false);
            this.generateBox(p_228094_, p_228098_, 4, 3, 14, 4, 4, 14, blockstate, blockstate, false);
            this.generateBox(p_228094_, p_228098_, 4, 1, 17, 4, 4, 17, blockstate, blockstate, false);
        }
    }

    public static class CastleCorridorStairsPiece extends NetherFortressPieces.NetherBridgePiece {
        private static final int WIDTH = 5;
        private static final int HEIGHT = 14;
        private static final int DEPTH = 10;

        public CastleCorridorStairsPiece(int pGenDepth, BoundingBox pBox, Direction pOrientation) {
            super(StructurePieceType.NETHER_FORTRESS_CASTLE_CORRIDOR_STAIRS, pGenDepth, pBox);
            this.setOrientation(pOrientation);
        }

        public CastleCorridorStairsPiece(CompoundTag pTag) {
            super(StructurePieceType.NETHER_FORTRESS_CASTLE_CORRIDOR_STAIRS, pTag);
        }

        @Override
        public void addChildren(StructurePiece p_228131_, StructurePieceAccessor p_228132_, RandomSource p_228133_) {
            this.generateChildForward((NetherFortressPieces.StartPiece)p_228131_, p_228132_, p_228133_, 1, 0, true);
        }

        public static NetherFortressPieces.CastleCorridorStairsPiece createPiece(
            StructurePieceAccessor pPieces, int pX, int pY, int pZ, Direction pOrientation, int pGenDepth
        ) {
            BoundingBox boundingbox = BoundingBox.orientBox(pX, pY, pZ, -1, -7, 0, 5, 14, 10, pOrientation);
            return isOkBox(boundingbox) && pPieces.findCollisionPiece(boundingbox) == null
                ? new NetherFortressPieces.CastleCorridorStairsPiece(pGenDepth, boundingbox, pOrientation)
                : null;
        }

        @Override
        public void postProcess(
            WorldGenLevel p_228123_,
            StructureManager p_228124_,
            ChunkGenerator p_228125_,
            RandomSource p_228126_,
            BoundingBox p_228127_,
            ChunkPos p_228128_,
            BlockPos p_228129_
        ) {
            BlockState blockstate = Blocks.NETHER_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH);
            BlockState blockstate1 = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.SOUTH, true);

            for (int i = 0; i <= 9; i++) {
                int j = Math.max(1, 7 - i);
                int k = Math.min(Math.max(j + 5, 14 - i), 13);
                int l = i;
                this.generateBox(p_228123_, p_228127_, 0, 0, i, 4, j, i, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
                this.generateBox(p_228123_, p_228127_, 1, j + 1, i, 3, k - 1, i, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
                if (i <= 6) {
                    this.placeBlock(p_228123_, blockstate, 1, j + 1, i, p_228127_);
                    this.placeBlock(p_228123_, blockstate, 2, j + 1, i, p_228127_);
                    this.placeBlock(p_228123_, blockstate, 3, j + 1, i, p_228127_);
                }

                this.generateBox(p_228123_, p_228127_, 0, k, i, 4, k, i, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
                this.generateBox(p_228123_, p_228127_, 0, j + 1, i, 0, k - 1, i, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
                this.generateBox(p_228123_, p_228127_, 4, j + 1, i, 4, k - 1, i, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
                if ((i & 1) == 0) {
                    this.generateBox(p_228123_, p_228127_, 0, j + 2, i, 0, j + 3, i, blockstate1, blockstate1, false);
                    this.generateBox(p_228123_, p_228127_, 4, j + 2, i, 4, j + 3, i, blockstate1, blockstate1, false);
                }

                for (int i1 = 0; i1 <= 4; i1++) {
                    this.fillColumnDown(p_228123_, Blocks.NETHER_BRICKS.defaultBlockState(), i1, -1, l, p_228127_);
                }
            }
        }
    }

    public static class CastleCorridorTBalconyPiece extends NetherFortressPieces.NetherBridgePiece {
        private static final int WIDTH = 9;
        private static final int HEIGHT = 7;
        private static final int DEPTH = 9;

        public CastleCorridorTBalconyPiece(int pGenDepth, BoundingBox pBox, Direction pOrientation) {
            super(StructurePieceType.NETHER_FORTRESS_CASTLE_CORRIDOR_T_BALCONY, pGenDepth, pBox);
            this.setOrientation(pOrientation);
        }

        public CastleCorridorTBalconyPiece(CompoundTag pTag) {
            super(StructurePieceType.NETHER_FORTRESS_CASTLE_CORRIDOR_T_BALCONY, pTag);
        }

        @Override
        public void addChildren(StructurePiece p_228159_, StructurePieceAccessor p_228160_, RandomSource p_228161_) {
            int i = 1;
            Direction direction = this.getOrientation();
            if (direction == Direction.WEST || direction == Direction.NORTH) {
                i = 5;
            }

            this.generateChildLeft((NetherFortressPieces.StartPiece)p_228159_, p_228160_, p_228161_, 0, i, p_228161_.nextInt(8) > 0);
            this.generateChildRight((NetherFortressPieces.StartPiece)p_228159_, p_228160_, p_228161_, 0, i, p_228161_.nextInt(8) > 0);
        }

        public static NetherFortressPieces.CastleCorridorTBalconyPiece createPiece(
            StructurePieceAccessor pPieces, int pX, int pY, int pZ, Direction pOrientation, int pGenDepth
        ) {
            BoundingBox boundingbox = BoundingBox.orientBox(pX, pY, pZ, -3, 0, 0, 9, 7, 9, pOrientation);
            return isOkBox(boundingbox) && pPieces.findCollisionPiece(boundingbox) == null
                ? new NetherFortressPieces.CastleCorridorTBalconyPiece(pGenDepth, boundingbox, pOrientation)
                : null;
        }

        @Override
        public void postProcess(
            WorldGenLevel p_228151_,
            StructureManager p_228152_,
            ChunkGenerator p_228153_,
            RandomSource p_228154_,
            BoundingBox p_228155_,
            ChunkPos p_228156_,
            BlockPos p_228157_
        ) {
            BlockState blockstate = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.SOUTH, true);
            BlockState blockstate1 = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true).setValue(FenceBlock.EAST, true);
            this.generateBox(p_228151_, p_228155_, 0, 0, 0, 8, 1, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228151_, p_228155_, 0, 2, 0, 8, 5, 8, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(p_228151_, p_228155_, 0, 6, 0, 8, 6, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228151_, p_228155_, 0, 2, 0, 2, 5, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228151_, p_228155_, 6, 2, 0, 8, 5, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228151_, p_228155_, 1, 3, 0, 1, 4, 0, blockstate1, blockstate1, false);
            this.generateBox(p_228151_, p_228155_, 7, 3, 0, 7, 4, 0, blockstate1, blockstate1, false);
            this.generateBox(p_228151_, p_228155_, 0, 2, 4, 8, 2, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228151_, p_228155_, 1, 1, 4, 2, 2, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(p_228151_, p_228155_, 6, 1, 4, 7, 2, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(p_228151_, p_228155_, 1, 3, 8, 7, 3, 8, blockstate1, blockstate1, false);
            this.placeBlock(p_228151_, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.EAST, true).setValue(FenceBlock.SOUTH, true), 0, 3, 8, p_228155_);
            this.placeBlock(p_228151_, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true).setValue(FenceBlock.SOUTH, true), 8, 3, 8, p_228155_);
            this.generateBox(p_228151_, p_228155_, 0, 3, 6, 0, 3, 7, blockstate, blockstate, false);
            this.generateBox(p_228151_, p_228155_, 8, 3, 6, 8, 3, 7, blockstate, blockstate, false);
            this.generateBox(p_228151_, p_228155_, 0, 3, 4, 0, 5, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228151_, p_228155_, 8, 3, 4, 8, 5, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228151_, p_228155_, 1, 3, 5, 2, 5, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228151_, p_228155_, 6, 3, 5, 7, 5, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228151_, p_228155_, 1, 4, 5, 1, 5, 5, blockstate1, blockstate1, false);
            this.generateBox(p_228151_, p_228155_, 7, 4, 5, 7, 5, 5, blockstate1, blockstate1, false);

            for (int i = 0; i <= 5; i++) {
                for (int j = 0; j <= 8; j++) {
                    this.fillColumnDown(p_228151_, Blocks.NETHER_BRICKS.defaultBlockState(), j, -1, i, p_228155_);
                }
            }
        }
    }

    public static class CastleEntrance extends NetherFortressPieces.NetherBridgePiece {
        private static final int WIDTH = 13;
        private static final int HEIGHT = 14;
        private static final int DEPTH = 13;

        public CastleEntrance(int pGenDepth, RandomSource pRandom, BoundingBox pBox, Direction pOrientation) {
            super(StructurePieceType.NETHER_FORTRESS_CASTLE_ENTRANCE, pGenDepth, pBox);
            this.setOrientation(pOrientation);
        }

        public CastleEntrance(CompoundTag pTag) {
            super(StructurePieceType.NETHER_FORTRESS_CASTLE_ENTRANCE, pTag);
        }

        @Override
        public void addChildren(StructurePiece p_228188_, StructurePieceAccessor p_228189_, RandomSource p_228190_) {
            this.generateChildForward((NetherFortressPieces.StartPiece)p_228188_, p_228189_, p_228190_, 5, 3, true);
        }

        public static NetherFortressPieces.CastleEntrance createPiece(
            StructurePieceAccessor pPieces, RandomSource pRandom, int pX, int pY, int pZ, Direction pOrientation, int pGenDepth
        ) {
            BoundingBox boundingbox = BoundingBox.orientBox(pX, pY, pZ, -5, -3, 0, 13, 14, 13, pOrientation);
            return isOkBox(boundingbox) && pPieces.findCollisionPiece(boundingbox) == null
                ? new NetherFortressPieces.CastleEntrance(pGenDepth, pRandom, boundingbox, pOrientation)
                : null;
        }

        @Override
        public void postProcess(
            WorldGenLevel p_228180_,
            StructureManager p_228181_,
            ChunkGenerator p_228182_,
            RandomSource p_228183_,
            BoundingBox p_228184_,
            ChunkPos p_228185_,
            BlockPos p_228186_
        ) {
            this.generateBox(p_228180_, p_228184_, 0, 3, 0, 12, 4, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228180_, p_228184_, 0, 5, 0, 12, 13, 12, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(p_228180_, p_228184_, 0, 5, 0, 1, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228180_, p_228184_, 11, 5, 0, 12, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228180_, p_228184_, 2, 5, 11, 4, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228180_, p_228184_, 8, 5, 11, 10, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228180_, p_228184_, 5, 9, 11, 7, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228180_, p_228184_, 2, 5, 0, 4, 12, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228180_, p_228184_, 8, 5, 0, 10, 12, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228180_, p_228184_, 5, 9, 0, 7, 12, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228180_, p_228184_, 2, 11, 2, 10, 12, 10, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228180_, p_228184_, 5, 8, 0, 7, 8, 0, Blocks.NETHER_BRICK_FENCE.defaultBlockState(), Blocks.NETHER_BRICK_FENCE.defaultBlockState(), false);
            BlockState blockstate = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true).setValue(FenceBlock.EAST, true);
            BlockState blockstate1 = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.SOUTH, true);

            for (int i = 1; i <= 11; i += 2) {
                this.generateBox(p_228180_, p_228184_, i, 10, 0, i, 11, 0, blockstate, blockstate, false);
                this.generateBox(p_228180_, p_228184_, i, 10, 12, i, 11, 12, blockstate, blockstate, false);
                this.generateBox(p_228180_, p_228184_, 0, 10, i, 0, 11, i, blockstate1, blockstate1, false);
                this.generateBox(p_228180_, p_228184_, 12, 10, i, 12, 11, i, blockstate1, blockstate1, false);
                this.placeBlock(p_228180_, Blocks.NETHER_BRICKS.defaultBlockState(), i, 13, 0, p_228184_);
                this.placeBlock(p_228180_, Blocks.NETHER_BRICKS.defaultBlockState(), i, 13, 12, p_228184_);
                this.placeBlock(p_228180_, Blocks.NETHER_BRICKS.defaultBlockState(), 0, 13, i, p_228184_);
                this.placeBlock(p_228180_, Blocks.NETHER_BRICKS.defaultBlockState(), 12, 13, i, p_228184_);
                if (i != 11) {
                    this.placeBlock(p_228180_, blockstate, i + 1, 13, 0, p_228184_);
                    this.placeBlock(p_228180_, blockstate, i + 1, 13, 12, p_228184_);
                    this.placeBlock(p_228180_, blockstate1, 0, 13, i + 1, p_228184_);
                    this.placeBlock(p_228180_, blockstate1, 12, 13, i + 1, p_228184_);
                }
            }

            this.placeBlock(p_228180_, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.EAST, true), 0, 13, 0, p_228184_);
            this.placeBlock(p_228180_, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.SOUTH, true).setValue(FenceBlock.EAST, true), 0, 13, 12, p_228184_);
            this.placeBlock(p_228180_, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.SOUTH, true).setValue(FenceBlock.WEST, true), 12, 13, 12, p_228184_);
            this.placeBlock(p_228180_, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.WEST, true), 12, 13, 0, p_228184_);

            for (int k = 3; k <= 9; k += 2) {
                this.generateBox(
                    p_228180_,
                    p_228184_,
                    1,
                    7,
                    k,
                    1,
                    8,
                    k,
                    blockstate1.setValue(FenceBlock.WEST, true),
                    blockstate1.setValue(FenceBlock.WEST, true),
                    false
                );
                this.generateBox(
                    p_228180_,
                    p_228184_,
                    11,
                    7,
                    k,
                    11,
                    8,
                    k,
                    blockstate1.setValue(FenceBlock.EAST, true),
                    blockstate1.setValue(FenceBlock.EAST, true),
                    false
                );
            }

            this.generateBox(p_228180_, p_228184_, 4, 2, 0, 8, 2, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228180_, p_228184_, 0, 2, 4, 12, 2, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228180_, p_228184_, 4, 0, 0, 8, 1, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228180_, p_228184_, 4, 0, 9, 8, 1, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228180_, p_228184_, 0, 0, 4, 3, 1, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228180_, p_228184_, 9, 0, 4, 12, 1, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

            for (int l = 4; l <= 8; l++) {
                for (int j = 0; j <= 2; j++) {
                    this.fillColumnDown(p_228180_, Blocks.NETHER_BRICKS.defaultBlockState(), l, -1, j, p_228184_);
                    this.fillColumnDown(p_228180_, Blocks.NETHER_BRICKS.defaultBlockState(), l, -1, 12 - j, p_228184_);
                }
            }

            for (int i1 = 0; i1 <= 2; i1++) {
                for (int j1 = 4; j1 <= 8; j1++) {
                    this.fillColumnDown(p_228180_, Blocks.NETHER_BRICKS.defaultBlockState(), i1, -1, j1, p_228184_);
                    this.fillColumnDown(p_228180_, Blocks.NETHER_BRICKS.defaultBlockState(), 12 - i1, -1, j1, p_228184_);
                }
            }

            this.generateBox(p_228180_, p_228184_, 5, 5, 5, 7, 5, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228180_, p_228184_, 6, 1, 6, 6, 4, 6, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.placeBlock(p_228180_, Blocks.NETHER_BRICKS.defaultBlockState(), 6, 0, 6, p_228184_);
            this.placeBlock(p_228180_, Blocks.LAVA.defaultBlockState(), 6, 5, 6, p_228184_);
            BlockPos blockpos = this.getWorldPos(6, 5, 6);
            if (p_228184_.isInside(blockpos)) {
                p_228180_.scheduleTick(blockpos, Fluids.LAVA, 0);
            }
        }
    }

    public static class CastleSmallCorridorCrossingPiece extends NetherFortressPieces.NetherBridgePiece {
        private static final int WIDTH = 5;
        private static final int HEIGHT = 7;
        private static final int DEPTH = 5;

        public CastleSmallCorridorCrossingPiece(int pGenDepth, BoundingBox pBox, Direction pOrientation) {
            super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR_CROSSING, pGenDepth, pBox);
            this.setOrientation(pOrientation);
        }

        public CastleSmallCorridorCrossingPiece(CompoundTag pTag) {
            super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR_CROSSING, pTag);
        }

        @Override
        public void addChildren(StructurePiece p_228217_, StructurePieceAccessor p_228218_, RandomSource p_228219_) {
            this.generateChildForward((NetherFortressPieces.StartPiece)p_228217_, p_228218_, p_228219_, 1, 0, true);
            this.generateChildLeft((NetherFortressPieces.StartPiece)p_228217_, p_228218_, p_228219_, 0, 1, true);
            this.generateChildRight((NetherFortressPieces.StartPiece)p_228217_, p_228218_, p_228219_, 0, 1, true);
        }

        public static NetherFortressPieces.CastleSmallCorridorCrossingPiece createPiece(
            StructurePieceAccessor pPieces, int pX, int pY, int pZ, Direction pOrientation, int pGenDepth
        ) {
            BoundingBox boundingbox = BoundingBox.orientBox(pX, pY, pZ, -1, 0, 0, 5, 7, 5, pOrientation);
            return isOkBox(boundingbox) && pPieces.findCollisionPiece(boundingbox) == null
                ? new NetherFortressPieces.CastleSmallCorridorCrossingPiece(pGenDepth, boundingbox, pOrientation)
                : null;
        }

        @Override
        public void postProcess(
            WorldGenLevel p_228209_,
            StructureManager p_228210_,
            ChunkGenerator p_228211_,
            RandomSource p_228212_,
            BoundingBox p_228213_,
            ChunkPos p_228214_,
            BlockPos p_228215_
        ) {
            this.generateBox(p_228209_, p_228213_, 0, 0, 0, 4, 1, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228209_, p_228213_, 0, 2, 0, 4, 5, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(p_228209_, p_228213_, 0, 2, 0, 0, 5, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228209_, p_228213_, 4, 2, 0, 4, 5, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228209_, p_228213_, 0, 2, 4, 0, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228209_, p_228213_, 4, 2, 4, 4, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228209_, p_228213_, 0, 6, 0, 4, 6, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

            for (int i = 0; i <= 4; i++) {
                for (int j = 0; j <= 4; j++) {
                    this.fillColumnDown(p_228209_, Blocks.NETHER_BRICKS.defaultBlockState(), i, -1, j, p_228213_);
                }
            }
        }
    }

    public static class CastleSmallCorridorLeftTurnPiece extends NetherFortressPieces.NetherBridgePiece {
        private static final int WIDTH = 5;
        private static final int HEIGHT = 7;
        private static final int DEPTH = 5;
        private boolean isNeedingChest;

        public CastleSmallCorridorLeftTurnPiece(int pGenDepth, RandomSource pRandom, BoundingBox pBox, Direction pOrientation) {
            super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR_LEFT_TURN, pGenDepth, pBox);
            this.setOrientation(pOrientation);
            this.isNeedingChest = pRandom.nextInt(3) == 0;
        }

        public CastleSmallCorridorLeftTurnPiece(CompoundTag pTag) {
            super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR_LEFT_TURN, pTag);
            this.isNeedingChest = pTag.getBooleanOr("Chest", false);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext p_228259_, CompoundTag p_228260_) {
            super.addAdditionalSaveData(p_228259_, p_228260_);
            p_228260_.putBoolean("Chest", this.isNeedingChest);
        }

        @Override
        public void addChildren(StructurePiece p_228247_, StructurePieceAccessor p_228248_, RandomSource p_228249_) {
            this.generateChildLeft((NetherFortressPieces.StartPiece)p_228247_, p_228248_, p_228249_, 0, 1, true);
        }

        public static NetherFortressPieces.CastleSmallCorridorLeftTurnPiece createPiece(
            StructurePieceAccessor pPieces, RandomSource pRandom, int pX, int pY, int pZ, Direction pOrientation, int pGenDepth
        ) {
            BoundingBox boundingbox = BoundingBox.orientBox(pX, pY, pZ, -1, 0, 0, 5, 7, 5, pOrientation);
            return isOkBox(boundingbox) && pPieces.findCollisionPiece(boundingbox) == null
                ? new NetherFortressPieces.CastleSmallCorridorLeftTurnPiece(pGenDepth, pRandom, boundingbox, pOrientation)
                : null;
        }

        @Override
        public void postProcess(
            WorldGenLevel p_228239_,
            StructureManager p_228240_,
            ChunkGenerator p_228241_,
            RandomSource p_228242_,
            BoundingBox p_228243_,
            ChunkPos p_228244_,
            BlockPos p_228245_
        ) {
            this.generateBox(p_228239_, p_228243_, 0, 0, 0, 4, 1, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228239_, p_228243_, 0, 2, 0, 4, 5, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            BlockState blockstate = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true).setValue(FenceBlock.EAST, true);
            BlockState blockstate1 = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.SOUTH, true);
            this.generateBox(p_228239_, p_228243_, 4, 2, 0, 4, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228239_, p_228243_, 4, 3, 1, 4, 4, 1, blockstate1, blockstate1, false);
            this.generateBox(p_228239_, p_228243_, 4, 3, 3, 4, 4, 3, blockstate1, blockstate1, false);
            this.generateBox(p_228239_, p_228243_, 0, 2, 0, 0, 5, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228239_, p_228243_, 0, 2, 4, 3, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228239_, p_228243_, 1, 3, 4, 1, 4, 4, blockstate, blockstate, false);
            this.generateBox(p_228239_, p_228243_, 3, 3, 4, 3, 4, 4, blockstate, blockstate, false);
            if (this.isNeedingChest && p_228243_.isInside(this.getWorldPos(3, 2, 3))) {
                this.isNeedingChest = false;
                this.createChest(p_228239_, p_228243_, p_228242_, 3, 2, 3, BuiltInLootTables.NETHER_BRIDGE);
            }

            this.generateBox(p_228239_, p_228243_, 0, 6, 0, 4, 6, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

            for (int i = 0; i <= 4; i++) {
                for (int j = 0; j <= 4; j++) {
                    this.fillColumnDown(p_228239_, Blocks.NETHER_BRICKS.defaultBlockState(), i, -1, j, p_228243_);
                }
            }
        }
    }

    public static class CastleSmallCorridorPiece extends NetherFortressPieces.NetherBridgePiece {
        private static final int WIDTH = 5;
        private static final int HEIGHT = 7;
        private static final int DEPTH = 5;

        public CastleSmallCorridorPiece(int pGenDepth, BoundingBox pBox, Direction pOrientation) {
            super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR, pGenDepth, pBox);
            this.setOrientation(pOrientation);
        }

        public CastleSmallCorridorPiece(CompoundTag pTag) {
            super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR, pTag);
        }

        @Override
        public void addChildren(StructurePiece p_228279_, StructurePieceAccessor p_228280_, RandomSource p_228281_) {
            this.generateChildForward((NetherFortressPieces.StartPiece)p_228279_, p_228280_, p_228281_, 1, 0, true);
        }

        public static NetherFortressPieces.CastleSmallCorridorPiece createPiece(
            StructurePieceAccessor pPieces, int pX, int pY, int pZ, Direction pOrientation, int pGenDepth
        ) {
            BoundingBox boundingbox = BoundingBox.orientBox(pX, pY, pZ, -1, 0, 0, 5, 7, 5, pOrientation);
            return isOkBox(boundingbox) && pPieces.findCollisionPiece(boundingbox) == null
                ? new NetherFortressPieces.CastleSmallCorridorPiece(pGenDepth, boundingbox, pOrientation)
                : null;
        }

        @Override
        public void postProcess(
            WorldGenLevel p_228271_,
            StructureManager p_228272_,
            ChunkGenerator p_228273_,
            RandomSource p_228274_,
            BoundingBox p_228275_,
            ChunkPos p_228276_,
            BlockPos p_228277_
        ) {
            this.generateBox(p_228271_, p_228275_, 0, 0, 0, 4, 1, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228271_, p_228275_, 0, 2, 0, 4, 5, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            BlockState blockstate = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.SOUTH, true);
            this.generateBox(p_228271_, p_228275_, 0, 2, 0, 0, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228271_, p_228275_, 4, 2, 0, 4, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228271_, p_228275_, 0, 3, 1, 0, 4, 1, blockstate, blockstate, false);
            this.generateBox(p_228271_, p_228275_, 0, 3, 3, 0, 4, 3, blockstate, blockstate, false);
            this.generateBox(p_228271_, p_228275_, 4, 3, 1, 4, 4, 1, blockstate, blockstate, false);
            this.generateBox(p_228271_, p_228275_, 4, 3, 3, 4, 4, 3, blockstate, blockstate, false);
            this.generateBox(p_228271_, p_228275_, 0, 6, 0, 4, 6, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

            for (int i = 0; i <= 4; i++) {
                for (int j = 0; j <= 4; j++) {
                    this.fillColumnDown(p_228271_, Blocks.NETHER_BRICKS.defaultBlockState(), i, -1, j, p_228275_);
                }
            }
        }
    }

    public static class CastleSmallCorridorRightTurnPiece extends NetherFortressPieces.NetherBridgePiece {
        private static final int WIDTH = 5;
        private static final int HEIGHT = 7;
        private static final int DEPTH = 5;
        private boolean isNeedingChest;

        public CastleSmallCorridorRightTurnPiece(int pGenDepth, RandomSource pRandom, BoundingBox pBox, Direction pOrientation) {
            super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR_RIGHT_TURN, pGenDepth, pBox);
            this.setOrientation(pOrientation);
            this.isNeedingChest = pRandom.nextInt(3) == 0;
        }

        public CastleSmallCorridorRightTurnPiece(CompoundTag pTag) {
            super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR_RIGHT_TURN, pTag);
            this.isNeedingChest = pTag.getBooleanOr("Chest", false);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext p_228321_, CompoundTag p_228322_) {
            super.addAdditionalSaveData(p_228321_, p_228322_);
            p_228322_.putBoolean("Chest", this.isNeedingChest);
        }

        @Override
        public void addChildren(StructurePiece p_228309_, StructurePieceAccessor p_228310_, RandomSource p_228311_) {
            this.generateChildRight((NetherFortressPieces.StartPiece)p_228309_, p_228310_, p_228311_, 0, 1, true);
        }

        public static NetherFortressPieces.CastleSmallCorridorRightTurnPiece createPiece(
            StructurePieceAccessor pPieces, RandomSource pRandom, int pX, int pY, int pZ, Direction pOrientation, int pGenDepth
        ) {
            BoundingBox boundingbox = BoundingBox.orientBox(pX, pY, pZ, -1, 0, 0, 5, 7, 5, pOrientation);
            return isOkBox(boundingbox) && pPieces.findCollisionPiece(boundingbox) == null
                ? new NetherFortressPieces.CastleSmallCorridorRightTurnPiece(pGenDepth, pRandom, boundingbox, pOrientation)
                : null;
        }

        @Override
        public void postProcess(
            WorldGenLevel p_228301_,
            StructureManager p_228302_,
            ChunkGenerator p_228303_,
            RandomSource p_228304_,
            BoundingBox p_228305_,
            ChunkPos p_228306_,
            BlockPos p_228307_
        ) {
            this.generateBox(p_228301_, p_228305_, 0, 0, 0, 4, 1, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228301_, p_228305_, 0, 2, 0, 4, 5, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            BlockState blockstate = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true).setValue(FenceBlock.EAST, true);
            BlockState blockstate1 = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.SOUTH, true);
            this.generateBox(p_228301_, p_228305_, 0, 2, 0, 0, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228301_, p_228305_, 0, 3, 1, 0, 4, 1, blockstate1, blockstate1, false);
            this.generateBox(p_228301_, p_228305_, 0, 3, 3, 0, 4, 3, blockstate1, blockstate1, false);
            this.generateBox(p_228301_, p_228305_, 4, 2, 0, 4, 5, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228301_, p_228305_, 1, 2, 4, 4, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228301_, p_228305_, 1, 3, 4, 1, 4, 4, blockstate, blockstate, false);
            this.generateBox(p_228301_, p_228305_, 3, 3, 4, 3, 4, 4, blockstate, blockstate, false);
            if (this.isNeedingChest && p_228305_.isInside(this.getWorldPos(1, 2, 3))) {
                this.isNeedingChest = false;
                this.createChest(p_228301_, p_228305_, p_228304_, 1, 2, 3, BuiltInLootTables.NETHER_BRIDGE);
            }

            this.generateBox(p_228301_, p_228305_, 0, 6, 0, 4, 6, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

            for (int i = 0; i <= 4; i++) {
                for (int j = 0; j <= 4; j++) {
                    this.fillColumnDown(p_228301_, Blocks.NETHER_BRICKS.defaultBlockState(), i, -1, j, p_228305_);
                }
            }
        }
    }

    public static class CastleStalkRoom extends NetherFortressPieces.NetherBridgePiece {
        private static final int WIDTH = 13;
        private static final int HEIGHT = 14;
        private static final int DEPTH = 13;

        public CastleStalkRoom(int pGenDepth, BoundingBox pBox, Direction pOrientation) {
            super(StructurePieceType.NETHER_FORTRESS_CASTLE_STALK_ROOM, pGenDepth, pBox);
            this.setOrientation(pOrientation);
        }

        public CastleStalkRoom(CompoundTag pTag) {
            super(StructurePieceType.NETHER_FORTRESS_CASTLE_STALK_ROOM, pTag);
        }

        @Override
        public void addChildren(StructurePiece p_228341_, StructurePieceAccessor p_228342_, RandomSource p_228343_) {
            this.generateChildForward((NetherFortressPieces.StartPiece)p_228341_, p_228342_, p_228343_, 5, 3, true);
            this.generateChildForward((NetherFortressPieces.StartPiece)p_228341_, p_228342_, p_228343_, 5, 11, true);
        }

        public static NetherFortressPieces.CastleStalkRoom createPiece(
            StructurePieceAccessor pPieces, int pX, int pY, int pZ, Direction pOrientation, int pGenDepth
        ) {
            BoundingBox boundingbox = BoundingBox.orientBox(pX, pY, pZ, -5, -3, 0, 13, 14, 13, pOrientation);
            return isOkBox(boundingbox) && pPieces.findCollisionPiece(boundingbox) == null
                ? new NetherFortressPieces.CastleStalkRoom(pGenDepth, boundingbox, pOrientation)
                : null;
        }

        @Override
        public void postProcess(
            WorldGenLevel p_228333_,
            StructureManager p_228334_,
            ChunkGenerator p_228335_,
            RandomSource p_228336_,
            BoundingBox p_228337_,
            ChunkPos p_228338_,
            BlockPos p_228339_
        ) {
            this.generateBox(p_228333_, p_228337_, 0, 3, 0, 12, 4, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228333_, p_228337_, 0, 5, 0, 12, 13, 12, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(p_228333_, p_228337_, 0, 5, 0, 1, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228333_, p_228337_, 11, 5, 0, 12, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228333_, p_228337_, 2, 5, 11, 4, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228333_, p_228337_, 8, 5, 11, 10, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228333_, p_228337_, 5, 9, 11, 7, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228333_, p_228337_, 2, 5, 0, 4, 12, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228333_, p_228337_, 8, 5, 0, 10, 12, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228333_, p_228337_, 5, 9, 0, 7, 12, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228333_, p_228337_, 2, 11, 2, 10, 12, 10, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            BlockState blockstate = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true).setValue(FenceBlock.EAST, true);
            BlockState blockstate1 = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.SOUTH, true);
            BlockState blockstate2 = blockstate1.setValue(FenceBlock.WEST, true);
            BlockState blockstate3 = blockstate1.setValue(FenceBlock.EAST, true);

            for (int i = 1; i <= 11; i += 2) {
                this.generateBox(p_228333_, p_228337_, i, 10, 0, i, 11, 0, blockstate, blockstate, false);
                this.generateBox(p_228333_, p_228337_, i, 10, 12, i, 11, 12, blockstate, blockstate, false);
                this.generateBox(p_228333_, p_228337_, 0, 10, i, 0, 11, i, blockstate1, blockstate1, false);
                this.generateBox(p_228333_, p_228337_, 12, 10, i, 12, 11, i, blockstate1, blockstate1, false);
                this.placeBlock(p_228333_, Blocks.NETHER_BRICKS.defaultBlockState(), i, 13, 0, p_228337_);
                this.placeBlock(p_228333_, Blocks.NETHER_BRICKS.defaultBlockState(), i, 13, 12, p_228337_);
                this.placeBlock(p_228333_, Blocks.NETHER_BRICKS.defaultBlockState(), 0, 13, i, p_228337_);
                this.placeBlock(p_228333_, Blocks.NETHER_BRICKS.defaultBlockState(), 12, 13, i, p_228337_);
                if (i != 11) {
                    this.placeBlock(p_228333_, blockstate, i + 1, 13, 0, p_228337_);
                    this.placeBlock(p_228333_, blockstate, i + 1, 13, 12, p_228337_);
                    this.placeBlock(p_228333_, blockstate1, 0, 13, i + 1, p_228337_);
                    this.placeBlock(p_228333_, blockstate1, 12, 13, i + 1, p_228337_);
                }
            }

            this.placeBlock(p_228333_, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.EAST, true), 0, 13, 0, p_228337_);
            this.placeBlock(p_228333_, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.SOUTH, true).setValue(FenceBlock.EAST, true), 0, 13, 12, p_228337_);
            this.placeBlock(p_228333_, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.SOUTH, true).setValue(FenceBlock.WEST, true), 12, 13, 12, p_228337_);
            this.placeBlock(p_228333_, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.WEST, true), 12, 13, 0, p_228337_);

            for (int j1 = 3; j1 <= 9; j1 += 2) {
                this.generateBox(p_228333_, p_228337_, 1, 7, j1, 1, 8, j1, blockstate2, blockstate2, false);
                this.generateBox(p_228333_, p_228337_, 11, 7, j1, 11, 8, j1, blockstate3, blockstate3, false);
            }

            BlockState blockstate4 = Blocks.NETHER_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);

            for (int j = 0; j <= 6; j++) {
                int k = j + 4;

                for (int l = 5; l <= 7; l++) {
                    this.placeBlock(p_228333_, blockstate4, l, 5 + j, k, p_228337_);
                }

                if (k >= 5 && k <= 8) {
                    this.generateBox(p_228333_, p_228337_, 5, 5, k, 7, j + 4, k, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
                } else if (k >= 9 && k <= 10) {
                    this.generateBox(p_228333_, p_228337_, 5, 8, k, 7, j + 4, k, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
                }

                if (j >= 1) {
                    this.generateBox(p_228333_, p_228337_, 5, 6 + j, k, 7, 9 + j, k, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
                }
            }

            for (int k1 = 5; k1 <= 7; k1++) {
                this.placeBlock(p_228333_, blockstate4, k1, 12, 11, p_228337_);
            }

            this.generateBox(p_228333_, p_228337_, 5, 6, 7, 5, 7, 7, blockstate3, blockstate3, false);
            this.generateBox(p_228333_, p_228337_, 7, 6, 7, 7, 7, 7, blockstate2, blockstate2, false);
            this.generateBox(p_228333_, p_228337_, 5, 13, 12, 7, 13, 12, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(p_228333_, p_228337_, 2, 5, 2, 3, 5, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228333_, p_228337_, 2, 5, 9, 3, 5, 10, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228333_, p_228337_, 2, 5, 4, 2, 5, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228333_, p_228337_, 9, 5, 2, 10, 5, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228333_, p_228337_, 9, 5, 9, 10, 5, 10, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228333_, p_228337_, 10, 5, 4, 10, 5, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            BlockState blockstate5 = blockstate4.setValue(StairBlock.FACING, Direction.EAST);
            BlockState blockstate6 = blockstate4.setValue(StairBlock.FACING, Direction.WEST);
            this.placeBlock(p_228333_, blockstate6, 4, 5, 2, p_228337_);
            this.placeBlock(p_228333_, blockstate6, 4, 5, 3, p_228337_);
            this.placeBlock(p_228333_, blockstate6, 4, 5, 9, p_228337_);
            this.placeBlock(p_228333_, blockstate6, 4, 5, 10, p_228337_);
            this.placeBlock(p_228333_, blockstate5, 8, 5, 2, p_228337_);
            this.placeBlock(p_228333_, blockstate5, 8, 5, 3, p_228337_);
            this.placeBlock(p_228333_, blockstate5, 8, 5, 9, p_228337_);
            this.placeBlock(p_228333_, blockstate5, 8, 5, 10, p_228337_);
            this.generateBox(p_228333_, p_228337_, 3, 4, 4, 4, 4, 8, Blocks.SOUL_SAND.defaultBlockState(), Blocks.SOUL_SAND.defaultBlockState(), false);
            this.generateBox(p_228333_, p_228337_, 8, 4, 4, 9, 4, 8, Blocks.SOUL_SAND.defaultBlockState(), Blocks.SOUL_SAND.defaultBlockState(), false);
            this.generateBox(p_228333_, p_228337_, 3, 5, 4, 4, 5, 8, Blocks.NETHER_WART.defaultBlockState(), Blocks.NETHER_WART.defaultBlockState(), false);
            this.generateBox(p_228333_, p_228337_, 8, 5, 4, 9, 5, 8, Blocks.NETHER_WART.defaultBlockState(), Blocks.NETHER_WART.defaultBlockState(), false);
            this.generateBox(p_228333_, p_228337_, 4, 2, 0, 8, 2, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228333_, p_228337_, 0, 2, 4, 12, 2, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228333_, p_228337_, 4, 0, 0, 8, 1, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228333_, p_228337_, 4, 0, 9, 8, 1, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228333_, p_228337_, 0, 0, 4, 3, 1, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228333_, p_228337_, 9, 0, 4, 12, 1, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

            for (int l1 = 4; l1 <= 8; l1++) {
                for (int i1 = 0; i1 <= 2; i1++) {
                    this.fillColumnDown(p_228333_, Blocks.NETHER_BRICKS.defaultBlockState(), l1, -1, i1, p_228337_);
                    this.fillColumnDown(p_228333_, Blocks.NETHER_BRICKS.defaultBlockState(), l1, -1, 12 - i1, p_228337_);
                }
            }

            for (int i2 = 0; i2 <= 2; i2++) {
                for (int j2 = 4; j2 <= 8; j2++) {
                    this.fillColumnDown(p_228333_, Blocks.NETHER_BRICKS.defaultBlockState(), i2, -1, j2, p_228337_);
                    this.fillColumnDown(p_228333_, Blocks.NETHER_BRICKS.defaultBlockState(), 12 - i2, -1, j2, p_228337_);
                }
            }
        }
    }

    public static class MonsterThrone extends NetherFortressPieces.NetherBridgePiece {
        private static final int WIDTH = 7;
        private static final int HEIGHT = 8;
        private static final int DEPTH = 9;
        private boolean hasPlacedSpawner;

        public MonsterThrone(int pGenDepth, BoundingBox pBox, Direction pOrientation) {
            super(StructurePieceType.NETHER_FORTRESS_MONSTER_THRONE, pGenDepth, pBox);
            this.setOrientation(pOrientation);
        }

        public MonsterThrone(CompoundTag pTag) {
            super(StructurePieceType.NETHER_FORTRESS_MONSTER_THRONE, pTag);
            this.hasPlacedSpawner = pTag.getBooleanOr("Mob", false);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext p_228377_, CompoundTag p_228378_) {
            super.addAdditionalSaveData(p_228377_, p_228378_);
            p_228378_.putBoolean("Mob", this.hasPlacedSpawner);
        }

        public static NetherFortressPieces.MonsterThrone createPiece(
            StructurePieceAccessor pPieces, int pX, int pY, int pZ, int pGenDepth, Direction pOrientation
        ) {
            BoundingBox boundingbox = BoundingBox.orientBox(pX, pY, pZ, -2, 0, 0, 7, 8, 9, pOrientation);
            return isOkBox(boundingbox) && pPieces.findCollisionPiece(boundingbox) == null
                ? new NetherFortressPieces.MonsterThrone(pGenDepth, boundingbox, pOrientation)
                : null;
        }

        @Override
        public void postProcess(
            WorldGenLevel p_228362_,
            StructureManager p_228363_,
            ChunkGenerator p_228364_,
            RandomSource p_228365_,
            BoundingBox p_228366_,
            ChunkPos p_228367_,
            BlockPos p_228368_
        ) {
            this.generateBox(p_228362_, p_228366_, 0, 2, 0, 6, 7, 7, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(p_228362_, p_228366_, 1, 0, 0, 5, 1, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228362_, p_228366_, 1, 2, 1, 5, 2, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228362_, p_228366_, 1, 3, 2, 5, 3, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228362_, p_228366_, 1, 4, 3, 5, 4, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228362_, p_228366_, 1, 2, 0, 1, 4, 2, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228362_, p_228366_, 5, 2, 0, 5, 4, 2, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228362_, p_228366_, 1, 5, 2, 1, 5, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228362_, p_228366_, 5, 5, 2, 5, 5, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228362_, p_228366_, 0, 5, 3, 0, 5, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228362_, p_228366_, 6, 5, 3, 6, 5, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228362_, p_228366_, 1, 5, 8, 5, 5, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            BlockState blockstate = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true).setValue(FenceBlock.EAST, true);
            BlockState blockstate1 = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.SOUTH, true);
            this.placeBlock(p_228362_, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true), 1, 6, 3, p_228366_);
            this.placeBlock(p_228362_, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.EAST, true), 5, 6, 3, p_228366_);
            this.placeBlock(p_228362_, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.EAST, true).setValue(FenceBlock.NORTH, true), 0, 6, 3, p_228366_);
            this.placeBlock(p_228362_, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true).setValue(FenceBlock.NORTH, true), 6, 6, 3, p_228366_);
            this.generateBox(p_228362_, p_228366_, 0, 6, 4, 0, 6, 7, blockstate1, blockstate1, false);
            this.generateBox(p_228362_, p_228366_, 6, 6, 4, 6, 6, 7, blockstate1, blockstate1, false);
            this.placeBlock(p_228362_, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.EAST, true).setValue(FenceBlock.SOUTH, true), 0, 6, 8, p_228366_);
            this.placeBlock(p_228362_, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true).setValue(FenceBlock.SOUTH, true), 6, 6, 8, p_228366_);
            this.generateBox(p_228362_, p_228366_, 1, 6, 8, 5, 6, 8, blockstate, blockstate, false);
            this.placeBlock(p_228362_, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.EAST, true), 1, 7, 8, p_228366_);
            this.generateBox(p_228362_, p_228366_, 2, 7, 8, 4, 7, 8, blockstate, blockstate, false);
            this.placeBlock(p_228362_, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true), 5, 7, 8, p_228366_);
            this.placeBlock(p_228362_, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.EAST, true), 2, 8, 8, p_228366_);
            this.placeBlock(p_228362_, blockstate, 3, 8, 8, p_228366_);
            this.placeBlock(p_228362_, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true), 4, 8, 8, p_228366_);
            if (!this.hasPlacedSpawner) {
                BlockPos blockpos = this.getWorldPos(3, 5, 5);
                if (p_228366_.isInside(blockpos)) {
                    this.hasPlacedSpawner = true;
                    p_228362_.setBlock(blockpos, Blocks.SPAWNER.defaultBlockState(), 2);
                    if (p_228362_.getBlockEntity(blockpos) instanceof SpawnerBlockEntity spawnerblockentity) {
                        spawnerblockentity.setEntityId(EntityType.BLAZE, p_228365_);
                    }
                }
            }

            for (int i = 0; i <= 6; i++) {
                for (int j = 0; j <= 6; j++) {
                    this.fillColumnDown(p_228362_, Blocks.NETHER_BRICKS.defaultBlockState(), i, -1, j, p_228366_);
                }
            }
        }
    }

    abstract static class NetherBridgePiece extends StructurePiece {
        protected NetherBridgePiece(StructurePieceType p_228380_, int p_228381_, BoundingBox p_228382_) {
            super(p_228380_, p_228381_, p_228382_);
        }

        public NetherBridgePiece(StructurePieceType p_228384_, CompoundTag p_228385_) {
            super(p_228384_, p_228385_);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext p_228389_, CompoundTag p_228390_) {
        }

        private int updatePieceWeight(List<NetherFortressPieces.PieceWeight> pWeights) {
            boolean flag = false;
            int i = 0;

            for (NetherFortressPieces.PieceWeight netherfortresspieces$pieceweight : pWeights) {
                if (netherfortresspieces$pieceweight.maxPlaceCount > 0 && netherfortresspieces$pieceweight.placeCount < netherfortresspieces$pieceweight.maxPlaceCount) {
                    flag = true;
                }

                i += netherfortresspieces$pieceweight.weight;
            }

            return flag ? i : -1;
        }

        private NetherFortressPieces.NetherBridgePiece generatePiece(
            NetherFortressPieces.StartPiece pStartPiece,
            List<NetherFortressPieces.PieceWeight> pWeights,
            StructurePieceAccessor pPieces,
            RandomSource pRandom,
            int pX,
            int pY,
            int pZ,
            Direction pOrientation,
            int pGenDepth
        ) {
            int i = this.updatePieceWeight(pWeights);
            boolean flag = i > 0 && pGenDepth <= 30;
            int j = 0;

            while (j < 5 && flag) {
                j++;
                int k = pRandom.nextInt(i);

                for (NetherFortressPieces.PieceWeight netherfortresspieces$pieceweight : pWeights) {
                    k -= netherfortresspieces$pieceweight.weight;
                    if (k < 0) {
                        if (!netherfortresspieces$pieceweight.doPlace(pGenDepth)
                            || netherfortresspieces$pieceweight == pStartPiece.previousPiece && !netherfortresspieces$pieceweight.allowInRow) {
                            break;
                        }

                        NetherFortressPieces.NetherBridgePiece netherfortresspieces$netherbridgepiece = NetherFortressPieces.findAndCreateBridgePieceFactory(
                            netherfortresspieces$pieceweight, pPieces, pRandom, pX, pY, pZ, pOrientation, pGenDepth
                        );
                        if (netherfortresspieces$netherbridgepiece != null) {
                            netherfortresspieces$pieceweight.placeCount++;
                            pStartPiece.previousPiece = netherfortresspieces$pieceweight;
                            if (!netherfortresspieces$pieceweight.isValid()) {
                                pWeights.remove(netherfortresspieces$pieceweight);
                            }

                            return netherfortresspieces$netherbridgepiece;
                        }
                    }
                }
            }

            return NetherFortressPieces.BridgeEndFiller.createPiece(pPieces, pRandom, pX, pY, pZ, pOrientation, pGenDepth);
        }

        private StructurePiece generateAndAddPiece(
            NetherFortressPieces.StartPiece pStartPiece,
            StructurePieceAccessor pPieces,
            RandomSource pRandom,
            int pX,
            int pY,
            int pZ,
            @Nullable Direction pOrientation,
            int pGenDepth,
            boolean pCastlePiece
        ) {
            if (Math.abs(pX - pStartPiece.getBoundingBox().minX()) <= 112 && Math.abs(pZ - pStartPiece.getBoundingBox().minZ()) <= 112) {
                List<NetherFortressPieces.PieceWeight> list = pStartPiece.availableBridgePieces;
                if (pCastlePiece) {
                    list = pStartPiece.availableCastlePieces;
                }

                StructurePiece structurepiece = this.generatePiece(pStartPiece, list, pPieces, pRandom, pX, pY, pZ, pOrientation, pGenDepth + 1);
                if (structurepiece != null) {
                    pPieces.addPiece(structurepiece);
                    pStartPiece.pendingChildren.add(structurepiece);
                }

                return structurepiece;
            } else {
                return NetherFortressPieces.BridgeEndFiller.createPiece(pPieces, pRandom, pX, pY, pZ, pOrientation, pGenDepth);
            }
        }

        @Nullable
        protected StructurePiece generateChildForward(
            NetherFortressPieces.StartPiece pStartPiece,
            StructurePieceAccessor pPieces,
            RandomSource pRandom,
            int pOffsetX,
            int pOffsetY,
            boolean pCastlePiece
        ) {
            Direction direction = this.getOrientation();
            if (direction != null) {
                switch (direction) {
                    case NORTH:
                        return this.generateAndAddPiece(
                            pStartPiece,
                            pPieces,
                            pRandom,
                            this.boundingBox.minX() + pOffsetX,
                            this.boundingBox.minY() + pOffsetY,
                            this.boundingBox.minZ() - 1,
                            direction,
                            this.getGenDepth(),
                            pCastlePiece
                        );
                    case SOUTH:
                        return this.generateAndAddPiece(
                            pStartPiece,
                            pPieces,
                            pRandom,
                            this.boundingBox.minX() + pOffsetX,
                            this.boundingBox.minY() + pOffsetY,
                            this.boundingBox.maxZ() + 1,
                            direction,
                            this.getGenDepth(),
                            pCastlePiece
                        );
                    case WEST:
                        return this.generateAndAddPiece(
                            pStartPiece,
                            pPieces,
                            pRandom,
                            this.boundingBox.minX() - 1,
                            this.boundingBox.minY() + pOffsetY,
                            this.boundingBox.minZ() + pOffsetX,
                            direction,
                            this.getGenDepth(),
                            pCastlePiece
                        );
                    case EAST:
                        return this.generateAndAddPiece(
                            pStartPiece,
                            pPieces,
                            pRandom,
                            this.boundingBox.maxX() + 1,
                            this.boundingBox.minY() + pOffsetY,
                            this.boundingBox.minZ() + pOffsetX,
                            direction,
                            this.getGenDepth(),
                            pCastlePiece
                        );
                }
            }

            return null;
        }

        @Nullable
        protected StructurePiece generateChildLeft(
            NetherFortressPieces.StartPiece pStartPiece,
            StructurePieceAccessor pPieces,
            RandomSource pRandom,
            int pOffsetY,
            int pOffsetX,
            boolean pCastlePiece
        ) {
            Direction direction = this.getOrientation();
            if (direction != null) {
                switch (direction) {
                    case NORTH:
                        return this.generateAndAddPiece(
                            pStartPiece,
                            pPieces,
                            pRandom,
                            this.boundingBox.minX() - 1,
                            this.boundingBox.minY() + pOffsetY,
                            this.boundingBox.minZ() + pOffsetX,
                            Direction.WEST,
                            this.getGenDepth(),
                            pCastlePiece
                        );
                    case SOUTH:
                        return this.generateAndAddPiece(
                            pStartPiece,
                            pPieces,
                            pRandom,
                            this.boundingBox.minX() - 1,
                            this.boundingBox.minY() + pOffsetY,
                            this.boundingBox.minZ() + pOffsetX,
                            Direction.WEST,
                            this.getGenDepth(),
                            pCastlePiece
                        );
                    case WEST:
                        return this.generateAndAddPiece(
                            pStartPiece,
                            pPieces,
                            pRandom,
                            this.boundingBox.minX() + pOffsetX,
                            this.boundingBox.minY() + pOffsetY,
                            this.boundingBox.minZ() - 1,
                            Direction.NORTH,
                            this.getGenDepth(),
                            pCastlePiece
                        );
                    case EAST:
                        return this.generateAndAddPiece(
                            pStartPiece,
                            pPieces,
                            pRandom,
                            this.boundingBox.minX() + pOffsetX,
                            this.boundingBox.minY() + pOffsetY,
                            this.boundingBox.minZ() - 1,
                            Direction.NORTH,
                            this.getGenDepth(),
                            pCastlePiece
                        );
                }
            }

            return null;
        }

        @Nullable
        protected StructurePiece generateChildRight(
            NetherFortressPieces.StartPiece pStartPiece,
            StructurePieceAccessor pPieces,
            RandomSource pRandom,
            int pOffsetY,
            int pOffsetX,
            boolean pCastlePiece
        ) {
            Direction direction = this.getOrientation();
            if (direction != null) {
                switch (direction) {
                    case NORTH:
                        return this.generateAndAddPiece(
                            pStartPiece,
                            pPieces,
                            pRandom,
                            this.boundingBox.maxX() + 1,
                            this.boundingBox.minY() + pOffsetY,
                            this.boundingBox.minZ() + pOffsetX,
                            Direction.EAST,
                            this.getGenDepth(),
                            pCastlePiece
                        );
                    case SOUTH:
                        return this.generateAndAddPiece(
                            pStartPiece,
                            pPieces,
                            pRandom,
                            this.boundingBox.maxX() + 1,
                            this.boundingBox.minY() + pOffsetY,
                            this.boundingBox.minZ() + pOffsetX,
                            Direction.EAST,
                            this.getGenDepth(),
                            pCastlePiece
                        );
                    case WEST:
                        return this.generateAndAddPiece(
                            pStartPiece,
                            pPieces,
                            pRandom,
                            this.boundingBox.minX() + pOffsetX,
                            this.boundingBox.minY() + pOffsetY,
                            this.boundingBox.maxZ() + 1,
                            Direction.SOUTH,
                            this.getGenDepth(),
                            pCastlePiece
                        );
                    case EAST:
                        return this.generateAndAddPiece(
                            pStartPiece,
                            pPieces,
                            pRandom,
                            this.boundingBox.minX() + pOffsetX,
                            this.boundingBox.minY() + pOffsetY,
                            this.boundingBox.maxZ() + 1,
                            Direction.SOUTH,
                            this.getGenDepth(),
                            pCastlePiece
                        );
                }
            }

            return null;
        }

        protected static boolean isOkBox(BoundingBox pBox) {
            return pBox != null && pBox.minY() > 10;
        }
    }

    static class PieceWeight {
        public final Class<? extends NetherFortressPieces.NetherBridgePiece> pieceClass;
        public final int weight;
        public int placeCount;
        public final int maxPlaceCount;
        public final boolean allowInRow;

        public PieceWeight(Class<? extends NetherFortressPieces.NetherBridgePiece> pPieceClass, int pWeight, int pMaxPlaceCount, boolean pAllowInRow) {
            this.pieceClass = pPieceClass;
            this.weight = pWeight;
            this.maxPlaceCount = pMaxPlaceCount;
            this.allowInRow = pAllowInRow;
        }

        public PieceWeight(Class<? extends NetherFortressPieces.NetherBridgePiece> pPieceClass, int pWeight, int pMaxPlaceCount) {
            this(pPieceClass, pWeight, pMaxPlaceCount, false);
        }

        public boolean doPlace(int pGenDepth) {
            return this.maxPlaceCount == 0 || this.placeCount < this.maxPlaceCount;
        }

        public boolean isValid() {
            return this.maxPlaceCount == 0 || this.placeCount < this.maxPlaceCount;
        }
    }

    public static class RoomCrossing extends NetherFortressPieces.NetherBridgePiece {
        private static final int WIDTH = 7;
        private static final int HEIGHT = 9;
        private static final int DEPTH = 7;

        public RoomCrossing(int pGenDepth, BoundingBox pBox, Direction pOrientation) {
            super(StructurePieceType.NETHER_FORTRESS_ROOM_CROSSING, pGenDepth, pBox);
            this.setOrientation(pOrientation);
        }

        public RoomCrossing(CompoundTag pTag) {
            super(StructurePieceType.NETHER_FORTRESS_ROOM_CROSSING, pTag);
        }

        @Override
        public void addChildren(StructurePiece p_228469_, StructurePieceAccessor p_228470_, RandomSource p_228471_) {
            this.generateChildForward((NetherFortressPieces.StartPiece)p_228469_, p_228470_, p_228471_, 2, 0, false);
            this.generateChildLeft((NetherFortressPieces.StartPiece)p_228469_, p_228470_, p_228471_, 0, 2, false);
            this.generateChildRight((NetherFortressPieces.StartPiece)p_228469_, p_228470_, p_228471_, 0, 2, false);
        }

        public static NetherFortressPieces.RoomCrossing createPiece(
            StructurePieceAccessor pPieces, int pX, int pY, int pZ, Direction pOrientation, int pGenDepth
        ) {
            BoundingBox boundingbox = BoundingBox.orientBox(pX, pY, pZ, -2, 0, 0, 7, 9, 7, pOrientation);
            return isOkBox(boundingbox) && pPieces.findCollisionPiece(boundingbox) == null
                ? new NetherFortressPieces.RoomCrossing(pGenDepth, boundingbox, pOrientation)
                : null;
        }

        @Override
        public void postProcess(
            WorldGenLevel p_228461_,
            StructureManager p_228462_,
            ChunkGenerator p_228463_,
            RandomSource p_228464_,
            BoundingBox p_228465_,
            ChunkPos p_228466_,
            BlockPos p_228467_
        ) {
            this.generateBox(p_228461_, p_228465_, 0, 0, 0, 6, 1, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228461_, p_228465_, 0, 2, 0, 6, 7, 6, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(p_228461_, p_228465_, 0, 2, 0, 1, 6, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228461_, p_228465_, 0, 2, 6, 1, 6, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228461_, p_228465_, 5, 2, 0, 6, 6, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228461_, p_228465_, 5, 2, 6, 6, 6, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228461_, p_228465_, 0, 2, 0, 0, 6, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228461_, p_228465_, 0, 2, 5, 0, 6, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228461_, p_228465_, 6, 2, 0, 6, 6, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228461_, p_228465_, 6, 2, 5, 6, 6, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            BlockState blockstate = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true).setValue(FenceBlock.EAST, true);
            BlockState blockstate1 = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.SOUTH, true);
            this.generateBox(p_228461_, p_228465_, 2, 6, 0, 4, 6, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228461_, p_228465_, 2, 5, 0, 4, 5, 0, blockstate, blockstate, false);
            this.generateBox(p_228461_, p_228465_, 2, 6, 6, 4, 6, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228461_, p_228465_, 2, 5, 6, 4, 5, 6, blockstate, blockstate, false);
            this.generateBox(p_228461_, p_228465_, 0, 6, 2, 0, 6, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228461_, p_228465_, 0, 5, 2, 0, 5, 4, blockstate1, blockstate1, false);
            this.generateBox(p_228461_, p_228465_, 6, 6, 2, 6, 6, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228461_, p_228465_, 6, 5, 2, 6, 5, 4, blockstate1, blockstate1, false);

            for (int i = 0; i <= 6; i++) {
                for (int j = 0; j <= 6; j++) {
                    this.fillColumnDown(p_228461_, Blocks.NETHER_BRICKS.defaultBlockState(), i, -1, j, p_228465_);
                }
            }
        }
    }

    public static class StairsRoom extends NetherFortressPieces.NetherBridgePiece {
        private static final int WIDTH = 7;
        private static final int HEIGHT = 11;
        private static final int DEPTH = 7;

        public StairsRoom(int pGenDepth, BoundingBox pBox, Direction pOrientation) {
            super(StructurePieceType.NETHER_FORTRESS_STAIRS_ROOM, pGenDepth, pBox);
            this.setOrientation(pOrientation);
        }

        public StairsRoom(CompoundTag pTag) {
            super(StructurePieceType.NETHER_FORTRESS_STAIRS_ROOM, pTag);
        }

        @Override
        public void addChildren(StructurePiece p_228497_, StructurePieceAccessor p_228498_, RandomSource p_228499_) {
            this.generateChildRight((NetherFortressPieces.StartPiece)p_228497_, p_228498_, p_228499_, 6, 2, false);
        }

        public static NetherFortressPieces.StairsRoom createPiece(
            StructurePieceAccessor pPieces, int pX, int pY, int pZ, int pGenDepth, Direction pOrientation
        ) {
            BoundingBox boundingbox = BoundingBox.orientBox(pX, pY, pZ, -2, 0, 0, 7, 11, 7, pOrientation);
            return isOkBox(boundingbox) && pPieces.findCollisionPiece(boundingbox) == null
                ? new NetherFortressPieces.StairsRoom(pGenDepth, boundingbox, pOrientation)
                : null;
        }

        @Override
        public void postProcess(
            WorldGenLevel p_228489_,
            StructureManager p_228490_,
            ChunkGenerator p_228491_,
            RandomSource p_228492_,
            BoundingBox p_228493_,
            ChunkPos p_228494_,
            BlockPos p_228495_
        ) {
            this.generateBox(p_228489_, p_228493_, 0, 0, 0, 6, 1, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228489_, p_228493_, 0, 2, 0, 6, 10, 6, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(p_228489_, p_228493_, 0, 2, 0, 1, 8, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228489_, p_228493_, 5, 2, 0, 6, 8, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228489_, p_228493_, 0, 2, 1, 0, 8, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228489_, p_228493_, 6, 2, 1, 6, 8, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228489_, p_228493_, 1, 2, 6, 5, 8, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            BlockState blockstate = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true).setValue(FenceBlock.EAST, true);
            BlockState blockstate1 = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.SOUTH, true);
            this.generateBox(p_228489_, p_228493_, 0, 3, 2, 0, 5, 4, blockstate1, blockstate1, false);
            this.generateBox(p_228489_, p_228493_, 6, 3, 2, 6, 5, 2, blockstate1, blockstate1, false);
            this.generateBox(p_228489_, p_228493_, 6, 3, 4, 6, 5, 4, blockstate1, blockstate1, false);
            this.placeBlock(p_228489_, Blocks.NETHER_BRICKS.defaultBlockState(), 5, 2, 5, p_228493_);
            this.generateBox(p_228489_, p_228493_, 4, 2, 5, 4, 3, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228489_, p_228493_, 3, 2, 5, 3, 4, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228489_, p_228493_, 2, 2, 5, 2, 5, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228489_, p_228493_, 1, 2, 5, 1, 6, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228489_, p_228493_, 1, 7, 1, 5, 7, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228489_, p_228493_, 6, 8, 2, 6, 8, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(p_228489_, p_228493_, 2, 6, 0, 4, 8, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228489_, p_228493_, 2, 5, 0, 4, 5, 0, blockstate, blockstate, false);

            for (int i = 0; i <= 6; i++) {
                for (int j = 0; j <= 6; j++) {
                    this.fillColumnDown(p_228489_, Blocks.NETHER_BRICKS.defaultBlockState(), i, -1, j, p_228493_);
                }
            }
        }
    }

    public static class StartPiece extends NetherFortressPieces.BridgeCrossing {
        public NetherFortressPieces.PieceWeight previousPiece;
        public List<NetherFortressPieces.PieceWeight> availableBridgePieces;
        public List<NetherFortressPieces.PieceWeight> availableCastlePieces;
        public final List<StructurePiece> pendingChildren = Lists.newArrayList();

        public StartPiece(RandomSource pRandom, int pX, int pZ) {
            super(pX, pZ, getRandomHorizontalDirection(pRandom));
            this.availableBridgePieces = Lists.newArrayList();

            for (NetherFortressPieces.PieceWeight netherfortresspieces$pieceweight : NetherFortressPieces.BRIDGE_PIECE_WEIGHTS) {
                netherfortresspieces$pieceweight.placeCount = 0;
                this.availableBridgePieces.add(netherfortresspieces$pieceweight);
            }

            this.availableCastlePieces = Lists.newArrayList();

            for (NetherFortressPieces.PieceWeight netherfortresspieces$pieceweight1 : NetherFortressPieces.CASTLE_PIECE_WEIGHTS) {
                netherfortresspieces$pieceweight1.placeCount = 0;
                this.availableCastlePieces.add(netherfortresspieces$pieceweight1);
            }
        }

        public StartPiece(CompoundTag p_228516_) {
            super(StructurePieceType.NETHER_FORTRESS_START, p_228516_);
        }
    }
}
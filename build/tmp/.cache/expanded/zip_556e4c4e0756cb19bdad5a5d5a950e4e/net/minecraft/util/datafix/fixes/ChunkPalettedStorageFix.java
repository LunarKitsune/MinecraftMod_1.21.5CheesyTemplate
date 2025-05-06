package net.minecraft.util.datafix.fixes;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.util.CrudeIncrementalIntIdentityHashBiMap;
import net.minecraft.util.datafix.ExtraDataFixUtils;
import net.minecraft.util.datafix.PackedBitStorage;
import org.slf4j.Logger;

public class ChunkPalettedStorageFix extends DataFix {
    private static final int NORTH_WEST_MASK = 128;
    private static final int WEST_MASK = 64;
    private static final int SOUTH_WEST_MASK = 32;
    private static final int SOUTH_MASK = 16;
    private static final int SOUTH_EAST_MASK = 8;
    private static final int EAST_MASK = 4;
    private static final int NORTH_EAST_MASK = 2;
    private static final int NORTH_MASK = 1;
    static final Logger LOGGER = LogUtils.getLogger();
    private static final int SIZE = 4096;

    public ChunkPalettedStorageFix(Schema pOutputSchema, boolean pChangesType) {
        super(pOutputSchema, pChangesType);
    }

    public static String getName(Dynamic<?> pData) {
        return pData.get("Name").asString("");
    }

    public static String getProperty(Dynamic<?> pData, String pKey) {
        return pData.get("Properties").get(pKey).asString("");
    }

    public static int idFor(CrudeIncrementalIntIdentityHashBiMap<Dynamic<?>> pPalette, Dynamic<?> pData) {
        int i = pPalette.getId(pData);
        if (i == -1) {
            i = pPalette.add(pData);
        }

        return i;
    }

    private Dynamic<?> fix(Dynamic<?> pDynamic) {
        Optional<? extends Dynamic<?>> optional = pDynamic.get("Level").result();
        return optional.isPresent() && optional.get().get("Sections").asStreamOpt().result().isPresent()
            ? pDynamic.set("Level", new ChunkPalettedStorageFix.UpgradeChunk((Dynamic<?>)optional.get()).write())
            : pDynamic;
    }

    @Override
    public TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(References.CHUNK);
        Type<?> type1 = this.getOutputSchema().getType(References.CHUNK);
        return this.writeFixAndRead("ChunkPalettedStorageFix", type, type1, this::fix);
    }

    public static int getSideMask(boolean pWest, boolean pEast, boolean pNorth, boolean pSouth) {
        int i = 0;
        if (pNorth) {
            if (pEast) {
                i |= 2;
            } else if (pWest) {
                i |= 128;
            } else {
                i |= 1;
            }
        } else if (pSouth) {
            if (pWest) {
                i |= 32;
            } else if (pEast) {
                i |= 8;
            } else {
                i |= 16;
            }
        } else if (pEast) {
            i |= 4;
        } else if (pWest) {
            i |= 64;
        }

        return i;
    }

    static class DataLayer {
        private static final int SIZE = 2048;
        private static final int NIBBLE_SIZE = 4;
        private final byte[] data;

        public DataLayer() {
            this.data = new byte[2048];
        }

        public DataLayer(byte[] pData) {
            this.data = pData;
            if (pData.length != 2048) {
                throw new IllegalArgumentException("ChunkNibbleArrays should be 2048 bytes not: " + pData.length);
            }
        }

        public int get(int pX, int pY, int pZ) {
            int i = this.getPosition(pY << 8 | pZ << 4 | pX);
            return this.isFirst(pY << 8 | pZ << 4 | pX) ? this.data[i] & 15 : this.data[i] >> 4 & 15;
        }

        private boolean isFirst(int pPackedPos) {
            return (pPackedPos & 1) == 0;
        }

        private int getPosition(int pPackedPos) {
            return pPackedPos >> 1;
        }
    }

    public static enum Direction {
        DOWN(ChunkPalettedStorageFix.Direction.AxisDirection.NEGATIVE, ChunkPalettedStorageFix.Direction.Axis.Y),
        UP(ChunkPalettedStorageFix.Direction.AxisDirection.POSITIVE, ChunkPalettedStorageFix.Direction.Axis.Y),
        NORTH(ChunkPalettedStorageFix.Direction.AxisDirection.NEGATIVE, ChunkPalettedStorageFix.Direction.Axis.Z),
        SOUTH(ChunkPalettedStorageFix.Direction.AxisDirection.POSITIVE, ChunkPalettedStorageFix.Direction.Axis.Z),
        WEST(ChunkPalettedStorageFix.Direction.AxisDirection.NEGATIVE, ChunkPalettedStorageFix.Direction.Axis.X),
        EAST(ChunkPalettedStorageFix.Direction.AxisDirection.POSITIVE, ChunkPalettedStorageFix.Direction.Axis.X);

        private final ChunkPalettedStorageFix.Direction.Axis axis;
        private final ChunkPalettedStorageFix.Direction.AxisDirection axisDirection;

        private Direction(final ChunkPalettedStorageFix.Direction.AxisDirection pAxisDirection, final ChunkPalettedStorageFix.Direction.Axis pAxis) {
            this.axis = pAxis;
            this.axisDirection = pAxisDirection;
        }

        public ChunkPalettedStorageFix.Direction.AxisDirection getAxisDirection() {
            return this.axisDirection;
        }

        public ChunkPalettedStorageFix.Direction.Axis getAxis() {
            return this.axis;
        }

        public static enum Axis {
            X,
            Y,
            Z;
        }

        public static enum AxisDirection {
            POSITIVE(1),
            NEGATIVE(-1);

            private final int step;

            private AxisDirection(final int pStep) {
                this.step = pStep;
            }

            public int getStep() {
                return this.step;
            }
        }
    }

    static class MappingConstants {
        static final BitSet VIRTUAL = new BitSet(256);
        static final BitSet FIX = new BitSet(256);
        static final Dynamic<?> PUMPKIN = ExtraDataFixUtils.blockState("minecraft:pumpkin");
        static final Dynamic<?> SNOWY_PODZOL = ExtraDataFixUtils.blockState("minecraft:podzol", Map.of("snowy", "true"));
        static final Dynamic<?> SNOWY_GRASS = ExtraDataFixUtils.blockState("minecraft:grass_block", Map.of("snowy", "true"));
        static final Dynamic<?> SNOWY_MYCELIUM = ExtraDataFixUtils.blockState("minecraft:mycelium", Map.of("snowy", "true"));
        static final Dynamic<?> UPPER_SUNFLOWER = ExtraDataFixUtils.blockState("minecraft:sunflower", Map.of("half", "upper"));
        static final Dynamic<?> UPPER_LILAC = ExtraDataFixUtils.blockState("minecraft:lilac", Map.of("half", "upper"));
        static final Dynamic<?> UPPER_TALL_GRASS = ExtraDataFixUtils.blockState("minecraft:tall_grass", Map.of("half", "upper"));
        static final Dynamic<?> UPPER_LARGE_FERN = ExtraDataFixUtils.blockState("minecraft:large_fern", Map.of("half", "upper"));
        static final Dynamic<?> UPPER_ROSE_BUSH = ExtraDataFixUtils.blockState("minecraft:rose_bush", Map.of("half", "upper"));
        static final Dynamic<?> UPPER_PEONY = ExtraDataFixUtils.blockState("minecraft:peony", Map.of("half", "upper"));
        static final Map<String, Dynamic<?>> FLOWER_POT_MAP = DataFixUtils.make(Maps.newHashMap(), p_369284_ -> {
            p_369284_.put("minecraft:air0", ExtraDataFixUtils.blockState("minecraft:flower_pot"));
            p_369284_.put("minecraft:red_flower0", ExtraDataFixUtils.blockState("minecraft:potted_poppy"));
            p_369284_.put("minecraft:red_flower1", ExtraDataFixUtils.blockState("minecraft:potted_blue_orchid"));
            p_369284_.put("minecraft:red_flower2", ExtraDataFixUtils.blockState("minecraft:potted_allium"));
            p_369284_.put("minecraft:red_flower3", ExtraDataFixUtils.blockState("minecraft:potted_azure_bluet"));
            p_369284_.put("minecraft:red_flower4", ExtraDataFixUtils.blockState("minecraft:potted_red_tulip"));
            p_369284_.put("minecraft:red_flower5", ExtraDataFixUtils.blockState("minecraft:potted_orange_tulip"));
            p_369284_.put("minecraft:red_flower6", ExtraDataFixUtils.blockState("minecraft:potted_white_tulip"));
            p_369284_.put("minecraft:red_flower7", ExtraDataFixUtils.blockState("minecraft:potted_pink_tulip"));
            p_369284_.put("minecraft:red_flower8", ExtraDataFixUtils.blockState("minecraft:potted_oxeye_daisy"));
            p_369284_.put("minecraft:yellow_flower0", ExtraDataFixUtils.blockState("minecraft:potted_dandelion"));
            p_369284_.put("minecraft:sapling0", ExtraDataFixUtils.blockState("minecraft:potted_oak_sapling"));
            p_369284_.put("minecraft:sapling1", ExtraDataFixUtils.blockState("minecraft:potted_spruce_sapling"));
            p_369284_.put("minecraft:sapling2", ExtraDataFixUtils.blockState("minecraft:potted_birch_sapling"));
            p_369284_.put("minecraft:sapling3", ExtraDataFixUtils.blockState("minecraft:potted_jungle_sapling"));
            p_369284_.put("minecraft:sapling4", ExtraDataFixUtils.blockState("minecraft:potted_acacia_sapling"));
            p_369284_.put("minecraft:sapling5", ExtraDataFixUtils.blockState("minecraft:potted_dark_oak_sapling"));
            p_369284_.put("minecraft:red_mushroom0", ExtraDataFixUtils.blockState("minecraft:potted_red_mushroom"));
            p_369284_.put("minecraft:brown_mushroom0", ExtraDataFixUtils.blockState("minecraft:potted_brown_mushroom"));
            p_369284_.put("minecraft:deadbush0", ExtraDataFixUtils.blockState("minecraft:potted_dead_bush"));
            p_369284_.put("minecraft:tallgrass2", ExtraDataFixUtils.blockState("minecraft:potted_fern"));
            p_369284_.put("minecraft:cactus0", ExtraDataFixUtils.blockState("minecraft:potted_cactus"));
        });
        static final Map<String, Dynamic<?>> SKULL_MAP = DataFixUtils.make(Maps.newHashMap(), p_366440_ -> {
            mapSkull(p_366440_, 0, "skeleton", "skull");
            mapSkull(p_366440_, 1, "wither_skeleton", "skull");
            mapSkull(p_366440_, 2, "zombie", "head");
            mapSkull(p_366440_, 3, "player", "head");
            mapSkull(p_366440_, 4, "creeper", "head");
            mapSkull(p_366440_, 5, "dragon", "head");
        });
        static final Map<String, Dynamic<?>> DOOR_MAP = DataFixUtils.make(Maps.newHashMap(), p_366018_ -> {
            mapDoor(p_366018_, "oak_door");
            mapDoor(p_366018_, "iron_door");
            mapDoor(p_366018_, "spruce_door");
            mapDoor(p_366018_, "birch_door");
            mapDoor(p_366018_, "jungle_door");
            mapDoor(p_366018_, "acacia_door");
            mapDoor(p_366018_, "dark_oak_door");
        });
        static final Map<String, Dynamic<?>> NOTE_BLOCK_MAP = DataFixUtils.make(Maps.newHashMap(), p_361432_ -> {
            for (int i = 0; i < 26; i++) {
                p_361432_.put("true" + i, ExtraDataFixUtils.blockState("minecraft:note_block", Map.of("powered", "true", "note", String.valueOf(i))));
                p_361432_.put("false" + i, ExtraDataFixUtils.blockState("minecraft:note_block", Map.of("powered", "false", "note", String.valueOf(i))));
            }
        });
        private static final Int2ObjectMap<String> DYE_COLOR_MAP = DataFixUtils.make(new Int2ObjectOpenHashMap<>(), p_366854_ -> {
            p_366854_.put(0, "white");
            p_366854_.put(1, "orange");
            p_366854_.put(2, "magenta");
            p_366854_.put(3, "light_blue");
            p_366854_.put(4, "yellow");
            p_366854_.put(5, "lime");
            p_366854_.put(6, "pink");
            p_366854_.put(7, "gray");
            p_366854_.put(8, "light_gray");
            p_366854_.put(9, "cyan");
            p_366854_.put(10, "purple");
            p_366854_.put(11, "blue");
            p_366854_.put(12, "brown");
            p_366854_.put(13, "green");
            p_366854_.put(14, "red");
            p_366854_.put(15, "black");
        });
        static final Map<String, Dynamic<?>> BED_BLOCK_MAP = DataFixUtils.make(Maps.newHashMap(), p_362358_ -> {
            for (Entry<String> entry : DYE_COLOR_MAP.int2ObjectEntrySet()) {
                if (!Objects.equals(entry.getValue(), "red")) {
                    addBeds(p_362358_, entry.getIntKey(), entry.getValue());
                }
            }
        });
        static final Map<String, Dynamic<?>> BANNER_BLOCK_MAP = DataFixUtils.make(Maps.newHashMap(), p_362796_ -> {
            for (Entry<String> entry : DYE_COLOR_MAP.int2ObjectEntrySet()) {
                if (!Objects.equals(entry.getValue(), "white")) {
                    addBanners(p_362796_, 15 - entry.getIntKey(), entry.getValue());
                }
            }
        });
        static final Dynamic<?> AIR = ExtraDataFixUtils.blockState("minecraft:air");

        private MappingConstants() {
        }

        private static void mapSkull(Map<String, Dynamic<?>> pMap, int pId, String pSkullType, String pSuffix) {
            pMap.put(pId + "north", ExtraDataFixUtils.blockState("minecraft:" + pSkullType + "_wall_" + pSuffix, Map.of("facing", "north")));
            pMap.put(pId + "east", ExtraDataFixUtils.blockState("minecraft:" + pSkullType + "_wall_" + pSuffix, Map.of("facing", "east")));
            pMap.put(pId + "south", ExtraDataFixUtils.blockState("minecraft:" + pSkullType + "_wall_" + pSuffix, Map.of("facing", "south")));
            pMap.put(pId + "west", ExtraDataFixUtils.blockState("minecraft:" + pSkullType + "_wall_" + pSuffix, Map.of("facing", "west")));

            for (int i = 0; i < 16; i++) {
                pMap.put(
                    "" + pId + i, ExtraDataFixUtils.blockState("minecraft:" + pSkullType + "_" + pSuffix, Map.of("rotation", String.valueOf(i)))
                );
            }
        }

        private static void mapDoor(Map<String, Dynamic<?>> pMap, String pDoorId) {
            String s = "minecraft:" + pDoorId;
            pMap.put(
                "minecraft:" + pDoorId + "eastlowerleftfalsefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "east", "half", "lower", "hinge", "left", "open", "false", "powered", "false"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "eastlowerleftfalsetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "east", "half", "lower", "hinge", "left", "open", "false", "powered", "true"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "eastlowerlefttruefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "east", "half", "lower", "hinge", "left", "open", "true", "powered", "false"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "eastlowerlefttruetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "east", "half", "lower", "hinge", "left", "open", "true", "powered", "true"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "eastlowerrightfalsefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "east", "half", "lower", "hinge", "right", "open", "false", "powered", "false"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "eastlowerrightfalsetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "east", "half", "lower", "hinge", "right", "open", "false", "powered", "true"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "eastlowerrighttruefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "east", "half", "lower", "hinge", "right", "open", "true", "powered", "false"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "eastlowerrighttruetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "east", "half", "lower", "hinge", "right", "open", "true", "powered", "true"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "eastupperleftfalsefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "east", "half", "upper", "hinge", "left", "open", "false", "powered", "false"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "eastupperleftfalsetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "east", "half", "upper", "hinge", "left", "open", "false", "powered", "true"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "eastupperlefttruefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "east", "half", "upper", "hinge", "left", "open", "true", "powered", "false"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "eastupperlefttruetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "east", "half", "upper", "hinge", "left", "open", "true", "powered", "true"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "eastupperrightfalsefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "east", "half", "upper", "hinge", "right", "open", "false", "powered", "false"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "eastupperrightfalsetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "east", "half", "upper", "hinge", "right", "open", "false", "powered", "true"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "eastupperrighttruefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "east", "half", "upper", "hinge", "right", "open", "true", "powered", "false"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "eastupperrighttruetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "east", "half", "upper", "hinge", "right", "open", "true", "powered", "true"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "northlowerleftfalsefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "north", "half", "lower", "hinge", "left", "open", "false", "powered", "false"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "northlowerleftfalsetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "north", "half", "lower", "hinge", "left", "open", "false", "powered", "true"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "northlowerlefttruefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "north", "half", "lower", "hinge", "left", "open", "true", "powered", "false"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "northlowerlefttruetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "north", "half", "lower", "hinge", "left", "open", "true", "powered", "true"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "northlowerrightfalsefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "north", "half", "lower", "hinge", "right", "open", "false", "powered", "false"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "northlowerrightfalsetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "north", "half", "lower", "hinge", "right", "open", "false", "powered", "true"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "northlowerrighttruefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "north", "half", "lower", "hinge", "right", "open", "true", "powered", "false"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "northlowerrighttruetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "north", "half", "lower", "hinge", "right", "open", "true", "powered", "true"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "northupperleftfalsefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "north", "half", "upper", "hinge", "left", "open", "false", "powered", "false"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "northupperleftfalsetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "north", "half", "upper", "hinge", "left", "open", "false", "powered", "true"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "northupperlefttruefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "north", "half", "upper", "hinge", "left", "open", "true", "powered", "false"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "northupperlefttruetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "north", "half", "upper", "hinge", "left", "open", "true", "powered", "true"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "northupperrightfalsefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "north", "half", "upper", "hinge", "right", "open", "false", "powered", "false"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "northupperrightfalsetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "north", "half", "upper", "hinge", "right", "open", "false", "powered", "true"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "northupperrighttruefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "north", "half", "upper", "hinge", "right", "open", "true", "powered", "false"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "northupperrighttruetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "north", "half", "upper", "hinge", "right", "open", "true", "powered", "true"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "southlowerleftfalsefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "south", "half", "lower", "hinge", "left", "open", "false", "powered", "false"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "southlowerleftfalsetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "south", "half", "lower", "hinge", "left", "open", "false", "powered", "true"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "southlowerlefttruefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "south", "half", "lower", "hinge", "left", "open", "true", "powered", "false"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "southlowerlefttruetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "south", "half", "lower", "hinge", "left", "open", "true", "powered", "true"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "southlowerrightfalsefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "south", "half", "lower", "hinge", "right", "open", "false", "powered", "false"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "southlowerrightfalsetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "south", "half", "lower", "hinge", "right", "open", "false", "powered", "true"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "southlowerrighttruefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "south", "half", "lower", "hinge", "right", "open", "true", "powered", "false"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "southlowerrighttruetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "south", "half", "lower", "hinge", "right", "open", "true", "powered", "true"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "southupperleftfalsefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "south", "half", "upper", "hinge", "left", "open", "false", "powered", "false"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "southupperleftfalsetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "south", "half", "upper", "hinge", "left", "open", "false", "powered", "true"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "southupperlefttruefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "south", "half", "upper", "hinge", "left", "open", "true", "powered", "false"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "southupperlefttruetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "south", "half", "upper", "hinge", "left", "open", "true", "powered", "true"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "southupperrightfalsefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "south", "half", "upper", "hinge", "right", "open", "false", "powered", "false"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "southupperrightfalsetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "south", "half", "upper", "hinge", "right", "open", "false", "powered", "true"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "southupperrighttruefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "south", "half", "upper", "hinge", "right", "open", "true", "powered", "false"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "southupperrighttruetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "south", "half", "upper", "hinge", "right", "open", "true", "powered", "true"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "westlowerleftfalsefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "west", "half", "lower", "hinge", "left", "open", "false", "powered", "false"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "westlowerleftfalsetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "west", "half", "lower", "hinge", "left", "open", "false", "powered", "true"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "westlowerlefttruefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "west", "half", "lower", "hinge", "left", "open", "true", "powered", "false"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "westlowerlefttruetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "west", "half", "lower", "hinge", "left", "open", "true", "powered", "true"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "westlowerrightfalsefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "west", "half", "lower", "hinge", "right", "open", "false", "powered", "false"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "westlowerrightfalsetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "west", "half", "lower", "hinge", "right", "open", "false", "powered", "true"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "westlowerrighttruefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "west", "half", "lower", "hinge", "right", "open", "true", "powered", "false"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "westlowerrighttruetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "west", "half", "lower", "hinge", "right", "open", "true", "powered", "true"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "westupperleftfalsefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "west", "half", "upper", "hinge", "left", "open", "false", "powered", "false"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "westupperleftfalsetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "west", "half", "upper", "hinge", "left", "open", "false", "powered", "true"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "westupperlefttruefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "west", "half", "upper", "hinge", "left", "open", "true", "powered", "false"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "westupperlefttruetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "west", "half", "upper", "hinge", "left", "open", "true", "powered", "true"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "westupperrightfalsefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "west", "half", "upper", "hinge", "right", "open", "false", "powered", "false"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "westupperrightfalsetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "west", "half", "upper", "hinge", "right", "open", "false", "powered", "true"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "westupperrighttruefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "west", "half", "upper", "hinge", "right", "open", "true", "powered", "false"))
            );
            pMap.put(
                "minecraft:" + pDoorId + "westupperrighttruetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "west", "half", "upper", "hinge", "right", "open", "true", "powered", "true"))
            );
        }

        private static void addBeds(Map<String, Dynamic<?>> pMap, int pId, String pBedColor) {
            pMap.put(
                "southfalsefoot" + pId,
                ExtraDataFixUtils.blockState("minecraft:" + pBedColor + "_bed", Map.of("facing", "south", "occupied", "false", "part", "foot"))
            );
            pMap.put(
                "westfalsefoot" + pId,
                ExtraDataFixUtils.blockState("minecraft:" + pBedColor + "_bed", Map.of("facing", "west", "occupied", "false", "part", "foot"))
            );
            pMap.put(
                "northfalsefoot" + pId,
                ExtraDataFixUtils.blockState("minecraft:" + pBedColor + "_bed", Map.of("facing", "north", "occupied", "false", "part", "foot"))
            );
            pMap.put(
                "eastfalsefoot" + pId,
                ExtraDataFixUtils.blockState("minecraft:" + pBedColor + "_bed", Map.of("facing", "east", "occupied", "false", "part", "foot"))
            );
            pMap.put(
                "southfalsehead" + pId,
                ExtraDataFixUtils.blockState("minecraft:" + pBedColor + "_bed", Map.of("facing", "south", "occupied", "false", "part", "head"))
            );
            pMap.put(
                "westfalsehead" + pId,
                ExtraDataFixUtils.blockState("minecraft:" + pBedColor + "_bed", Map.of("facing", "west", "occupied", "false", "part", "head"))
            );
            pMap.put(
                "northfalsehead" + pId,
                ExtraDataFixUtils.blockState("minecraft:" + pBedColor + "_bed", Map.of("facing", "north", "occupied", "false", "part", "head"))
            );
            pMap.put(
                "eastfalsehead" + pId,
                ExtraDataFixUtils.blockState("minecraft:" + pBedColor + "_bed", Map.of("facing", "east", "occupied", "false", "part", "head"))
            );
            pMap.put(
                "southtruehead" + pId,
                ExtraDataFixUtils.blockState("minecraft:" + pBedColor + "_bed", Map.of("facing", "south", "occupied", "true", "part", "head"))
            );
            pMap.put(
                "westtruehead" + pId,
                ExtraDataFixUtils.blockState("minecraft:" + pBedColor + "_bed", Map.of("facing", "west", "occupied", "true", "part", "head"))
            );
            pMap.put(
                "northtruehead" + pId,
                ExtraDataFixUtils.blockState("minecraft:" + pBedColor + "_bed", Map.of("facing", "north", "occupied", "true", "part", "head"))
            );
            pMap.put(
                "easttruehead" + pId,
                ExtraDataFixUtils.blockState("minecraft:" + pBedColor + "_bed", Map.of("facing", "east", "occupied", "true", "part", "head"))
            );
        }

        private static void addBanners(Map<String, Dynamic<?>> pMap, int pId, String pBannerColor) {
            for (int i = 0; i < 16; i++) {
                pMap.put(i + "_" + pId, ExtraDataFixUtils.blockState("minecraft:" + pBannerColor + "_banner", Map.of("rotation", String.valueOf(i))));
            }

            pMap.put("north_" + pId, ExtraDataFixUtils.blockState("minecraft:" + pBannerColor + "_wall_banner", Map.of("facing", "north")));
            pMap.put("south_" + pId, ExtraDataFixUtils.blockState("minecraft:" + pBannerColor + "_wall_banner", Map.of("facing", "south")));
            pMap.put("west_" + pId, ExtraDataFixUtils.blockState("minecraft:" + pBannerColor + "_wall_banner", Map.of("facing", "west")));
            pMap.put("east_" + pId, ExtraDataFixUtils.blockState("minecraft:" + pBannerColor + "_wall_banner", Map.of("facing", "east")));
        }

        static {
            FIX.set(2);
            FIX.set(3);
            FIX.set(110);
            FIX.set(140);
            FIX.set(144);
            FIX.set(25);
            FIX.set(86);
            FIX.set(26);
            FIX.set(176);
            FIX.set(177);
            FIX.set(175);
            FIX.set(64);
            FIX.set(71);
            FIX.set(193);
            FIX.set(194);
            FIX.set(195);
            FIX.set(196);
            FIX.set(197);
            VIRTUAL.set(54);
            VIRTUAL.set(146);
            VIRTUAL.set(25);
            VIRTUAL.set(26);
            VIRTUAL.set(51);
            VIRTUAL.set(53);
            VIRTUAL.set(67);
            VIRTUAL.set(108);
            VIRTUAL.set(109);
            VIRTUAL.set(114);
            VIRTUAL.set(128);
            VIRTUAL.set(134);
            VIRTUAL.set(135);
            VIRTUAL.set(136);
            VIRTUAL.set(156);
            VIRTUAL.set(163);
            VIRTUAL.set(164);
            VIRTUAL.set(180);
            VIRTUAL.set(203);
            VIRTUAL.set(55);
            VIRTUAL.set(85);
            VIRTUAL.set(113);
            VIRTUAL.set(188);
            VIRTUAL.set(189);
            VIRTUAL.set(190);
            VIRTUAL.set(191);
            VIRTUAL.set(192);
            VIRTUAL.set(93);
            VIRTUAL.set(94);
            VIRTUAL.set(101);
            VIRTUAL.set(102);
            VIRTUAL.set(160);
            VIRTUAL.set(106);
            VIRTUAL.set(107);
            VIRTUAL.set(183);
            VIRTUAL.set(184);
            VIRTUAL.set(185);
            VIRTUAL.set(186);
            VIRTUAL.set(187);
            VIRTUAL.set(132);
            VIRTUAL.set(139);
            VIRTUAL.set(199);
        }
    }

    static class Section {
        private final CrudeIncrementalIntIdentityHashBiMap<Dynamic<?>> palette = CrudeIncrementalIntIdentityHashBiMap.create(32);
        private final List<Dynamic<?>> listTag;
        private final Dynamic<?> section;
        private final boolean hasData;
        final Int2ObjectMap<IntList> toFix = new Int2ObjectLinkedOpenHashMap<>();
        final IntList update = new IntArrayList();
        public final int y;
        private final Set<Dynamic<?>> seen = Sets.newIdentityHashSet();
        private final int[] buffer = new int[4096];

        public Section(Dynamic<?> pSection) {
            this.listTag = Lists.newArrayList();
            this.section = pSection;
            this.y = pSection.get("Y").asInt(0);
            this.hasData = pSection.get("Blocks").result().isPresent();
        }

        public Dynamic<?> getBlock(int pIndex) {
            if (pIndex >= 0 && pIndex <= 4095) {
                Dynamic<?> dynamic = this.palette.byId(this.buffer[pIndex]);
                return dynamic == null ? ChunkPalettedStorageFix.MappingConstants.AIR : dynamic;
            } else {
                return ChunkPalettedStorageFix.MappingConstants.AIR;
            }
        }

        public void setBlock(int pIndex, Dynamic<?> pBlock) {
            if (this.seen.add(pBlock)) {
                this.listTag
                    .add("%%FILTER_ME%%".equals(ChunkPalettedStorageFix.getName(pBlock)) ? ChunkPalettedStorageFix.MappingConstants.AIR : pBlock);
            }

            this.buffer[pIndex] = ChunkPalettedStorageFix.idFor(this.palette, pBlock);
        }

        public int upgrade(int pSides) {
            if (!this.hasData) {
                return pSides;
            } else {
                ByteBuffer bytebuffer = this.section.get("Blocks").asByteBufferOpt().result().get();
                ChunkPalettedStorageFix.DataLayer chunkpalettedstoragefix$datalayer = this.section
                    .get("Data")
                    .asByteBufferOpt()
                    .map(p_15214_ -> new ChunkPalettedStorageFix.DataLayer(DataFixUtils.toArray(p_15214_)))
                    .result()
                    .orElseGet(ChunkPalettedStorageFix.DataLayer::new);
                ChunkPalettedStorageFix.DataLayer chunkpalettedstoragefix$datalayer1 = this.section
                    .get("Add")
                    .asByteBufferOpt()
                    .map(p_15208_ -> new ChunkPalettedStorageFix.DataLayer(DataFixUtils.toArray(p_15208_)))
                    .result()
                    .orElseGet(ChunkPalettedStorageFix.DataLayer::new);
                this.seen.add(ChunkPalettedStorageFix.MappingConstants.AIR);
                ChunkPalettedStorageFix.idFor(this.palette, ChunkPalettedStorageFix.MappingConstants.AIR);
                this.listTag.add(ChunkPalettedStorageFix.MappingConstants.AIR);

                for (int i = 0; i < 4096; i++) {
                    int j = i & 15;
                    int k = i >> 8 & 15;
                    int l = i >> 4 & 15;
                    int i1 = chunkpalettedstoragefix$datalayer1.get(j, k, l) << 12
                        | (bytebuffer.get(i) & 255) << 4
                        | chunkpalettedstoragefix$datalayer.get(j, k, l);
                    if (ChunkPalettedStorageFix.MappingConstants.FIX.get(i1 >> 4)) {
                        this.addFix(i1 >> 4, i);
                    }

                    if (ChunkPalettedStorageFix.MappingConstants.VIRTUAL.get(i1 >> 4)) {
                        int j1 = ChunkPalettedStorageFix.getSideMask(j == 0, j == 15, l == 0, l == 15);
                        if (j1 == 0) {
                            this.update.add(i);
                        } else {
                            pSides |= j1;
                        }
                    }

                    this.setBlock(i, BlockStateData.getTag(i1));
                }

                return pSides;
            }
        }

        private void addFix(int pIndex, int pValue) {
            IntList intlist = this.toFix.get(pIndex);
            if (intlist == null) {
                intlist = new IntArrayList();
                this.toFix.put(pIndex, intlist);
            }

            intlist.add(pValue);
        }

        public Dynamic<?> write() {
            Dynamic<?> dynamic = this.section;
            if (!this.hasData) {
                return dynamic;
            } else {
                dynamic = dynamic.set("Palette", dynamic.createList(this.listTag.stream()));
                int i = Math.max(4, DataFixUtils.ceillog2(this.seen.size()));
                PackedBitStorage packedbitstorage = new PackedBitStorage(i, 4096);

                for (int j = 0; j < this.buffer.length; j++) {
                    packedbitstorage.set(j, this.buffer[j]);
                }

                dynamic = dynamic.set("BlockStates", dynamic.createLongList(Arrays.stream(packedbitstorage.getRaw())));
                dynamic = dynamic.remove("Blocks");
                dynamic = dynamic.remove("Data");
                return dynamic.remove("Add");
            }
        }
    }

    static final class UpgradeChunk {
        private int sides;
        private final ChunkPalettedStorageFix.Section[] sections = new ChunkPalettedStorageFix.Section[16];
        private final Dynamic<?> level;
        private final int x;
        private final int z;
        private final Int2ObjectMap<Dynamic<?>> blockEntities = new Int2ObjectLinkedOpenHashMap<>(16);

        public UpgradeChunk(Dynamic<?> pLevel) {
            this.level = pLevel;
            this.x = pLevel.get("xPos").asInt(0) << 4;
            this.z = pLevel.get("zPos").asInt(0) << 4;
            pLevel.get("TileEntities")
                .asStreamOpt()
                .ifSuccess(
                    p_15241_ -> p_15241_.forEach(
                        p_145228_ -> {
                            int l3 = p_145228_.get("x").asInt(0) - this.x & 15;
                            int i4 = p_145228_.get("y").asInt(0);
                            int j4 = p_145228_.get("z").asInt(0) - this.z & 15;
                            int k4 = i4 << 8 | j4 << 4 | l3;
                            if (this.blockEntities.put(k4, (Dynamic<?>)p_145228_) != null) {
                                ChunkPalettedStorageFix.LOGGER
                                    .warn("In chunk: {}x{} found a duplicate block entity at position: [{}, {}, {}]", this.x, this.z, l3, i4, j4);
                            }
                        }
                    )
                );
            boolean flag = pLevel.get("convertedFromAlphaFormat").asBoolean(false);
            pLevel.get("Sections").asStreamOpt().ifSuccess(p_15235_ -> p_15235_.forEach(p_145226_ -> {
                ChunkPalettedStorageFix.Section chunkpalettedstoragefix$section1 = new ChunkPalettedStorageFix.Section((Dynamic<?>)p_145226_);
                this.sides = chunkpalettedstoragefix$section1.upgrade(this.sides);
                this.sections[chunkpalettedstoragefix$section1.y] = chunkpalettedstoragefix$section1;
            }));

            for (ChunkPalettedStorageFix.Section chunkpalettedstoragefix$section : this.sections) {
                if (chunkpalettedstoragefix$section != null) {
                    for (Entry<IntList> entry : chunkpalettedstoragefix$section.toFix.int2ObjectEntrySet()) {
                        int i = chunkpalettedstoragefix$section.y << 12;
                        switch (entry.getIntKey()) {
                            case 2:
                                for (int i3 : entry.getValue()) {
                                    i3 |= i;
                                    Dynamic<?> dynamic11 = this.getBlock(i3);
                                    if ("minecraft:grass_block".equals(ChunkPalettedStorageFix.getName(dynamic11))) {
                                        String s11 = ChunkPalettedStorageFix.getName(this.getBlock(relative(i3, ChunkPalettedStorageFix.Direction.UP)));
                                        if ("minecraft:snow".equals(s11) || "minecraft:snow_layer".equals(s11)) {
                                            this.setBlock(i3, ChunkPalettedStorageFix.MappingConstants.SNOWY_GRASS);
                                        }
                                    }
                                }
                                break;
                            case 3:
                                for (int l2 : entry.getValue()) {
                                    l2 |= i;
                                    Dynamic<?> dynamic10 = this.getBlock(l2);
                                    if ("minecraft:podzol".equals(ChunkPalettedStorageFix.getName(dynamic10))) {
                                        String s10 = ChunkPalettedStorageFix.getName(this.getBlock(relative(l2, ChunkPalettedStorageFix.Direction.UP)));
                                        if ("minecraft:snow".equals(s10) || "minecraft:snow_layer".equals(s10)) {
                                            this.setBlock(l2, ChunkPalettedStorageFix.MappingConstants.SNOWY_PODZOL);
                                        }
                                    }
                                }
                                break;
                            case 25:
                                for (int k2 : entry.getValue()) {
                                    k2 |= i;
                                    Dynamic<?> dynamic9 = this.removeBlockEntity(k2);
                                    if (dynamic9 != null) {
                                        String s9 = Boolean.toString(dynamic9.get("powered").asBoolean(false))
                                            + (byte)Math.min(Math.max(dynamic9.get("note").asInt(0), 0), 24);
                                        this.setBlock(
                                            k2,
                                            ChunkPalettedStorageFix.MappingConstants.NOTE_BLOCK_MAP
                                                .getOrDefault(s9, ChunkPalettedStorageFix.MappingConstants.NOTE_BLOCK_MAP.get("false0"))
                                        );
                                    }
                                }
                                break;
                            case 26:
                                for (int j2 : entry.getValue()) {
                                    j2 |= i;
                                    Dynamic<?> dynamic8 = this.getBlockEntity(j2);
                                    Dynamic<?> dynamic14 = this.getBlock(j2);
                                    if (dynamic8 != null) {
                                        int k3 = dynamic8.get("color").asInt(0);
                                        if (k3 != 14 && k3 >= 0 && k3 < 16) {
                                            String s15 = ChunkPalettedStorageFix.getProperty(dynamic14, "facing")
                                                + ChunkPalettedStorageFix.getProperty(dynamic14, "occupied")
                                                + ChunkPalettedStorageFix.getProperty(dynamic14, "part")
                                                + k3;
                                            if (ChunkPalettedStorageFix.MappingConstants.BED_BLOCK_MAP.containsKey(s15)) {
                                                this.setBlock(j2, ChunkPalettedStorageFix.MappingConstants.BED_BLOCK_MAP.get(s15));
                                            }
                                        }
                                    }
                                }
                                break;
                            case 64:
                            case 71:
                            case 193:
                            case 194:
                            case 195:
                            case 196:
                            case 197:
                                for (int i2 : entry.getValue()) {
                                    i2 |= i;
                                    Dynamic<?> dynamic7 = this.getBlock(i2);
                                    if (ChunkPalettedStorageFix.getName(dynamic7).endsWith("_door")) {
                                        Dynamic<?> dynamic13 = this.getBlock(i2);
                                        if ("lower".equals(ChunkPalettedStorageFix.getProperty(dynamic13, "half"))) {
                                            int j3 = relative(i2, ChunkPalettedStorageFix.Direction.UP);
                                            Dynamic<?> dynamic15 = this.getBlock(j3);
                                            String s16 = ChunkPalettedStorageFix.getName(dynamic13);
                                            if (s16.equals(ChunkPalettedStorageFix.getName(dynamic15))) {
                                                String s1 = ChunkPalettedStorageFix.getProperty(dynamic13, "facing");
                                                String s2 = ChunkPalettedStorageFix.getProperty(dynamic13, "open");
                                                String s3 = flag ? "left" : ChunkPalettedStorageFix.getProperty(dynamic15, "hinge");
                                                String s4 = flag ? "false" : ChunkPalettedStorageFix.getProperty(dynamic15, "powered");
                                                this.setBlock(i2, ChunkPalettedStorageFix.MappingConstants.DOOR_MAP.get(s16 + s1 + "lower" + s3 + s2 + s4));
                                                this.setBlock(j3, ChunkPalettedStorageFix.MappingConstants.DOOR_MAP.get(s16 + s1 + "upper" + s3 + s2 + s4));
                                            }
                                        }
                                    }
                                }
                                break;
                            case 86:
                                for (int l1 : entry.getValue()) {
                                    l1 |= i;
                                    Dynamic<?> dynamic6 = this.getBlock(l1);
                                    if ("minecraft:carved_pumpkin".equals(ChunkPalettedStorageFix.getName(dynamic6))) {
                                        String s8 = ChunkPalettedStorageFix.getName(this.getBlock(relative(l1, ChunkPalettedStorageFix.Direction.DOWN)));
                                        if ("minecraft:grass_block".equals(s8) || "minecraft:dirt".equals(s8)) {
                                            this.setBlock(l1, ChunkPalettedStorageFix.MappingConstants.PUMPKIN);
                                        }
                                    }
                                }
                                break;
                            case 110:
                                for (int k1 : entry.getValue()) {
                                    k1 |= i;
                                    Dynamic<?> dynamic5 = this.getBlock(k1);
                                    if ("minecraft:mycelium".equals(ChunkPalettedStorageFix.getName(dynamic5))) {
                                        String s7 = ChunkPalettedStorageFix.getName(this.getBlock(relative(k1, ChunkPalettedStorageFix.Direction.UP)));
                                        if ("minecraft:snow".equals(s7) || "minecraft:snow_layer".equals(s7)) {
                                            this.setBlock(k1, ChunkPalettedStorageFix.MappingConstants.SNOWY_MYCELIUM);
                                        }
                                    }
                                }
                                break;
                            case 140:
                                for (int j1 : entry.getValue()) {
                                    j1 |= i;
                                    Dynamic<?> dynamic4 = this.removeBlockEntity(j1);
                                    if (dynamic4 != null) {
                                        String s6 = dynamic4.get("Item").asString("") + dynamic4.get("Data").asInt(0);
                                        this.setBlock(
                                            j1,
                                            ChunkPalettedStorageFix.MappingConstants.FLOWER_POT_MAP
                                                .getOrDefault(s6, ChunkPalettedStorageFix.MappingConstants.FLOWER_POT_MAP.get("minecraft:air0"))
                                        );
                                    }
                                }
                                break;
                            case 144:
                                for (int i1 : entry.getValue()) {
                                    i1 |= i;
                                    Dynamic<?> dynamic3 = this.getBlockEntity(i1);
                                    if (dynamic3 != null) {
                                        String s5 = String.valueOf(dynamic3.get("SkullType").asInt(0));
                                        String s13 = ChunkPalettedStorageFix.getProperty(this.getBlock(i1), "facing");
                                        String s14;
                                        if (!"up".equals(s13) && !"down".equals(s13)) {
                                            s14 = s5 + s13;
                                        } else {
                                            s14 = s5 + dynamic3.get("Rot").asInt(0);
                                        }

                                        dynamic3.remove("SkullType");
                                        dynamic3.remove("facing");
                                        dynamic3.remove("Rot");
                                        this.setBlock(
                                            i1,
                                            ChunkPalettedStorageFix.MappingConstants.SKULL_MAP
                                                .getOrDefault(s14, ChunkPalettedStorageFix.MappingConstants.SKULL_MAP.get("0north"))
                                        );
                                    }
                                }
                                break;
                            case 175:
                                for (int l : entry.getValue()) {
                                    l |= i;
                                    Dynamic<?> dynamic2 = this.getBlock(l);
                                    if ("upper".equals(ChunkPalettedStorageFix.getProperty(dynamic2, "half"))) {
                                        Dynamic<?> dynamic12 = this.getBlock(relative(l, ChunkPalettedStorageFix.Direction.DOWN));
                                        String s12 = ChunkPalettedStorageFix.getName(dynamic12);
                                        switch (s12) {
                                            case "minecraft:sunflower":
                                                this.setBlock(l, ChunkPalettedStorageFix.MappingConstants.UPPER_SUNFLOWER);
                                                break;
                                            case "minecraft:lilac":
                                                this.setBlock(l, ChunkPalettedStorageFix.MappingConstants.UPPER_LILAC);
                                                break;
                                            case "minecraft:tall_grass":
                                                this.setBlock(l, ChunkPalettedStorageFix.MappingConstants.UPPER_TALL_GRASS);
                                                break;
                                            case "minecraft:large_fern":
                                                this.setBlock(l, ChunkPalettedStorageFix.MappingConstants.UPPER_LARGE_FERN);
                                                break;
                                            case "minecraft:rose_bush":
                                                this.setBlock(l, ChunkPalettedStorageFix.MappingConstants.UPPER_ROSE_BUSH);
                                                break;
                                            case "minecraft:peony":
                                                this.setBlock(l, ChunkPalettedStorageFix.MappingConstants.UPPER_PEONY);
                                        }
                                    }
                                }
                                break;
                            case 176:
                            case 177:
                                for (int j : entry.getValue()) {
                                    j |= i;
                                    Dynamic<?> dynamic = this.getBlockEntity(j);
                                    Dynamic<?> dynamic1 = this.getBlock(j);
                                    if (dynamic != null) {
                                        int k = dynamic.get("Base").asInt(0);
                                        if (k != 15 && k >= 0 && k < 16) {
                                            String s = ChunkPalettedStorageFix.getProperty(dynamic1, entry.getIntKey() == 176 ? "rotation" : "facing") + "_" + k;
                                            if (ChunkPalettedStorageFix.MappingConstants.BANNER_BLOCK_MAP.containsKey(s)) {
                                                this.setBlock(j, ChunkPalettedStorageFix.MappingConstants.BANNER_BLOCK_MAP.get(s));
                                            }
                                        }
                                    }
                                }
                        }
                    }
                }
            }
        }

        @Nullable
        private Dynamic<?> getBlockEntity(int pIndex) {
            return this.blockEntities.get(pIndex);
        }

        @Nullable
        private Dynamic<?> removeBlockEntity(int pIndex) {
            return this.blockEntities.remove(pIndex);
        }

        public static int relative(int pData, ChunkPalettedStorageFix.Direction pDirection) {
            return switch (pDirection.getAxis()) {
                case X -> {
                    int k = (pData & 15) + pDirection.getAxisDirection().getStep();
                    yield k >= 0 && k <= 15 ? pData & -16 | k : -1;
                }
                case Y -> {
                    int j = (pData >> 8) + pDirection.getAxisDirection().getStep();
                    yield j >= 0 && j <= 255 ? pData & 0xFF | j << 8 : -1;
                }
                case Z -> {
                    int i = (pData >> 4 & 15) + pDirection.getAxisDirection().getStep();
                    yield i >= 0 && i <= 15 ? pData & -241 | i << 4 : -1;
                }
            };
        }

        private void setBlock(int pIndex, Dynamic<?> pBlock) {
            if (pIndex >= 0 && pIndex <= 65535) {
                ChunkPalettedStorageFix.Section chunkpalettedstoragefix$section = this.getSection(pIndex);
                if (chunkpalettedstoragefix$section != null) {
                    chunkpalettedstoragefix$section.setBlock(pIndex & 4095, pBlock);
                }
            }
        }

        @Nullable
        private ChunkPalettedStorageFix.Section getSection(int pIndex) {
            int i = pIndex >> 12;
            return i < this.sections.length ? this.sections[i] : null;
        }

        public Dynamic<?> getBlock(int pIndex) {
            if (pIndex >= 0 && pIndex <= 65535) {
                ChunkPalettedStorageFix.Section chunkpalettedstoragefix$section = this.getSection(pIndex);
                return chunkpalettedstoragefix$section == null
                    ? ChunkPalettedStorageFix.MappingConstants.AIR
                    : chunkpalettedstoragefix$section.getBlock(pIndex & 4095);
            } else {
                return ChunkPalettedStorageFix.MappingConstants.AIR;
            }
        }

        public Dynamic<?> write() {
            Dynamic<?> dynamic = this.level;
            if (this.blockEntities.isEmpty()) {
                dynamic = dynamic.remove("TileEntities");
            } else {
                dynamic = dynamic.set("TileEntities", dynamic.createList(this.blockEntities.values().stream()));
            }

            Dynamic<?> dynamic1 = dynamic.emptyMap();
            List<Dynamic<?>> list = Lists.newArrayList();

            for (ChunkPalettedStorageFix.Section chunkpalettedstoragefix$section : this.sections) {
                if (chunkpalettedstoragefix$section != null) {
                    list.add(chunkpalettedstoragefix$section.write());
                    dynamic1 = dynamic1.set(
                        String.valueOf(chunkpalettedstoragefix$section.y),
                        dynamic1.createIntList(Arrays.stream(chunkpalettedstoragefix$section.update.toIntArray()))
                    );
                }
            }

            Dynamic<?> dynamic2 = dynamic.emptyMap();
            dynamic2 = dynamic2.set("Sides", dynamic2.createByte((byte)this.sides));
            dynamic2 = dynamic2.set("Indices", dynamic1);
            return dynamic.set("UpgradeData", dynamic2).set("Sections", dynamic2.createList(list.stream()));
        }
    }
}
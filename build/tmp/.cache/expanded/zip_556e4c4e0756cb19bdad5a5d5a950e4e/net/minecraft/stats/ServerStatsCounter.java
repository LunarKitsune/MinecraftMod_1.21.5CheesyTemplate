package net.minecraft.stats;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundAwardStatsPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

/**
 * Server-side implementation of {@link net.minecraft.stats.StatsCounter}; handles counting, serialising, and de-
 * serialising statistics, as well as sending them to connected clients via the {@linkplain
 * net.minecraft.network.protocol.game.ClientboundAwardStatsPacket award stats packet}.
 */
public class ServerStatsCounter extends StatsCounter {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Codec<Map<Stat<?>, Integer>> STATS_CODEC = Codec.dispatchedMap(
            BuiltInRegistries.STAT_TYPE.byNameCodec(), Util.memoize(ServerStatsCounter::createTypedStatsCodec)
        )
        .xmap(p_390196_ -> {
            Map<Stat<?>, Integer> map = new HashMap<>();
            p_390196_.forEach((p_390199_, p_390200_) -> map.putAll((Map<? extends Stat<?>, ? extends Integer>)p_390200_));
            return map;
        }, p_390195_ -> p_390195_.entrySet().stream().collect(Collectors.groupingBy(p_390201_ -> p_390201_.getKey().getType(), Util.toMap())));
    private final MinecraftServer server;
    private final File file;
    private final Set<Stat<?>> dirty = Sets.newHashSet();

    private static <T> Codec<Map<Stat<?>, Integer>> createTypedStatsCodec(StatType<T> pType) {
        Codec<T> codec = pType.getRegistry().byNameCodec();
        Codec<Stat<?>> codec1 = codec.flatComapMap(
            pType::get,
            p_390205_ -> p_390205_.getType() == pType
                ? DataResult.success((T)p_390205_.getValue())
                : DataResult.error(() -> "Expected type " + pType + ", but got " + p_390205_.getType())
        );
        return Codec.unboundedMap(codec1, Codec.INT);
    }

    public ServerStatsCounter(MinecraftServer pServer, File pFile) {
        this.server = pServer;
        this.file = pFile;
        if (pFile.isFile()) {
            try {
                this.parseLocal(pServer.getFixerUpper(), FileUtils.readFileToString(pFile));
            } catch (IOException ioexception) {
                LOGGER.error("Couldn't read statistics file {}", pFile, ioexception);
            } catch (JsonParseException jsonparseexception) {
                LOGGER.error("Couldn't parse statistics file {}", pFile, jsonparseexception);
            }
        }
    }

    public void save() {
        try {
            FileUtils.writeStringToFile(this.file, this.toJson());
        } catch (IOException ioexception) {
            LOGGER.error("Couldn't save stats", (Throwable)ioexception);
        }
    }

    @Override
    public void setValue(Player pPlayer, Stat<?> pStat, int p_12829_) {
        super.setValue(pPlayer, pStat, p_12829_);
        this.dirty.add(pStat);
    }

    private Set<Stat<?>> getDirty() {
        Set<Stat<?>> set = Sets.newHashSet(this.dirty);
        this.dirty.clear();
        return set;
    }

    public void parseLocal(DataFixer pFixerUpper, String pJson) {
        try {
            try (JsonReader jsonreader = new JsonReader(new StringReader(pJson))) {
                jsonreader.setLenient(false);
                JsonElement jsonelement = Streams.parse(jsonreader);
                if (!jsonelement.isJsonNull()) {
                    Dynamic<JsonElement> dynamic = new Dynamic<>(JsonOps.INSTANCE, jsonelement);
                    dynamic = DataFixTypes.STATS.updateToCurrentVersion(pFixerUpper, dynamic, NbtUtils.getDataVersion(dynamic, 1343));
                    this.stats
                        .putAll(
                            STATS_CODEC.parse(dynamic.get("stats").orElseEmptyMap())
                                .resultOrPartial(p_390197_ -> LOGGER.error("Failed to parse statistics for {}: {}", this.file, p_390197_))
                                .orElse(Map.of())
                        );
                    return;
                }

                LOGGER.error("Unable to parse Stat data from {}", this.file);
            }
        } catch (IOException | JsonParseException jsonparseexception) {
            LOGGER.error("Unable to parse Stat data from {}", this.file, jsonparseexception);
        }
    }

    protected String toJson() {
        JsonObject jsonobject = new JsonObject();
        jsonobject.add("stats", STATS_CODEC.encodeStart(JsonOps.INSTANCE, this.stats).getOrThrow());
        jsonobject.addProperty("DataVersion", SharedConstants.getCurrentVersion().getDataVersion().getVersion());
        return jsonobject.toString();
    }

    public void markAllDirty() {
        this.dirty.addAll(this.stats.keySet());
    }

    public void sendStats(ServerPlayer pPlayer) {
        Object2IntMap<Stat<?>> object2intmap = new Object2IntOpenHashMap<>();

        for (Stat<?> stat : this.getDirty()) {
            object2intmap.put(stat, this.getValue(stat));
        }

        pPlayer.connection.send(new ClientboundAwardStatsPacket(object2intmap));
    }
}
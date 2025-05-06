package net.minecraft.server.bossevents;

import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import java.util.Collection;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

public class CustomBossEvents {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Codec<Map<ResourceLocation, CustomBossEvent.Packed>> EVENTS_CODEC = Codec.unboundedMap(
        ResourceLocation.CODEC, CustomBossEvent.Packed.CODEC
    );
    private final Map<ResourceLocation, CustomBossEvent> events = Maps.newHashMap();

    @Nullable
    public CustomBossEvent get(ResourceLocation pId) {
        return this.events.get(pId);
    }

    public CustomBossEvent create(ResourceLocation pId, Component pName) {
        CustomBossEvent custombossevent = new CustomBossEvent(pId, pName);
        this.events.put(pId, custombossevent);
        return custombossevent;
    }

    public void remove(CustomBossEvent pBossbar) {
        this.events.remove(pBossbar.getTextId());
    }

    public Collection<ResourceLocation> getIds() {
        return this.events.keySet();
    }

    public Collection<CustomBossEvent> getEvents() {
        return this.events.values();
    }

    public CompoundTag save(HolderLookup.Provider pLevelRegistry) {
        Map<ResourceLocation, CustomBossEvent.Packed> map = Util.mapValues(this.events, CustomBossEvent::pack);
        return (CompoundTag)EVENTS_CODEC.encodeStart(pLevelRegistry.createSerializationContext(NbtOps.INSTANCE), map).getOrThrow();
    }

    public void load(CompoundTag pTag, HolderLookup.Provider pLevelRegistry) {
        Map<ResourceLocation, CustomBossEvent.Packed> map = EVENTS_CODEC.parse(pLevelRegistry.createSerializationContext(NbtOps.INSTANCE), pTag)
            .resultOrPartial(p_397243_ -> LOGGER.error("Failed to parse boss bar events: {}", p_397243_))
            .orElse(Map.of());
        map.forEach((p_393451_, p_392436_) -> this.events.put(p_393451_, CustomBossEvent.load(p_393451_, p_392436_)));
    }

    public void onPlayerConnect(ServerPlayer pPlayer) {
        for (CustomBossEvent custombossevent : this.events.values()) {
            custombossevent.onPlayerConnect(pPlayer);
        }
    }

    public void onPlayerDisconnect(ServerPlayer pPlayer) {
        for (CustomBossEvent custombossevent : this.events.values()) {
            custombossevent.onPlayerDisconnect(pPlayer);
        }
    }
}
package net.minecraft.util.profiling.jfr.stats;

import java.time.Duration;
import jdk.jfr.consumer.RecordedEvent;
import net.minecraft.world.level.ChunkPos;

public record StructureGenStat(Duration duration, ChunkPos chunkPos, String structureName, String level, boolean success) implements TimedStat {
    public static StructureGenStat from(RecordedEvent pEvent) {
        return new StructureGenStat(
            pEvent.getDuration(),
            new ChunkPos(pEvent.getInt("chunkPosX"), pEvent.getInt("chunkPosX")),
            pEvent.getString("structure"),
            pEvent.getString("level"),
            pEvent.getBoolean("success")
        );
    }

    @Override
    public Duration duration() {
        return this.duration;
    }
}
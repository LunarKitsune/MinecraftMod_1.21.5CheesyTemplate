package net.minecraft.util.profiling.jfr.stats;

import jdk.jfr.consumer.RecordedEvent;

public record CpuLoadStat(double jvm, double userJvm, double system) {
    public static CpuLoadStat from(RecordedEvent pEvent) {
        return new CpuLoadStat(pEvent.getFloat("jvmSystem"), pEvent.getFloat("jvmUser"), pEvent.getFloat("machineTotal"));
    }
}
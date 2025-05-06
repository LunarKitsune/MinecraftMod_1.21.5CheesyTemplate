package com.mojang.blaze3d.platform;

import java.io.File;
import java.time.Duration;
import net.minecraft.CrashReport;
import net.minecraft.client.Minecraft;
import net.minecraft.server.dedicated.ServerWatchdog;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientShutdownWatchdog {
    private static final Duration CRASH_REPORT_PRELOAD_LOAD = Duration.ofSeconds(15L);

    public static void startShutdownWatchdog(File pFile, long pThreadId) {
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(CRASH_REPORT_PRELOAD_LOAD);
            } catch (InterruptedException interruptedexception) {
                return;
            }

            CrashReport crashreport = ServerWatchdog.createWatchdogCrashReport("Client shutdown", pThreadId);
            Minecraft.saveReport(pFile, crashreport);
        });
        thread.setDaemon(true);
        thread.setName("Client shutdown watchdog");
        thread.start();
    }
}
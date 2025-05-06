package net.minecraft;

import com.mojang.jtracy.TracyClient;
import com.mojang.jtracy.Zone;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public record TracingExecutor(ExecutorService service) implements Executor {
    public Executor forName(String pName) {
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            return p_369604_ -> this.service.execute(() -> {
                Thread thread = Thread.currentThread();
                String s = thread.getName();
                thread.setName(pName);

                try (Zone zone = TracyClient.beginZone(pName, SharedConstants.IS_RUNNING_IN_IDE)) {
                    p_369604_.run();
                } finally {
                    thread.setName(s);
                }
            });
        } else {
            return (TracyClient.isAvailable() ? p_366279_ -> this.service.execute(() -> {
                try (Zone zone = TracyClient.beginZone(pName, SharedConstants.IS_RUNNING_IN_IDE)) {
                    p_366279_.run();
                }
            }) : this.service);
        }
    }

    @Override
    public void execute(Runnable pTask) {
        this.service.execute(wrapUnnamed(pTask));
    }

    public void shutdownAndAwait(long pTimeout, TimeUnit pUnit) {
        this.service.shutdown();

        boolean flag;
        try {
            flag = this.service.awaitTermination(pTimeout, pUnit);
        } catch (InterruptedException interruptedexception) {
            flag = false;
        }

        if (!flag) {
            this.service.shutdownNow();
        }
    }

    private static Runnable wrapUnnamed(Runnable pTask) {
        return !TracyClient.isAvailable() ? pTask : () -> {
            try (Zone zone = TracyClient.beginZone("task", SharedConstants.IS_RUNNING_IN_IDE)) {
                pTask.run();
            }
        };
    }
}
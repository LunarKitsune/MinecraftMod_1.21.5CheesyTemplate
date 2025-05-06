package net.minecraft.world.level.timers;

import com.mojang.serialization.MapCodec;

public interface TimerCallback<T> {
    void handle(T pObj, TimerQueue<T> pManager, long pGameTime);

    MapCodec<? extends TimerCallback<T>> codec();
}
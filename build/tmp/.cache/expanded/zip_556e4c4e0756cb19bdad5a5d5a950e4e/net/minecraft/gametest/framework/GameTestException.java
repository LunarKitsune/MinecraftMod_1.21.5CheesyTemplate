package net.minecraft.gametest.framework;

import net.minecraft.network.chat.Component;

public abstract class GameTestException extends RuntimeException {
    public GameTestException(String pMesage) {
        super(pMesage);
    }

    public abstract Component getDescription();
}
package net.minecraft.gametest.framework;

import net.minecraft.network.chat.Component;

public class GameTestTimeoutException extends GameTestException {
    protected final Component message;

    public GameTestTimeoutException(Component pMessage) {
        super(pMessage.getString());
        this.message = pMessage;
    }

    @Override
    public Component getDescription() {
        return this.message;
    }
}
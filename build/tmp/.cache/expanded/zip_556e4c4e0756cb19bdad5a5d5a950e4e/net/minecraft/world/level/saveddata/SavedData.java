package net.minecraft.world.level.saveddata;

import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;

public abstract class SavedData {
    private boolean dirty;

    public void setDirty() {
        this.setDirty(true);
    }

    public void setDirty(boolean pDirty) {
        this.dirty = pDirty;
    }

    public boolean isDirty() {
        return this.dirty;
    }

    public record Context(@Nullable ServerLevel level, long worldSeed) {
        public Context(ServerLevel pLevel) {
            this(pLevel, pLevel.getSeed());
        }

        public ServerLevel levelOrThrow() {
            return Objects.requireNonNull(this.level);
        }
    }
}
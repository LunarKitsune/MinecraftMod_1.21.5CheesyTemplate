package net.minecraft.client.sounds;

import javax.annotation.Nullable;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.Music;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record MusicInfo(@Nullable Music music, float volume) {
    public MusicInfo(Music pMusic) {
        this(pMusic, 1.0F);
    }

    public boolean canReplace(SoundInstance pSoundInstance) {
        return this.music == null ? false : this.music.replaceCurrentMusic() && !this.music.getEvent().value().location().equals(pSoundInstance.getLocation());
    }
}
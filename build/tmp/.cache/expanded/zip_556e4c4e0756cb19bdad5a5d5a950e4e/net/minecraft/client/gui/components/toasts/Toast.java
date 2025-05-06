package net.minecraft.client.gui.components.toasts;

import javax.annotation.Nullable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface Toast {
    Object NO_TOKEN = new Object();
    int DEFAULT_WIDTH = 160;
    int SLOT_HEIGHT = 32;

    Toast.Visibility getWantedVisibility();

    void update(ToastManager pToastManager, long pVisibilityTime);

    @Nullable
    default SoundEvent getSoundEvent() {
        return null;
    }

    void render(GuiGraphics pGuiGraphics, Font pFont, long pVisibilityTime);

    default Object getToken() {
        return NO_TOKEN;
    }

    default int width() {
        return 160;
    }

    default int height() {
        return 32;
    }

    default int occcupiedSlotCount() {
        return Mth.positiveCeilDiv(this.height(), 32);
    }

    @OnlyIn(Dist.CLIENT)
    public static enum Visibility {
        SHOW(SoundEvents.UI_TOAST_IN),
        HIDE(SoundEvents.UI_TOAST_OUT);

        private final SoundEvent soundEvent;

        private Visibility(final SoundEvent pSoundEvent) {
            this.soundEvent = pSoundEvent;
        }

        public void playSound(SoundManager pHandler) {
            pHandler.play(SimpleSoundInstance.forUI(this.soundEvent, 1.0F, 1.0F));
        }
    }
}
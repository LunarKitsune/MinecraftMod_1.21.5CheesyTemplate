package net.minecraft.client.gui.screens;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BackupConfirmScreen extends Screen {
    private static final Component SKIP_AND_JOIN = Component.translatable("selectWorld.backupJoinSkipButton");
    public static final Component BACKUP_AND_JOIN = Component.translatable("selectWorld.backupJoinConfirmButton");
    private final Runnable onCancel;
    protected final BackupConfirmScreen.Listener onProceed;
    private final Component description;
    private final boolean promptForCacheErase;
    private MultiLineLabel message = MultiLineLabel.EMPTY;
    final Component confirmation;
    protected int id;
    private Checkbox eraseCache;

    public BackupConfirmScreen(Runnable pOnCancel, BackupConfirmScreen.Listener pOnProceed, Component pTitle, Component pDescription, boolean pPromptForCacheErase) {
        this(pOnCancel, pOnProceed, pTitle, pDescription, BACKUP_AND_JOIN, pPromptForCacheErase);
    }

    public BackupConfirmScreen(
        Runnable pOnCancel, BackupConfirmScreen.Listener pOnProceed, Component pTitle, Component pDescription, Component pConfirmation, boolean pPromptForCacheErase
    ) {
        super(pTitle);
        this.onCancel = pOnCancel;
        this.onProceed = pOnProceed;
        this.description = pDescription;
        this.promptForCacheErase = pPromptForCacheErase;
        this.confirmation = pConfirmation;
    }

    @Override
    protected void init() {
        super.init();
        this.message = MultiLineLabel.create(this.font, this.description, this.width - 50);
        int i = (this.message.getLineCount() + 1) * 9;
        this.eraseCache = Checkbox.builder(Component.translatable("selectWorld.backupEraseCache"), this.font)
            .pos(this.width / 2 - 155 + 80, 76 + i)
            .build();
        if (this.promptForCacheErase) {
            this.addRenderableWidget(this.eraseCache);
        }

        this.addRenderableWidget(
            Button.builder(this.confirmation, p_308190_ -> this.onProceed.proceed(true, this.eraseCache.selected()))
                .bounds(this.width / 2 - 155, 100 + i, 150, 20)
                .build()
        );
        this.addRenderableWidget(
            Button.builder(SKIP_AND_JOIN, p_308188_ -> this.onProceed.proceed(false, this.eraseCache.selected()))
                .bounds(this.width / 2 - 155 + 160, 100 + i, 150, 20)
                .build()
        );
        this.addRenderableWidget(
            Button.builder(CommonComponents.GUI_CANCEL, p_308189_ -> this.onCancel.run())
                .bounds(this.width / 2 - 155 + 80, 124 + i, 150, 20)
                .build()
        );
    }

    @Override
    public void render(GuiGraphics p_282759_, int p_282356_, int p_282725_, float p_281518_) {
        super.render(p_282759_, p_282356_, p_282725_, p_281518_);
        p_282759_.drawCenteredString(this.font, this.title, this.width / 2, 50, 16777215);
        this.message.renderCentered(p_282759_, this.width / 2, 70);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (pKeyCode == 256) {
            this.onCancel.run();
            return true;
        } else {
            return super.keyPressed(pKeyCode, pScanCode, pModifiers);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public interface Listener {
        void proceed(boolean pConfirmed, boolean pEraseCache);
    }
}
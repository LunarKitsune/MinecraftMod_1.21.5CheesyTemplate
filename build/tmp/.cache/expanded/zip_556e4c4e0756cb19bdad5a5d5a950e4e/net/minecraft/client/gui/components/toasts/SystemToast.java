package net.minecraft.client.gui.components.toasts;

import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SystemToast implements Toast {
    private static final ResourceLocation BACKGROUND_SPRITE = ResourceLocation.withDefaultNamespace("toast/system");
    private static final int MAX_LINE_SIZE = 200;
    private static final int LINE_SPACING = 12;
    private static final int MARGIN = 10;
    private final SystemToast.SystemToastId id;
    private Component title;
    private List<FormattedCharSequence> messageLines;
    private long lastChanged;
    private boolean changed;
    private final int width;
    private boolean forceHide;
    private Toast.Visibility wantedVisibility = Toast.Visibility.HIDE;

    public SystemToast(SystemToast.SystemToastId pId, Component pTitle, @Nullable Component pMessage) {
        this(
            pId,
            pTitle,
            nullToEmpty(pMessage),
            Math.max(
                160, 30 + Math.max(Minecraft.getInstance().font.width(pTitle), pMessage == null ? 0 : Minecraft.getInstance().font.width(pMessage))
            )
        );
    }

    public static SystemToast multiline(Minecraft pMinecraft, SystemToast.SystemToastId pId, Component pTitle, Component pMessage) {
        Font font = pMinecraft.font;
        List<FormattedCharSequence> list = font.split(pMessage, 200);
        int i = Math.max(200, list.stream().mapToInt(font::width).max().orElse(200));
        return new SystemToast(pId, pTitle, list, i + 30);
    }

    private SystemToast(SystemToast.SystemToastId pId, Component pTitle, List<FormattedCharSequence> pMessageLines, int pWidth) {
        this.id = pId;
        this.title = pTitle;
        this.messageLines = pMessageLines;
        this.width = pWidth;
    }

    private static ImmutableList<FormattedCharSequence> nullToEmpty(@Nullable Component pMessage) {
        return pMessage == null ? ImmutableList.of() : ImmutableList.of(pMessage.getVisualOrderText());
    }

    @Override
    public int width() {
        return this.width;
    }

    @Override
    public int height() {
        return 20 + Math.max(this.messageLines.size(), 1) * 12;
    }

    public void forceHide() {
        this.forceHide = true;
    }

    @Override
    public Toast.Visibility getWantedVisibility() {
        return this.wantedVisibility;
    }

    @Override
    public void update(ToastManager p_361843_, long p_364076_) {
        if (this.changed) {
            this.lastChanged = p_364076_;
            this.changed = false;
        }

        double d0 = this.id.displayTime * p_361843_.getNotificationDisplayTimeMultiplier();
        long i = p_364076_ - this.lastChanged;
        this.wantedVisibility = !this.forceHide && i < d0 ? Toast.Visibility.SHOW : Toast.Visibility.HIDE;
    }

    @Override
    public void render(GuiGraphics p_281624_, Font p_368558_, long p_282762_) {
        p_281624_.blitSprite(RenderType::guiTextured, BACKGROUND_SPRITE, 0, 0, this.width(), this.height());
        if (this.messageLines.isEmpty()) {
            p_281624_.drawString(p_368558_, this.title, 18, 12, -256, false);
        } else {
            p_281624_.drawString(p_368558_, this.title, 18, 7, -256, false);

            for (int i = 0; i < this.messageLines.size(); i++) {
                p_281624_.drawString(p_368558_, this.messageLines.get(i), 18, 18 + i * 12, -1, false);
            }
        }
    }

    public void reset(Component pTitle, @Nullable Component pMessage) {
        this.title = pTitle;
        this.messageLines = nullToEmpty(pMessage);
        this.changed = true;
    }

    public SystemToast.SystemToastId getToken() {
        return this.id;
    }

    public static void add(ToastManager pToastManager, SystemToast.SystemToastId pId, Component pTitle, @Nullable Component pMessage) {
        pToastManager.addToast(new SystemToast(pId, pTitle, pMessage));
    }

    public static void addOrUpdate(ToastManager pToastManager, SystemToast.SystemToastId pId, Component pTitle, @Nullable Component pMessage) {
        SystemToast systemtoast = pToastManager.getToast(SystemToast.class, pId);
        if (systemtoast == null) {
            add(pToastManager, pId, pTitle, pMessage);
        } else {
            systemtoast.reset(pTitle, pMessage);
        }
    }

    public static void forceHide(ToastManager pToastManager, SystemToast.SystemToastId pId) {
        SystemToast systemtoast = pToastManager.getToast(SystemToast.class, pId);
        if (systemtoast != null) {
            systemtoast.forceHide();
        }
    }

    public static void onWorldAccessFailure(Minecraft pMinecraft, String pMessage) {
        add(pMinecraft.getToastManager(), SystemToast.SystemToastId.WORLD_ACCESS_FAILURE, Component.translatable("selectWorld.access_failure"), Component.literal(pMessage));
    }

    public static void onWorldDeleteFailure(Minecraft pMinecraft, String pMessage) {
        add(pMinecraft.getToastManager(), SystemToast.SystemToastId.WORLD_ACCESS_FAILURE, Component.translatable("selectWorld.delete_failure"), Component.literal(pMessage));
    }

    public static void onPackCopyFailure(Minecraft pMinecraft, String pMessage) {
        add(pMinecraft.getToastManager(), SystemToast.SystemToastId.PACK_COPY_FAILURE, Component.translatable("pack.copyFailure"), Component.literal(pMessage));
    }

    public static void onFileDropFailure(Minecraft pMinecraft, int pFailedFileCount) {
        add(
            pMinecraft.getToastManager(),
            SystemToast.SystemToastId.FILE_DROP_FAILURE,
            Component.translatable("gui.fileDropFailure.title"),
            Component.translatable("gui.fileDropFailure.detail", pFailedFileCount)
        );
    }

    public static void onLowDiskSpace(Minecraft pMinecraft) {
        addOrUpdate(
            pMinecraft.getToastManager(),
            SystemToast.SystemToastId.LOW_DISK_SPACE,
            Component.translatable("chunk.toast.lowDiskSpace"),
            Component.translatable("chunk.toast.lowDiskSpace.description")
        );
    }

    public static void onChunkLoadFailure(Minecraft pMinecraft, ChunkPos pChunkPos) {
        addOrUpdate(
            pMinecraft.getToastManager(),
            SystemToast.SystemToastId.CHUNK_LOAD_FAILURE,
            Component.translatable("chunk.toast.loadFailure", Component.translationArg(pChunkPos)).withStyle(ChatFormatting.RED),
            Component.translatable("chunk.toast.checkLog")
        );
    }

    public static void onChunkSaveFailure(Minecraft pMinecraft, ChunkPos pChunkPos) {
        addOrUpdate(
            pMinecraft.getToastManager(),
            SystemToast.SystemToastId.CHUNK_SAVE_FAILURE,
            Component.translatable("chunk.toast.saveFailure", Component.translationArg(pChunkPos)).withStyle(ChatFormatting.RED),
            Component.translatable("chunk.toast.checkLog")
        );
    }

    @OnlyIn(Dist.CLIENT)
    public static class SystemToastId {
        public static final SystemToast.SystemToastId NARRATOR_TOGGLE = new SystemToast.SystemToastId();
        public static final SystemToast.SystemToastId WORLD_BACKUP = new SystemToast.SystemToastId();
        public static final SystemToast.SystemToastId PACK_LOAD_FAILURE = new SystemToast.SystemToastId();
        public static final SystemToast.SystemToastId WORLD_ACCESS_FAILURE = new SystemToast.SystemToastId();
        public static final SystemToast.SystemToastId PACK_COPY_FAILURE = new SystemToast.SystemToastId();
        public static final SystemToast.SystemToastId FILE_DROP_FAILURE = new SystemToast.SystemToastId();
        public static final SystemToast.SystemToastId PERIODIC_NOTIFICATION = new SystemToast.SystemToastId();
        public static final SystemToast.SystemToastId LOW_DISK_SPACE = new SystemToast.SystemToastId(10000L);
        public static final SystemToast.SystemToastId CHUNK_LOAD_FAILURE = new SystemToast.SystemToastId();
        public static final SystemToast.SystemToastId CHUNK_SAVE_FAILURE = new SystemToast.SystemToastId();
        public static final SystemToast.SystemToastId UNSECURE_SERVER_WARNING = new SystemToast.SystemToastId(10000L);
        final long displayTime;

        public SystemToastId(long pDisplayTime) {
            this.displayTime = pDisplayTime;
        }

        public SystemToastId() {
            this(5000L);
        }
    }
}
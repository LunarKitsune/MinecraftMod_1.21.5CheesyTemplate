package net.minecraft.client.gui.screens;

import javax.annotation.Nullable;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;

@OnlyIn(Dist.CLIENT)
public class ChatScreen extends Screen {
    public static final double MOUSE_SCROLL_SPEED = 7.0;
    private static final Component USAGE_TEXT = Component.translatable("chat_screen.usage");
    private static final int TOOLTIP_MAX_WIDTH = 210;
    private String historyBuffer = "";
    private int historyPos = -1;
    protected EditBox input;
    private String initial;
    CommandSuggestions commandSuggestions;

    public ChatScreen(String pInitial) {
        super(Component.translatable("chat_screen.title"));
        this.initial = pInitial;
    }

    @Override
    protected void init() {
        this.historyPos = this.minecraft.gui.getChat().getRecentChat().size();
        this.input = new EditBox(this.minecraft.fontFilterFishy, 4, this.height - 12, this.width - 4, 12, Component.translatable("chat.editBox")) {
            @Override
            protected MutableComponent createNarrationMessage() {
                return super.createNarrationMessage().append(ChatScreen.this.commandSuggestions.getNarrationMessage());
            }
        };
        this.input.setMaxLength(256);
        this.input.setBordered(false);
        this.input.setValue(this.initial);
        this.input.setResponder(this::onEdited);
        this.input.setCanLoseFocus(false);
        this.addWidget(this.input);
        this.commandSuggestions = new CommandSuggestions(this.minecraft, this, this.input, this.font, false, false, 1, 10, true, -805306368);
        this.commandSuggestions.setAllowHiding(false);
        this.commandSuggestions.updateCommandInfo();
    }

    @Override
    protected void setInitialFocus() {
        this.setInitialFocus(this.input);
    }

    @Override
    public void resize(Minecraft pMinecraft, int pWidth, int pHeight) {
        String s = this.input.getValue();
        this.init(pMinecraft, pWidth, pHeight);
        this.setChatLine(s);
        this.commandSuggestions.updateCommandInfo();
    }

    @Override
    public void removed() {
        this.minecraft.gui.getChat().resetChatScroll();
    }

    private void onEdited(String pValue) {
        String s = this.input.getValue();
        this.commandSuggestions.setAllowSuggestions(!s.equals(this.initial));
        this.commandSuggestions.updateCommandInfo();
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (this.commandSuggestions.keyPressed(pKeyCode, pScanCode, pModifiers)) {
            return true;
        } else if (super.keyPressed(pKeyCode, pScanCode, pModifiers)) {
            return true;
        } else if (pKeyCode == 256) {
            this.minecraft.setScreen(null);
            return true;
        } else if (pKeyCode == 257 || pKeyCode == 335) {
            this.handleChatInput(this.input.getValue(), true);
            if (this.minecraft.screen == this) // FORGE: Prevent closing the screen if another screen has been opened.
            this.minecraft.setScreen(null);
            return true;
        } else if (pKeyCode == 265) {
            this.moveInHistory(-1);
            return true;
        } else if (pKeyCode == 264) {
            this.moveInHistory(1);
            return true;
        } else if (pKeyCode == 266) {
            this.minecraft.gui.getChat().scrollChat(this.minecraft.gui.getChat().getLinesPerPage() - 1);
            return true;
        } else if (pKeyCode == 267) {
            this.minecraft.gui.getChat().scrollChat(-this.minecraft.gui.getChat().getLinesPerPage() + 1);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean mouseScrolled(double p_95581_, double p_95582_, double p_95583_, double p_300876_) {
        p_300876_ = Mth.clamp(p_300876_, -1.0, 1.0);
        if (this.commandSuggestions.mouseScrolled(p_300876_)) {
            return true;
        } else {
            if (!hasShiftDown()) {
                p_300876_ *= 7.0;
            }

            this.minecraft.gui.getChat().scrollChat((int)p_300876_);
            return true;
        }
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (this.commandSuggestions.mouseClicked((int)pMouseX, (int)pMouseY, pButton)) {
            return true;
        } else {
            if (pButton == 0) {
                ChatComponent chatcomponent = this.minecraft.gui.getChat();
                if (chatcomponent.handleChatQueueClicked(pMouseX, pMouseY)) {
                    return true;
                }

                Style style = this.getComponentStyleAt(pMouseX, pMouseY);
                if (style != null && this.handleComponentClicked(style)) {
                    this.initial = this.input.getValue();
                    return true;
                }
            }

            return this.input.mouseClicked(pMouseX, pMouseY, pButton) ? true : super.mouseClicked(pMouseX, pMouseY, pButton);
        }
    }

    @Override
    protected void insertText(String pText, boolean pOverwrite) {
        if (pOverwrite) {
            this.input.setValue(pText);
        } else {
            this.input.insertText(pText);
        }
    }

    public void moveInHistory(int pMsgPos) {
        int i = this.historyPos + pMsgPos;
        int j = this.minecraft.gui.getChat().getRecentChat().size();
        i = Mth.clamp(i, 0, j);
        if (i != this.historyPos) {
            if (i == j) {
                this.historyPos = j;
                this.input.setValue(this.historyBuffer);
            } else {
                if (this.historyPos == j) {
                    this.historyBuffer = this.input.getValue();
                }

                this.input.setValue(this.minecraft.gui.getChat().getRecentChat().get(i));
                this.commandSuggestions.setAllowSuggestions(false);
                this.historyPos = i;
            }
        }
    }

    @Override
    public void render(GuiGraphics p_282470_, int p_282674_, int p_282014_, float p_283132_) {
        this.minecraft.gui.getChat().render(p_282470_, this.minecraft.gui.getGuiTicks(), p_282674_, p_282014_, true);
        p_282470_.fill(2, this.height - 14, this.width - 2, this.height - 2, this.minecraft.options.getBackgroundColor(Integer.MIN_VALUE));
        this.input.render(p_282470_, p_282674_, p_282014_, p_283132_);
        super.render(p_282470_, p_282674_, p_282014_, p_283132_);
        p_282470_.pose().pushPose();
        p_282470_.pose().translate(0.0F, 0.0F, 200.0F);
        this.commandSuggestions.render(p_282470_, p_282674_, p_282014_);
        p_282470_.pose().popPose();
        GuiMessageTag guimessagetag = this.minecraft.gui.getChat().getMessageTagAt(p_282674_, p_282014_);
        if (guimessagetag != null && guimessagetag.text() != null) {
            p_282470_.renderTooltip(this.font, this.font.split(guimessagetag.text(), 210), p_282674_, p_282014_);
        } else {
            Style style = this.getComponentStyleAt(p_282674_, p_282014_);
            if (style != null && style.getHoverEvent() != null) {
                p_282470_.renderComponentHoverEffect(this.font, style, p_282674_, p_282014_);
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics p_298203_, int p_299897_, int p_297752_, float p_300216_) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void setChatLine(String pChatLine) {
        this.input.setValue(pChatLine);
    }

    @Override
    protected void updateNarrationState(NarrationElementOutput p_169238_) {
        p_169238_.add(NarratedElementType.TITLE, this.getTitle());
        p_169238_.add(NarratedElementType.USAGE, USAGE_TEXT);
        String s = this.input.getValue();
        if (!s.isEmpty()) {
            p_169238_.nest().add(NarratedElementType.TITLE, Component.translatable("chat_screen.message", s));
        }
    }

    @Nullable
    private Style getComponentStyleAt(double pMouseX, double pMouseY) {
        return this.minecraft.gui.getChat().getClickedComponentStyleAt(pMouseX, pMouseY);
    }

    public void handleChatInput(String pMessage, boolean pAddToRecentChat) {
        pMessage = this.normalizeChatMessage(pMessage);
        if (!pMessage.isEmpty()) {
            if (pAddToRecentChat) {
                this.minecraft.gui.getChat().addRecentChat(pMessage);
            }

            if (pMessage.startsWith("/")) {
                this.minecraft.player.connection.sendCommand(pMessage.substring(1));
            } else {
                this.minecraft.player.connection.sendChat(pMessage);
            }
        }
    }

    public String normalizeChatMessage(String pMessage) {
        return StringUtil.trimChatMessage(StringUtils.normalizeSpace(pMessage.trim()));
    }
}

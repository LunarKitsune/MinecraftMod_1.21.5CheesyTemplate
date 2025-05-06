package net.minecraft.client.gui.spectator;

import com.mojang.authlib.GameProfile;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundTeleportToEntityPacket;
import net.minecraft.util.ARGB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PlayerMenuItem implements SpectatorMenuItem {
    private final GameProfile profile;
    private final Supplier<PlayerSkin> skin;
    private final Component name;

    public PlayerMenuItem(GameProfile pProfile) {
        this.profile = pProfile;
        this.skin = Minecraft.getInstance().getSkinManager().lookupInsecure(pProfile);
        this.name = Component.literal(pProfile.getName());
    }

    @Override
    public void selectItem(SpectatorMenu pMenu) {
        Minecraft.getInstance().getConnection().send(new ServerboundTeleportToEntityPacket(this.profile.getId()));
    }

    @Override
    public Component getName() {
        return this.name;
    }

    @Override
    public void renderIcon(GuiGraphics p_282282_, float p_282686_, float p_368587_) {
        PlayerFaceRenderer.draw(p_282282_, this.skin.get(), 2, 2, 12, ARGB.white(p_368587_));
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
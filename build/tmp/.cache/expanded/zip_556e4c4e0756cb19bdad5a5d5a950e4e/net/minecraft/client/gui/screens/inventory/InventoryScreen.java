package net.minecraft.client.gui.screens.inventory;

import com.mojang.blaze3d.platform.Lighting;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.screens.recipebook.CraftingRecipeBookComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class InventoryScreen extends AbstractRecipeBookScreen<InventoryMenu> {
    private float xMouse;
    private float yMouse;
    private boolean buttonClicked;
    private final EffectsInInventory effects;

    public InventoryScreen(Player pPlayer) {
        super(pPlayer.inventoryMenu, new CraftingRecipeBookComponent(pPlayer.inventoryMenu), pPlayer.getInventory(), Component.translatable("container.crafting"));
        this.titleLabelX = 97;
        this.effects = new EffectsInInventory(this);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (this.minecraft.player.hasInfiniteMaterials()) {
            this.minecraft
                .setScreen(
                    new CreativeModeInventoryScreen(
                        this.minecraft.player, this.minecraft.player.connection.enabledFeatures(), this.minecraft.options.operatorItemsTab().get()
                    )
                );
        }
    }

    @Override
    protected void init() {
        if (this.minecraft.player.hasInfiniteMaterials()) {
            this.minecraft
                .setScreen(
                    new CreativeModeInventoryScreen(
                        this.minecraft.player, this.minecraft.player.connection.enabledFeatures(), this.minecraft.options.operatorItemsTab().get()
                    )
                );
        } else {
            super.init();
        }
    }

    @Override
    protected ScreenPosition getRecipeBookButtonPosition() {
        return new ScreenPosition(this.leftPos + 104, this.height / 2 - 22);
    }

    @Override
    protected void onRecipeBookButtonClick() {
        this.buttonClicked = true;
    }

    @Override
    protected void renderLabels(GuiGraphics p_281654_, int p_283517_, int p_283464_) {
        p_281654_.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
    }

    @Override
    public void render(GuiGraphics p_283246_, int p_98876_, int p_98877_, float p_98878_) {
        super.render(p_283246_, p_98876_, p_98877_, p_98878_);
        this.effects.render(p_283246_, p_98876_, p_98877_, p_98878_);
        this.xMouse = p_98876_;
        this.yMouse = p_98877_;
    }

    @Override
    public boolean showsActiveEffects() {
        return this.effects.canSeeEffects();
    }

    @Override
    protected boolean isBiggerResultSlot() {
        return false;
    }

    @Override
    protected void renderBg(GuiGraphics p_281500_, float p_281299_, int p_283481_, int p_281831_) {
        int i = this.leftPos;
        int j = this.topPos;
        p_281500_.blit(RenderType::guiTextured, INVENTORY_LOCATION, i, j, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        renderEntityInInventoryFollowsMouse(p_281500_, i + 26, j + 8, i + 75, j + 78, 30, 0.0625F, this.xMouse, this.yMouse, this.minecraft.player);
    }

    public static void renderEntityInInventoryFollowsMouse(
        GuiGraphics pGuiGraphics,
        int pX1,
        int pY1,
        int pX2,
        int pY2,
        int pScale,
        float pYOffset,
        float pMouseX,
        float pMouseY,
        LivingEntity pEntity
    ) {
        float f = (pX1 + pX2) / 2.0F;
        float f1 = (pY1 + pY2) / 2.0F;
        pGuiGraphics.enableScissor(pX1, pY1, pX2, pY2);
        float f2 = (float)Math.atan((f - pMouseX) / 40.0F);
        float f3 = (float)Math.atan((f1 - pMouseY) / 40.0F);
        Quaternionf quaternionf = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf quaternionf1 = new Quaternionf().rotateX(f3 * 20.0F * (float) (Math.PI / 180.0));
        quaternionf.mul(quaternionf1);
        float f4 = pEntity.yBodyRot;
        float f5 = pEntity.getYRot();
        float f6 = pEntity.getXRot();
        float f7 = pEntity.yHeadRotO;
        float f8 = pEntity.yHeadRot;
        pEntity.yBodyRot = 180.0F + f2 * 20.0F;
        pEntity.setYRot(180.0F + f2 * 40.0F);
        pEntity.setXRot(-f3 * 20.0F);
        pEntity.yHeadRot = pEntity.getYRot();
        pEntity.yHeadRotO = pEntity.getYRot();
        float f9 = pEntity.getScale();
        Vector3f vector3f = new Vector3f(0.0F, pEntity.getBbHeight() / 2.0F + pYOffset * f9, 0.0F);
        float f10 = pScale / f9;
        renderEntityInInventory(pGuiGraphics, f, f1, f10, vector3f, quaternionf, quaternionf1, pEntity);
        pEntity.yBodyRot = f4;
        pEntity.setYRot(f5);
        pEntity.setXRot(f6);
        pEntity.yHeadRotO = f7;
        pEntity.yHeadRot = f8;
        pGuiGraphics.disableScissor();
    }

    public static void renderEntityInInventory(
        GuiGraphics pGuiGraphics,
        float pX,
        float pY,
        float pScale,
        Vector3f pTranslate,
        Quaternionf pPose,
        @Nullable Quaternionf pCameraOrientation,
        LivingEntity pEntity
    ) {
        pGuiGraphics.pose().pushPose();
        pGuiGraphics.pose().translate(pX, pY, 50.0);
        pGuiGraphics.pose().scale(pScale, pScale, -pScale);
        pGuiGraphics.pose().translate(pTranslate.x, pTranslate.y, pTranslate.z);
        pGuiGraphics.pose().mulPose(pPose);
        pGuiGraphics.flush();
        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        if (pCameraOrientation != null) {
            entityrenderdispatcher.overrideCameraOrientation(pCameraOrientation.conjugate(new Quaternionf()).rotateY((float) Math.PI));
        }

        entityrenderdispatcher.setRenderShadow(false);
        pGuiGraphics.drawSpecial(p_357680_ -> entityrenderdispatcher.render(pEntity, 0.0, 0.0, 0.0, 1.0F, pGuiGraphics.pose(), p_357680_, 15728880));
        pGuiGraphics.flush();
        entityrenderdispatcher.setRenderShadow(true);
        pGuiGraphics.pose().popPose();
        Lighting.setupFor3DItems();
    }

    @Override
    public boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
        if (this.buttonClicked) {
            this.buttonClicked = false;
            return true;
        } else {
            return super.mouseReleased(pMouseX, pMouseY, pButton);
        }
    }
}
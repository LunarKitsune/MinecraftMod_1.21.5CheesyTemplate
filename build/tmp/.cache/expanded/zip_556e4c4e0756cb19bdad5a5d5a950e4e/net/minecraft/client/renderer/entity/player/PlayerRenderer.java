package net.minecraft.client.renderer.entity.player;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import javax.annotation.Nullable;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.ArrowLayer;
import net.minecraft.client.renderer.entity.layers.BeeStingerLayer;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.Deadmau5EarsLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ParrotOnShoulderLayer;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.SpinAttackEffectLayer;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PlayerRenderer extends LivingEntityRenderer<AbstractClientPlayer, PlayerRenderState, PlayerModel> {
    public PlayerRenderer(EntityRendererProvider.Context pContext, boolean pUseSlimModel) {
        super(pContext, new PlayerModel(pContext.bakeLayer(pUseSlimModel ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), pUseSlimModel), 0.5F);
        this.addLayer(
            new HumanoidArmorLayer<>(
                this,
                new HumanoidArmorModel(pContext.bakeLayer(pUseSlimModel ? ModelLayers.PLAYER_SLIM_INNER_ARMOR : ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidArmorModel(pContext.bakeLayer(pUseSlimModel ? ModelLayers.PLAYER_SLIM_OUTER_ARMOR : ModelLayers.PLAYER_OUTER_ARMOR)),
                pContext.getEquipmentRenderer()
            )
        );
        this.addLayer(new PlayerItemInHandLayer<>(this));
        this.addLayer(new ArrowLayer<>(this, pContext));
        this.addLayer(new Deadmau5EarsLayer(this, pContext.getModelSet()));
        this.addLayer(new CapeLayer(this, pContext.getModelSet(), pContext.getEquipmentAssets()));
        this.addLayer(new CustomHeadLayer<>(this, pContext.getModelSet()));
        this.addLayer(new WingsLayer<>(this, pContext.getModelSet(), pContext.getEquipmentRenderer()));
        this.addLayer(new ParrotOnShoulderLayer(this, pContext.getModelSet()));
        this.addLayer(new SpinAttackEffectLayer(this, pContext.getModelSet()));
        this.addLayer(new BeeStingerLayer<>(this, pContext));
    }

    protected boolean shouldRenderLayers(PlayerRenderState p_362318_) {
        return !p_362318_.isSpectator;
    }

    public Vec3 getRenderOffset(PlayerRenderState p_365223_) {
        Vec3 vec3 = super.getRenderOffset(p_365223_);
        return p_365223_.isCrouching ? vec3.add(0.0, p_365223_.scale * -2.0F / 16.0, 0.0) : vec3;
    }

    private static HumanoidModel.ArmPose getArmPose(AbstractClientPlayer pPlayer, HumanoidArm pArm) {
        ItemStack itemstack = pPlayer.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack itemstack1 = pPlayer.getItemInHand(InteractionHand.OFF_HAND);
        HumanoidModel.ArmPose humanoidmodel$armpose = getArmPose(pPlayer, itemstack, InteractionHand.MAIN_HAND);
        HumanoidModel.ArmPose humanoidmodel$armpose1 = getArmPose(pPlayer, itemstack1, InteractionHand.OFF_HAND);
        if (humanoidmodel$armpose.isTwoHanded()) {
            humanoidmodel$armpose1 = itemstack1.isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
        }

        return pPlayer.getMainArm() == pArm ? humanoidmodel$armpose : humanoidmodel$armpose1;
    }

    private static HumanoidModel.ArmPose getArmPose(Player pPlayer, ItemStack pStack, InteractionHand pHand) {
        if (pStack.isEmpty()) {
            return HumanoidModel.ArmPose.EMPTY;
        } else if (!pPlayer.swinging && pStack.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(pStack)) {
            return HumanoidModel.ArmPose.CROSSBOW_HOLD;
        } else {
            if (pPlayer.getUsedItemHand() == pHand && pPlayer.getUseItemRemainingTicks() > 0) {
                ItemUseAnimation itemuseanimation = pStack.getUseAnimation();
                if (itemuseanimation == ItemUseAnimation.BLOCK) {
                    return HumanoidModel.ArmPose.BLOCK;
                }

                if (itemuseanimation == ItemUseAnimation.BOW) {
                    return HumanoidModel.ArmPose.BOW_AND_ARROW;
                }

                if (itemuseanimation == ItemUseAnimation.SPEAR) {
                    return HumanoidModel.ArmPose.THROW_SPEAR;
                }

                if (itemuseanimation == ItemUseAnimation.CROSSBOW) {
                    return HumanoidModel.ArmPose.CROSSBOW_CHARGE;
                }

                if (itemuseanimation == ItemUseAnimation.SPYGLASS) {
                    return HumanoidModel.ArmPose.SPYGLASS;
                }

                if (itemuseanimation == ItemUseAnimation.TOOT_HORN) {
                    return HumanoidModel.ArmPose.TOOT_HORN;
                }

                if (itemuseanimation == ItemUseAnimation.BRUSH) {
                    return HumanoidModel.ArmPose.BRUSH;
                }
            }

            var pose = net.minecraftforge.client.extensions.common.IClientItemExtensions.of(pStack).getArmPose(pPlayer, pHand, pStack);
            if (pose != null)
                return pose;

            return HumanoidModel.ArmPose.ITEM;
        }
    }

    public ResourceLocation getTextureLocation(PlayerRenderState p_364988_) {
        return p_364988_.skin.texture();
    }

    protected void scale(PlayerRenderState p_368476_, PoseStack p_117799_) {
        float f = 0.9375F;
        p_117799_.scale(0.9375F, 0.9375F, 0.9375F);
    }

    protected void renderNameTag(PlayerRenderState p_360888_, Component p_117809_, PoseStack p_117810_, MultiBufferSource p_117811_, int p_117812_) {
        p_117810_.pushPose();
        if (p_360888_.scoreText != null) {
            super.renderNameTag(p_360888_, p_360888_.scoreText, p_117810_, p_117811_, p_117812_);
            p_117810_.translate(0.0F, 9.0F * 1.15F * 0.025F, 0.0F);
        }

        super.renderNameTag(p_360888_, p_117809_, p_117810_, p_117811_, p_117812_);
        p_117810_.popPose();
    }

    public PlayerRenderState createRenderState() {
        return new PlayerRenderState();
    }

    public void extractRenderState(AbstractClientPlayer p_366577_, PlayerRenderState p_364437_, float p_365590_) {
        super.extractRenderState(p_366577_, p_364437_, p_365590_);
        HumanoidMobRenderer.extractHumanoidRenderState(p_366577_, p_364437_, p_365590_, this.itemModelResolver);
        p_364437_.leftArmPose = getArmPose(p_366577_, HumanoidArm.LEFT);
        p_364437_.rightArmPose = getArmPose(p_366577_, HumanoidArm.RIGHT);
        p_364437_.skin = p_366577_.getSkin();
        p_364437_.arrowCount = p_366577_.getArrowCount();
        p_364437_.stingerCount = p_366577_.getStingerCount();
        p_364437_.useItemRemainingTicks = p_366577_.getUseItemRemainingTicks();
        p_364437_.swinging = p_366577_.swinging;
        p_364437_.isSpectator = p_366577_.isSpectator();
        p_364437_.showHat = p_366577_.isModelPartShown(PlayerModelPart.HAT);
        p_364437_.showJacket = p_366577_.isModelPartShown(PlayerModelPart.JACKET);
        p_364437_.showLeftPants = p_366577_.isModelPartShown(PlayerModelPart.LEFT_PANTS_LEG);
        p_364437_.showRightPants = p_366577_.isModelPartShown(PlayerModelPart.RIGHT_PANTS_LEG);
        p_364437_.showLeftSleeve = p_366577_.isModelPartShown(PlayerModelPart.LEFT_SLEEVE);
        p_364437_.showRightSleeve = p_366577_.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE);
        p_364437_.showCape = p_366577_.isModelPartShown(PlayerModelPart.CAPE);
        extractFlightData(p_366577_, p_364437_, p_365590_);
        extractCapeState(p_366577_, p_364437_, p_365590_);
        if (p_364437_.distanceToCameraSq < 100.0) {
            Scoreboard scoreboard = p_366577_.getScoreboard();
            Objective objective = scoreboard.getDisplayObjective(DisplaySlot.BELOW_NAME);
            if (objective != null) {
                ReadOnlyScoreInfo readonlyscoreinfo = scoreboard.getPlayerScoreInfo(p_366577_, objective);
                Component component = ReadOnlyScoreInfo.safeFormatValue(readonlyscoreinfo, objective.numberFormatOrDefault(StyledFormat.NO_STYLE));
                p_364437_.scoreText = Component.empty().append(component).append(CommonComponents.SPACE).append(objective.getDisplayName());
            } else {
                p_364437_.scoreText = null;
            }
        } else {
            p_364437_.scoreText = null;
        }

        p_364437_.parrotOnLeftShoulder = getParrotOnShoulder(p_366577_, true);
        p_364437_.parrotOnRightShoulder = getParrotOnShoulder(p_366577_, false);
        p_364437_.id = p_366577_.getId();
        p_364437_.name = p_366577_.getGameProfile().getName();
        p_364437_.heldOnHead.clear();
        if (p_364437_.isUsingItem) {
            ItemStack itemstack = p_366577_.getItemInHand(p_364437_.useItemHand);
            if (itemstack.is(Items.SPYGLASS)) {
                this.itemModelResolver.updateForLiving(p_364437_.heldOnHead, itemstack, ItemDisplayContext.HEAD, p_366577_);
            }
        }
    }

    private static void extractFlightData(AbstractClientPlayer pPlayer, PlayerRenderState pRenderState, float pPartialTick) {
        pRenderState.fallFlyingTimeInTicks = pPlayer.getFallFlyingTicks() + pPartialTick;
        Vec3 vec3 = pPlayer.getViewVector(pPartialTick);
        Vec3 vec31 = pPlayer.getDeltaMovementLerped(pPartialTick);
        if (vec31.horizontalDistanceSqr() > 1.0E-5F && vec3.horizontalDistanceSqr() > 1.0E-5F) {
            pRenderState.shouldApplyFlyingYRot = true;
            double d0 = vec31.horizontal().normalize().dot(vec3.horizontal().normalize());
            double d1 = vec31.x * vec3.z - vec31.z * vec3.x;
            pRenderState.flyingYRot = (float)(Math.signum(d1) * Math.acos(Math.min(1.0, Math.abs(d0))));
        } else {
            pRenderState.shouldApplyFlyingYRot = false;
            pRenderState.flyingYRot = 0.0F;
        }
    }

    private static void extractCapeState(AbstractClientPlayer pPlayer, PlayerRenderState pRenderState, float pPartialTick) {
        double d0 = Mth.lerp(pPartialTick, pPlayer.xCloakO, pPlayer.xCloak) - Mth.lerp(pPartialTick, pPlayer.xo, pPlayer.getX());
        double d1 = Mth.lerp(pPartialTick, pPlayer.yCloakO, pPlayer.yCloak) - Mth.lerp(pPartialTick, pPlayer.yo, pPlayer.getY());
        double d2 = Mth.lerp(pPartialTick, pPlayer.zCloakO, pPlayer.zCloak) - Mth.lerp(pPartialTick, pPlayer.zo, pPlayer.getZ());
        float f = Mth.rotLerp(pPartialTick, pPlayer.yBodyRotO, pPlayer.yBodyRot);
        double d3 = Mth.sin(f * (float) (Math.PI / 180.0));
        double d4 = -Mth.cos(f * (float) (Math.PI / 180.0));
        pRenderState.capeFlap = (float)d1 * 10.0F;
        pRenderState.capeFlap = Mth.clamp(pRenderState.capeFlap, -6.0F, 32.0F);
        pRenderState.capeLean = (float)(d0 * d3 + d2 * d4) * 100.0F;
        pRenderState.capeLean = pRenderState.capeLean * (1.0F - pRenderState.fallFlyingScale());
        pRenderState.capeLean = Mth.clamp(pRenderState.capeLean, 0.0F, 150.0F);
        pRenderState.capeLean2 = (float)(d0 * d4 - d2 * d3) * 100.0F;
        pRenderState.capeLean2 = Mth.clamp(pRenderState.capeLean2, -20.0F, 20.0F);
        float f1 = Mth.lerp(pPartialTick, pPlayer.oBob, pPlayer.bob);
        float f2 = Mth.lerp(pPartialTick, pPlayer.walkDistO, pPlayer.walkDist);
        pRenderState.capeFlap = pRenderState.capeFlap + Mth.sin(f2 * 6.0F) * 32.0F * f1;
    }

    @Nullable
    private static Parrot.Variant getParrotOnShoulder(AbstractClientPlayer pPlayer, boolean pLeftShoulder) {
        CompoundTag compoundtag = pLeftShoulder ? pPlayer.getShoulderEntityLeft() : pPlayer.getShoulderEntityRight();
        if (compoundtag.isEmpty()) {
            return null;
        } else {
            EntityType<?> entitytype = compoundtag.read("id", EntityType.CODEC).orElse(null);
            return entitytype == EntityType.PARROT ? compoundtag.read("Variant", Parrot.Variant.LEGACY_CODEC).orElse(Parrot.Variant.RED_BLUE) : null;
        }
    }

    public void renderRightHand(PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, ResourceLocation pSkinTexture, boolean pIsSleeveVisible) {
        if (!net.minecraftforge.client.event.ForgeEventFactoryClient.onRenderArm(pPoseStack, pBufferSource, pPackedLight, HumanoidArm.RIGHT))
        this.renderHand(pPoseStack, pBufferSource, pPackedLight, pSkinTexture, this.model.rightArm, pIsSleeveVisible);
    }

    public void renderLeftHand(PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, ResourceLocation pSkinTexture, boolean pIsSleeveVisible) {
        if (!net.minecraftforge.client.event.ForgeEventFactoryClient.onRenderArm(pPoseStack, pBufferSource, pPackedLight, HumanoidArm.LEFT))
        this.renderHand(pPoseStack, pBufferSource, pPackedLight, pSkinTexture, this.model.leftArm, pIsSleeveVisible);
    }

    private void renderHand(PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, ResourceLocation pSkinTexture, ModelPart pArm, boolean pIsSleeveVisible) {
        PlayerModel playermodel = this.getModel();
        pArm.resetPose();
        pArm.visible = true;
        playermodel.leftSleeve.visible = pIsSleeveVisible;
        playermodel.rightSleeve.visible = pIsSleeveVisible;
        playermodel.leftArm.zRot = -0.1F;
        playermodel.rightArm.zRot = 0.1F;
        pArm.render(pPoseStack, pBufferSource.getBuffer(RenderType.entityTranslucent(pSkinTexture)), pPackedLight, OverlayTexture.NO_OVERLAY);
    }

    protected void setupRotations(PlayerRenderState p_369667_, PoseStack p_117803_, float p_117804_, float p_117805_) {
        float f = p_369667_.swimAmount;
        float f1 = p_369667_.xRot;
        if (p_369667_.isFallFlying) {
            super.setupRotations(p_369667_, p_117803_, p_117804_, p_117805_);
            float f2 = p_369667_.fallFlyingScale();
            if (!p_369667_.isAutoSpinAttack) {
                p_117803_.mulPose(Axis.XP.rotationDegrees(f2 * (-90.0F - f1)));
            }

            if (p_369667_.shouldApplyFlyingYRot) {
                p_117803_.mulPose(Axis.YP.rotation(p_369667_.flyingYRot));
            }
        } else if (f > 0.0F) {
            super.setupRotations(p_369667_, p_117803_, p_117804_, p_117805_);
            float f4 = p_369667_.isInWater ? -90.0F - f1 : -90.0F;
            float f3 = Mth.lerp(f, 0.0F, f4);
            p_117803_.mulPose(Axis.XP.rotationDegrees(f3));
            if (p_369667_.isVisuallySwimming) {
                p_117803_.translate(0.0F, -1.0F, 0.3F);
            }
        } else {
            super.setupRotations(p_369667_, p_117803_, p_117804_, p_117805_);
        }
    }

    @Override
    public void render(PlayerRenderState state, PoseStack pose, MultiBufferSource source, int p_115313_) {
        if (net.minecraftforge.client.event.ForgeEventFactoryClient.onRenderPlayerPre(state, this, pose, source, p_115313_)) return;
        super.render(state, pose, source, p_115313_);
        net.minecraftforge.client.event.ForgeEventFactoryClient.onRenderPlayerPost(state, this, pose, source, p_115313_);
    }
}

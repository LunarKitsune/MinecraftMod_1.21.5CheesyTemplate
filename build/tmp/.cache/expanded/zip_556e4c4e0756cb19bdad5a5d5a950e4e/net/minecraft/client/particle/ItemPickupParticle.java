package net.minecraft.client.particle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemPickupParticle extends Particle {
    private static final int LIFE_TIME = 3;
    private final Entity itemEntity;
    private final Entity target;
    private int life;
    private final EntityRenderDispatcher entityRenderDispatcher;
    private double targetX;
    private double targetY;
    private double targetZ;
    private double targetXOld;
    private double targetYOld;
    private double targetZOld;

    public ItemPickupParticle(EntityRenderDispatcher pEntityRenderDispatcher, ClientLevel pLevel, Entity pItemEntity, Entity pTarget) {
        this(pEntityRenderDispatcher, pLevel, pItemEntity, pTarget, pItemEntity.getDeltaMovement());
    }

    private ItemPickupParticle(EntityRenderDispatcher pEntityRenderDispatcher, ClientLevel pLevel, Entity pItemEntity, Entity pTarget, Vec3 pSpeed) {
        super(pLevel, pItemEntity.getX(), pItemEntity.getY(), pItemEntity.getZ(), pSpeed.x, pSpeed.y, pSpeed.z);
        this.itemEntity = this.getSafeCopy(pItemEntity);
        this.target = pTarget;
        this.entityRenderDispatcher = pEntityRenderDispatcher;
        this.updatePosition();
        this.saveOldPosition();
    }

    private Entity getSafeCopy(Entity pEntity) {
        return (Entity)(!(pEntity instanceof ItemEntity) ? pEntity : ((ItemEntity)pEntity).copy());
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.CUSTOM;
    }

    @Override
    public void renderCustom(PoseStack p_375620_, MultiBufferSource p_377939_, Camera p_376327_, float p_377781_) {
        float f = (this.life + p_377781_) / 3.0F;
        f *= f;
        double d0 = Mth.lerp(p_377781_, this.targetXOld, this.targetX);
        double d1 = Mth.lerp(p_377781_, this.targetYOld, this.targetY);
        double d2 = Mth.lerp(p_377781_, this.targetZOld, this.targetZ);
        double d3 = Mth.lerp(f, this.itemEntity.getX(), d0);
        double d4 = Mth.lerp(f, this.itemEntity.getY(), d1);
        double d5 = Mth.lerp(f, this.itemEntity.getZ(), d2);
        Vec3 vec3 = p_376327_.getPosition();
        this.entityRenderDispatcher
            .render(
                this.itemEntity,
                d3 - vec3.x(),
                d4 - vec3.y(),
                d5 - vec3.z(),
                p_377781_,
                new PoseStack(),
                p_377939_,
                this.entityRenderDispatcher.getPackedLightCoords(this.itemEntity, p_377781_)
            );
    }

    @Override
    public void render(VertexConsumer pBuffer, Camera pRenderInfo, float pPartialTicks) {
    }

    @Override
    public void tick() {
        this.life++;
        if (this.life == 3) {
            this.remove();
        }

        this.saveOldPosition();
        this.updatePosition();
    }

    private void updatePosition() {
        this.targetX = this.target.getX();
        this.targetY = (this.target.getY() + this.target.getEyeY()) / 2.0;
        this.targetZ = this.target.getZ();
    }

    private void saveOldPosition() {
        this.targetXOld = this.targetX;
        this.targetYOld = this.targetY;
        this.targetZOld = this.targetZ;
    }
}
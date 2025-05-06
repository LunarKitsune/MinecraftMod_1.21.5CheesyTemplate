package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.TrailParticleOption;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TrailParticle extends TextureSheetParticle {
    private final Vec3 target;

    TrailParticle(
        ClientLevel pLevel,
        double pX,
        double pY,
        double pZ,
        double pXSpeed,
        double pYSpeed,
        double pZSpeed,
        Vec3 pTarget,
        int pColor
    ) {
        super(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
        pColor = ARGB.scaleRGB(
            pColor, 0.875F + this.random.nextFloat() * 0.25F, 0.875F + this.random.nextFloat() * 0.25F, 0.875F + this.random.nextFloat() * 0.25F
        );
        this.rCol = ARGB.red(pColor) / 255.0F;
        this.gCol = ARGB.green(pColor) / 255.0F;
        this.bCol = ARGB.blue(pColor) / 255.0F;
        this.quadSize = 0.26F;
        this.target = pTarget;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            int i = this.lifetime - this.age;
            double d0 = 1.0 / i;
            this.x = Mth.lerp(d0, this.x, this.target.x());
            this.y = Mth.lerp(d0, this.y, this.target.y());
            this.z = Mth.lerp(d0, this.z, this.target.z());
        }
    }

    @Override
    public int getLightColor(float p_360980_) {
        return 15728880;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<TrailParticleOption> {
        private final SpriteSet sprite;

        public Provider(SpriteSet pSprite) {
            this.sprite = pSprite;
        }

        public Particle createParticle(
            TrailParticleOption p_376768_,
            ClientLevel p_369121_,
            double p_360788_,
            double p_367642_,
            double p_369587_,
            double p_368409_,
            double p_365137_,
            double p_367012_
        ) {
            TrailParticle trailparticle = new TrailParticle(
                p_369121_, p_360788_, p_367642_, p_369587_, p_368409_, p_365137_, p_367012_, p_376768_.target(), p_376768_.color()
            );
            trailparticle.pickSprite(this.sprite);
            trailparticle.setLifetime(p_376768_.duration());
            return trailparticle;
        }
    }
}
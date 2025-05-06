package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SuspendedTownParticle extends TextureSheetParticle {
    SuspendedTownParticle(ClientLevel p_108104_, double p_108105_, double p_108106_, double p_108107_, double p_108108_, double p_108109_, double p_108110_) {
        super(p_108104_, p_108105_, p_108106_, p_108107_, p_108108_, p_108109_, p_108110_);
        float f = this.random.nextFloat() * 0.1F + 0.2F;
        this.rCol = f;
        this.gCol = f;
        this.bCol = f;
        this.setSize(0.02F, 0.02F);
        this.quadSize = this.quadSize * (this.random.nextFloat() * 0.6F + 0.5F);
        this.xd *= 0.02F;
        this.yd *= 0.02F;
        this.zd *= 0.02F;
        this.lifetime = (int)(20.0 / (Math.random() * 0.8 + 0.2));
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    @Override
    public void move(double pX, double pY, double pZ) {
        this.setBoundingBox(this.getBoundingBox().move(pX, pY, pZ));
        this.setLocationFromBoundingbox();
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.lifetime-- <= 0) {
            this.remove();
        } else {
            this.move(this.xd, this.yd, this.zd);
            this.xd *= 0.99;
            this.yd *= 0.99;
            this.zd *= 0.99;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class ComposterFillProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public ComposterFillProvider(SpriteSet pSprites) {
            this.sprite = pSprites;
        }

        public Particle createParticle(
            SimpleParticleType pType,
            ClientLevel pLevel,
            double pX,
            double pY,
            double pZ,
            double pXSpeed,
            double pYSpeed,
            double pZSpeed
        ) {
            SuspendedTownParticle suspendedtownparticle = new SuspendedTownParticle(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
            suspendedtownparticle.pickSprite(this.sprite);
            suspendedtownparticle.setColor(1.0F, 1.0F, 1.0F);
            suspendedtownparticle.setLifetime(3 + pLevel.getRandom().nextInt(5));
            return suspendedtownparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class DolphinSpeedProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public DolphinSpeedProvider(SpriteSet pSprites) {
            this.sprite = pSprites;
        }

        public Particle createParticle(
            SimpleParticleType pType,
            ClientLevel pLevel,
            double pX,
            double pY,
            double pZ,
            double pXSpeed,
            double pYSpeed,
            double pZSpeed
        ) {
            SuspendedTownParticle suspendedtownparticle = new SuspendedTownParticle(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
            suspendedtownparticle.setColor(0.3F, 0.5F, 1.0F);
            suspendedtownparticle.pickSprite(this.sprite);
            suspendedtownparticle.setAlpha(1.0F - pLevel.random.nextFloat() * 0.7F);
            suspendedtownparticle.setLifetime(suspendedtownparticle.getLifetime() / 2);
            return suspendedtownparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class EggCrackProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public EggCrackProvider(SpriteSet pSprite) {
            this.sprite = pSprite;
        }

        public Particle createParticle(
            SimpleParticleType p_277584_,
            ClientLevel p_277587_,
            double p_277722_,
            double p_277508_,
            double p_277797_,
            double p_277537_,
            double p_277578_,
            double p_277397_
        ) {
            SuspendedTownParticle suspendedtownparticle = new SuspendedTownParticle(p_277587_, p_277722_, p_277508_, p_277797_, p_277537_, p_277578_, p_277397_);
            suspendedtownparticle.pickSprite(this.sprite);
            suspendedtownparticle.setColor(1.0F, 1.0F, 1.0F);
            return suspendedtownparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class HappyVillagerProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public HappyVillagerProvider(SpriteSet pSprites) {
            this.sprite = pSprites;
        }

        public Particle createParticle(
            SimpleParticleType pType,
            ClientLevel pLevel,
            double pX,
            double pY,
            double pZ,
            double pXSpeed,
            double pYSpeed,
            double pZSpeed
        ) {
            SuspendedTownParticle suspendedtownparticle = new SuspendedTownParticle(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
            suspendedtownparticle.pickSprite(this.sprite);
            suspendedtownparticle.setColor(1.0F, 1.0F, 1.0F);
            return suspendedtownparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public Provider(SpriteSet pSprites) {
            this.sprite = pSprites;
        }

        public Particle createParticle(
            SimpleParticleType pType,
            ClientLevel pLevel,
            double pX,
            double pY,
            double pZ,
            double pXSpeed,
            double pYSpeed,
            double pZSpeed
        ) {
            SuspendedTownParticle suspendedtownparticle = new SuspendedTownParticle(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
            suspendedtownparticle.pickSprite(this.sprite);
            return suspendedtownparticle;
        }
    }
}
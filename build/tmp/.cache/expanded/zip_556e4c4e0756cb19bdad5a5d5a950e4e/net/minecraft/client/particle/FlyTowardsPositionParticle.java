package net.minecraft.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FlyTowardsPositionParticle extends TextureSheetParticle {
    private final double xStart;
    private final double yStart;
    private final double zStart;
    private final boolean isGlowing;
    private final Particle.LifetimeAlpha lifetimeAlpha;

    FlyTowardsPositionParticle(
        ClientLevel p_333327_, double p_328158_, double p_336092_, double p_331009_, double p_335556_, double p_328514_, double p_331083_
    ) {
        this(p_333327_, p_328158_, p_336092_, p_331009_, p_335556_, p_328514_, p_331083_, false, Particle.LifetimeAlpha.ALWAYS_OPAQUE);
    }

    FlyTowardsPositionParticle(
        ClientLevel pLevel,
        double pX,
        double pY,
        double pZ,
        double pXSpeed,
        double pYSpeed,
        double pZSpeed,
        boolean pIsGlowing,
        Particle.LifetimeAlpha pLifetimeAlpha
    ) {
        super(pLevel, pX, pY, pZ);
        this.isGlowing = pIsGlowing;
        this.lifetimeAlpha = pLifetimeAlpha;
        this.setAlpha(pLifetimeAlpha.startAlpha());
        this.xd = pXSpeed;
        this.yd = pYSpeed;
        this.zd = pZSpeed;
        this.xStart = pX;
        this.yStart = pY;
        this.zStart = pZ;
        this.xo = pX + pXSpeed;
        this.yo = pY + pYSpeed;
        this.zo = pZ + pZSpeed;
        this.x = this.xo;
        this.y = this.yo;
        this.z = this.zo;
        this.quadSize = 0.1F * (this.random.nextFloat() * 0.5F + 0.2F);
        float f = this.random.nextFloat() * 0.6F + 0.4F;
        this.rCol = 0.9F * f;
        this.gCol = 0.9F * f;
        this.bCol = f;
        this.hasPhysics = false;
        this.lifetime = (int)(Math.random() * 10.0) + 30;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return this.lifetimeAlpha.isOpaque() ? ParticleRenderType.PARTICLE_SHEET_OPAQUE : ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void move(double p_335599_, double p_330355_, double p_329221_) {
        this.setBoundingBox(this.getBoundingBox().move(p_335599_, p_330355_, p_329221_));
        this.setLocationFromBoundingbox();
    }

    @Override
    public int getLightColor(float p_334485_) {
        if (this.isGlowing) {
            return 240;
        } else {
            int i = super.getLightColor(p_334485_);
            float f = (float)this.age / this.lifetime;
            f *= f;
            f *= f;
            int j = i & 0xFF;
            int k = i >> 16 & 0xFF;
            k += (int)(f * 15.0F * 16.0F);
            if (k > 240) {
                k = 240;
            }

            return j | k << 16;
        }
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            float f = (float)this.age / this.lifetime;
            f = 1.0F - f;
            float f1 = 1.0F - f;
            f1 *= f1;
            f1 *= f1;
            this.x = this.xStart + this.xd * f;
            this.y = this.yStart + this.yd * f - f1 * 1.2F;
            this.z = this.zStart + this.zd * f;
            this.setPos(this.x, this.y, this.z); // FORGE: update the particle's bounding box
        }
    }

    @Override
    public void render(VertexConsumer p_329880_, Camera p_328408_, float p_328709_) {
        this.setAlpha(this.lifetimeAlpha.currentAlphaForAge(this.age, this.lifetime, p_328709_));
        super.render(p_329880_, p_328408_, p_328709_);
    }

    @OnlyIn(Dist.CLIENT)
    public static class EnchantProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public EnchantProvider(SpriteSet pSprite) {
            this.sprite = pSprite;
        }

        public Particle createParticle(
            SimpleParticleType p_330246_,
            ClientLevel p_334642_,
            double p_331946_,
            double p_331936_,
            double p_330331_,
            double p_330075_,
            double p_332423_,
            double p_336053_
        ) {
            FlyTowardsPositionParticle flytowardspositionparticle = new FlyTowardsPositionParticle(
                p_334642_, p_331946_, p_331936_, p_330331_, p_330075_, p_332423_, p_336053_
            );
            flytowardspositionparticle.pickSprite(this.sprite);
            return flytowardspositionparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class NautilusProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public NautilusProvider(SpriteSet pSprite) {
            this.sprite = pSprite;
        }

        public Particle createParticle(
            SimpleParticleType p_327773_,
            ClientLevel p_332234_,
            double p_328567_,
            double p_328371_,
            double p_328714_,
            double p_333049_,
            double p_332373_,
            double p_331353_
        ) {
            FlyTowardsPositionParticle flytowardspositionparticle = new FlyTowardsPositionParticle(
                p_332234_, p_328567_, p_328371_, p_328714_, p_333049_, p_332373_, p_331353_
            );
            flytowardspositionparticle.pickSprite(this.sprite);
            return flytowardspositionparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class VaultConnectionProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public VaultConnectionProvider(SpriteSet pSprite) {
            this.sprite = pSprite;
        }

        public Particle createParticle(
            SimpleParticleType p_328352_,
            ClientLevel p_333387_,
            double p_328138_,
            double p_329009_,
            double p_334265_,
            double p_336214_,
            double p_330704_,
            double p_328353_
        ) {
            FlyTowardsPositionParticle flytowardspositionparticle = new FlyTowardsPositionParticle(
                p_333387_, p_328138_, p_329009_, p_334265_, p_336214_, p_330704_, p_328353_, true, new Particle.LifetimeAlpha(0.0F, 0.6F, 0.25F, 1.0F)
            );
            flytowardspositionparticle.scale(1.5F);
            flytowardspositionparticle.pickSprite(this.sprite);
            return flytowardspositionparticle;
        }
    }
}

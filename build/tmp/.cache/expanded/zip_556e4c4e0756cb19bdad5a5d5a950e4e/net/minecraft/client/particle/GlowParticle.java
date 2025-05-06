package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GlowParticle extends TextureSheetParticle {
    static final RandomSource RANDOM = RandomSource.create();
    private final SpriteSet sprites;

    GlowParticle(
        ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed, SpriteSet pSprites
    ) {
        super(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
        this.friction = 0.96F;
        this.speedUpWhenYMotionIsBlocked = true;
        this.sprites = pSprites;
        this.quadSize *= 0.75F;
        this.hasPhysics = false;
        this.setSpriteFromAge(pSprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getLightColor(float p_172146_) {
        float f = (this.age + p_172146_) / this.lifetime;
        f = Mth.clamp(f, 0.0F, 1.0F);
        int i = super.getLightColor(p_172146_);
        int j = i & 0xFF;
        int k = i >> 16 & 0xFF;
        j += (int)(f * 15.0F * 16.0F);
        if (j > 240) {
            j = 240;
        }

        return j | k << 16;
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.sprites);
    }

    @OnlyIn(Dist.CLIENT)
    public static class ElectricSparkProvider implements ParticleProvider<SimpleParticleType> {
        private final double SPEED_FACTOR = 0.25;
        private final SpriteSet sprite;

        public ElectricSparkProvider(SpriteSet pSprites) {
            this.sprite = pSprites;
        }

        public Particle createParticle(
            SimpleParticleType p_172162_,
            ClientLevel p_172163_,
            double p_172164_,
            double p_172165_,
            double p_172166_,
            double p_172167_,
            double p_172168_,
            double p_172169_
        ) {
            GlowParticle glowparticle = new GlowParticle(p_172163_, p_172164_, p_172165_, p_172166_, 0.0, 0.0, 0.0, this.sprite);
            glowparticle.setColor(1.0F, 0.9F, 1.0F);
            glowparticle.setParticleSpeed(p_172167_ * 0.25, p_172168_ * 0.25, p_172169_ * 0.25);
            int i = 2;
            int j = 4;
            glowparticle.setLifetime(p_172163_.random.nextInt(2) + 2);
            return glowparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class GlowSquidProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public GlowSquidProvider(SpriteSet pSprites) {
            this.sprite = pSprites;
        }

        public Particle createParticle(
            SimpleParticleType p_172183_,
            ClientLevel p_172184_,
            double p_172185_,
            double p_172186_,
            double p_172187_,
            double p_172188_,
            double p_172189_,
            double p_172190_
        ) {
            GlowParticle glowparticle = new GlowParticle(
                p_172184_,
                p_172185_,
                p_172186_,
                p_172187_,
                0.5 - GlowParticle.RANDOM.nextDouble(),
                p_172189_,
                0.5 - GlowParticle.RANDOM.nextDouble(),
                this.sprite
            );
            if (p_172184_.random.nextBoolean()) {
                glowparticle.setColor(0.6F, 1.0F, 0.8F);
            } else {
                glowparticle.setColor(0.08F, 0.4F, 0.4F);
            }

            glowparticle.yd *= 0.2F;
            if (p_172188_ == 0.0 && p_172190_ == 0.0) {
                glowparticle.xd *= 0.1F;
                glowparticle.zd *= 0.1F;
            }

            glowparticle.setLifetime((int)(8.0 / (p_172184_.random.nextDouble() * 0.8 + 0.2)));
            return glowparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class ScrapeProvider implements ParticleProvider<SimpleParticleType> {
        private final double SPEED_FACTOR = 0.01;
        private final SpriteSet sprite;

        public ScrapeProvider(SpriteSet pSprites) {
            this.sprite = pSprites;
        }

        public Particle createParticle(
            SimpleParticleType p_172205_,
            ClientLevel p_172206_,
            double p_172207_,
            double p_172208_,
            double p_172209_,
            double p_172210_,
            double p_172211_,
            double p_172212_
        ) {
            GlowParticle glowparticle = new GlowParticle(p_172206_, p_172207_, p_172208_, p_172209_, 0.0, 0.0, 0.0, this.sprite);
            if (p_172206_.random.nextBoolean()) {
                glowparticle.setColor(0.29F, 0.58F, 0.51F);
            } else {
                glowparticle.setColor(0.43F, 0.77F, 0.62F);
            }

            glowparticle.setParticleSpeed(p_172210_ * 0.01, p_172211_ * 0.01, p_172212_ * 0.01);
            int i = 10;
            int j = 40;
            glowparticle.setLifetime(p_172206_.random.nextInt(30) + 10);
            return glowparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class WaxOffProvider implements ParticleProvider<SimpleParticleType> {
        private final double SPEED_FACTOR = 0.01;
        private final SpriteSet sprite;

        public WaxOffProvider(SpriteSet pSprites) {
            this.sprite = pSprites;
        }

        public Particle createParticle(
            SimpleParticleType p_172227_,
            ClientLevel p_172228_,
            double p_172229_,
            double p_172230_,
            double p_172231_,
            double p_172232_,
            double p_172233_,
            double p_172234_
        ) {
            GlowParticle glowparticle = new GlowParticle(p_172228_, p_172229_, p_172230_, p_172231_, 0.0, 0.0, 0.0, this.sprite);
            glowparticle.setColor(1.0F, 0.9F, 1.0F);
            glowparticle.setParticleSpeed(p_172232_ * 0.01 / 2.0, p_172233_ * 0.01, p_172234_ * 0.01 / 2.0);
            int i = 10;
            int j = 40;
            glowparticle.setLifetime(p_172228_.random.nextInt(30) + 10);
            return glowparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class WaxOnProvider implements ParticleProvider<SimpleParticleType> {
        private final double SPEED_FACTOR = 0.01;
        private final SpriteSet sprite;

        public WaxOnProvider(SpriteSet pSprites) {
            this.sprite = pSprites;
        }

        public Particle createParticle(
            SimpleParticleType p_172249_,
            ClientLevel p_172250_,
            double p_172251_,
            double p_172252_,
            double p_172253_,
            double p_172254_,
            double p_172255_,
            double p_172256_
        ) {
            GlowParticle glowparticle = new GlowParticle(p_172250_, p_172251_, p_172252_, p_172253_, 0.0, 0.0, 0.0, this.sprite);
            glowparticle.setColor(0.91F, 0.55F, 0.08F);
            glowparticle.setParticleSpeed(p_172254_ * 0.01 / 2.0, p_172255_ * 0.01, p_172256_ * 0.01 / 2.0);
            int i = 10;
            int j = 40;
            glowparticle.setLifetime(p_172250_.random.nextInt(30) + 10);
            return glowparticle;
        }
    }
}
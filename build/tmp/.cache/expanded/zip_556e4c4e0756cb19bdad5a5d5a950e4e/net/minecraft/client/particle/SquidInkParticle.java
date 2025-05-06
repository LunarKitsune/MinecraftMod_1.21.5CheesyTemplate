package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.ARGB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SquidInkParticle extends SimpleAnimatedParticle {
    SquidInkParticle(
        ClientLevel pLevel,
        double pX,
        double pY,
        double pZ,
        double pXSpeed,
        double pYSpeed,
        double pZSpeed,
        int pPackedColor,
        SpriteSet pSprites
    ) {
        super(pLevel, pX, pY, pZ, pSprites, 0.0F);
        this.friction = 0.92F;
        this.quadSize = 0.5F;
        this.setAlpha(1.0F);
        this.setColor(ARGB.red(pPackedColor), ARGB.green(pPackedColor), ARGB.blue(pPackedColor));
        this.lifetime = (int)(this.quadSize * 12.0F / (Math.random() * 0.8F + 0.2F));
        this.setSpriteFromAge(pSprites);
        this.hasPhysics = false;
        this.xd = pXSpeed;
        this.yd = pYSpeed;
        this.zd = pZSpeed;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.removed) {
            this.setSpriteFromAge(this.sprites);
            if (this.age > this.lifetime / 2) {
                this.setAlpha(1.0F - ((float)this.age - this.lifetime / 2) / this.lifetime);
            }

            if (this.level.getBlockState(BlockPos.containing(this.x, this.y, this.z)).isAir()) {
                this.yd -= 0.0074F;
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class GlowInkProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public GlowInkProvider(SpriteSet pSprites) {
            this.sprites = pSprites;
        }

        public Particle createParticle(
            SimpleParticleType p_172347_,
            ClientLevel p_172348_,
            double p_172349_,
            double p_172350_,
            double p_172351_,
            double p_172352_,
            double p_172353_,
            double p_172354_
        ) {
            return new SquidInkParticle(
                p_172348_, p_172349_, p_172350_, p_172351_, p_172352_, p_172353_, p_172354_, ARGB.color(255, 204, 31, 102), this.sprites
            );
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet pSprites) {
            this.sprites = pSprites;
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
            return new SquidInkParticle(
                pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed, ARGB.color(255, 255, 255, 255), this.sprites
            );
        }
    }
}
package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GustSeedParticle extends NoRenderParticle {
    private final double scale;
    private final int tickDelayInBetween;

    GustSeedParticle(ClientLevel pLevel, double pX, double pY, double pZ, double pScale, int pLifetime, int pTickDelayInBetween) {
        super(pLevel, pX, pY, pZ, 0.0, 0.0, 0.0);
        this.scale = pScale;
        this.lifetime = pLifetime;
        this.tickDelayInBetween = pTickDelayInBetween;
    }

    @Override
    public void tick() {
        if (this.age % (this.tickDelayInBetween + 1) == 0) {
            for (int i = 0; i < 3; i++) {
                double d0 = this.x + (this.random.nextDouble() - this.random.nextDouble()) * this.scale;
                double d1 = this.y + (this.random.nextDouble() - this.random.nextDouble()) * this.scale;
                double d2 = this.z + (this.random.nextDouble() - this.random.nextDouble()) * this.scale;
                this.level.addParticle(ParticleTypes.GUST, d0, d1, d2, (float)this.age / this.lifetime, 0.0, 0.0);
            }
        }

        if (this.age++ == this.lifetime) {
            this.remove();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final double scale;
        private final int lifetime;
        private final int tickDelayInBetween;

        public Provider(double pScale, int pLifetime, int pTickDelayInBetween) {
            this.scale = pScale;
            this.lifetime = pLifetime;
            this.tickDelayInBetween = pTickDelayInBetween;
        }

        public Particle createParticle(
            SimpleParticleType p_309959_,
            ClientLevel p_312995_,
            double p_310097_,
            double p_313201_,
            double p_310511_,
            double p_310468_,
            double p_310282_,
            double p_311555_
        ) {
            return new GustSeedParticle(p_312995_, p_310097_, p_313201_, p_310511_, this.scale, this.lifetime, this.tickDelayInBetween);
        }
    }
}
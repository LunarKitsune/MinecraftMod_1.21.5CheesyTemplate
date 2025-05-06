package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class DustParticle extends DustParticleBase<DustParticleOptions> {
    protected DustParticle(
        ClientLevel pLevel,
        double pX,
        double pY,
        double pZ,
        double pXSpeed,
        double pYSpeed,
        double pZSpeed,
        DustParticleOptions pOptions,
        SpriteSet pSprites
    ) {
        super(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed, pOptions, pSprites);
        float f = this.random.nextFloat() * 0.4F + 0.6F;
        Vector3f vector3f = pOptions.getColor();
        this.rCol = this.randomizeColor(vector3f.x(), f);
        this.gCol = this.randomizeColor(vector3f.y(), f);
        this.bCol = this.randomizeColor(vector3f.z(), f);
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<DustParticleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet pSprites) {
            this.sprites = pSprites;
        }

        public Particle createParticle(
            DustParticleOptions pType,
            ClientLevel pLevel,
            double pX,
            double pY,
            double pZ,
            double pXSpeed,
            double pYSpeed,
            double pZSpeed
        ) {
            return new DustParticle(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed, pType, this.sprites);
        }
    }
}
package net.minecraft.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class DustColorTransitionParticle extends DustParticleBase<DustColorTransitionOptions> {
    private final Vector3f fromColor;
    private final Vector3f toColor;

    protected DustColorTransitionParticle(
        ClientLevel pLevel,
        double pX,
        double pY,
        double pZ,
        double pXSpeed,
        double pYSpeed,
        double pZSpeed,
        DustColorTransitionOptions pOptions,
        SpriteSet pSprites
    ) {
        super(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed, pOptions, pSprites);
        float f = this.random.nextFloat() * 0.4F + 0.6F;
        this.fromColor = this.randomizeColor(pOptions.getFromColor(), f);
        this.toColor = this.randomizeColor(pOptions.getToColor(), f);
    }

    private Vector3f randomizeColor(Vector3f pVector, float pMultiplier) {
        return new Vector3f(this.randomizeColor(pVector.x(), pMultiplier), this.randomizeColor(pVector.y(), pMultiplier), this.randomizeColor(pVector.z(), pMultiplier));
    }

    private void lerpColors(float pPartialTick) {
        float f = (this.age + pPartialTick) / (this.lifetime + 1.0F);
        Vector3f vector3f = new Vector3f(this.fromColor).lerp(this.toColor, f);
        this.rCol = vector3f.x();
        this.gCol = vector3f.y();
        this.bCol = vector3f.z();
    }

    @Override
    public void render(VertexConsumer p_172063_, Camera p_172064_, float p_172065_) {
        this.lerpColors(p_172065_);
        super.render(p_172063_, p_172064_, p_172065_);
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<DustColorTransitionOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet pSprites) {
            this.sprites = pSprites;
        }

        public Particle createParticle(
            DustColorTransitionOptions p_172075_,
            ClientLevel p_172076_,
            double p_172077_,
            double p_172078_,
            double p_172079_,
            double p_172080_,
            double p_172081_,
            double p_172082_
        ) {
            return new DustColorTransitionParticle(p_172076_, p_172077_, p_172078_, p_172079_, p_172080_, p_172081_, p_172082_, p_172075_, this.sprites);
        }
    }
}
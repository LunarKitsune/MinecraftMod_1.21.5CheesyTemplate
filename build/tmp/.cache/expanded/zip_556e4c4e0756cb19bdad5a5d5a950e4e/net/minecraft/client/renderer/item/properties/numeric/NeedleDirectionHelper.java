package net.minecraft.client.renderer.item.properties.numeric;

import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class NeedleDirectionHelper {
    private final boolean wobble;

    protected NeedleDirectionHelper(boolean pWobble) {
        this.wobble = pWobble;
    }

    public float get(ItemStack pStack, @Nullable ClientLevel pLevel, @Nullable LivingEntity pEntity, int pSeed) {
        Entity entity = (Entity)(pEntity != null ? pEntity : pStack.getEntityRepresentation());
        if (entity == null) {
            return 0.0F;
        } else {
            if (pLevel == null && entity.level() instanceof ClientLevel clientlevel) {
                pLevel = clientlevel;
            }

            return pLevel == null ? 0.0F : this.calculate(pStack, pLevel, pSeed, entity);
        }
    }

    protected abstract float calculate(ItemStack pStack, ClientLevel pLevel, int pSeed, Entity pEntity);

    protected boolean wobble() {
        return this.wobble;
    }

    protected NeedleDirectionHelper.Wobbler newWobbler(float pScale) {
        return this.wobble ? standardWobbler(pScale) : nonWobbler();
    }

    public static NeedleDirectionHelper.Wobbler standardWobbler(final float pScale) {
        return new NeedleDirectionHelper.Wobbler() {
            private float rotation;
            private float deltaRotation;
            private long lastUpdateTick;

            @Override
            public float rotation() {
                return this.rotation;
            }

            @Override
            public boolean shouldUpdate(long p_378605_) {
                return this.lastUpdateTick != p_378605_;
            }

            @Override
            public void update(long p_378620_, float p_377269_) {
                this.lastUpdateTick = p_378620_;
                float f = Mth.positiveModulo(p_377269_ - this.rotation + 0.5F, 1.0F) - 0.5F;
                this.deltaRotation += f * 0.1F;
                this.deltaRotation = this.deltaRotation * pScale;
                this.rotation = Mth.positiveModulo(this.rotation + this.deltaRotation, 1.0F);
            }
        };
    }

    public static NeedleDirectionHelper.Wobbler nonWobbler() {
        return new NeedleDirectionHelper.Wobbler() {
            private float targetValue;

            @Override
            public float rotation() {
                return this.targetValue;
            }

            @Override
            public boolean shouldUpdate(long p_377058_) {
                return true;
            }

            @Override
            public void update(long p_377475_, float p_376108_) {
                this.targetValue = p_376108_;
            }
        };
    }

    @OnlyIn(Dist.CLIENT)
    public interface Wobbler {
        float rotation();

        boolean shouldUpdate(long pGameTime);

        void update(long pGameTime, float pTargetValue);
    }
}
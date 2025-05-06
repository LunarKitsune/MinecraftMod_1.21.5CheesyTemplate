package net.minecraft.world.effect;

import com.google.common.collect.ComparisonChain;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;

public class MobEffectInstance implements Comparable<MobEffectInstance> {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int INFINITE_DURATION = -1;
    public static final int MIN_AMPLIFIER = 0;
    public static final int MAX_AMPLIFIER = 255;
    public static final Codec<MobEffectInstance> CODEC = RecordCodecBuilder.create(
        p_341259_ -> p_341259_.group(
                MobEffect.CODEC.fieldOf("id").forGetter(MobEffectInstance::getEffect),
                MobEffectInstance.Details.MAP_CODEC.forGetter(MobEffectInstance::asDetails)
            )
            .apply(p_341259_, MobEffectInstance::new)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, MobEffectInstance> STREAM_CODEC = StreamCodec.composite(
        MobEffect.STREAM_CODEC, MobEffectInstance::getEffect, MobEffectInstance.Details.STREAM_CODEC, MobEffectInstance::asDetails, MobEffectInstance::new
    );
    private final Holder<MobEffect> effect;
    private int duration;
    private int amplifier;
    private boolean ambient;
    private boolean visible;
    private boolean showIcon;
    @Nullable
    private MobEffectInstance hiddenEffect;
    private final MobEffectInstance.BlendState blendState = new MobEffectInstance.BlendState();

    public MobEffectInstance(Holder<MobEffect> pEffect) {
        this(pEffect, 0, 0);
    }

    public MobEffectInstance(Holder<MobEffect> pEffect, int pDuration) {
        this(pEffect, pDuration, 0);
    }

    public MobEffectInstance(Holder<MobEffect> pEffect, int pDuration, int pAmplifier) {
        this(pEffect, pDuration, pAmplifier, false, true);
    }

    public MobEffectInstance(Holder<MobEffect> pEffect, int pDuration, int pAmplifier, boolean pAmbient, boolean pVisible) {
        this(pEffect, pDuration, pAmplifier, pAmbient, pVisible, pVisible);
    }

    public MobEffectInstance(Holder<MobEffect> pEffect, int pDuration, int pAmplifier, boolean pAmbient, boolean pVisible, boolean pShowIcon) {
        this(pEffect, pDuration, pAmplifier, pAmbient, pVisible, pShowIcon, null);
    }

    public MobEffectInstance(
        Holder<MobEffect> pEffect, int pDuration, int pAmplifier, boolean pAmbient, boolean pVisible, boolean pShowIcon, @Nullable MobEffectInstance pHiddenEffect
    ) {
        this.effect = pEffect;
        this.duration = pDuration;
        this.amplifier = Mth.clamp(pAmplifier, 0, 255);
        this.ambient = pAmbient;
        this.visible = pVisible;
        this.showIcon = pShowIcon;
        this.hiddenEffect = pHiddenEffect;
    }

    public MobEffectInstance(MobEffectInstance pOther) {
        this.effect = pOther.effect;
        this.setDetailsFrom(pOther);
    }

    private MobEffectInstance(Holder<MobEffect> pEffect, MobEffectInstance.Details pDetails) {
        this(
            pEffect,
            pDetails.duration(),
            pDetails.amplifier(),
            pDetails.ambient(),
            pDetails.showParticles(),
            pDetails.showIcon(),
            pDetails.hiddenEffect().map(p_326756_ -> new MobEffectInstance(pEffect, p_326756_)).orElse(null)
        );
    }

    private MobEffectInstance.Details asDetails() {
        return new MobEffectInstance.Details(
            this.getAmplifier(),
            this.getDuration(),
            this.isAmbient(),
            this.isVisible(),
            this.showIcon(),
            Optional.ofNullable(this.hiddenEffect).map(MobEffectInstance::asDetails)
        );
    }

    public float getBlendFactor(LivingEntity pEntity, float pDelta) {
        return this.blendState.getFactor(pEntity, pDelta);
    }

    public ParticleOptions getParticleOptions() {
        return this.effect.value().createParticleOptions(this);
    }

    void setDetailsFrom(MobEffectInstance pEffectInstance) {
        this.duration = pEffectInstance.duration;
        this.amplifier = pEffectInstance.amplifier;
        this.ambient = pEffectInstance.ambient;
        this.visible = pEffectInstance.visible;
        this.showIcon = pEffectInstance.showIcon;
    }

    public boolean update(MobEffectInstance pOther) {
        if (!this.effect.equals(pOther.effect)) {
            LOGGER.warn("This method should only be called for matching effects!");
        }

        boolean flag = false;
        if (pOther.amplifier > this.amplifier) {
            if (pOther.isShorterDurationThan(this)) {
                MobEffectInstance mobeffectinstance = this.hiddenEffect;
                this.hiddenEffect = new MobEffectInstance(this);
                this.hiddenEffect.hiddenEffect = mobeffectinstance;
            }

            this.amplifier = pOther.amplifier;
            this.duration = pOther.duration;
            flag = true;
        } else if (this.isShorterDurationThan(pOther)) {
            if (pOther.amplifier == this.amplifier) {
                this.duration = pOther.duration;
                flag = true;
            } else if (this.hiddenEffect == null) {
                this.hiddenEffect = new MobEffectInstance(pOther);
            } else {
                this.hiddenEffect.update(pOther);
            }
        }

        if (!pOther.ambient && this.ambient || flag) {
            this.ambient = pOther.ambient;
            flag = true;
        }

        if (pOther.visible != this.visible) {
            this.visible = pOther.visible;
            flag = true;
        }

        if (pOther.showIcon != this.showIcon) {
            this.showIcon = pOther.showIcon;
            flag = true;
        }

        return flag;
    }

    private boolean isShorterDurationThan(MobEffectInstance pOther) {
        return !this.isInfiniteDuration() && (this.duration < pOther.duration || pOther.isInfiniteDuration());
    }

    public boolean isInfiniteDuration() {
        return this.duration == -1;
    }

    public boolean endsWithin(int pDuration) {
        return !this.isInfiniteDuration() && this.duration <= pDuration;
    }

    public MobEffectInstance withScaledDuration(float pScale) {
        MobEffectInstance mobeffectinstance = new MobEffectInstance(this);
        mobeffectinstance.duration = mobeffectinstance.mapDuration(p_390476_ -> Math.max(Mth.floor(p_390476_ * pScale), 1));
        return mobeffectinstance;
    }

    public int mapDuration(Int2IntFunction pMapper) {
        return !this.isInfiniteDuration() && this.duration != 0 ? pMapper.applyAsInt(this.duration) : this.duration;
    }

    public Holder<MobEffect> getEffect() {
        return this.effect;
    }

    public int getDuration() {
        return this.duration;
    }

    public int getAmplifier() {
        return this.amplifier;
    }

    public boolean isAmbient() {
        return this.ambient;
    }

    public boolean isVisible() {
        return this.visible;
    }

    public boolean showIcon() {
        return this.showIcon;
    }

    public boolean tickServer(ServerLevel pLevel, LivingEntity pEntity, Runnable pOnEffectUpdated) {
        if (!this.hasRemainingDuration()) {
            return false;
        } else {
            int i = this.isInfiniteDuration() ? pEntity.tickCount : this.duration;
            if (this.effect.value().shouldApplyEffectTickThisTick(i, this.amplifier) && !this.effect.value().applyEffectTick(pLevel, pEntity, this.amplifier)) {
                return false;
            } else {
                this.tickDownDuration();
                if (this.downgradeToHiddenEffect()) {
                    pOnEffectUpdated.run();
                }

                return this.hasRemainingDuration();
            }
        }
    }

    public void tickClient() {
        if (this.hasRemainingDuration()) {
            this.tickDownDuration();
            this.downgradeToHiddenEffect();
        }

        this.blendState.tick(this);
    }

    private boolean hasRemainingDuration() {
        return this.isInfiniteDuration() || this.duration > 0;
    }

    private void tickDownDuration() {
        if (this.hiddenEffect != null) {
            this.hiddenEffect.tickDownDuration();
        }

        this.duration = this.mapDuration(p_267916_ -> p_267916_ - 1);
    }

    private boolean downgradeToHiddenEffect() {
        if (this.duration == 0 && this.hiddenEffect != null) {
            this.setDetailsFrom(this.hiddenEffect);
            this.hiddenEffect = this.hiddenEffect.hiddenEffect;
            return true;
        } else {
            return false;
        }
    }

    public void onEffectStarted(LivingEntity pEntity) {
        this.effect.value().onEffectStarted(pEntity, this.amplifier);
    }

    public void onMobRemoved(ServerLevel pLevel, LivingEntity pEntity, Entity.RemovalReason pReason) {
        this.effect.value().onMobRemoved(pLevel, pEntity, this.amplifier, pReason);
    }

    public void onMobHurt(ServerLevel pLevel, LivingEntity pEntity, DamageSource pDamageSource, float pAmount) {
        this.effect.value().onMobHurt(pLevel, pEntity, this.amplifier, pDamageSource, pAmount);
    }

    public String getDescriptionId() {
        return this.effect.value().getDescriptionId();
    }

    @Override
    public String toString() {
        String s;
        if (this.amplifier > 0) {
            s = this.getDescriptionId() + " x " + (this.amplifier + 1) + ", Duration: " + this.describeDuration();
        } else {
            s = this.getDescriptionId() + ", Duration: " + this.describeDuration();
        }

        if (!this.visible) {
            s = s + ", Particles: false";
        }

        if (!this.showIcon) {
            s = s + ", Show Icon: false";
        }

        return s;
    }

    private String describeDuration() {
        return this.isInfiniteDuration() ? "infinite" : Integer.toString(this.duration);
    }

    @Override
    public boolean equals(Object pOther) {
        if (this == pOther) {
            return true;
        } else {
            return !(pOther instanceof MobEffectInstance mobeffectinstance)
                ? false
                : this.duration == mobeffectinstance.duration
                    && this.amplifier == mobeffectinstance.amplifier
                    && this.ambient == mobeffectinstance.ambient
                    && this.visible == mobeffectinstance.visible
                    && this.showIcon == mobeffectinstance.showIcon
                    && this.effect.equals(mobeffectinstance.effect);
        }
    }

    @Override
    public int hashCode() {
        int i = this.effect.hashCode();
        i = 31 * i + this.duration;
        i = 31 * i + this.amplifier;
        i = 31 * i + (this.ambient ? 1 : 0);
        i = 31 * i + (this.visible ? 1 : 0);
        return 31 * i + (this.showIcon ? 1 : 0);
    }

    public int compareTo(MobEffectInstance pOther) {
        int i = 32147;
        return (this.getDuration() <= 32147 || pOther.getDuration() <= 32147) && (!this.isAmbient() || !pOther.isAmbient())
            ? ComparisonChain.start()
                .compareFalseFirst(this.isAmbient(), pOther.isAmbient())
                .compareFalseFirst(this.isInfiniteDuration(), pOther.isInfiniteDuration())
                .compare(this.getDuration(), pOther.getDuration())
                .compare(this.getEffect().value().getColor(), pOther.getEffect().value().getColor())
                .result()
            : ComparisonChain.start()
                .compare(this.isAmbient(), pOther.isAmbient())
                .compare(this.getEffect().value().getColor(), pOther.getEffect().value().getColor())
                .result();
    }

    public void onEffectAdded(LivingEntity pLivingEntity) {
        this.effect.value().onEffectAdded(pLivingEntity, this.amplifier);
    }

    public boolean is(Holder<MobEffect> pEffect) {
        return this.effect.equals(pEffect);
    }

    public void copyBlendState(MobEffectInstance pEffectInstance) {
        this.blendState.copyFrom(pEffectInstance.blendState);
    }

    public void skipBlending() {
        this.blendState.setImmediate(this);
    }

    static class BlendState {
        private float factor;
        private float factorPreviousFrame;

        public void setImmediate(MobEffectInstance pEffectInstance) {
            this.factor = hasEffect(pEffectInstance) ? 1.0F : 0.0F;
            this.factorPreviousFrame = this.factor;
        }

        public void copyFrom(MobEffectInstance.BlendState pBlendState) {
            this.factor = pBlendState.factor;
            this.factorPreviousFrame = pBlendState.factorPreviousFrame;
        }

        public void tick(MobEffectInstance pEffect) {
            this.factorPreviousFrame = this.factor;
            boolean flag = hasEffect(pEffect);
            float f = flag ? 1.0F : 0.0F;
            if (this.factor != f) {
                MobEffect mobeffect = pEffect.getEffect().value();
                int i = flag ? mobeffect.getBlendInDurationTicks() : mobeffect.getBlendOutDurationTicks();
                if (i == 0) {
                    this.factor = f;
                } else {
                    float f1 = 1.0F / i;
                    this.factor = this.factor + Mth.clamp(f - this.factor, -f1, f1);
                }
            }
        }

        private static boolean hasEffect(MobEffectInstance pEffect) {
            return !pEffect.endsWithin(pEffect.getEffect().value().getBlendOutAdvanceTicks());
        }

        public float getFactor(LivingEntity pEntity, float pDelta) {
            if (pEntity.isRemoved()) {
                this.factorPreviousFrame = this.factor;
            }

            return Mth.lerp(pDelta, this.factorPreviousFrame, this.factor);
        }
    }

    record Details(int amplifier, int duration, boolean ambient, boolean showParticles, boolean showIcon, Optional<MobEffectInstance.Details> hiddenEffect) {
        public static final MapCodec<MobEffectInstance.Details> MAP_CODEC = MapCodec.recursive(
            "MobEffectInstance.Details",
            p_332855_ -> RecordCodecBuilder.mapCodec(
                p_327980_ -> p_327980_.group(
                        ExtraCodecs.UNSIGNED_BYTE.optionalFieldOf("amplifier", 0).forGetter(MobEffectInstance.Details::amplifier),
                        Codec.INT.optionalFieldOf("duration", 0).forGetter(MobEffectInstance.Details::duration),
                        Codec.BOOL.optionalFieldOf("ambient", false).forGetter(MobEffectInstance.Details::ambient),
                        Codec.BOOL.optionalFieldOf("show_particles", true).forGetter(MobEffectInstance.Details::showParticles),
                        Codec.BOOL.optionalFieldOf("show_icon").forGetter(p_330483_ -> Optional.of(p_330483_.showIcon())),
                        p_332855_.optionalFieldOf("hidden_effect").forGetter(MobEffectInstance.Details::hiddenEffect)
                    )
                    .apply(p_327980_, MobEffectInstance.Details::create)
            )
        );
        public static final StreamCodec<ByteBuf, MobEffectInstance.Details> STREAM_CODEC = StreamCodec.recursive(
            p_333279_ -> StreamCodec.composite(
                ByteBufCodecs.VAR_INT,
                MobEffectInstance.Details::amplifier,
                ByteBufCodecs.VAR_INT,
                MobEffectInstance.Details::duration,
                ByteBufCodecs.BOOL,
                MobEffectInstance.Details::ambient,
                ByteBufCodecs.BOOL,
                MobEffectInstance.Details::showParticles,
                ByteBufCodecs.BOOL,
                MobEffectInstance.Details::showIcon,
                p_333279_.apply(ByteBufCodecs::optional),
                MobEffectInstance.Details::hiddenEffect,
                MobEffectInstance.Details::new
            )
        );

        private static MobEffectInstance.Details create(
            int pAmplifier, int pDuration, boolean pAmbient, boolean pShowParticles, Optional<Boolean> pShowIcon, Optional<MobEffectInstance.Details> pHiddenEffect
        ) {
            return new MobEffectInstance.Details(pAmplifier, pDuration, pAmbient, pShowParticles, pShowIcon.orElse(pShowParticles), pHiddenEffect);
        }
    }
}
package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.item.consume_effects.PlaySoundConsumeEffect;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

public record Consumable(float consumeSeconds, ItemUseAnimation animation, Holder<SoundEvent> sound, boolean hasConsumeParticles, List<ConsumeEffect> onConsumeEffects) {
    public static final float DEFAULT_CONSUME_SECONDS = 1.6F;
    private static final int CONSUME_EFFECTS_INTERVAL = 4;
    private static final float CONSUME_EFFECTS_START_FRACTION = 0.21875F;
    public static final Codec<Consumable> CODEC = RecordCodecBuilder.create(
        p_367547_ -> p_367547_.group(
                ExtraCodecs.NON_NEGATIVE_FLOAT.optionalFieldOf("consume_seconds", 1.6F).forGetter(Consumable::consumeSeconds),
                ItemUseAnimation.CODEC.optionalFieldOf("animation", ItemUseAnimation.EAT).forGetter(Consumable::animation),
                SoundEvent.CODEC.optionalFieldOf("sound", SoundEvents.GENERIC_EAT).forGetter(Consumable::sound),
                Codec.BOOL.optionalFieldOf("has_consume_particles", true).forGetter(Consumable::hasConsumeParticles),
                ConsumeEffect.CODEC.listOf().optionalFieldOf("on_consume_effects", List.of()).forGetter(Consumable::onConsumeEffects)
            )
            .apply(p_367547_, Consumable::new)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, Consumable> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.FLOAT,
        Consumable::consumeSeconds,
        ItemUseAnimation.STREAM_CODEC,
        Consumable::animation,
        SoundEvent.STREAM_CODEC,
        Consumable::sound,
        ByteBufCodecs.BOOL,
        Consumable::hasConsumeParticles,
        ConsumeEffect.STREAM_CODEC.apply(ByteBufCodecs.list()),
        Consumable::onConsumeEffects,
        Consumable::new
    );

    public InteractionResult startConsuming(LivingEntity pEntity, ItemStack pStack, InteractionHand pHand) {
        if (!this.canConsume(pEntity, pStack)) {
            return InteractionResult.FAIL;
        } else {
            boolean flag = this.consumeTicks() > 0;
            if (flag) {
                pEntity.startUsingItem(pHand);
                return InteractionResult.CONSUME;
            } else {
                ItemStack itemstack = this.onConsume(pEntity.level(), pEntity, pStack);
                return InteractionResult.CONSUME.heldItemTransformedTo(itemstack);
            }
        }
    }

    public ItemStack onConsume(Level pLevel, LivingEntity pEntity, ItemStack pStack) {
        RandomSource randomsource = pEntity.getRandom();
        this.emitParticlesAndSounds(randomsource, pEntity, pStack, 16);
        if (pEntity instanceof ServerPlayer serverplayer) {
            serverplayer.awardStat(Stats.ITEM_USED.get(pStack.getItem()));
            CriteriaTriggers.CONSUME_ITEM.trigger(serverplayer, pStack);
        }

        pStack.getAllOfType(ConsumableListener.class).forEach(p_363704_ -> p_363704_.onConsume(pLevel, pEntity, pStack, this));
        if (!pLevel.isClientSide) {
            this.onConsumeEffects.forEach(p_360884_ -> p_360884_.apply(pLevel, pStack, pEntity));
        }

        pEntity.gameEvent(this.animation == ItemUseAnimation.DRINK ? GameEvent.DRINK : GameEvent.EAT);
        pStack.consume(1, pEntity);
        return pStack;
    }

    public boolean canConsume(LivingEntity pEntity, ItemStack pStack) {
        FoodProperties foodproperties = pStack.get(DataComponents.FOOD);
        return foodproperties != null && pEntity instanceof Player player ? player.canEat(foodproperties.canAlwaysEat()) : true;
    }

    public int consumeTicks() {
        return (int)(this.consumeSeconds * 20.0F);
    }

    public void emitParticlesAndSounds(RandomSource pRandom, LivingEntity pEntity, ItemStack pStack, int pAmount) {
        float f = pRandom.nextBoolean() ? 0.5F : 1.0F;
        float f1 = pRandom.triangle(1.0F, 0.2F);
        float f2 = 0.5F;
        float f3 = Mth.randomBetween(pRandom, 0.9F, 1.0F);
        float f4 = this.animation == ItemUseAnimation.DRINK ? 0.5F : f;
        float f5 = this.animation == ItemUseAnimation.DRINK ? f3 : f1;
        if (this.hasConsumeParticles) {
            pEntity.spawnItemParticles(pStack, pAmount);
        }

        SoundEvent soundevent = pEntity instanceof Consumable.OverrideConsumeSound consumable$overrideconsumesound
            ? consumable$overrideconsumesound.getConsumeSound(pStack)
            : this.sound.value();
        pEntity.playSound(soundevent, f4, f5);
    }

    public boolean shouldEmitParticlesAndSounds(int pRemainingUseDuration) {
        int i = this.consumeTicks() - pRemainingUseDuration;
        int j = (int)(this.consumeTicks() * 0.21875F);
        boolean flag = i > j;
        return flag && pRemainingUseDuration % 4 == 0;
    }

    public static Consumable.Builder builder() {
        return new Consumable.Builder();
    }

    public static class Builder {
        private float consumeSeconds = 1.6F;
        private ItemUseAnimation animation = ItemUseAnimation.EAT;
        private Holder<SoundEvent> sound = SoundEvents.GENERIC_EAT;
        private boolean hasConsumeParticles = true;
        private final List<ConsumeEffect> onConsumeEffects = new ArrayList<>();

        Builder() {
        }

        public Consumable.Builder consumeSeconds(float pConsumeSounds) {
            this.consumeSeconds = pConsumeSounds;
            return this;
        }

        public Consumable.Builder animation(ItemUseAnimation pAnimation) {
            this.animation = pAnimation;
            return this;
        }

        public Consumable.Builder sound(Holder<SoundEvent> pSound) {
            this.sound = pSound;
            return this;
        }

        public Consumable.Builder soundAfterConsume(Holder<SoundEvent> pConsumptionSound) {
            return this.onConsume(new PlaySoundConsumeEffect(pConsumptionSound));
        }

        public Consumable.Builder hasConsumeParticles(boolean pHasConsumeParticles) {
            this.hasConsumeParticles = pHasConsumeParticles;
            return this;
        }

        public Consumable.Builder onConsume(ConsumeEffect pEffect) {
            this.onConsumeEffects.add(pEffect);
            return this;
        }

        public Consumable build() {
            return new Consumable(this.consumeSeconds, this.animation, this.sound, this.hasConsumeParticles, this.onConsumeEffects);
        }
    }

    public interface OverrideConsumeSound {
        SoundEvent getConsumeSound(ItemStack pStack);
    }
}
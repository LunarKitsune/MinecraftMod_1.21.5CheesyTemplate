package net.minecraft.world.item.alchemy;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ARGB;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.ConsumableListener;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.level.Level;

public record PotionContents(Optional<Holder<Potion>> potion, Optional<Integer> customColor, List<MobEffectInstance> customEffects, Optional<String> customName)
    implements ConsumableListener,
    TooltipProvider {
    public static final PotionContents EMPTY = new PotionContents(Optional.empty(), Optional.empty(), List.of(), Optional.empty());
    private static final Component NO_EFFECT = Component.translatable("effect.none").withStyle(ChatFormatting.GRAY);
    public static final int BASE_POTION_COLOR = -13083194;
    private static final Codec<PotionContents> FULL_CODEC = RecordCodecBuilder.create(
        p_359800_ -> p_359800_.group(
                Potion.CODEC.optionalFieldOf("potion").forGetter(PotionContents::potion),
                Codec.INT.optionalFieldOf("custom_color").forGetter(PotionContents::customColor),
                MobEffectInstance.CODEC.listOf().optionalFieldOf("custom_effects", List.of()).forGetter(PotionContents::customEffects),
                Codec.STRING.optionalFieldOf("custom_name").forGetter(PotionContents::customName)
            )
            .apply(p_359800_, PotionContents::new)
    );
    public static final Codec<PotionContents> CODEC = Codec.withAlternative(FULL_CODEC, Potion.CODEC, PotionContents::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, PotionContents> STREAM_CODEC = StreamCodec.composite(
        Potion.STREAM_CODEC.apply(ByteBufCodecs::optional),
        PotionContents::potion,
        ByteBufCodecs.INT.apply(ByteBufCodecs::optional),
        PotionContents::customColor,
        MobEffectInstance.STREAM_CODEC.apply(ByteBufCodecs.list()),
        PotionContents::customEffects,
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs::optional),
        PotionContents::customName,
        PotionContents::new
    );

    public PotionContents(Holder<Potion> pPotion) {
        this(Optional.of(pPotion), Optional.empty(), List.of(), Optional.empty());
    }

    public static ItemStack createItemStack(Item pItem, Holder<Potion> pPotion) {
        ItemStack itemstack = new ItemStack(pItem);
        itemstack.set(DataComponents.POTION_CONTENTS, new PotionContents(pPotion));
        return itemstack;
    }

    public boolean is(Holder<Potion> pPotion) {
        return this.potion.isPresent() && this.potion.get().is(pPotion) && this.customEffects.isEmpty();
    }

    public Iterable<MobEffectInstance> getAllEffects() {
        if (this.potion.isEmpty()) {
            return this.customEffects;
        } else {
            return (Iterable<MobEffectInstance>)(this.customEffects.isEmpty()
                ? this.potion.get().value().getEffects()
                : Iterables.concat(this.potion.get().value().getEffects(), this.customEffects));
        }
    }

    public void forEachEffect(Consumer<MobEffectInstance> pAction, float pDurationScale) {
        if (this.potion.isPresent()) {
            for (MobEffectInstance mobeffectinstance : this.potion.get().value().getEffects()) {
                pAction.accept(mobeffectinstance.withScaledDuration(pDurationScale));
            }
        }

        for (MobEffectInstance mobeffectinstance1 : this.customEffects) {
            pAction.accept(mobeffectinstance1.withScaledDuration(pDurationScale));
        }
    }

    public PotionContents withPotion(Holder<Potion> pPotion) {
        return new PotionContents(Optional.of(pPotion), this.customColor, this.customEffects, this.customName);
    }

    public PotionContents withEffectAdded(MobEffectInstance pEffect) {
        return new PotionContents(this.potion, this.customColor, Util.copyAndAdd(this.customEffects, pEffect), this.customName);
    }

    public int getColor() {
        return this.getColorOr(-13083194);
    }

    public int getColorOr(int pDefaultValue) {
        return this.customColor.isPresent() ? this.customColor.get() : getColorOptional(this.getAllEffects()).orElse(pDefaultValue);
    }

    public Component getName(String pName) {
        String s = this.customName.or(() -> this.potion.map(p_359801_ -> p_359801_.value().name())).orElse("empty");
        return Component.translatable(pName + s);
    }

    public static OptionalInt getColorOptional(Iterable<MobEffectInstance> pEffects) {
        int i = 0;
        int j = 0;
        int k = 0;
        int l = 0;

        for (MobEffectInstance mobeffectinstance : pEffects) {
            if (mobeffectinstance.isVisible()) {
                int i1 = mobeffectinstance.getEffect().value().getColor();
                int j1 = mobeffectinstance.getAmplifier() + 1;
                i += j1 * ARGB.red(i1);
                j += j1 * ARGB.green(i1);
                k += j1 * ARGB.blue(i1);
                l += j1;
            }
        }

        return l == 0 ? OptionalInt.empty() : OptionalInt.of(ARGB.color(i / l, j / l, k / l));
    }

    public boolean hasEffects() {
        return !this.customEffects.isEmpty() ? true : this.potion.isPresent() && !this.potion.get().value().getEffects().isEmpty();
    }

    public List<MobEffectInstance> customEffects() {
        return Lists.transform(this.customEffects, MobEffectInstance::new);
    }

    public void applyToLivingEntity(LivingEntity pEntity, float pDurationScale) {
        if (pEntity.level() instanceof ServerLevel serverlevel) {
            Player player1 = pEntity instanceof Player player ? player : null;
            this.forEachEffect(p_359805_ -> {
                if (p_359805_.getEffect().value().isInstantenous()) {
                    p_359805_.getEffect().value().applyInstantenousEffect(serverlevel, player1, player1, pEntity, p_359805_.getAmplifier(), 1.0);
                } else {
                    pEntity.addEffect(p_359805_);
                }
            }, pDurationScale);
        }
    }

    public static void addPotionTooltip(Iterable<MobEffectInstance> pEffects, Consumer<Component> pTooltipAdder, float pDurationFactor, float pTicksPerSecond) {
        List<Pair<Holder<Attribute>, AttributeModifier>> list = Lists.newArrayList();
        boolean flag = true;

        for (MobEffectInstance mobeffectinstance : pEffects) {
            flag = false;
            Holder<MobEffect> holder = mobeffectinstance.getEffect();
            int i = mobeffectinstance.getAmplifier();
            holder.value().createModifiers(i, (p_329075_, p_331827_) -> list.add(new Pair<>(p_329075_, p_331827_)));
            MutableComponent mutablecomponent = getPotionDescription(holder, i);
            if (!mobeffectinstance.endsWithin(20)) {
                mutablecomponent = Component.translatable(
                    "potion.withDuration", mutablecomponent, MobEffectUtil.formatDuration(mobeffectinstance, pDurationFactor, pTicksPerSecond)
                );
            }

            pTooltipAdder.accept(mutablecomponent.withStyle(holder.value().getCategory().getTooltipFormatting()));
        }

        if (flag) {
            pTooltipAdder.accept(NO_EFFECT);
        }

        if (!list.isEmpty()) {
            pTooltipAdder.accept(CommonComponents.EMPTY);
            pTooltipAdder.accept(Component.translatable("potion.whenDrank").withStyle(ChatFormatting.DARK_PURPLE));

            for (Pair<Holder<Attribute>, AttributeModifier> pair : list) {
                AttributeModifier attributemodifier = pair.getSecond();
                double d1 = attributemodifier.amount();
                double d0;
                if (attributemodifier.operation() != AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                    && attributemodifier.operation() != AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
                    d0 = attributemodifier.amount();
                } else {
                    d0 = attributemodifier.amount() * 100.0;
                }

                if (d1 > 0.0) {
                    pTooltipAdder.accept(
                        Component.translatable(
                                "attribute.modifier.plus." + attributemodifier.operation().id(),
                                ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(d0),
                                Component.translatable(pair.getFirst().value().getDescriptionId())
                            )
                            .withStyle(ChatFormatting.BLUE)
                    );
                } else if (d1 < 0.0) {
                    d0 *= -1.0;
                    pTooltipAdder.accept(
                        Component.translatable(
                                "attribute.modifier.take." + attributemodifier.operation().id(),
                                ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(d0),
                                Component.translatable(pair.getFirst().value().getDescriptionId())
                            )
                            .withStyle(ChatFormatting.RED)
                    );
                }
            }
        }
    }

    public static MutableComponent getPotionDescription(Holder<MobEffect> pEffect, int pLevel) {
        MutableComponent mutablecomponent = Component.translatable(pEffect.value().getDescriptionId());
        return pLevel > 0
            ? Component.translatable("potion.withAmplifier", mutablecomponent, Component.translatable("potion.potency." + pLevel))
            : mutablecomponent;
    }

    @Override
    public void onConsume(Level p_367284_, LivingEntity p_369037_, ItemStack p_368087_, Consumable p_366370_) {
        this.applyToLivingEntity(p_369037_, p_368087_.getOrDefault(DataComponents.POTION_DURATION_SCALE, 1.0F));
    }

    @Override
    public void addToTooltip(Item.TooltipContext p_395642_, Consumer<Component> p_397372_, TooltipFlag p_396062_, DataComponentGetter p_392179_) {
        addPotionTooltip(this.getAllEffects(), p_397372_, p_392179_.getOrDefault(DataComponents.POTION_DURATION_SCALE, 1.0F), p_395642_.tickRate());
    }
}
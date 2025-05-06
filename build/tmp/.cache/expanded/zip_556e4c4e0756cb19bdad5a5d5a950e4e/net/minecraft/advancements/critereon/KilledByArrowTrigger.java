package net.minecraft.advancements.critereon;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.core.HolderGetter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;

public class KilledByArrowTrigger extends SimpleCriterionTrigger<KilledByArrowTrigger.TriggerInstance> {
    @Override
    public Codec<KilledByArrowTrigger.TriggerInstance> codec() {
        return KilledByArrowTrigger.TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer pPlayer, Collection<Entity> pVictims, @Nullable ItemStack pFiredFromWeapon) {
        List<LootContext> list = Lists.newArrayList();
        Set<EntityType<?>> set = Sets.newHashSet();

        for (Entity entity : pVictims) {
            set.add(entity.getType());
            list.add(EntityPredicate.createContext(pPlayer, entity));
        }

        this.trigger(pPlayer, p_363274_ -> p_363274_.matches(list, set.size(), pFiredFromWeapon));
    }

    public record TriggerInstance(
        Optional<ContextAwarePredicate> player, List<ContextAwarePredicate> victims, MinMaxBounds.Ints uniqueEntityTypes, Optional<ItemPredicate> firedFromWeapon
    ) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<KilledByArrowTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
            p_363742_ -> p_363742_.group(
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(KilledByArrowTrigger.TriggerInstance::player),
                    EntityPredicate.ADVANCEMENT_CODEC.listOf().optionalFieldOf("victims", List.of()).forGetter(KilledByArrowTrigger.TriggerInstance::victims),
                    MinMaxBounds.Ints.CODEC
                        .optionalFieldOf("unique_entity_types", MinMaxBounds.Ints.ANY)
                        .forGetter(KilledByArrowTrigger.TriggerInstance::uniqueEntityTypes),
                    ItemPredicate.CODEC.optionalFieldOf("fired_from_weapon").forGetter(KilledByArrowTrigger.TriggerInstance::firedFromWeapon)
                )
                .apply(p_363742_, KilledByArrowTrigger.TriggerInstance::new)
        );

        public static Criterion<KilledByArrowTrigger.TriggerInstance> crossbowKilled(HolderGetter<Item> pItemRegistry, EntityPredicate.Builder... pVictims) {
            return CriteriaTriggers.KILLED_BY_ARROW
                .createCriterion(
                    new KilledByArrowTrigger.TriggerInstance(
                        Optional.empty(),
                        EntityPredicate.wrap(pVictims),
                        MinMaxBounds.Ints.ANY,
                        Optional.of(ItemPredicate.Builder.item().of(pItemRegistry, Items.CROSSBOW).build())
                    )
                );
        }

        public static Criterion<KilledByArrowTrigger.TriggerInstance> crossbowKilled(HolderGetter<Item> pItemRegistry, MinMaxBounds.Ints pUniqueEntityTypes) {
            return CriteriaTriggers.KILLED_BY_ARROW
                .createCriterion(
                    new KilledByArrowTrigger.TriggerInstance(
                        Optional.empty(), List.of(), pUniqueEntityTypes, Optional.of(ItemPredicate.Builder.item().of(pItemRegistry, Items.CROSSBOW).build())
                    )
                );
        }

        public boolean matches(Collection<LootContext> pContext, int pUniqueEntityTypes, @Nullable ItemStack pFiredFromWeapon) {
            if (!this.firedFromWeapon.isPresent() || pFiredFromWeapon != null && this.firedFromWeapon.get().test(pFiredFromWeapon)) {
                if (!this.victims.isEmpty()) {
                    List<LootContext> list = Lists.newArrayList(pContext);

                    for (ContextAwarePredicate contextawarepredicate : this.victims) {
                        boolean flag = false;
                        Iterator<LootContext> iterator = list.iterator();

                        while (iterator.hasNext()) {
                            LootContext lootcontext = iterator.next();
                            if (contextawarepredicate.matches(lootcontext)) {
                                iterator.remove();
                                flag = true;
                                break;
                            }
                        }

                        if (!flag) {
                            return false;
                        }
                    }
                }

                return this.uniqueEntityTypes.matches(pUniqueEntityTypes);
            } else {
                return false;
            }
        }

        @Override
        public void validate(CriterionValidator p_365351_) {
            SimpleCriterionTrigger.SimpleInstance.super.validate(p_365351_);
            p_365351_.validateEntities(this.victims, ".victims");
        }

        @Override
        public Optional<ContextAwarePredicate> player() {
            return this.player;
        }
    }
}
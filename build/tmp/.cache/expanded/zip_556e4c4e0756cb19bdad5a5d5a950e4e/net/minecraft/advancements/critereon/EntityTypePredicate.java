package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public record EntityTypePredicate(HolderSet<EntityType<?>> types) {
    public static final Codec<EntityTypePredicate> CODEC = RegistryCodecs.homogeneousList(Registries.ENTITY_TYPE)
        .xmap(EntityTypePredicate::new, EntityTypePredicate::types);

    public static EntityTypePredicate of(HolderGetter<EntityType<?>> pEntityTypeRegistry, EntityType<?> pEntityType) {
        return new EntityTypePredicate(HolderSet.direct(pEntityType.builtInRegistryHolder()));
    }

    public static EntityTypePredicate of(HolderGetter<EntityType<?>> pEntityTypeRegistry, TagKey<EntityType<?>> pEntityTypeTag) {
        return new EntityTypePredicate(pEntityTypeRegistry.getOrThrow(pEntityTypeTag));
    }

    public boolean matches(EntityType<?> pType) {
        return pType.is(this.types);
    }
}
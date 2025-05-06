package net.minecraft.world.entity.variant;

import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public class VariantUtils {
    public static final String TAG_VARIANT = "variant";

    public static <T> Holder<T> getDefaultOrAny(RegistryAccess pRegistryAccess, ResourceKey<T> pKey) {
        Registry<T> registry = pRegistryAccess.lookupOrThrow(pKey.registryKey());
        return registry.get(pKey).or(registry::getAny).orElseThrow();
    }

    public static <T> Holder<T> getAny(RegistryAccess pRegistryAccess, ResourceKey<? extends Registry<T>> pRegistryKey) {
        return pRegistryAccess.lookupOrThrow(pRegistryKey).getAny().orElseThrow();
    }

    public static <T> void writeVariant(CompoundTag pTag, Holder<T> pVariant) {
        pVariant.unwrapKey().ifPresent(p_392288_ -> pTag.store("variant", ResourceLocation.CODEC, p_392288_.location()));
    }

    public static <T> Optional<Holder<T>> readVariant(CompoundTag pTag, RegistryAccess pRegistryAccess, ResourceKey<? extends Registry<T>> pRegistryKey) {
        return pTag.read("variant", ResourceLocation.CODEC)
            .map(p_397274_ -> ResourceKey.create(pRegistryKey, p_397274_))
            .flatMap(pRegistryAccess::get);
    }
}
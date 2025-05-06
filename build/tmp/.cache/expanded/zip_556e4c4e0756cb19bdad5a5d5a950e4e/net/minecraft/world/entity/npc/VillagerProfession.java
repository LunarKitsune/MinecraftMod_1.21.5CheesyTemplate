package net.minecraft.world.entity.npc;

import com.google.common.collect.ImmutableSet;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public record VillagerProfession(
    Component name,
    Predicate<Holder<PoiType>> heldJobSite,
    Predicate<Holder<PoiType>> acquirableJobSite,
    ImmutableSet<Item> requestedItems,
    ImmutableSet<Block> secondaryPoi,
    @Nullable SoundEvent workSound
) {
    public static final Predicate<Holder<PoiType>> ALL_ACQUIRABLE_JOBS = p_238239_ -> p_238239_.is(PoiTypeTags.ACQUIRABLE_JOB_SITE);
    public static final ResourceKey<VillagerProfession> NONE = createKey("none");
    public static final ResourceKey<VillagerProfession> ARMORER = createKey("armorer");
    public static final ResourceKey<VillagerProfession> BUTCHER = createKey("butcher");
    public static final ResourceKey<VillagerProfession> CARTOGRAPHER = createKey("cartographer");
    public static final ResourceKey<VillagerProfession> CLERIC = createKey("cleric");
    public static final ResourceKey<VillagerProfession> FARMER = createKey("farmer");
    public static final ResourceKey<VillagerProfession> FISHERMAN = createKey("fisherman");
    public static final ResourceKey<VillagerProfession> FLETCHER = createKey("fletcher");
    public static final ResourceKey<VillagerProfession> LEATHERWORKER = createKey("leatherworker");
    public static final ResourceKey<VillagerProfession> LIBRARIAN = createKey("librarian");
    public static final ResourceKey<VillagerProfession> MASON = createKey("mason");
    public static final ResourceKey<VillagerProfession> NITWIT = createKey("nitwit");
    public static final ResourceKey<VillagerProfession> SHEPHERD = createKey("shepherd");
    public static final ResourceKey<VillagerProfession> TOOLSMITH = createKey("toolsmith");
    public static final ResourceKey<VillagerProfession> WEAPONSMITH = createKey("weaponsmith");

    private static ResourceKey<VillagerProfession> createKey(String pName) {
        return ResourceKey.create(Registries.VILLAGER_PROFESSION, ResourceLocation.withDefaultNamespace(pName));
    }

    private static VillagerProfession register(
        Registry<VillagerProfession> pRegistry, ResourceKey<VillagerProfession> pName, ResourceKey<PoiType> pPoiType, @Nullable SoundEvent pWorkSound
    ) {
        return register(pRegistry, pName, p_219668_ -> p_219668_.is(pPoiType), p_219640_ -> p_219640_.is(pPoiType), pWorkSound);
    }

    private static VillagerProfession register(
        Registry<VillagerProfession> pRegistry,
        ResourceKey<VillagerProfession> pName,
        Predicate<Holder<PoiType>> pHeldJobSite,
        Predicate<Holder<PoiType>> pAcquirableJobSite,
        @Nullable SoundEvent pWorkSound
    ) {
        return register(pRegistry, pName, pHeldJobSite, pAcquirableJobSite, ImmutableSet.of(), ImmutableSet.of(), pWorkSound);
    }

    private static VillagerProfession register(
        Registry<VillagerProfession> pRegistry,
        ResourceKey<VillagerProfession> pName,
        ResourceKey<PoiType> pJobSite,
        ImmutableSet<Item> pRequestedItems,
        ImmutableSet<Block> pSecondaryPoi,
        @Nullable SoundEvent pWorkSound
    ) {
        return register(
            pRegistry, pName, p_238234_ -> p_238234_.is(pJobSite), p_238237_ -> p_238237_.is(pJobSite), pRequestedItems, pSecondaryPoi, pWorkSound
        );
    }

    private static VillagerProfession register(
        Registry<VillagerProfession> pRegistry,
        ResourceKey<VillagerProfession> pName,
        Predicate<Holder<PoiType>> pHeldJobSite,
        Predicate<Holder<PoiType>> pAcquirableJobSite,
        ImmutableSet<Item> pRequestedItems,
        ImmutableSet<Block> pSecondaryPoi,
        @Nullable SoundEvent pWorkSound
    ) {
        return Registry.register(
            pRegistry,
            pName,
            new VillagerProfession(
                Component.translatable("entity." + pName.location().getNamespace() + ".villager." + pName.location().getPath()),
                pHeldJobSite,
                pAcquirableJobSite,
                pRequestedItems,
                pSecondaryPoi,
                pWorkSound
            )
        );
    }

    public static VillagerProfession bootstrap(Registry<VillagerProfession> pRegistry) {
        register(pRegistry, NONE, PoiType.NONE, ALL_ACQUIRABLE_JOBS, null);
        register(pRegistry, ARMORER, PoiTypes.ARMORER, SoundEvents.VILLAGER_WORK_ARMORER);
        register(pRegistry, BUTCHER, PoiTypes.BUTCHER, SoundEvents.VILLAGER_WORK_BUTCHER);
        register(pRegistry, CARTOGRAPHER, PoiTypes.CARTOGRAPHER, SoundEvents.VILLAGER_WORK_CARTOGRAPHER);
        register(pRegistry, CLERIC, PoiTypes.CLERIC, SoundEvents.VILLAGER_WORK_CLERIC);
        register(
            pRegistry,
            FARMER,
            PoiTypes.FARMER,
            ImmutableSet.of(Items.WHEAT, Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, Items.BONE_MEAL),
            ImmutableSet.of(Blocks.FARMLAND),
            SoundEvents.VILLAGER_WORK_FARMER
        );
        register(pRegistry, FISHERMAN, PoiTypes.FISHERMAN, SoundEvents.VILLAGER_WORK_FISHERMAN);
        register(pRegistry, FLETCHER, PoiTypes.FLETCHER, SoundEvents.VILLAGER_WORK_FLETCHER);
        register(pRegistry, LEATHERWORKER, PoiTypes.LEATHERWORKER, SoundEvents.VILLAGER_WORK_LEATHERWORKER);
        register(pRegistry, LIBRARIAN, PoiTypes.LIBRARIAN, SoundEvents.VILLAGER_WORK_LIBRARIAN);
        register(pRegistry, MASON, PoiTypes.MASON, SoundEvents.VILLAGER_WORK_MASON);
        register(pRegistry, NITWIT, PoiType.NONE, PoiType.NONE, null);
        register(pRegistry, SHEPHERD, PoiTypes.SHEPHERD, SoundEvents.VILLAGER_WORK_SHEPHERD);
        register(pRegistry, TOOLSMITH, PoiTypes.TOOLSMITH, SoundEvents.VILLAGER_WORK_TOOLSMITH);
        return register(pRegistry, WEAPONSMITH, PoiTypes.WEAPONSMITH, SoundEvents.VILLAGER_WORK_WEAPONSMITH);
    }
}
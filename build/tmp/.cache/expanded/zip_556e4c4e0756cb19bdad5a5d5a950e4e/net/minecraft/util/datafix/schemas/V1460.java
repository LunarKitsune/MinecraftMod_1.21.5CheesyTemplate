package net.minecraft.util.datafix.schemas;

import com.google.common.collect.Maps;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.datafixers.types.templates.Hook.HookFunction;
import com.mojang.datafixers.util.Pair;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V1460 extends NamespacedSchema {
    public V1460(int p_17553_, Schema p_17554_) {
        super(p_17553_, p_17554_);
    }

    protected static void registerMob(Schema pSchema, Map<String, Supplier<TypeTemplate>> pMap, String pName) {
        pSchema.registerSimple(pMap, pName);
    }

    protected static void registerInventory(Schema pSchema, Map<String, Supplier<TypeTemplate>> pMap, String pName) {
        pSchema.register(pMap, pName, () -> V1458.nameableInventory(pSchema));
    }

    @Override
    public Map<String, Supplier<TypeTemplate>> registerEntities(Schema pSchema) {
        Map<String, Supplier<TypeTemplate>> map = Maps.newHashMap();
        pSchema.register(map, "minecraft:area_effect_cloud", p_326692_ -> DSL.optionalFields("Particle", References.PARTICLE.in(pSchema)));
        registerMob(pSchema, map, "minecraft:armor_stand");
        pSchema.register(map, "minecraft:arrow", p_17683_ -> DSL.optionalFields("inBlockState", References.BLOCK_STATE.in(pSchema)));
        registerMob(pSchema, map, "minecraft:bat");
        registerMob(pSchema, map, "minecraft:blaze");
        pSchema.registerSimple(map, "minecraft:boat");
        registerMob(pSchema, map, "minecraft:cave_spider");
        pSchema.register(
            map,
            "minecraft:chest_minecart",
            p_17680_ -> DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(pSchema), "Items", DSL.list(References.ITEM_STACK.in(pSchema)))
        );
        registerMob(pSchema, map, "minecraft:chicken");
        pSchema.register(
            map,
            "minecraft:commandblock_minecart",
            p_390346_ -> DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(pSchema), "LastOutput", References.TEXT_COMPONENT.in(pSchema))
        );
        registerMob(pSchema, map, "minecraft:cow");
        registerMob(pSchema, map, "minecraft:creeper");
        pSchema.register(
            map,
            "minecraft:donkey",
            p_390366_ -> DSL.optionalFields("Items", DSL.list(References.ITEM_STACK.in(pSchema)), "SaddleItem", References.ITEM_STACK.in(pSchema))
        );
        pSchema.registerSimple(map, "minecraft:dragon_fireball");
        pSchema.registerSimple(map, "minecraft:egg");
        registerMob(pSchema, map, "minecraft:elder_guardian");
        pSchema.registerSimple(map, "minecraft:ender_crystal");
        registerMob(pSchema, map, "minecraft:ender_dragon");
        pSchema.register(map, "minecraft:enderman", p_17677_ -> DSL.optionalFields("carriedBlockState", References.BLOCK_STATE.in(pSchema)));
        registerMob(pSchema, map, "minecraft:endermite");
        pSchema.registerSimple(map, "minecraft:ender_pearl");
        pSchema.registerSimple(map, "minecraft:evocation_fangs");
        registerMob(pSchema, map, "minecraft:evocation_illager");
        pSchema.registerSimple(map, "minecraft:eye_of_ender_signal");
        pSchema.register(
            map,
            "minecraft:falling_block",
            p_17668_ -> DSL.optionalFields("BlockState", References.BLOCK_STATE.in(pSchema), "TileEntityData", References.BLOCK_ENTITY.in(pSchema))
        );
        pSchema.registerSimple(map, "minecraft:fireball");
        pSchema.register(map, "minecraft:fireworks_rocket", p_390348_ -> DSL.optionalFields("FireworksItem", References.ITEM_STACK.in(pSchema)));
        pSchema.register(map, "minecraft:furnace_minecart", p_17654_ -> DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(pSchema)));
        registerMob(pSchema, map, "minecraft:ghast");
        registerMob(pSchema, map, "minecraft:giant");
        registerMob(pSchema, map, "minecraft:guardian");
        pSchema.register(
            map,
            "minecraft:hopper_minecart",
            p_17651_ -> DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(pSchema), "Items", DSL.list(References.ITEM_STACK.in(pSchema)))
        );
        pSchema.register(
            map,
            "minecraft:horse",
            p_390377_ -> DSL.optionalFields("ArmorItem", References.ITEM_STACK.in(pSchema), "SaddleItem", References.ITEM_STACK.in(pSchema))
        );
        registerMob(pSchema, map, "minecraft:husk");
        registerMob(pSchema, map, "minecraft:illusion_illager");
        pSchema.register(map, "minecraft:item", p_17645_ -> DSL.optionalFields("Item", References.ITEM_STACK.in(pSchema)));
        pSchema.register(map, "minecraft:item_frame", p_17642_ -> DSL.optionalFields("Item", References.ITEM_STACK.in(pSchema)));
        pSchema.registerSimple(map, "minecraft:leash_knot");
        pSchema.register(
            map,
            "minecraft:llama",
            p_390355_ -> DSL.optionalFields(
                "Items",
                DSL.list(References.ITEM_STACK.in(pSchema)),
                "SaddleItem",
                References.ITEM_STACK.in(pSchema),
                "DecorItem",
                References.ITEM_STACK.in(pSchema)
            )
        );
        pSchema.registerSimple(map, "minecraft:llama_spit");
        registerMob(pSchema, map, "minecraft:magma_cube");
        pSchema.register(map, "minecraft:minecart", p_17634_ -> DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(pSchema)));
        registerMob(pSchema, map, "minecraft:mooshroom");
        pSchema.register(
            map,
            "minecraft:mule",
            p_390362_ -> DSL.optionalFields("Items", DSL.list(References.ITEM_STACK.in(pSchema)), "SaddleItem", References.ITEM_STACK.in(pSchema))
        );
        registerMob(pSchema, map, "minecraft:ocelot");
        pSchema.registerSimple(map, "minecraft:painting");
        registerMob(pSchema, map, "minecraft:parrot");
        registerMob(pSchema, map, "minecraft:pig");
        registerMob(pSchema, map, "minecraft:polar_bear");
        pSchema.register(map, "minecraft:potion", p_17624_ -> DSL.optionalFields("Potion", References.ITEM_STACK.in(pSchema)));
        registerMob(pSchema, map, "minecraft:rabbit");
        registerMob(pSchema, map, "minecraft:sheep");
        registerMob(pSchema, map, "minecraft:shulker");
        pSchema.registerSimple(map, "minecraft:shulker_bullet");
        registerMob(pSchema, map, "minecraft:silverfish");
        registerMob(pSchema, map, "minecraft:skeleton");
        pSchema.register(map, "minecraft:skeleton_horse", p_17665_ -> DSL.optionalFields("SaddleItem", References.ITEM_STACK.in(pSchema)));
        registerMob(pSchema, map, "minecraft:slime");
        pSchema.registerSimple(map, "minecraft:small_fireball");
        pSchema.registerSimple(map, "minecraft:snowball");
        registerMob(pSchema, map, "minecraft:snowman");
        pSchema.register(
            map,
            "minecraft:spawner_minecart",
            p_17614_ -> DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(pSchema), References.UNTAGGED_SPAWNER.in(pSchema))
        );
        pSchema.register(map, "minecraft:spectral_arrow", p_17609_ -> DSL.optionalFields("inBlockState", References.BLOCK_STATE.in(pSchema)));
        registerMob(pSchema, map, "minecraft:spider");
        registerMob(pSchema, map, "minecraft:squid");
        registerMob(pSchema, map, "minecraft:stray");
        pSchema.registerSimple(map, "minecraft:tnt");
        pSchema.register(map, "minecraft:tnt_minecart", p_17604_ -> DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(pSchema)));
        registerMob(pSchema, map, "minecraft:vex");
        pSchema.register(
            map,
            "minecraft:villager",
            p_390353_ -> DSL.optionalFields(
                "Inventory", DSL.list(References.ITEM_STACK.in(pSchema)), "Offers", DSL.optionalFields("Recipes", DSL.list(References.VILLAGER_TRADE.in(pSchema)))
            )
        );
        registerMob(pSchema, map, "minecraft:villager_golem");
        registerMob(pSchema, map, "minecraft:vindication_illager");
        registerMob(pSchema, map, "minecraft:witch");
        registerMob(pSchema, map, "minecraft:wither");
        registerMob(pSchema, map, "minecraft:wither_skeleton");
        pSchema.registerSimple(map, "minecraft:wither_skull");
        registerMob(pSchema, map, "minecraft:wolf");
        pSchema.registerSimple(map, "minecraft:xp_bottle");
        pSchema.registerSimple(map, "minecraft:xp_orb");
        registerMob(pSchema, map, "minecraft:zombie");
        pSchema.register(map, "minecraft:zombie_horse", p_390379_ -> DSL.optionalFields("SaddleItem", References.ITEM_STACK.in(pSchema)));
        registerMob(pSchema, map, "minecraft:zombie_pigman");
        pSchema.register(
            map,
            "minecraft:zombie_villager",
            p_390357_ -> DSL.optionalFields("Offers", DSL.optionalFields("Recipes", DSL.list(References.VILLAGER_TRADE.in(pSchema))))
        );
        return map;
    }

    @Override
    public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema pSchema) {
        Map<String, Supplier<TypeTemplate>> map = Maps.newHashMap();
        registerInventory(pSchema, map, "minecraft:furnace");
        registerInventory(pSchema, map, "minecraft:chest");
        registerInventory(pSchema, map, "minecraft:trapped_chest");
        pSchema.registerSimple(map, "minecraft:ender_chest");
        pSchema.register(map, "minecraft:jukebox", p_17586_ -> DSL.optionalFields("RecordItem", References.ITEM_STACK.in(pSchema)));
        registerInventory(pSchema, map, "minecraft:dispenser");
        registerInventory(pSchema, map, "minecraft:dropper");
        pSchema.register(map, "minecraft:sign", () -> V99.sign(pSchema));
        pSchema.register(map, "minecraft:mob_spawner", p_17574_ -> References.UNTAGGED_SPAWNER.in(pSchema));
        pSchema.register(map, "minecraft:piston", p_17559_ -> DSL.optionalFields("blockState", References.BLOCK_STATE.in(pSchema)));
        registerInventory(pSchema, map, "minecraft:brewing_stand");
        pSchema.register(map, "minecraft:enchanting_table", () -> V1458.nameable(pSchema));
        pSchema.registerSimple(map, "minecraft:end_portal");
        pSchema.register(map, "minecraft:beacon", () -> V1458.nameable(pSchema));
        pSchema.register(map, "minecraft:skull", () -> DSL.optionalFields("custom_name", References.TEXT_COMPONENT.in(pSchema)));
        pSchema.registerSimple(map, "minecraft:daylight_detector");
        registerInventory(pSchema, map, "minecraft:hopper");
        pSchema.registerSimple(map, "minecraft:comparator");
        pSchema.register(map, "minecraft:banner", () -> V1458.nameable(pSchema));
        pSchema.registerSimple(map, "minecraft:structure_block");
        pSchema.registerSimple(map, "minecraft:end_gateway");
        pSchema.register(map, "minecraft:command_block", () -> DSL.optionalFields("LastOutput", References.TEXT_COMPONENT.in(pSchema)));
        registerInventory(pSchema, map, "minecraft:shulker_box");
        pSchema.registerSimple(map, "minecraft:bed");
        return map;
    }

    @Override
    public void registerTypes(Schema pSchema, Map<String, Supplier<TypeTemplate>> pEntityTypes, Map<String, Supplier<TypeTemplate>> pBlockEntityTypes) {
        pSchema.registerType(
            false,
            References.LEVEL,
            () -> DSL.optionalFields("CustomBossEvents", DSL.compoundList(DSL.optionalFields("Name", References.TEXT_COMPONENT.in(pSchema))))
        );
        pSchema.registerType(false, References.RECIPE, () -> DSL.constType(namespacedString()));
        pSchema.registerType(
            false,
            References.PLAYER,
            () -> DSL.optionalFields(
                Pair.of("RootVehicle", DSL.optionalFields("Entity", References.ENTITY_TREE.in(pSchema))),
                Pair.of("ender_pearls", DSL.list(References.ENTITY_TREE.in(pSchema))),
                Pair.of("Inventory", DSL.list(References.ITEM_STACK.in(pSchema))),
                Pair.of("EnderItems", DSL.list(References.ITEM_STACK.in(pSchema))),
                Pair.of("ShoulderEntityLeft", References.ENTITY_TREE.in(pSchema)),
                Pair.of("ShoulderEntityRight", References.ENTITY_TREE.in(pSchema)),
                Pair.of(
                    "recipeBook",
                    DSL.optionalFields("recipes", DSL.list(References.RECIPE.in(pSchema)), "toBeDisplayed", DSL.list(References.RECIPE.in(pSchema)))
                )
            )
        );
        pSchema.registerType(
            false,
            References.CHUNK,
            () -> DSL.fields(
                "Level",
                DSL.optionalFields(
                    "Entities",
                    DSL.list(References.ENTITY_TREE.in(pSchema)),
                    "TileEntities",
                    DSL.list(DSL.or(References.BLOCK_ENTITY.in(pSchema), DSL.remainder())),
                    "TileTicks",
                    DSL.list(DSL.fields("i", References.BLOCK_NAME.in(pSchema))),
                    "Sections",
                    DSL.list(DSL.optionalFields("Palette", DSL.list(References.BLOCK_STATE.in(pSchema))))
                )
            )
        );
        pSchema.registerType(
            true,
            References.BLOCK_ENTITY,
            () -> DSL.optionalFields("components", References.DATA_COMPONENTS.in(pSchema), DSL.taggedChoiceLazy("id", namespacedString(), pBlockEntityTypes))
        );
        pSchema.registerType(
            true, References.ENTITY_TREE, () -> DSL.optionalFields("Passengers", DSL.list(References.ENTITY_TREE.in(pSchema)), References.ENTITY.in(pSchema))
        );
        pSchema.registerType(
            true,
            References.ENTITY,
            () -> DSL.and(
                References.ENTITY_EQUIPMENT.in(pSchema),
                DSL.optionalFields("CustomName", References.TEXT_COMPONENT.in(pSchema), DSL.taggedChoiceLazy("id", namespacedString(), pEntityTypes))
            )
        );
        pSchema.registerType(
            true,
            References.ITEM_STACK,
            () -> DSL.hook(DSL.optionalFields("id", References.ITEM_NAME.in(pSchema), "tag", V99.itemStackTag(pSchema)), V705.ADD_NAMES, HookFunction.IDENTITY)
        );
        pSchema.registerType(false, References.HOTBAR, () -> DSL.compoundList(DSL.list(References.ITEM_STACK.in(pSchema))));
        pSchema.registerType(false, References.OPTIONS, DSL::remainder);
        pSchema.registerType(
            false,
            References.STRUCTURE,
            () -> DSL.optionalFields(
                "entities",
                DSL.list(DSL.optionalFields("nbt", References.ENTITY_TREE.in(pSchema))),
                "blocks",
                DSL.list(DSL.optionalFields("nbt", References.BLOCK_ENTITY.in(pSchema))),
                "palette",
                DSL.list(References.BLOCK_STATE.in(pSchema))
            )
        );
        pSchema.registerType(false, References.BLOCK_NAME, () -> DSL.constType(namespacedString()));
        pSchema.registerType(false, References.ITEM_NAME, () -> DSL.constType(namespacedString()));
        pSchema.registerType(false, References.BLOCK_STATE, DSL::remainder);
        pSchema.registerType(false, References.FLAT_BLOCK_STATE, DSL::remainder);
        Supplier<TypeTemplate> supplier = () -> DSL.compoundList(References.ITEM_NAME.in(pSchema), DSL.constType(DSL.intType()));
        pSchema.registerType(
            false,
            References.STATS,
            () -> DSL.optionalFields(
                "stats",
                DSL.optionalFields(
                    Pair.of("minecraft:mined", DSL.compoundList(References.BLOCK_NAME.in(pSchema), DSL.constType(DSL.intType()))),
                    Pair.of("minecraft:crafted", supplier.get()),
                    Pair.of("minecraft:used", supplier.get()),
                    Pair.of("minecraft:broken", supplier.get()),
                    Pair.of("minecraft:picked_up", supplier.get()),
                    Pair.of("minecraft:dropped", supplier.get()),
                    Pair.of("minecraft:killed", DSL.compoundList(References.ENTITY_NAME.in(pSchema), DSL.constType(DSL.intType()))),
                    Pair.of("minecraft:killed_by", DSL.compoundList(References.ENTITY_NAME.in(pSchema), DSL.constType(DSL.intType()))),
                    Pair.of("minecraft:custom", DSL.compoundList(DSL.constType(namespacedString()), DSL.constType(DSL.intType())))
                )
            )
        );
        pSchema.registerType(false, References.SAVED_DATA_COMMAND_STORAGE, DSL::remainder);
        pSchema.registerType(false, References.SAVED_DATA_TICKETS, DSL::remainder);
        pSchema.registerType(
            false, References.SAVED_DATA_MAP_DATA, () -> DSL.optionalFields("banners", DSL.list(DSL.optionalFields("Name", References.TEXT_COMPONENT.in(pSchema))))
        );
        pSchema.registerType(false, References.SAVED_DATA_MAP_INDEX, DSL::remainder);
        pSchema.registerType(false, References.SAVED_DATA_RAIDS, DSL::remainder);
        pSchema.registerType(false, References.SAVED_DATA_RANDOM_SEQUENCES, DSL::remainder);
        pSchema.registerType(
            false,
            References.SAVED_DATA_SCOREBOARD,
            () -> DSL.optionalFields(
                "data",
                DSL.optionalFields(
                    "Objectives",
                    DSL.list(References.OBJECTIVE.in(pSchema)),
                    "Teams",
                    DSL.list(References.TEAM.in(pSchema)),
                    "PlayerScores",
                    DSL.list(DSL.optionalFields("display", References.TEXT_COMPONENT.in(pSchema)))
                )
            )
        );
        pSchema.registerType(
            false, References.SAVED_DATA_STRUCTURE_FEATURE_INDICES, () -> DSL.optionalFields("data", DSL.optionalFields("Features", DSL.compoundList(References.STRUCTURE_FEATURE.in(pSchema))))
        );
        pSchema.registerType(false, References.STRUCTURE_FEATURE, DSL::remainder);
        Map<String, Supplier<TypeTemplate>> map = V1451_6.createCriterionTypes(pSchema);
        pSchema.registerType(
            false,
            References.OBJECTIVE,
            () -> DSL.hook(
                DSL.optionalFields("CriteriaType", DSL.taggedChoiceLazy("type", DSL.string(), map), "DisplayName", References.TEXT_COMPONENT.in(pSchema)),
                V1451_6.UNPACK_OBJECTIVE_ID,
                V1451_6.REPACK_OBJECTIVE_ID
            )
        );
        pSchema.registerType(
            false,
            References.TEAM,
            () -> DSL.optionalFields(
                "MemberNamePrefix",
                References.TEXT_COMPONENT.in(pSchema),
                "MemberNameSuffix",
                References.TEXT_COMPONENT.in(pSchema),
                "DisplayName",
                References.TEXT_COMPONENT.in(pSchema)
            )
        );
        pSchema.registerType(
            true,
            References.UNTAGGED_SPAWNER,
            () -> DSL.optionalFields(
                "SpawnPotentials", DSL.list(DSL.fields("Entity", References.ENTITY_TREE.in(pSchema))), "SpawnData", References.ENTITY_TREE.in(pSchema)
            )
        );
        pSchema.registerType(
            false,
            References.ADVANCEMENTS,
            () -> DSL.optionalFields(
                "minecraft:adventure/adventuring_time",
                DSL.optionalFields("criteria", DSL.compoundList(References.BIOME.in(pSchema), DSL.constType(DSL.string()))),
                "minecraft:adventure/kill_a_mob",
                DSL.optionalFields("criteria", DSL.compoundList(References.ENTITY_NAME.in(pSchema), DSL.constType(DSL.string()))),
                "minecraft:adventure/kill_all_mobs",
                DSL.optionalFields("criteria", DSL.compoundList(References.ENTITY_NAME.in(pSchema), DSL.constType(DSL.string()))),
                "minecraft:husbandry/bred_all_animals",
                DSL.optionalFields("criteria", DSL.compoundList(References.ENTITY_NAME.in(pSchema), DSL.constType(DSL.string())))
            )
        );
        pSchema.registerType(false, References.BIOME, () -> DSL.constType(namespacedString()));
        pSchema.registerType(false, References.ENTITY_NAME, () -> DSL.constType(namespacedString()));
        pSchema.registerType(false, References.POI_CHUNK, DSL::remainder);
        pSchema.registerType(false, References.WORLD_GEN_SETTINGS, DSL::remainder);
        pSchema.registerType(false, References.ENTITY_CHUNK, () -> DSL.optionalFields("Entities", DSL.list(References.ENTITY_TREE.in(pSchema))));
        pSchema.registerType(true, References.DATA_COMPONENTS, DSL::remainder);
        pSchema.registerType(
            true,
            References.VILLAGER_TRADE,
            () -> DSL.optionalFields(
                "buy", References.ITEM_STACK.in(pSchema), "buyB", References.ITEM_STACK.in(pSchema), "sell", References.ITEM_STACK.in(pSchema)
            )
        );
        pSchema.registerType(true, References.PARTICLE, () -> DSL.constType(DSL.string()));
        pSchema.registerType(true, References.TEXT_COMPONENT, () -> DSL.constType(DSL.string()));
        pSchema.registerType(
            true,
            References.ENTITY_EQUIPMENT,
            () -> DSL.and(
                DSL.optional(DSL.field("ArmorItems", DSL.list(References.ITEM_STACK.in(pSchema)))),
                DSL.optional(DSL.field("HandItems", DSL.list(References.ITEM_STACK.in(pSchema)))),
                DSL.optional(DSL.field("body_armor_item", References.ITEM_STACK.in(pSchema))),
                DSL.optional(DSL.field("saddle", References.ITEM_STACK.in(pSchema)))
            )
        );
    }
}
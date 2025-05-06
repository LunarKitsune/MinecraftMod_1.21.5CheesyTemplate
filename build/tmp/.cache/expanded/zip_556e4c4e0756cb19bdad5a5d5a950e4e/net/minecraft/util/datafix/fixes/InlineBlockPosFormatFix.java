package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.util.datafix.ExtraDataFixUtils;

public class InlineBlockPosFormatFix extends DataFix {
    public InlineBlockPosFormatFix(Schema pOutputSchema) {
        super(pOutputSchema, false);
    }

    @Override
    public TypeRewriteRule makeRule() {
        OpticFinder<?> opticfinder = this.entityFinder("minecraft:vex");
        OpticFinder<?> opticfinder1 = this.entityFinder("minecraft:phantom");
        OpticFinder<?> opticfinder2 = this.entityFinder("minecraft:turtle");
        List<OpticFinder<?>> list = List.of(
            this.entityFinder("minecraft:item_frame"),
            this.entityFinder("minecraft:glow_item_frame"),
            this.entityFinder("minecraft:painting"),
            this.entityFinder("minecraft:leash_knot")
        );
        return TypeRewriteRule.seq(
            this.fixTypeEverywhereTyped(
                "InlineBlockPosFormatFix - player",
                this.getInputSchema().getType(References.PLAYER),
                p_391680_ -> p_391680_.update(DSL.remainderFinder(), this::fixPlayer)
            ),
            this.fixTypeEverywhereTyped(
                "InlineBlockPosFormatFix - entity",
                this.getInputSchema().getType(References.ENTITY),
                p_397287_ -> {
                    p_397287_ = p_397287_.update(DSL.remainderFinder(), this::fixLivingEntity)
                        .updateTyped(opticfinder, p_393874_ -> p_393874_.update(DSL.remainderFinder(), this::fixVex))
                        .updateTyped(opticfinder1, p_392186_ -> p_392186_.update(DSL.remainderFinder(), this::fixPhantom))
                        .updateTyped(opticfinder2, p_391416_ -> p_391416_.update(DSL.remainderFinder(), this::fixTurtle));

                    for (OpticFinder<?> opticfinder3 : list) {
                        p_397287_ = p_397287_.updateTyped(opticfinder3, p_391296_ -> p_391296_.update(DSL.remainderFinder(), this::fixBlockAttached));
                    }

                    return p_397287_;
                }
            )
        );
    }

    private OpticFinder<?> entityFinder(String pEntityName) {
        return DSL.namedChoice(pEntityName, this.getInputSchema().getChoiceType(References.ENTITY, pEntityName));
    }

    private Dynamic<?> fixPlayer(Dynamic<?> pData) {
        pData = this.fixLivingEntity(pData);
        Optional<Number> optional = pData.get("SpawnX").asNumber().result();
        Optional<Number> optional1 = pData.get("SpawnY").asNumber().result();
        Optional<Number> optional2 = pData.get("SpawnZ").asNumber().result();
        if (optional.isPresent() && optional1.isPresent() && optional2.isPresent()) {
            Dynamic<?> dynamic = pData.createMap(
                Map.of(
                    pData.createString("pos"),
                    ExtraDataFixUtils.createBlockPos(pData, optional.get().intValue(), optional1.get().intValue(), optional2.get().intValue())
                )
            );
            dynamic = Dynamic.copyField(pData, "SpawnAngle", dynamic, "angle");
            dynamic = Dynamic.copyField(pData, "SpawnDimension", dynamic, "dimension");
            dynamic = Dynamic.copyField(pData, "SpawnForced", dynamic, "forced");
            pData = pData.remove("SpawnX").remove("SpawnY").remove("SpawnZ").remove("SpawnAngle").remove("SpawnDimension").remove("SpawnForced");
            pData = pData.set("respawn", dynamic);
        }

        Optional<? extends Dynamic<?>> optional3 = pData.get("enteredNetherPosition").result();
        if (optional3.isPresent()) {
            pData = pData.remove("enteredNetherPosition")
                .set(
                    "entered_nether_pos",
                    pData.createList(
                        Stream.of(
                            pData.createDouble(optional3.get().get("x").asDouble(0.0)),
                            pData.createDouble(optional3.get().get("y").asDouble(0.0)),
                            pData.createDouble(optional3.get().get("z").asDouble(0.0))
                        )
                    )
                );
        }

        return pData;
    }

    private Dynamic<?> fixLivingEntity(Dynamic<?> pData) {
        return ExtraDataFixUtils.fixInlineBlockPos(pData, "SleepingX", "SleepingY", "SleepingZ", "sleeping_pos");
    }

    private Dynamic<?> fixVex(Dynamic<?> pData) {
        return ExtraDataFixUtils.fixInlineBlockPos(pData.renameField("LifeTicks", "life_ticks"), "BoundX", "BoundY", "BoundZ", "bound_pos");
    }

    private Dynamic<?> fixPhantom(Dynamic<?> pData) {
        return ExtraDataFixUtils.fixInlineBlockPos(pData.renameField("Size", "size"), "AX", "AY", "AZ", "anchor_pos");
    }

    private Dynamic<?> fixTurtle(Dynamic<?> pData) {
        pData = pData.remove("TravelPosX").remove("TravelPosY").remove("TravelPosZ");
        pData = ExtraDataFixUtils.fixInlineBlockPos(pData, "HomePosX", "HomePosY", "HomePosZ", "home_pos");
        return pData.renameField("HasEgg", "has_egg");
    }

    private Dynamic<?> fixBlockAttached(Dynamic<?> pData) {
        return ExtraDataFixUtils.fixInlineBlockPos(pData, "TileX", "TileY", "TileZ", "block_pos");
    }
}
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class EntityItemFrameDirectionFix extends NamedEntityFix {
    public EntityItemFrameDirectionFix(Schema pOutputSchema, boolean pChangesType) {
        super(pOutputSchema, pChangesType, "EntityItemFrameDirectionFix", References.ENTITY, "minecraft:item_frame");
    }

    public Dynamic<?> fixTag(Dynamic<?> pTag) {
        return pTag.set("Facing", pTag.createByte(direction2dTo3d(pTag.get("Facing").asByte((byte)0))));
    }

    @Override
    protected Typed<?> fix(Typed<?> p_15473_) {
        return p_15473_.update(DSL.remainderFinder(), this::fixTag);
    }

    private static byte direction2dTo3d(byte pDirection2d) {
        switch (pDirection2d) {
            case 0:
                return 3;
            case 1:
                return 4;
            case 2:
            default:
                return 2;
            case 3:
                return 5;
        }
    }
}
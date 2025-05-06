package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class JukeboxTicksSinceSongStartedFix extends NamedEntityFix {
    public JukeboxTicksSinceSongStartedFix(Schema pOutputSchema) {
        super(pOutputSchema, false, "JukeboxTicksSinceSongStartedFix", References.BLOCK_ENTITY, "minecraft:jukebox");
    }

    public Dynamic<?> fixTag(Dynamic<?> pTag) {
        long i = pTag.get("TickCount").asLong(0L) - pTag.get("RecordStartTick").asLong(0L);
        Dynamic<?> dynamic = pTag.remove("IsPlaying").remove("TickCount").remove("RecordStartTick");
        return i > 0L ? dynamic.set("ticks_since_song_started", pTag.createLong(i)) : dynamic;
    }

    @Override
    protected Typed<?> fix(Typed<?> p_344432_) {
        return p_344432_.update(DSL.remainderFinder(), this::fixTag);
    }
}
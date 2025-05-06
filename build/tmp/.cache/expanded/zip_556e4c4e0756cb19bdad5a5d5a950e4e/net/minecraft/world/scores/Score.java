package net.minecraft.world.scores;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.NumberFormatTypes;

public class Score implements ReadOnlyScoreInfo {
    public static final MapCodec<Score> MAP_CODEC = RecordCodecBuilder.mapCodec(
        p_391144_ -> p_391144_.group(
                Codec.INT.optionalFieldOf("Score", 0).forGetter(Score::value),
                Codec.BOOL.optionalFieldOf("Locked", false).forGetter(Score::isLocked),
                ComponentSerialization.CODEC.optionalFieldOf("display").forGetter(p_391142_ -> Optional.ofNullable(p_391142_.display)),
                NumberFormatTypes.CODEC.optionalFieldOf("format").forGetter(p_391143_ -> Optional.ofNullable(p_391143_.numberFormat))
            )
            .apply(p_391144_, Score::new)
    );
    private int value;
    private boolean locked = true;
    @Nullable
    private Component display;
    @Nullable
    private NumberFormat numberFormat;

    public Score() {
    }

    private Score(int pValue, boolean pLocked, Optional<Component> pDisplay, Optional<NumberFormat> pNumberFormat) {
        this.value = pValue;
        this.locked = pLocked;
        this.display = pDisplay.orElse(null);
        this.numberFormat = pNumberFormat.orElse(null);
    }

    @Override
    public int value() {
        return this.value;
    }

    public void value(int pValue) {
        this.value = pValue;
    }

    @Override
    public boolean isLocked() {
        return this.locked;
    }

    public void setLocked(boolean pLocked) {
        this.locked = pLocked;
    }

    @Nullable
    public Component display() {
        return this.display;
    }

    public void display(@Nullable Component pDisplay) {
        this.display = pDisplay;
    }

    @Nullable
    @Override
    public NumberFormat numberFormat() {
        return this.numberFormat;
    }

    public void numberFormat(@Nullable NumberFormat pNumberFormat) {
        this.numberFormat = pNumberFormat;
    }
}
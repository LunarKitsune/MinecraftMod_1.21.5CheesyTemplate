package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.world.entity.player.Input;

public record InputPredicate(
    Optional<Boolean> forward,
    Optional<Boolean> backward,
    Optional<Boolean> left,
    Optional<Boolean> right,
    Optional<Boolean> jump,
    Optional<Boolean> sneak,
    Optional<Boolean> sprint
) {
    public static final Codec<InputPredicate> CODEC = RecordCodecBuilder.create(
        p_366376_ -> p_366376_.group(
                Codec.BOOL.optionalFieldOf("forward").forGetter(InputPredicate::forward),
                Codec.BOOL.optionalFieldOf("backward").forGetter(InputPredicate::backward),
                Codec.BOOL.optionalFieldOf("left").forGetter(InputPredicate::left),
                Codec.BOOL.optionalFieldOf("right").forGetter(InputPredicate::right),
                Codec.BOOL.optionalFieldOf("jump").forGetter(InputPredicate::jump),
                Codec.BOOL.optionalFieldOf("sneak").forGetter(InputPredicate::sneak),
                Codec.BOOL.optionalFieldOf("sprint").forGetter(InputPredicate::sprint)
            )
            .apply(p_366376_, InputPredicate::new)
    );

    public boolean matches(Input pInput) {
        return this.matches(this.forward, pInput.forward())
            && this.matches(this.backward, pInput.backward())
            && this.matches(this.left, pInput.left())
            && this.matches(this.right, pInput.right())
            && this.matches(this.jump, pInput.jump())
            && this.matches(this.sneak, pInput.shift())
            && this.matches(this.sprint, pInput.sprint());
    }

    private boolean matches(Optional<Boolean> pInput, boolean pExpectedValue) {
        return pInput.<Boolean>map(p_365276_ -> p_365276_ == pExpectedValue).orElse(true);
    }
}
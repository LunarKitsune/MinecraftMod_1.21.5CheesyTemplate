package net.minecraft.client.player;

import net.minecraft.client.Options;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class KeyboardInput extends ClientInput {
    private final Options options;

    public KeyboardInput(Options pOptions) {
        this.options = pOptions;
    }

    private static float calculateImpulse(boolean pInput, boolean pOtherInput) {
        if (pInput == pOtherInput) {
            return 0.0F;
        } else {
            return pInput ? 1.0F : -1.0F;
        }
    }

    @Override
    public void tick() {
        this.keyPresses = new Input(
            this.options.keyUp.isDown(),
            this.options.keyDown.isDown(),
            this.options.keyLeft.isDown(),
            this.options.keyRight.isDown(),
            this.options.keyJump.isDown(),
            this.options.keyShift.isDown(),
            this.options.keySprint.isDown()
        );
        float f = calculateImpulse(this.keyPresses.forward(), this.keyPresses.backward());
        float f1 = calculateImpulse(this.keyPresses.left(), this.keyPresses.right());
        this.moveVector = new Vec2(f1, f).normalized();
    }
}
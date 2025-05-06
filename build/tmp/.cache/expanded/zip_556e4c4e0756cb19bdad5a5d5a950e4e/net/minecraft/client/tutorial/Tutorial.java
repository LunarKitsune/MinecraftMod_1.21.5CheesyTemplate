package net.minecraft.client.tutorial;

import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.ClientInput;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class Tutorial {
    private final Minecraft minecraft;
    @Nullable
    private TutorialStepInstance instance;

    public Tutorial(Minecraft pMinecraft, Options pOptions) {
        this.minecraft = pMinecraft;
    }

    public void onInput(ClientInput pInput) {
        if (this.instance != null) {
            this.instance.onInput(pInput);
        }
    }

    public void onMouse(double pVelocityX, double pVelocityY) {
        if (this.instance != null) {
            this.instance.onMouse(pVelocityX, pVelocityY);
        }
    }

    public void onLookAt(@Nullable ClientLevel pLevel, @Nullable HitResult pResult) {
        if (this.instance != null && pResult != null && pLevel != null) {
            this.instance.onLookAt(pLevel, pResult);
        }
    }

    public void onDestroyBlock(ClientLevel pLevel, BlockPos pPos, BlockState pState, float pDiggingStage) {
        if (this.instance != null) {
            this.instance.onDestroyBlock(pLevel, pPos, pState, pDiggingStage);
        }
    }

    public void onOpenInventory() {
        if (this.instance != null) {
            this.instance.onOpenInventory();
        }
    }

    public void onGetItem(ItemStack pStack) {
        if (this.instance != null) {
            this.instance.onGetItem(pStack);
        }
    }

    public void stop() {
        if (this.instance != null) {
            this.instance.clear();
            this.instance = null;
        }
    }

    public void start() {
        if (this.instance != null) {
            this.stop();
        }

        this.instance = this.minecraft.options.tutorialStep.create(this);
    }

    public void tick() {
        if (this.instance != null) {
            if (this.minecraft.level != null) {
                this.instance.tick();
            } else {
                this.stop();
            }
        } else if (this.minecraft.level != null) {
            this.start();
        }
    }

    public void setStep(TutorialSteps pStep) {
        this.minecraft.options.tutorialStep = pStep;
        this.minecraft.options.save();
        if (this.instance != null) {
            this.instance.clear();
            this.instance = pStep.create(this);
        }
    }

    public Minecraft getMinecraft() {
        return this.minecraft;
    }

    public boolean isSurvival() {
        return this.minecraft.gameMode == null ? false : this.minecraft.gameMode.getPlayerMode() == GameType.SURVIVAL;
    }

    public static Component key(String pKeybind) {
        return Component.keybind("key." + pKeybind).withStyle(ChatFormatting.BOLD);
    }

    public void onInventoryAction(ItemStack pCarriedStack, ItemStack pSlottedStack, ClickAction pAction) {
    }
}
package net.minecraft.world.item;

import com.google.common.collect.Maps;
import java.util.Map;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;

public class DyeItem extends Item implements SignApplicator {
    private static final Map<DyeColor, DyeItem> ITEM_BY_COLOR = Maps.newEnumMap(DyeColor.class);
    private final DyeColor dyeColor;

    public DyeItem(DyeColor pDyeColor, Item.Properties pProperties) {
        super(pProperties);
        this.dyeColor = pDyeColor;
        ITEM_BY_COLOR.put(pDyeColor, this);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack pStack, Player pPlayer, LivingEntity pTarget, InteractionHand pHand) {
        if (pTarget instanceof Sheep sheep && sheep.isAlive() && !sheep.isSheared() && sheep.getColor() != this.dyeColor) {
            sheep.level().playSound(pPlayer, sheep, SoundEvents.DYE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
            if (!pPlayer.level().isClientSide) {
                sheep.setColor(this.dyeColor);
                pStack.shrink(1);
            }

            return InteractionResult.SUCCESS;
        } else {
            return InteractionResult.PASS;
        }
    }

    public DyeColor getDyeColor() {
        return this.dyeColor;
    }

    public static DyeItem byColor(DyeColor pColor) {
        return ITEM_BY_COLOR.get(pColor);
    }

    @Override
    public boolean tryApplyToSign(Level p_277691_, SignBlockEntity p_277488_, boolean p_277951_, Player p_277932_) {
        if (p_277488_.updateText(p_277649_ -> p_277649_.setColor(this.getDyeColor()), p_277951_)) {
            p_277691_.playSound(null, p_277488_.getBlockPos(), SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            return true;
        } else {
            return false;
        }
    }
}
package net.minecraft.client.color.item;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface ItemTintSource {
    int calculate(ItemStack pStack, @Nullable ClientLevel pLevel, @Nullable LivingEntity pEntity);

    MapCodec<? extends ItemTintSource> type();
}
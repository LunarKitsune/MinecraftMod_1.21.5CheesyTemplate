package net.minecraft.world.level;

import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.component.CustomData;

public interface Spawner {
    void setEntityId(EntityType<?> pEntityType, RandomSource pRandom);

    static void appendHoverText(CustomData pCustomData, Consumer<Component> pTooltipAdder, String pSpawnDataKey) {
        Component component = getSpawnEntityDisplayName(pCustomData, pSpawnDataKey);
        if (component != null) {
            pTooltipAdder.accept(component);
        } else {
            pTooltipAdder.accept(CommonComponents.EMPTY);
            pTooltipAdder.accept(Component.translatable("block.minecraft.spawner.desc1").withStyle(ChatFormatting.GRAY));
            pTooltipAdder.accept(CommonComponents.space().append(Component.translatable("block.minecraft.spawner.desc2").withStyle(ChatFormatting.BLUE)));
        }
    }

    @Nullable
    static Component getSpawnEntityDisplayName(CustomData pCustomData, String pSpawnDataKey) {
        return pCustomData.getUnsafe()
            .getCompound(pSpawnDataKey)
            .flatMap(p_390886_ -> p_390886_.getCompound("entity"))
            .flatMap(p_390887_ -> p_390887_.read("id", EntityType.CODEC))
            .map(p_311493_ -> Component.translatable(p_311493_.getDescriptionId()).withStyle(ChatFormatting.GRAY))
            .orElse(null);
    }
}
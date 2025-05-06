package net.minecraft.world.entity;

import javax.annotation.Nullable;
import net.minecraft.world.scores.PlayerTeam;

public record ConversionParams(ConversionType type, boolean keepEquipment, boolean preserveCanPickUpLoot, @Nullable PlayerTeam team) {
    public static ConversionParams single(Mob pMob, boolean pKeepEquipment, boolean pPreserveCanPickUpLoot) {
        return new ConversionParams(ConversionType.SINGLE, pKeepEquipment, pPreserveCanPickUpLoot, pMob.getTeam());
    }

    @FunctionalInterface
    public interface AfterConversion<T extends Mob> {
        void finalizeConversion(T pMob);
    }
}
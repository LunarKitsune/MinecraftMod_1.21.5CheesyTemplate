package net.minecraft.client.model;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record AdultAndBabyModelPair<T extends Model>(T adultModel, T babyModel) {
    public T getModel(boolean pIsBaby) {
        return pIsBaby ? this.babyModel : this.adultModel;
    }
}
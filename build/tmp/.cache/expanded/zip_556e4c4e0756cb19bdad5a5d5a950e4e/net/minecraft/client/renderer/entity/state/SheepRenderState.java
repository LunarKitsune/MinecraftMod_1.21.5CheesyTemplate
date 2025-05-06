package net.minecraft.client.renderer.entity.state;

import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SheepRenderState extends LivingEntityRenderState {
    public float headEatPositionScale;
    public float headEatAngleScale;
    public boolean isSheared;
    public DyeColor woolColor = DyeColor.WHITE;
    public int id;

    public int getWoolColor() {
        if (this.isJebSheep()) {
            int i = 25;
            int j = Mth.floor(this.ageInTicks);
            int k = j / 25 + this.id;
            int l = DyeColor.values().length;
            int i1 = k % l;
            int j1 = (k + 1) % l;
            float f = (j % 25 + Mth.frac(this.ageInTicks)) / 25.0F;
            int k1 = Sheep.getColor(DyeColor.byId(i1));
            int l1 = Sheep.getColor(DyeColor.byId(j1));
            return ARGB.lerp(f, k1, l1);
        } else {
            return Sheep.getColor(this.woolColor);
        }
    }

    public boolean isJebSheep() {
        return this.customName != null && "jeb_".equals(this.customName.getString());
    }
}
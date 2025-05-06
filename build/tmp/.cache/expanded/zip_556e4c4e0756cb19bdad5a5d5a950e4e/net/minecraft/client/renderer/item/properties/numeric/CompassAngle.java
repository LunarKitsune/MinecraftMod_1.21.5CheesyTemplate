package net.minecraft.client.renderer.item.properties.numeric;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CompassAngle implements RangeSelectItemModelProperty {
    public static final MapCodec<CompassAngle> MAP_CODEC = CompassAngleState.MAP_CODEC.xmap(CompassAngle::new, p_375840_ -> p_375840_.state);
    private final CompassAngleState state;

    public CompassAngle(boolean pWobble, CompassAngleState.CompassTarget pCompassTarget) {
        this(new CompassAngleState(pWobble, pCompassTarget));
    }

    private CompassAngle(CompassAngleState pState) {
        this.state = pState;
    }

    @Override
    public float get(ItemStack p_378698_, @Nullable ClientLevel p_375696_, @Nullable LivingEntity p_377419_, int p_377498_) {
        return this.state.get(p_378698_, p_375696_, p_377419_, p_377498_);
    }

    @Override
    public MapCodec<CompassAngle> type() {
        return MAP_CODEC;
    }
}
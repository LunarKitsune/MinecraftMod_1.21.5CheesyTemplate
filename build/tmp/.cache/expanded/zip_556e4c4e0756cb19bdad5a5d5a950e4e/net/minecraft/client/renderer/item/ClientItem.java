package net.minecraft.client.renderer.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import javax.annotation.Nullable;
import net.minecraft.util.RegistryContextSwapper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record ClientItem(ItemModel.Unbaked model, ClientItem.Properties properties, @Nullable RegistryContextSwapper registrySwapper) {
    public static final Codec<ClientItem> CODEC = RecordCodecBuilder.create(
        p_377165_ -> p_377165_.group(
                ItemModels.CODEC.fieldOf("model").forGetter(ClientItem::model), ClientItem.Properties.MAP_CODEC.forGetter(ClientItem::properties)
            )
            .apply(p_377165_, ClientItem::new)
    );

    public ClientItem(ItemModel.Unbaked pModel, ClientItem.Properties pProperties) {
        this(pModel, pProperties, null);
    }

    public ClientItem withRegistrySwapper(RegistryContextSwapper pRegistrySwapper) {
        return new ClientItem(this.model, this.properties, pRegistrySwapper);
    }

    @OnlyIn(Dist.CLIENT)
    public record Properties(boolean handAnimationOnSwap) {
        public static final ClientItem.Properties DEFAULT = new ClientItem.Properties(true);
        public static final MapCodec<ClientItem.Properties> MAP_CODEC = RecordCodecBuilder.mapCodec(
            p_376311_ -> p_376311_.group(Codec.BOOL.optionalFieldOf("hand_animation_on_swap", true).forGetter(ClientItem.Properties::handAnimationOnSwap))
                .apply(p_376311_, ClientItem.Properties::new)
        );
    }
}
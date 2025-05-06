package net.minecraft.advancements;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.core.ClientAsset;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public class DisplayInfo {
    public static final Codec<DisplayInfo> CODEC = RecordCodecBuilder.create(
        p_309653_ -> p_309653_.group(
                ItemStack.STRICT_CODEC.fieldOf("icon").forGetter(DisplayInfo::getIcon),
                ComponentSerialization.CODEC.fieldOf("title").forGetter(DisplayInfo::getTitle),
                ComponentSerialization.CODEC.fieldOf("description").forGetter(DisplayInfo::getDescription),
                ClientAsset.CODEC.optionalFieldOf("background").forGetter(DisplayInfo::getBackground),
                AdvancementType.CODEC.optionalFieldOf("frame", AdvancementType.TASK).forGetter(DisplayInfo::getType),
                Codec.BOOL.optionalFieldOf("show_toast", true).forGetter(DisplayInfo::shouldShowToast),
                Codec.BOOL.optionalFieldOf("announce_to_chat", true).forGetter(DisplayInfo::shouldAnnounceChat),
                Codec.BOOL.optionalFieldOf("hidden", false).forGetter(DisplayInfo::isHidden)
            )
            .apply(p_309653_, DisplayInfo::new)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, DisplayInfo> STREAM_CODEC = StreamCodec.ofMember(DisplayInfo::serializeToNetwork, DisplayInfo::fromNetwork);
    private final Component title;
    private final Component description;
    private final ItemStack icon;
    private final Optional<ClientAsset> background;
    private final AdvancementType type;
    private final boolean showToast;
    private final boolean announceChat;
    private final boolean hidden;
    private float x;
    private float y;

    public DisplayInfo(
        ItemStack pIcon,
        Component pTitle,
        Component pDescription,
        Optional<ClientAsset> pBackground,
        AdvancementType pType,
        boolean pShowToast,
        boolean pAnnounceChat,
        boolean pHidden
    ) {
        this.title = pTitle;
        this.description = pDescription;
        this.icon = pIcon;
        this.background = pBackground;
        this.type = pType;
        this.showToast = pShowToast;
        this.announceChat = pAnnounceChat;
        this.hidden = pHidden;
    }

    public void setLocation(float pX, float pY) {
        this.x = pX;
        this.y = pY;
    }

    public Component getTitle() {
        return this.title;
    }

    public Component getDescription() {
        return this.description;
    }

    public ItemStack getIcon() {
        return this.icon;
    }

    public Optional<ClientAsset> getBackground() {
        return this.background;
    }

    public AdvancementType getType() {
        return this.type;
    }

    public float getX() {
        return this.x;
    }

    public float getY() {
        return this.y;
    }

    public boolean shouldShowToast() {
        return this.showToast;
    }

    public boolean shouldAnnounceChat() {
        return this.announceChat;
    }

    public boolean isHidden() {
        return this.hidden;
    }

    private void serializeToNetwork(RegistryFriendlyByteBuf pBuffer) {
        ComponentSerialization.TRUSTED_STREAM_CODEC.encode(pBuffer, this.title);
        ComponentSerialization.TRUSTED_STREAM_CODEC.encode(pBuffer, this.description);
        ItemStack.STREAM_CODEC.encode(pBuffer, this.icon);
        pBuffer.writeEnum(this.type);
        int i = 0;
        if (this.background.isPresent()) {
            i |= 1;
        }

        if (this.showToast) {
            i |= 2;
        }

        if (this.hidden) {
            i |= 4;
        }

        pBuffer.writeInt(i);
        this.background.map(ClientAsset::id).ifPresent(pBuffer::writeResourceLocation);
        pBuffer.writeFloat(this.x);
        pBuffer.writeFloat(this.y);
    }

    private static DisplayInfo fromNetwork(RegistryFriendlyByteBuf pBuffer) {
        Component component = ComponentSerialization.TRUSTED_STREAM_CODEC.decode(pBuffer);
        Component component1 = ComponentSerialization.TRUSTED_STREAM_CODEC.decode(pBuffer);
        ItemStack itemstack = ItemStack.STREAM_CODEC.decode(pBuffer);
        AdvancementType advancementtype = pBuffer.readEnum(AdvancementType.class);
        int i = pBuffer.readInt();
        Optional<ClientAsset> optional = (i & 1) != 0 ? Optional.of(new ClientAsset(pBuffer.readResourceLocation())) : Optional.empty();
        boolean flag = (i & 2) != 0;
        boolean flag1 = (i & 4) != 0;
        DisplayInfo displayinfo = new DisplayInfo(itemstack, component, component1, optional, advancementtype, flag, false, flag1);
        displayinfo.setLocation(pBuffer.readFloat(), pBuffer.readFloat());
        return displayinfo;
    }
}
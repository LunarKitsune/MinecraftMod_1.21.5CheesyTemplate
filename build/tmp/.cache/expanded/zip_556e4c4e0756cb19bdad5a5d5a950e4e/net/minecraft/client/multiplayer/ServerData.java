package net.minecraft.client.multiplayer;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.PngInfo;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ServerData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_ICON_SIZE = 1024;
    public String name;
    public String ip;
    public Component status;
    public Component motd;
    @Nullable
    public ServerStatus.Players players;
    public long ping;
    public int protocol = SharedConstants.getCurrentVersion().getProtocolVersion();
    public Component version = Component.literal(SharedConstants.getCurrentVersion().getName());
    public List<Component> playerList = Collections.emptyList();
    private ServerData.ServerPackStatus packStatus = ServerData.ServerPackStatus.PROMPT;
    @Nullable
    private byte[] iconBytes;
    private ServerData.Type type;
    private ServerData.State state = ServerData.State.INITIAL;
    public net.minecraftforge.client.ExtendedServerListData forgeData = null;

    public ServerData(String pName, String pIp, ServerData.Type pType) {
        this.name = pName;
        this.ip = pIp;
        this.type = pType;
    }

    public CompoundTag write() {
        CompoundTag compoundtag = new CompoundTag();
        compoundtag.putString("name", this.name);
        compoundtag.putString("ip", this.ip);
        compoundtag.storeNullable("icon", ExtraCodecs.BASE64_STRING, this.iconBytes);
        compoundtag.store(ServerData.ServerPackStatus.FIELD_CODEC, this.packStatus);
        return compoundtag;
    }

    public ServerData.ServerPackStatus getResourcePackStatus() {
        return this.packStatus;
    }

    public void setResourcePackStatus(ServerData.ServerPackStatus pPackStatus) {
        this.packStatus = pPackStatus;
    }

    public static ServerData read(CompoundTag pNbtCompound) {
        ServerData serverdata = new ServerData(pNbtCompound.getStringOr("name", ""), pNbtCompound.getStringOr("ip", ""), ServerData.Type.OTHER);
        serverdata.setIconBytes(pNbtCompound.read("icon", ExtraCodecs.BASE64_STRING).orElse(null));
        serverdata.setResourcePackStatus(pNbtCompound.read(ServerData.ServerPackStatus.FIELD_CODEC).orElse(ServerData.ServerPackStatus.PROMPT));
        return serverdata;
    }

    @Nullable
    public byte[] getIconBytes() {
        return this.iconBytes;
    }

    public void setIconBytes(@Nullable byte[] pIconBytes) {
        this.iconBytes = pIconBytes;
    }

    public boolean isLan() {
        return this.type == ServerData.Type.LAN;
    }

    public boolean isRealm() {
        return this.type == ServerData.Type.REALM;
    }

    public ServerData.Type type() {
        return this.type;
    }

    public void copyNameIconFrom(ServerData pOther) {
        this.ip = pOther.ip;
        this.name = pOther.name;
        this.iconBytes = pOther.iconBytes;
    }

    public void copyFrom(ServerData pServerData) {
        this.copyNameIconFrom(pServerData);
        this.setResourcePackStatus(pServerData.getResourcePackStatus());
        this.type = pServerData.type;
    }

    public ServerData.State state() {
        return this.state;
    }

    public void setState(ServerData.State pState) {
        this.state = pState;
    }

    @Nullable
    public static byte[] validateIcon(@Nullable byte[] pIcon) {
        if (pIcon != null) {
            try {
                PngInfo pnginfo = PngInfo.fromBytes(pIcon);
                if (pnginfo.width() <= 1024 && pnginfo.height() <= 1024) {
                    return pIcon;
                }
            } catch (IOException ioexception) {
                LOGGER.warn("Failed to decode server icon", (Throwable)ioexception);
            }
        }

        return null;
    }

    @OnlyIn(Dist.CLIENT)
    public static enum ServerPackStatus {
        ENABLED("enabled"),
        DISABLED("disabled"),
        PROMPT("prompt");

        public static final MapCodec<ServerData.ServerPackStatus> FIELD_CODEC = Codec.BOOL
            .optionalFieldOf("acceptTextures")
            .xmap(p_391336_ -> p_391336_.<ServerData.ServerPackStatus>map(p_396401_ -> p_396401_ ? ENABLED : DISABLED).orElse(PROMPT), p_394049_ -> {
                return switch (p_394049_) {
                    case ENABLED -> Optional.of(true);
                    case DISABLED -> Optional.of(false);
                    case PROMPT -> Optional.empty();
                };
            });
        private final Component name;

        private ServerPackStatus(final String pName) {
            this.name = Component.translatable("addServer.resourcePack." + pName);
        }

        public Component getName() {
            return this.name;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static enum State {
        INITIAL,
        PINGING,
        UNREACHABLE,
        INCOMPATIBLE,
        SUCCESSFUL;
    }

    @OnlyIn(Dist.CLIENT)
    public static enum Type {
        LAN,
        REALM,
        OTHER;
    }
}

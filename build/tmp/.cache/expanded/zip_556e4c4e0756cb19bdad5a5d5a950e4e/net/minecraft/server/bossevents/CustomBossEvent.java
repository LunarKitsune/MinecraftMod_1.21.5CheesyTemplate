package net.minecraft.server.bossevents;

import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;

public class CustomBossEvent extends ServerBossEvent {
    private static final int DEFAULT_MAX = 100;
    private final ResourceLocation id;
    private final Set<UUID> players = Sets.newHashSet();
    private int value;
    private int max = 100;

    public CustomBossEvent(ResourceLocation pId, Component pName) {
        super(pName, BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);
        this.id = pId;
        this.setProgress(0.0F);
    }

    public ResourceLocation getTextId() {
        return this.id;
    }

    @Override
    public void addPlayer(ServerPlayer pPlayer) {
        super.addPlayer(pPlayer);
        this.players.add(pPlayer.getUUID());
    }

    public void addOfflinePlayer(UUID pPlayer) {
        this.players.add(pPlayer);
    }

    @Override
    public void removePlayer(ServerPlayer pPlayer) {
        super.removePlayer(pPlayer);
        this.players.remove(pPlayer.getUUID());
    }

    @Override
    public void removeAllPlayers() {
        super.removeAllPlayers();
        this.players.clear();
    }

    public int getValue() {
        return this.value;
    }

    public int getMax() {
        return this.max;
    }

    public void setValue(int pValue) {
        this.value = pValue;
        this.setProgress(Mth.clamp((float)pValue / this.max, 0.0F, 1.0F));
    }

    public void setMax(int pMax) {
        this.max = pMax;
        this.setProgress(Mth.clamp((float)this.value / pMax, 0.0F, 1.0F));
    }

    public final Component getDisplayName() {
        return ComponentUtils.wrapInSquareBrackets(this.getName())
            .withStyle(
                p_389944_ -> p_389944_.withColor(this.getColor().getFormatting())
                    .withHoverEvent(new HoverEvent.ShowText(Component.literal(this.getTextId().toString())))
                    .withInsertion(this.getTextId().toString())
            );
    }

    public boolean setPlayers(Collection<ServerPlayer> pServerPlayerList) {
        Set<UUID> set = Sets.newHashSet();
        Set<ServerPlayer> set1 = Sets.newHashSet();

        for (UUID uuid : this.players) {
            boolean flag = false;

            for (ServerPlayer serverplayer : pServerPlayerList) {
                if (serverplayer.getUUID().equals(uuid)) {
                    flag = true;
                    break;
                }
            }

            if (!flag) {
                set.add(uuid);
            }
        }

        for (ServerPlayer serverplayer1 : pServerPlayerList) {
            boolean flag1 = false;

            for (UUID uuid2 : this.players) {
                if (serverplayer1.getUUID().equals(uuid2)) {
                    flag1 = true;
                    break;
                }
            }

            if (!flag1) {
                set1.add(serverplayer1);
            }
        }

        for (UUID uuid1 : set) {
            for (ServerPlayer serverplayer3 : this.getPlayers()) {
                if (serverplayer3.getUUID().equals(uuid1)) {
                    this.removePlayer(serverplayer3);
                    break;
                }
            }

            this.players.remove(uuid1);
        }

        for (ServerPlayer serverplayer2 : set1) {
            this.addPlayer(serverplayer2);
        }

        return !set.isEmpty() || !set1.isEmpty();
    }

    public static CustomBossEvent load(ResourceLocation pId, CustomBossEvent.Packed pPacked) {
        CustomBossEvent custombossevent = new CustomBossEvent(pId, pPacked.name);
        custombossevent.setVisible(pPacked.visible);
        custombossevent.setValue(pPacked.value);
        custombossevent.setMax(pPacked.max);
        custombossevent.setColor(pPacked.color);
        custombossevent.setOverlay(pPacked.overlay);
        custombossevent.setDarkenScreen(pPacked.darkenScreen);
        custombossevent.setPlayBossMusic(pPacked.playBossMusic);
        custombossevent.setCreateWorldFog(pPacked.createWorldFog);
        pPacked.players.forEach(custombossevent::addOfflinePlayer);
        return custombossevent;
    }

    public CustomBossEvent.Packed pack() {
        return new CustomBossEvent.Packed(
            this.getName(),
            this.isVisible(),
            this.getValue(),
            this.getMax(),
            this.getColor(),
            this.getOverlay(),
            this.shouldDarkenScreen(),
            this.shouldPlayBossMusic(),
            this.shouldCreateWorldFog(),
            Set.copyOf(this.players)
        );
    }

    public void onPlayerConnect(ServerPlayer pPlayer) {
        if (this.players.contains(pPlayer.getUUID())) {
            this.addPlayer(pPlayer);
        }
    }

    public void onPlayerDisconnect(ServerPlayer pPlayer) {
        super.removePlayer(pPlayer);
    }

    public record Packed(
        Component name,
        boolean visible,
        int value,
        int max,
        BossEvent.BossBarColor color,
        BossEvent.BossBarOverlay overlay,
        boolean darkenScreen,
        boolean playBossMusic,
        boolean createWorldFog,
        Set<UUID> players
    ) {
        public static final Codec<CustomBossEvent.Packed> CODEC = RecordCodecBuilder.create(
            p_397740_ -> p_397740_.group(
                    ComponentSerialization.CODEC.fieldOf("Name").forGetter(CustomBossEvent.Packed::name),
                    Codec.BOOL.optionalFieldOf("Visible", false).forGetter(CustomBossEvent.Packed::visible),
                    Codec.INT.optionalFieldOf("Value", 0).forGetter(CustomBossEvent.Packed::value),
                    Codec.INT.optionalFieldOf("Max", 100).forGetter(CustomBossEvent.Packed::max),
                    BossEvent.BossBarColor.CODEC.optionalFieldOf("Color", BossEvent.BossBarColor.WHITE).forGetter(CustomBossEvent.Packed::color),
                    BossEvent.BossBarOverlay.CODEC
                        .optionalFieldOf("Overlay", BossEvent.BossBarOverlay.PROGRESS)
                        .forGetter(CustomBossEvent.Packed::overlay),
                    Codec.BOOL.optionalFieldOf("DarkenScreen", false).forGetter(CustomBossEvent.Packed::darkenScreen),
                    Codec.BOOL.optionalFieldOf("PlayBossMusic", false).forGetter(CustomBossEvent.Packed::playBossMusic),
                    Codec.BOOL.optionalFieldOf("CreateWorldFog", false).forGetter(CustomBossEvent.Packed::createWorldFog),
                    UUIDUtil.CODEC_SET.optionalFieldOf("Players", Set.of()).forGetter(CustomBossEvent.Packed::players)
                )
                .apply(p_397740_, CustomBossEvent.Packed::new)
        );
    }
}
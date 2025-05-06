package net.minecraft.world.level.block.entity.vault;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public class VaultSharedData {
    static final String TAG_NAME = "shared_data";
    static Codec<VaultSharedData> CODEC = RecordCodecBuilder.create(
        p_332167_ -> p_332167_.group(
                ItemStack.lenientOptionalFieldOf("display_item").forGetter(p_328885_ -> p_328885_.displayItem),
                UUIDUtil.CODEC_LINKED_SET.lenientOptionalFieldOf("connected_players", Set.of()).forGetter(p_333733_ -> p_333733_.connectedPlayers),
                Codec.DOUBLE.lenientOptionalFieldOf("connected_particles_range", VaultConfig.DEFAULT.deactivationRange()).forGetter(p_333675_ -> p_333675_.connectedParticlesRange)
            )
            .apply(p_332167_, VaultSharedData::new)
    );
    private ItemStack displayItem = ItemStack.EMPTY;
    private Set<UUID> connectedPlayers = new ObjectLinkedOpenHashSet<>();
    private double connectedParticlesRange = VaultConfig.DEFAULT.deactivationRange();
    boolean isDirty;

    VaultSharedData(ItemStack pDisplayItem, Set<UUID> pConnectedPlayers, double pConnectedParticlesRange) {
        this.displayItem = pDisplayItem;
        this.connectedPlayers.addAll(pConnectedPlayers);
        this.connectedParticlesRange = pConnectedParticlesRange;
    }

    VaultSharedData() {
    }

    public ItemStack getDisplayItem() {
        return this.displayItem;
    }

    public boolean hasDisplayItem() {
        return !this.displayItem.isEmpty();
    }

    public void setDisplayItem(ItemStack pDisplayItem) {
        if (!ItemStack.matches(this.displayItem, pDisplayItem)) {
            this.displayItem = pDisplayItem.copy();
            this.markDirty();
        }
    }

    boolean hasConnectedPlayers() {
        return !this.connectedPlayers.isEmpty();
    }

    Set<UUID> getConnectedPlayers() {
        return this.connectedPlayers;
    }

    double connectedParticlesRange() {
        return this.connectedParticlesRange;
    }

    void updateConnectedPlayersWithinRange(ServerLevel pLevel, BlockPos pPos, VaultServerData pServerData, VaultConfig pConfig, double pDeactivationRange) {
        Set<UUID> set = pConfig.playerDetector()
            .detect(pLevel, pConfig.entitySelector(), pPos, pDeactivationRange, false)
            .stream()
            .filter(p_335249_ -> !pServerData.getRewardedPlayers().contains(p_335249_))
            .collect(Collectors.toSet());
        if (!this.connectedPlayers.equals(set)) {
            this.connectedPlayers = set;
            this.markDirty();
        }
    }

    private void markDirty() {
        this.isDirty = true;
    }

    void set(VaultSharedData pOther) {
        this.displayItem = pOther.displayItem;
        this.connectedPlayers = pOther.connectedPlayers;
        this.connectedParticlesRange = pOther.connectedParticlesRange;
    }
}
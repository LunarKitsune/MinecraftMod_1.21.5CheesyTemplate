package net.minecraft.server;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundResetScorePacket;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.ScoreboardSaveData;

public class ServerScoreboard extends Scoreboard {
    public static final SavedDataType<ScoreboardSaveData> TYPE = new SavedDataType<>(
        "scoreboard", p_391457_ -> p_391457_.levelOrThrow().getScoreboard().createData(), p_396054_ -> {
            ServerScoreboard serverscoreboard = p_396054_.levelOrThrow().getScoreboard();
            return ScoreboardSaveData.Packed.CODEC.xmap(serverscoreboard::createData, ScoreboardSaveData::pack);
        }, DataFixTypes.SAVED_DATA_SCOREBOARD
    );
    private final MinecraftServer server;
    private final Set<Objective> trackedObjectives = Sets.newHashSet();
    private final List<Runnable> dirtyListeners = Lists.newArrayList();

    public ServerScoreboard(MinecraftServer pServer) {
        this.server = pServer;
    }

    @Override
    protected void onScoreChanged(ScoreHolder p_311591_, Objective p_310366_, Score p_136206_) {
        super.onScoreChanged(p_311591_, p_310366_, p_136206_);
        if (this.trackedObjectives.contains(p_310366_)) {
            this.server
                .getPlayerList()
                .broadcastAll(
                    new ClientboundSetScorePacket(
                        p_311591_.getScoreboardName(),
                        p_310366_.getName(),
                        p_136206_.value(),
                        Optional.ofNullable(p_136206_.display()),
                        Optional.ofNullable(p_136206_.numberFormat())
                    )
                );
        }

        this.setDirty();
    }

    @Override
    protected void onScoreLockChanged(ScoreHolder p_309548_, Objective p_312571_) {
        super.onScoreLockChanged(p_309548_, p_312571_);
        this.setDirty();
    }

    @Override
    public void onPlayerRemoved(ScoreHolder p_310662_) {
        super.onPlayerRemoved(p_310662_);
        this.server.getPlayerList().broadcastAll(new ClientboundResetScorePacket(p_310662_.getScoreboardName(), null));
        this.setDirty();
    }

    @Override
    public void onPlayerScoreRemoved(ScoreHolder p_310122_, Objective p_136213_) {
        super.onPlayerScoreRemoved(p_310122_, p_136213_);
        if (this.trackedObjectives.contains(p_136213_)) {
            this.server.getPlayerList().broadcastAll(new ClientboundResetScorePacket(p_310122_.getScoreboardName(), p_136213_.getName()));
        }

        this.setDirty();
    }

    @Override
    public void setDisplayObjective(DisplaySlot p_297629_, @Nullable Objective p_136200_) {
        Objective objective = this.getDisplayObjective(p_297629_);
        super.setDisplayObjective(p_297629_, p_136200_);
        if (objective != p_136200_ && objective != null) {
            if (this.getObjectiveDisplaySlotCount(objective) > 0) {
                this.server.getPlayerList().broadcastAll(new ClientboundSetDisplayObjectivePacket(p_297629_, p_136200_));
            } else {
                this.stopTrackingObjective(objective);
            }
        }

        if (p_136200_ != null) {
            if (this.trackedObjectives.contains(p_136200_)) {
                this.server.getPlayerList().broadcastAll(new ClientboundSetDisplayObjectivePacket(p_297629_, p_136200_));
            } else {
                this.startTrackingObjective(p_136200_);
            }
        }

        this.setDirty();
    }

    @Override
    public boolean addPlayerToTeam(String p_136215_, PlayerTeam p_136216_) {
        if (super.addPlayerToTeam(p_136215_, p_136216_)) {
            this.server.getPlayerList().broadcastAll(ClientboundSetPlayerTeamPacket.createPlayerPacket(p_136216_, p_136215_, ClientboundSetPlayerTeamPacket.Action.ADD));
            this.setDirty();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void removePlayerFromTeam(String pUsername, PlayerTeam pPlayerTeam) {
        super.removePlayerFromTeam(pUsername, pPlayerTeam);
        this.server.getPlayerList().broadcastAll(ClientboundSetPlayerTeamPacket.createPlayerPacket(pPlayerTeam, pUsername, ClientboundSetPlayerTeamPacket.Action.REMOVE));
        this.setDirty();
    }

    @Override
    public void onObjectiveAdded(Objective pObjective) {
        super.onObjectiveAdded(pObjective);
        this.setDirty();
    }

    @Override
    public void onObjectiveChanged(Objective pObjective) {
        super.onObjectiveChanged(pObjective);
        if (this.trackedObjectives.contains(pObjective)) {
            this.server.getPlayerList().broadcastAll(new ClientboundSetObjectivePacket(pObjective, 2));
        }

        this.setDirty();
    }

    @Override
    public void onObjectiveRemoved(Objective pObjective) {
        super.onObjectiveRemoved(pObjective);
        if (this.trackedObjectives.contains(pObjective)) {
            this.stopTrackingObjective(pObjective);
        }

        this.setDirty();
    }

    @Override
    public void onTeamAdded(PlayerTeam pPlayerTeam) {
        super.onTeamAdded(pPlayerTeam);
        this.server.getPlayerList().broadcastAll(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(pPlayerTeam, true));
        this.setDirty();
    }

    @Override
    public void onTeamChanged(PlayerTeam pPlayerTeam) {
        super.onTeamChanged(pPlayerTeam);
        this.server.getPlayerList().broadcastAll(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(pPlayerTeam, false));
        this.setDirty();
    }

    @Override
    public void onTeamRemoved(PlayerTeam pPlayerTeam) {
        super.onTeamRemoved(pPlayerTeam);
        this.server.getPlayerList().broadcastAll(ClientboundSetPlayerTeamPacket.createRemovePacket(pPlayerTeam));
        this.setDirty();
    }

    public void addDirtyListener(Runnable pRunnable) {
        this.dirtyListeners.add(pRunnable);
    }

    protected void setDirty() {
        for (Runnable runnable : this.dirtyListeners) {
            runnable.run();
        }
    }

    public List<Packet<?>> getStartTrackingPackets(Objective pObjective) {
        List<Packet<?>> list = Lists.newArrayList();
        list.add(new ClientboundSetObjectivePacket(pObjective, 0));

        for (DisplaySlot displayslot : DisplaySlot.values()) {
            if (this.getDisplayObjective(displayslot) == pObjective) {
                list.add(new ClientboundSetDisplayObjectivePacket(displayslot, pObjective));
            }
        }

        for (PlayerScoreEntry playerscoreentry : this.listPlayerScores(pObjective)) {
            list.add(
                new ClientboundSetScorePacket(
                    playerscoreentry.owner(),
                    pObjective.getName(),
                    playerscoreentry.value(),
                    Optional.ofNullable(playerscoreentry.display()),
                    Optional.ofNullable(playerscoreentry.numberFormatOverride())
                )
            );
        }

        return list;
    }

    public void startTrackingObjective(Objective pObjective) {
        List<Packet<?>> list = this.getStartTrackingPackets(pObjective);

        for (ServerPlayer serverplayer : this.server.getPlayerList().getPlayers()) {
            for (Packet<?> packet : list) {
                serverplayer.connection.send(packet);
            }
        }

        this.trackedObjectives.add(pObjective);
    }

    public List<Packet<?>> getStopTrackingPackets(Objective pObjective) {
        List<Packet<?>> list = Lists.newArrayList();
        list.add(new ClientboundSetObjectivePacket(pObjective, 1));

        for (DisplaySlot displayslot : DisplaySlot.values()) {
            if (this.getDisplayObjective(displayslot) == pObjective) {
                list.add(new ClientboundSetDisplayObjectivePacket(displayslot, pObjective));
            }
        }

        return list;
    }

    public void stopTrackingObjective(Objective pObjective) {
        List<Packet<?>> list = this.getStopTrackingPackets(pObjective);

        for (ServerPlayer serverplayer : this.server.getPlayerList().getPlayers()) {
            for (Packet<?> packet : list) {
                serverplayer.connection.send(packet);
            }
        }

        this.trackedObjectives.remove(pObjective);
    }

    public int getObjectiveDisplaySlotCount(Objective pObjective) {
        int i = 0;

        for (DisplaySlot displayslot : DisplaySlot.values()) {
            if (this.getDisplayObjective(displayslot) == pObjective) {
                i++;
            }
        }

        return i;
    }

    private ScoreboardSaveData createData() {
        ScoreboardSaveData scoreboardsavedata = new ScoreboardSaveData(this);
        this.addDirtyListener(scoreboardsavedata::setDirty);
        return scoreboardsavedata;
    }

    private ScoreboardSaveData createData(ScoreboardSaveData.Packed pData) {
        ScoreboardSaveData scoreboardsavedata = this.createData();
        scoreboardsavedata.loadFrom(pData);
        return scoreboardsavedata;
    }

    public static enum Method {
        CHANGE,
        REMOVE;
    }
}
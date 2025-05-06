package net.minecraft.client.multiplayer;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.thread.ConsecutiveExecutor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ServerList {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ConsecutiveExecutor IO_EXECUTOR = new ConsecutiveExecutor(Util.backgroundExecutor(), "server-list-io");
    private static final int MAX_HIDDEN_SERVERS = 16;
    private final Minecraft minecraft;
    private final List<ServerData> serverList = Lists.newArrayList();
    private final List<ServerData> hiddenServerList = Lists.newArrayList();

    public ServerList(Minecraft pMinecraft) {
        this.minecraft = pMinecraft;
    }

    public void load() {
        try {
            this.serverList.clear();
            this.hiddenServerList.clear();
            CompoundTag compoundtag = NbtIo.read(this.minecraft.gameDirectory.toPath().resolve("servers.dat"));
            if (compoundtag == null) {
                return;
            }

            compoundtag.getListOrEmpty("servers").compoundStream().forEach(p_389344_ -> {
                ServerData serverdata = ServerData.read(p_389344_);
                if (p_389344_.getBooleanOr("hidden", false)) {
                    this.hiddenServerList.add(serverdata);
                } else {
                    this.serverList.add(serverdata);
                }
            });
        } catch (Exception exception) {
            LOGGER.error("Couldn't load server list", (Throwable)exception);
        }
    }

    public void save() {
        try {
            ListTag listtag = new ListTag();

            for (ServerData serverdata : this.serverList) {
                CompoundTag compoundtag = serverdata.write();
                compoundtag.putBoolean("hidden", false);
                listtag.add(compoundtag);
            }

            for (ServerData serverdata1 : this.hiddenServerList) {
                CompoundTag compoundtag2 = serverdata1.write();
                compoundtag2.putBoolean("hidden", true);
                listtag.add(compoundtag2);
            }

            CompoundTag compoundtag1 = new CompoundTag();
            compoundtag1.put("servers", listtag);
            Path path2 = this.minecraft.gameDirectory.toPath();
            Path path3 = Files.createTempFile(path2, "servers", ".dat");
            NbtIo.write(compoundtag1, path3);
            Path path = path2.resolve("servers.dat_old");
            Path path1 = path2.resolve("servers.dat");
            Util.safeReplaceFile(path1, path3, path);
        } catch (Exception exception) {
            LOGGER.error("Couldn't save server list", (Throwable)exception);
        }
    }

    public ServerData get(int pIndex) {
        return this.serverList.get(pIndex);
    }

    @Nullable
    public ServerData get(String pIp) {
        for (ServerData serverdata : this.serverList) {
            if (serverdata.ip.equals(pIp)) {
                return serverdata;
            }
        }

        for (ServerData serverdata1 : this.hiddenServerList) {
            if (serverdata1.ip.equals(pIp)) {
                return serverdata1;
            }
        }

        return null;
    }

    @Nullable
    public ServerData unhide(String pIp) {
        for (int i = 0; i < this.hiddenServerList.size(); i++) {
            ServerData serverdata = this.hiddenServerList.get(i);
            if (serverdata.ip.equals(pIp)) {
                this.hiddenServerList.remove(i);
                this.serverList.add(serverdata);
                return serverdata;
            }
        }

        return null;
    }

    public void remove(ServerData pServerData) {
        if (!this.serverList.remove(pServerData)) {
            this.hiddenServerList.remove(pServerData);
        }
    }

    public void add(ServerData pServer, boolean pHidden) {
        if (pHidden) {
            this.hiddenServerList.add(0, pServer);

            while (this.hiddenServerList.size() > 16) {
                this.hiddenServerList.remove(this.hiddenServerList.size() - 1);
            }
        } else {
            this.serverList.add(pServer);
        }
    }

    public int size() {
        return this.serverList.size();
    }

    public void swap(int pPos1, int pPos2) {
        ServerData serverdata = this.get(pPos1);
        this.serverList.set(pPos1, this.get(pPos2));
        this.serverList.set(pPos2, serverdata);
        this.save();
    }

    public void replace(int pIndex, ServerData pServer) {
        this.serverList.set(pIndex, pServer);
    }

    private static boolean set(ServerData pServer, List<ServerData> pServerList) {
        for (int i = 0; i < pServerList.size(); i++) {
            ServerData serverdata = pServerList.get(i);
            if (Objects.equals(serverdata.name, pServer.name) && serverdata.ip.equals(pServer.ip)) {
                pServerList.set(i, pServer);
                return true;
            }
        }

        return false;
    }

    public static void saveSingleServer(ServerData pServer) {
        IO_EXECUTOR.schedule(() -> {
            ServerList serverlist = new ServerList(Minecraft.getInstance());
            serverlist.load();
            if (!set(pServer, serverlist.serverList)) {
                set(pServer, serverlist.hiddenServerList);
            }

            serverlist.save();
        });
    }
}
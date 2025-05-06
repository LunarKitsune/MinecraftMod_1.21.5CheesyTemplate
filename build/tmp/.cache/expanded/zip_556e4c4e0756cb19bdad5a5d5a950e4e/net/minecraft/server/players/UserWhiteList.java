package net.minecraft.server.players;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import java.io.File;
import java.util.Objects;

public class UserWhiteList extends StoredUserList<GameProfile, UserWhiteListEntry> {
    public UserWhiteList(File p_11449_) {
        super(p_11449_);
    }

    @Override
    protected StoredUserEntry<GameProfile> createEntry(JsonObject pEntryData) {
        return new UserWhiteListEntry(pEntryData);
    }

    public boolean isWhiteListed(GameProfile pProfile) {
        return this.contains(pProfile);
    }

    @Override
    public String[] getUserList() {
        return this.getEntries().stream().map(StoredUserEntry::getUser).filter(Objects::nonNull).map(GameProfile::getName).toArray(String[]::new);
    }

    protected String getKeyForUser(GameProfile pObj) {
        return pObj.getId().toString();
    }
}
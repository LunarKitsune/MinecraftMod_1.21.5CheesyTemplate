package net.minecraft.client;

import com.mojang.serialization.Codec;
import net.minecraft.util.OptionEnum;
import net.minecraft.util.StringRepresentable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum InactivityFpsLimit implements OptionEnum, StringRepresentable {
    MINIMIZED(0, "minimized", "options.inactivityFpsLimit.minimized"),
    AFK(1, "afk", "options.inactivityFpsLimit.afk");

    public static final Codec<InactivityFpsLimit> CODEC = StringRepresentable.fromEnum(InactivityFpsLimit::values);
    private final int id;
    private final String serializedName;
    private final String key;

    private InactivityFpsLimit(final int pId, final String pSerializedName, final String pKey) {
        this.id = pId;
        this.serializedName = pSerializedName;
        this.key = pKey;
    }

    @Override
    public int getId() {
        return this.id;
    }

    @Override
    public String getKey() {
        return this.key;
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }
}
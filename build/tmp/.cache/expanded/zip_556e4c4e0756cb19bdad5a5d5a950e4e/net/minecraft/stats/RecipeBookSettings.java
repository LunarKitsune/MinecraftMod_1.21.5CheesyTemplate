package net.minecraft.stats;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.UnaryOperator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.inventory.RecipeBookType;

public final class RecipeBookSettings {
    public static final StreamCodec<FriendlyByteBuf, RecipeBookSettings> STREAM_CODEC = StreamCodec.ofMember(
        RecipeBookSettings::write, RecipeBookSettings::read
    );
    private static final Map<RecipeBookType, Pair<String, String>> TAG_FIELDS = new java.util.HashMap<>(ImmutableMap.of(
        RecipeBookType.CRAFTING,
        Pair.of("isGuiOpen", "isFilteringCraftable"),
        RecipeBookType.FURNACE,
        Pair.of("isFurnaceGuiOpen", "isFurnaceFilteringCraftable"),
        RecipeBookType.BLAST_FURNACE,
        Pair.of("isBlastingFurnaceGuiOpen", "isBlastingFurnaceFilteringCraftable"),
        RecipeBookType.SMOKER,
        Pair.of("isSmokerGuiOpen", "isSmokerFilteringCraftable")
    ));
    private final Map<RecipeBookType, RecipeBookSettings.TypeSettings> states;

    private RecipeBookSettings(Map<RecipeBookType, RecipeBookSettings.TypeSettings> pStates) {
        this.states = pStates;
    }

    public RecipeBookSettings() {
        this(new EnumMap<>(RecipeBookType.class));
    }

    private RecipeBookSettings.TypeSettings getSettings(RecipeBookType pType) {
        return this.states.getOrDefault(pType, RecipeBookSettings.TypeSettings.DEFAULT);
    }

    private void updateSettings(RecipeBookType pType, UnaryOperator<RecipeBookSettings.TypeSettings> pUpdater) {
        this.states.compute(pType, (p_358767_, p_358768_) -> {
            if (p_358768_ == null) {
                p_358768_ = RecipeBookSettings.TypeSettings.DEFAULT;
            }

            p_358768_ = pUpdater.apply(p_358768_);
            if (p_358768_.equals(RecipeBookSettings.TypeSettings.DEFAULT)) {
                p_358768_ = null;
            }

            return p_358768_;
        });
    }

    public boolean isOpen(RecipeBookType pBookType) {
        return this.getSettings(pBookType).open;
    }

    public void setOpen(RecipeBookType pBookType, boolean pOpen) {
        this.updateSettings(pBookType, p_358758_ -> p_358758_.setOpen(pOpen));
    }

    public boolean isFiltering(RecipeBookType pBookType) {
        return this.getSettings(pBookType).filtering;
    }

    public void setFiltering(RecipeBookType pBookType, boolean pFiltering) {
        this.updateSettings(pBookType, p_358756_ -> p_358756_.setFiltering(pFiltering));
    }

    private static RecipeBookSettings read(FriendlyByteBuf pBuffer) {
        Map<RecipeBookType, RecipeBookSettings.TypeSettings> map = new EnumMap<>(RecipeBookType.class);

        for (RecipeBookType recipebooktype : RecipeBookType.values()) {
            boolean flag = pBuffer.readBoolean();
            boolean flag1 = pBuffer.readBoolean();
            if (flag || flag1) {
                map.put(recipebooktype, new RecipeBookSettings.TypeSettings(flag, flag1));
            }
        }

        return new RecipeBookSettings(map);
    }

    private void write(FriendlyByteBuf pBuffer) {
        for (RecipeBookType recipebooktype : RecipeBookType.values()) {
            RecipeBookSettings.TypeSettings recipebooksettings$typesettings = this.states
                .getOrDefault(recipebooktype, RecipeBookSettings.TypeSettings.DEFAULT);
            pBuffer.writeBoolean(recipebooksettings$typesettings.open);
            pBuffer.writeBoolean(recipebooksettings$typesettings.filtering);
        }
    }

    public static RecipeBookSettings read(CompoundTag pTag) {
        Map<RecipeBookType, RecipeBookSettings.TypeSettings> map = new EnumMap<>(RecipeBookType.class);
        TAG_FIELDS.forEach((p_390193_, p_390194_) -> {
            boolean flag = pTag.getBooleanOr(p_390194_.getFirst(), RecipeBookSettings.TypeSettings.DEFAULT.open());
            boolean flag1 = pTag.getBooleanOr(p_390194_.getSecond(), RecipeBookSettings.TypeSettings.DEFAULT.filtering());
            if (flag || flag1) {
                map.put(p_390193_, new RecipeBookSettings.TypeSettings(flag, flag1));
            }
        });
        return new RecipeBookSettings(map);
    }

    public void write(CompoundTag pTag) {
        TAG_FIELDS.forEach((p_358760_, p_358761_) -> {
            RecipeBookSettings.TypeSettings recipebooksettings$typesettings = this.states.getOrDefault(p_358760_, RecipeBookSettings.TypeSettings.DEFAULT);
            pTag.putBoolean(p_358761_.getFirst(), recipebooksettings$typesettings.open);
            pTag.putBoolean(p_358761_.getSecond(), recipebooksettings$typesettings.filtering);
        });
    }

    public RecipeBookSettings copy() {
        return new RecipeBookSettings(new EnumMap<>(this.states));
    }

    public void replaceFrom(RecipeBookSettings pOther) {
        this.states.clear();
        this.states.putAll(pOther.states);
    }

    @Override
    public boolean equals(Object pOther) {
        return this == pOther || pOther instanceof RecipeBookSettings && this.states.equals(((RecipeBookSettings)pOther).states);
    }

    @Override
    public int hashCode() {
        return this.states.hashCode();
    }

    record TypeSettings(boolean open, boolean filtering) {
        public static final RecipeBookSettings.TypeSettings DEFAULT = new RecipeBookSettings.TypeSettings(false, false);

        @Override
        public String toString() {
            return "[open=" + this.open + ", filtering=" + this.filtering + "]";
        }

        public RecipeBookSettings.TypeSettings setOpen(boolean pOpen) {
            return new RecipeBookSettings.TypeSettings(pOpen, this.filtering);
        }

        public RecipeBookSettings.TypeSettings setFiltering(boolean pFiltering) {
            return new RecipeBookSettings.TypeSettings(this.open, pFiltering);
        }
    }

    // FORGE -- called automatically on Enum creation - used for serialization
    public static void addTagsForType(RecipeBookType type, String openTag, String filteringTag) {
        TAG_FIELDS.put(type, Pair.of(openTag, filteringTag));
    }
}

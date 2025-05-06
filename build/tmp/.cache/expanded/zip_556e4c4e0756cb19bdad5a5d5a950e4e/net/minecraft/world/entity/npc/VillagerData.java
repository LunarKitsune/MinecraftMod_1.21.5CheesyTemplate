package net.minecraft.world.entity.npc;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;

public record VillagerData(Holder<VillagerType> type, Holder<VillagerProfession> profession, int level) {
    public static final int MIN_VILLAGER_LEVEL = 1;
    public static final int MAX_VILLAGER_LEVEL = 5;
    private static final int[] NEXT_LEVEL_XP_THRESHOLDS = new int[]{0, 10, 70, 150, 250};
    public static final Codec<VillagerData> CODEC = RecordCodecBuilder.create(
        p_390725_ -> p_390725_.group(
                BuiltInRegistries.VILLAGER_TYPE
                    .holderByNameCodec()
                    .fieldOf("type")
                    .orElseGet(() -> BuiltInRegistries.VILLAGER_TYPE.getOrThrow(VillagerType.PLAINS))
                    .forGetter(p_390724_ -> p_390724_.type),
                BuiltInRegistries.VILLAGER_PROFESSION
                    .holderByNameCodec()
                    .fieldOf("profession")
                    .orElseGet(() -> BuiltInRegistries.VILLAGER_PROFESSION.getOrThrow(VillagerProfession.NONE))
                    .forGetter(p_390726_ -> p_390726_.profession),
                Codec.INT.fieldOf("level").orElse(1).forGetter(p_150020_ -> p_150020_.level)
            )
            .apply(p_390725_, VillagerData::new)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, VillagerData> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.holderRegistry(Registries.VILLAGER_TYPE),
        VillagerData::type,
        ByteBufCodecs.holderRegistry(Registries.VILLAGER_PROFESSION),
        VillagerData::profession,
        ByteBufCodecs.VAR_INT,
        VillagerData::level,
        VillagerData::new
    );

    public VillagerData(Holder<VillagerType> type, Holder<VillagerProfession> profession, int level) {
        level = Math.max(1, level);
        this.type = type;
        this.profession = profession;
        this.level = level;
    }

    public VillagerData withType(Holder<VillagerType> pType) {
        return new VillagerData(pType, this.profession, this.level);
    }

    public VillagerData withType(HolderGetter.Provider pRegistries, ResourceKey<VillagerType> pType) {
        return this.withType(pRegistries.getOrThrow(pType));
    }

    public VillagerData withProfession(Holder<VillagerProfession> pProfession) {
        return new VillagerData(this.type, pProfession, this.level);
    }

    public VillagerData withProfession(HolderGetter.Provider pRegistries, ResourceKey<VillagerProfession> pProfession) {
        return this.withProfession(pRegistries.getOrThrow(pProfession));
    }

    public VillagerData withLevel(int pLevel) {
        return new VillagerData(this.type, this.profession, pLevel);
    }

    public static int getMinXpPerLevel(int pLevel) {
        return canLevelUp(pLevel) ? NEXT_LEVEL_XP_THRESHOLDS[pLevel - 1] : 0;
    }

    public static int getMaxXpPerLevel(int pLevel) {
        return canLevelUp(pLevel) ? NEXT_LEVEL_XP_THRESHOLDS[pLevel] : 0;
    }

    public static boolean canLevelUp(int pLevel) {
        return pLevel >= 1 && pLevel < 5;
    }
}
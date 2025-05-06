package net.minecraft.world.item;

import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

public interface Instruments {
    int GOAT_HORN_RANGE_BLOCKS = 256;
    float GOAT_HORN_DURATION = 7.0F;
    ResourceKey<Instrument> PONDER_GOAT_HORN = create("ponder_goat_horn");
    ResourceKey<Instrument> SING_GOAT_HORN = create("sing_goat_horn");
    ResourceKey<Instrument> SEEK_GOAT_HORN = create("seek_goat_horn");
    ResourceKey<Instrument> FEEL_GOAT_HORN = create("feel_goat_horn");
    ResourceKey<Instrument> ADMIRE_GOAT_HORN = create("admire_goat_horn");
    ResourceKey<Instrument> CALL_GOAT_HORN = create("call_goat_horn");
    ResourceKey<Instrument> YEARN_GOAT_HORN = create("yearn_goat_horn");
    ResourceKey<Instrument> DREAM_GOAT_HORN = create("dream_goat_horn");

    private static ResourceKey<Instrument> create(String pName) {
        return ResourceKey.create(Registries.INSTRUMENT, ResourceLocation.withDefaultNamespace(pName));
    }

    static void bootstrap(BootstrapContext<Instrument> pContext) {
        register(pContext, PONDER_GOAT_HORN, SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(0), 7.0F, 256.0F);
        register(pContext, SING_GOAT_HORN, SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(1), 7.0F, 256.0F);
        register(pContext, SEEK_GOAT_HORN, SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(2), 7.0F, 256.0F);
        register(pContext, FEEL_GOAT_HORN, SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(3), 7.0F, 256.0F);
        register(pContext, ADMIRE_GOAT_HORN, SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(4), 7.0F, 256.0F);
        register(pContext, CALL_GOAT_HORN, SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(5), 7.0F, 256.0F);
        register(pContext, YEARN_GOAT_HORN, SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(6), 7.0F, 256.0F);
        register(pContext, DREAM_GOAT_HORN, SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(7), 7.0F, 256.0F);
    }

    static void register(
        BootstrapContext<Instrument> pContext, ResourceKey<Instrument> pKey, Holder<SoundEvent> pSoundEvent, float pUseDuration, float pRange
    ) {
        MutableComponent mutablecomponent = Component.translatable(Util.makeDescriptionId("instrument", pKey.location()));
        pContext.register(pKey, new Instrument(pSoundEvent, pUseDuration, pRange, mutablecomponent));
    }
}
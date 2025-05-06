package net.minecraft.world.level.levelgen.carver;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.HolderSet;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;

public class CaveCarverConfiguration extends CarverConfiguration {
    public static final Codec<CaveCarverConfiguration> CODEC = RecordCodecBuilder.create(
        p_159184_ -> p_159184_.group(
                CarverConfiguration.CODEC.forGetter(p_159192_ -> p_159192_),
                FloatProvider.CODEC.fieldOf("horizontal_radius_multiplier").forGetter(p_159190_ -> p_159190_.horizontalRadiusMultiplier),
                FloatProvider.CODEC.fieldOf("vertical_radius_multiplier").forGetter(p_159188_ -> p_159188_.verticalRadiusMultiplier),
                FloatProvider.codec(-1.0F, 1.0F).fieldOf("floor_level").forGetter(p_159186_ -> p_159186_.floorLevel)
            )
            .apply(p_159184_, CaveCarverConfiguration::new)
    );
    public final FloatProvider horizontalRadiusMultiplier;
    public final FloatProvider verticalRadiusMultiplier;
    final FloatProvider floorLevel;

    public CaveCarverConfiguration(
        float pProbability,
        HeightProvider pY,
        FloatProvider pYScale,
        VerticalAnchor pLavaLevel,
        CarverDebugSettings pDebugSettings,
        HolderSet<Block> pReplaceable,
        FloatProvider pHorizontalRadiusMultiplier,
        FloatProvider pVerticalRadiusMultiplier,
        FloatProvider pFloorLevel
    ) {
        super(pProbability, pY, pYScale, pLavaLevel, pDebugSettings, pReplaceable);
        this.horizontalRadiusMultiplier = pHorizontalRadiusMultiplier;
        this.verticalRadiusMultiplier = pVerticalRadiusMultiplier;
        this.floorLevel = pFloorLevel;
    }

    public CaveCarverConfiguration(
        float pProbability,
        HeightProvider pY,
        FloatProvider pYScale,
        VerticalAnchor pLavaLevel,
        HolderSet<Block> pReplaceable,
        FloatProvider pHorizontalRadiusMultiplier,
        FloatProvider pVerticalRadiusMultiplier,
        FloatProvider pFloorLevel
    ) {
        this(pProbability, pY, pYScale, pLavaLevel, CarverDebugSettings.DEFAULT, pReplaceable, pHorizontalRadiusMultiplier, pVerticalRadiusMultiplier, pFloorLevel);
    }

    public CaveCarverConfiguration(CarverConfiguration pConfig, FloatProvider pHorizontalRadiusMultiplier, FloatProvider pVerticalRadiusMultiplier, FloatProvider pFloorLevel) {
        this(
            pConfig.probability,
            pConfig.y,
            pConfig.yScale,
            pConfig.lavaLevel,
            pConfig.debugSettings,
            pConfig.replaceable,
            pHorizontalRadiusMultiplier,
            pVerticalRadiusMultiplier,
            pFloorLevel
        );
    }
}
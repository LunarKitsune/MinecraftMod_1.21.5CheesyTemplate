package net.minecraft.client.gui.screens.worldselection;

import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record WorldCreationContext(
    WorldOptions options,
    Registry<LevelStem> datapackDimensions,
    WorldDimensions selectedDimensions,
    LayeredRegistryAccess<RegistryLayer> worldgenRegistries,
    ReloadableServerResources dataPackResources,
    WorldDataConfiguration dataConfiguration,
    InitialWorldCreationOptions initialWorldCreationOptions
) {
    public WorldCreationContext(
        WorldGenSettings pWorldGenSettings, LayeredRegistryAccess<RegistryLayer> pWorldGenRegistries, ReloadableServerResources pDataPackResources, WorldDataConfiguration pDataConfiguration
    ) {
        this(
            pWorldGenSettings.options(),
            pWorldGenSettings.dimensions(),
            pWorldGenRegistries,
            pDataPackResources,
            pDataConfiguration,
            new InitialWorldCreationOptions(WorldCreationUiState.SelectedGameMode.SURVIVAL, Set.of(), null)
        );
    }

    public WorldCreationContext(
        WorldOptions pWorldOptions,
        WorldDimensions pWorldDimensions,
        LayeredRegistryAccess<RegistryLayer> pWorldGenRegistries,
        ReloadableServerResources pDataPackResources,
        WorldDataConfiguration pDataConfiguration,
        InitialWorldCreationOptions pInitialWorldCreationOptions
    ) {
        this(
            pWorldOptions,
            pWorldGenRegistries.getLayer(RegistryLayer.DIMENSIONS).lookupOrThrow(Registries.LEVEL_STEM),
            pWorldDimensions,
            pWorldGenRegistries.replaceFrom(RegistryLayer.DIMENSIONS),
            pDataPackResources,
            pDataConfiguration,
            pInitialWorldCreationOptions
        );
    }

    public WorldCreationContext withSettings(WorldOptions pOptions, WorldDimensions pSelectedDimensions) {
        return new WorldCreationContext(pOptions, this.datapackDimensions, pSelectedDimensions, this.worldgenRegistries, this.dataPackResources, this.dataConfiguration, this.initialWorldCreationOptions);
    }

    public WorldCreationContext withOptions(WorldCreationContext.OptionsModifier pOptionsModifier) {
        return new WorldCreationContext(
            pOptionsModifier.apply(this.options), this.datapackDimensions, this.selectedDimensions, this.worldgenRegistries, this.dataPackResources, this.dataConfiguration, this.initialWorldCreationOptions
        );
    }

    public WorldCreationContext withDimensions(WorldCreationContext.DimensionsUpdater pDimensionsUpdater) {
        return new WorldCreationContext(
            this.options, this.datapackDimensions, pDimensionsUpdater.apply(this.worldgenLoadContext(), this.selectedDimensions), this.worldgenRegistries, this.dataPackResources, this.dataConfiguration, this.initialWorldCreationOptions
        );
    }

    public WorldCreationContext withDataConfiguration(WorldDataConfiguration dataConfiguration) {
        return new WorldCreationContext(this.options, this.datapackDimensions, this.selectedDimensions, this.worldgenRegistries, this.dataPackResources, dataConfiguration, this.initialWorldCreationOptions);
    }

    public WorldCreationContext withInitalOptions(InitialWorldCreationOptions initialWorldCreationOptions) {
        return new WorldCreationContext(this.options, this.datapackDimensions, this.selectedDimensions, this.worldgenRegistries, this.dataPackResources, this.dataConfiguration, initialWorldCreationOptions);
    }

    public RegistryAccess.Frozen worldgenLoadContext() {
        return this.worldgenRegistries.compositeAccess();
    }

    public void validate() {
        for (LevelStem levelstem : this.datapackDimensions()) {
            levelstem.generator().validate();
        }
    }

    @FunctionalInterface
    @OnlyIn(Dist.CLIENT)
    public interface DimensionsUpdater extends BiFunction<RegistryAccess.Frozen, WorldDimensions, WorldDimensions> {
    }

    @OnlyIn(Dist.CLIENT)
    public interface OptionsModifier extends UnaryOperator<WorldOptions> {
    }
}

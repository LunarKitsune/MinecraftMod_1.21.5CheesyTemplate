package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WeatherEffectRenderer {
    private static final int RAIN_RADIUS = 10;
    private static final int RAIN_DIAMETER = 21;
    private static final ResourceLocation RAIN_LOCATION = ResourceLocation.withDefaultNamespace("textures/environment/rain.png");
    private static final ResourceLocation SNOW_LOCATION = ResourceLocation.withDefaultNamespace("textures/environment/snow.png");
    private static final int RAIN_TABLE_SIZE = 32;
    private static final int HALF_RAIN_TABLE_SIZE = 16;
    private int rainSoundTime;
    private final float[] columnSizeX = new float[1024];
    private final float[] columnSizeZ = new float[1024];

    public WeatherEffectRenderer() {
        for (int i = 0; i < 32; i++) {
            for (int j = 0; j < 32; j++) {
                float f = j - 16;
                float f1 = i - 16;
                float f2 = Mth.length(f, f1);
                this.columnSizeX[i * 32 + j] = -f1 / f2;
                this.columnSizeZ[i * 32 + j] = f / f2;
            }
        }
    }

    public void render(Level pLevel, MultiBufferSource pBufferSource, int pTicks, float pPartialTick, Vec3 pCameraPosition) {
        float f = pLevel.getRainLevel(pPartialTick);
        if (!(f <= 0.0F)) {
            int i = Minecraft.useFancyGraphics() ? 10 : 5;
            List<WeatherEffectRenderer.ColumnInstance> list = new ArrayList<>();
            List<WeatherEffectRenderer.ColumnInstance> list1 = new ArrayList<>();
            this.collectColumnInstances(pLevel, pTicks, pPartialTick, pCameraPosition, i, list, list1);
            if (!list.isEmpty() || !list1.isEmpty()) {
                this.render(pBufferSource, pCameraPosition, i, f, list, list1);
            }
        }
    }

    private void collectColumnInstances(
        Level pLevel,
        int pTicks,
        float pPartialTick,
        Vec3 pCameraPosition,
        int pRadius,
        List<WeatherEffectRenderer.ColumnInstance> pRainColumnInstances,
        List<WeatherEffectRenderer.ColumnInstance> pSnowColumnInstances
    ) {
        int i = Mth.floor(pCameraPosition.x);
        int j = Mth.floor(pCameraPosition.y);
        int k = Mth.floor(pCameraPosition.z);
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
        RandomSource randomsource = RandomSource.create();

        for (int l = k - pRadius; l <= k + pRadius; l++) {
            for (int i1 = i - pRadius; i1 <= i + pRadius; i1++) {
                int j1 = pLevel.getHeight(Heightmap.Types.MOTION_BLOCKING, i1, l);
                int k1 = Math.max(j - pRadius, j1);
                int l1 = Math.max(j + pRadius, j1);
                if (l1 - k1 != 0) {
                    Biome.Precipitation biome$precipitation = this.getPrecipitationAt(pLevel, blockpos$mutableblockpos.set(i1, j, l));
                    if (biome$precipitation != Biome.Precipitation.NONE) {
                        int i2 = i1 * i1 * 3121 + i1 * 45238971 ^ l * l * 418711 + l * 13761;
                        randomsource.setSeed(i2);
                        int j2 = Math.max(j, j1);
                        int k2 = LevelRenderer.getLightColor(pLevel, blockpos$mutableblockpos.set(i1, j2, l));
                        if (biome$precipitation == Biome.Precipitation.RAIN) {
                            pRainColumnInstances.add(this.createRainColumnInstance(randomsource, pTicks, i1, k1, l1, l, k2, pPartialTick));
                        } else if (biome$precipitation == Biome.Precipitation.SNOW) {
                            pSnowColumnInstances.add(this.createSnowColumnInstance(randomsource, pTicks, i1, k1, l1, l, k2, pPartialTick));
                        }
                    }
                }
            }
        }
    }

    private void render(
        MultiBufferSource pBufferSource,
        Vec3 pCameraPosition,
        int pRadius,
        float pRainLevel,
        List<WeatherEffectRenderer.ColumnInstance> pRainColumnInstances,
        List<WeatherEffectRenderer.ColumnInstance> pSnowColumnInstances
    ) {
        if (!pRainColumnInstances.isEmpty()) {
            RenderType rendertype = RenderType.weather(RAIN_LOCATION, Minecraft.useShaderTransparency());
            this.renderInstances(pBufferSource.getBuffer(rendertype), pRainColumnInstances, pCameraPosition, 1.0F, pRadius, pRainLevel);
        }

        if (!pSnowColumnInstances.isEmpty()) {
            RenderType rendertype1 = RenderType.weather(SNOW_LOCATION, Minecraft.useShaderTransparency());
            this.renderInstances(pBufferSource.getBuffer(rendertype1), pSnowColumnInstances, pCameraPosition, 0.8F, pRadius, pRainLevel);
        }
    }

    private WeatherEffectRenderer.ColumnInstance createRainColumnInstance(
        RandomSource pRandom, int pTicks, int pX, int pBottomY, int pTopY, int pZ, int pLightCoords, float pPartialTick
    ) {
        int i = pTicks & 131071;
        int j = pX * pX * 3121 + pX * 45238971 + pZ * pZ * 418711 + pZ * 13761 & 0xFF;
        float f = 3.0F + pRandom.nextFloat();
        float f1 = -(i + j + pPartialTick) / 32.0F * f;
        float f2 = f1 % 32.0F;
        return new WeatherEffectRenderer.ColumnInstance(pX, pZ, pBottomY, pTopY, 0.0F, f2, pLightCoords);
    }

    private WeatherEffectRenderer.ColumnInstance createSnowColumnInstance(
        RandomSource pRandom, int pTicks, int pX, int pBottomY, int pTopY, int pZ, int pLightCoords, float pPartialTick
    ) {
        float f = pTicks + pPartialTick;
        float f1 = (float)(pRandom.nextDouble() + f * 0.01F * (float)pRandom.nextGaussian());
        float f2 = (float)(pRandom.nextDouble() + f * (float)pRandom.nextGaussian() * 0.001F);
        float f3 = -((pTicks & 511) + pPartialTick) / 512.0F;
        int i = LightTexture.pack((LightTexture.block(pLightCoords) * 3 + 15) / 4, (LightTexture.sky(pLightCoords) * 3 + 15) / 4);
        return new WeatherEffectRenderer.ColumnInstance(pX, pZ, pBottomY, pTopY, f1, f3 + f2, i);
    }

    private void renderInstances(
        VertexConsumer pBuffer, List<WeatherEffectRenderer.ColumnInstance> pColumnInstances, Vec3 pCameraPosition, float pAmount, int pRadius, float pRainLevel
    ) {
        for (WeatherEffectRenderer.ColumnInstance weathereffectrenderer$columninstance : pColumnInstances) {
            float f = (float)(weathereffectrenderer$columninstance.x + 0.5 - pCameraPosition.x);
            float f1 = (float)(weathereffectrenderer$columninstance.z + 0.5 - pCameraPosition.z);
            float f2 = (float)Mth.lengthSquared(f, f1);
            float f3 = Mth.lerp(f2 / (pRadius * pRadius), pAmount, 0.5F) * pRainLevel;
            int i = ARGB.white(f3);
            int j = (weathereffectrenderer$columninstance.z - Mth.floor(pCameraPosition.z) + 16) * 32
                + weathereffectrenderer$columninstance.x
                - Mth.floor(pCameraPosition.x)
                + 16;
            float f4 = this.columnSizeX[j] / 2.0F;
            float f5 = this.columnSizeZ[j] / 2.0F;
            float f6 = f - f4;
            float f7 = f + f4;
            float f8 = (float)(weathereffectrenderer$columninstance.topY - pCameraPosition.y);
            float f9 = (float)(weathereffectrenderer$columninstance.bottomY - pCameraPosition.y);
            float f10 = f1 - f5;
            float f11 = f1 + f5;
            float f12 = weathereffectrenderer$columninstance.uOffset + 0.0F;
            float f13 = weathereffectrenderer$columninstance.uOffset + 1.0F;
            float f14 = weathereffectrenderer$columninstance.bottomY * 0.25F + weathereffectrenderer$columninstance.vOffset;
            float f15 = weathereffectrenderer$columninstance.topY * 0.25F + weathereffectrenderer$columninstance.vOffset;
            pBuffer.addVertex(f6, f8, f10).setUv(f12, f14).setColor(i).setLight(weathereffectrenderer$columninstance.lightCoords);
            pBuffer.addVertex(f7, f8, f11).setUv(f13, f14).setColor(i).setLight(weathereffectrenderer$columninstance.lightCoords);
            pBuffer.addVertex(f7, f9, f11).setUv(f13, f15).setColor(i).setLight(weathereffectrenderer$columninstance.lightCoords);
            pBuffer.addVertex(f6, f9, f10).setUv(f12, f15).setColor(i).setLight(weathereffectrenderer$columninstance.lightCoords);
        }
    }

    public void tickRainParticles(ClientLevel pLevel, Camera pCamera, int pTicks, ParticleStatus pParticleStatus) {
        float f = pLevel.getRainLevel(1.0F) / (Minecraft.useFancyGraphics() ? 1.0F : 2.0F);
        if (!(f <= 0.0F)) {
            RandomSource randomsource = RandomSource.create(pTicks * 312987231L);
            BlockPos blockpos = BlockPos.containing(pCamera.getPosition());
            BlockPos blockpos1 = null;
            int i = (int)(100.0F * f * f) / (pParticleStatus == ParticleStatus.DECREASED ? 2 : 1);

            for (int j = 0; j < i; j++) {
                int k = randomsource.nextInt(21) - 10;
                int l = randomsource.nextInt(21) - 10;
                BlockPos blockpos2 = pLevel.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, blockpos.offset(k, 0, l));
                if (blockpos2.getY() > pLevel.getMinY()
                    && blockpos2.getY() <= blockpos.getY() + 10
                    && blockpos2.getY() >= blockpos.getY() - 10
                    && this.getPrecipitationAt(pLevel, blockpos2) == Biome.Precipitation.RAIN) {
                    blockpos1 = blockpos2.below();
                    if (pParticleStatus == ParticleStatus.MINIMAL) {
                        break;
                    }

                    double d0 = randomsource.nextDouble();
                    double d1 = randomsource.nextDouble();
                    BlockState blockstate = pLevel.getBlockState(blockpos1);
                    FluidState fluidstate = pLevel.getFluidState(blockpos1);
                    VoxelShape voxelshape = blockstate.getCollisionShape(pLevel, blockpos1);
                    double d2 = voxelshape.max(Direction.Axis.Y, d0, d1);
                    double d3 = fluidstate.getHeight(pLevel, blockpos1);
                    double d4 = Math.max(d2, d3);
                    ParticleOptions particleoptions = !fluidstate.is(FluidTags.LAVA)
                            && !blockstate.is(Blocks.MAGMA_BLOCK)
                            && !CampfireBlock.isLitCampfire(blockstate)
                        ? ParticleTypes.RAIN
                        : ParticleTypes.SMOKE;
                    pLevel.addParticle(particleoptions, blockpos1.getX() + d0, blockpos1.getY() + d4, blockpos1.getZ() + d1, 0.0, 0.0, 0.0);
                }
            }

            if (blockpos1 != null && randomsource.nextInt(3) < this.rainSoundTime++) {
                this.rainSoundTime = 0;
                if (blockpos1.getY() > blockpos.getY() + 1
                    && pLevel.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, blockpos).getY() > Mth.floor(blockpos.getY())) {
                    pLevel.playLocalSound(blockpos1, SoundEvents.WEATHER_RAIN_ABOVE, SoundSource.WEATHER, 0.1F, 0.5F, false);
                } else {
                    pLevel.playLocalSound(blockpos1, SoundEvents.WEATHER_RAIN, SoundSource.WEATHER, 0.2F, 1.0F, false);
                }
            }
        }
    }

    private Biome.Precipitation getPrecipitationAt(Level pLevel, BlockPos pPos) {
        if (!pLevel.getChunkSource().hasChunk(SectionPos.blockToSectionCoord(pPos.getX()), SectionPos.blockToSectionCoord(pPos.getZ()))) {
            return Biome.Precipitation.NONE;
        } else {
            Biome biome = pLevel.getBiome(pPos).value();
            return biome.getPrecipitationAt(pPos, pLevel.getSeaLevel());
        }
    }

    @OnlyIn(Dist.CLIENT)
    record ColumnInstance(int x, int z, int bottomY, int topY, float uOffset, float vOffset, int lightCoords) {
    }
}
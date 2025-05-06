package net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class LightTexture implements AutoCloseable {
    public static final int FULL_BRIGHT = 15728880;
    public static final int FULL_SKY = 15728640;
    public static final int FULL_BLOCK = 240;
    private static final int TEXTURE_SIZE = 16;
    private final GpuTexture texture;
    private boolean updateLightTexture;
    private float blockLightRedFlicker;
    private final GameRenderer renderer;
    private final Minecraft minecraft;

    public LightTexture(GameRenderer pRenderer, Minecraft pMinecraft) {
        this.renderer = pRenderer;
        this.minecraft = pMinecraft;
        GpuDevice gpudevice = RenderSystem.getDevice();
        this.texture = gpudevice.createTexture("Light Texture", TextureFormat.RGBA8, 16, 16, 1);
        this.texture.setTextureFilter(FilterMode.LINEAR, false);
        gpudevice.createCommandEncoder().clearColorTexture(this.texture, -1);
    }

    public GpuTexture getTarget() {
        return this.texture;
    }

    @Override
    public void close() {
        this.texture.close();
    }

    public void tick() {
        this.blockLightRedFlicker = this.blockLightRedFlicker + (float)((Math.random() - Math.random()) * Math.random() * Math.random() * 0.1);
        this.blockLightRedFlicker *= 0.9F;
        this.updateLightTexture = true;
    }

    public void turnOffLightLayer() {
        RenderSystem.setShaderTexture(2, null);
    }

    public void turnOnLightLayer() {
        RenderSystem.setShaderTexture(2, this.texture);
    }

    private float calculateDarknessScale(LivingEntity pEntity, float pGamma, float pPartialTick) {
        float f = 0.45F * pGamma;
        return Math.max(0.0F, Mth.cos((pEntity.tickCount - pPartialTick) * (float) Math.PI * 0.025F) * f);
    }

    public void updateLightTexture(float pPartialTicks) {
        if (this.updateLightTexture) {
            this.updateLightTexture = false;
            ProfilerFiller profilerfiller = Profiler.get();
            profilerfiller.push("lightTex");
            ClientLevel clientlevel = this.minecraft.level;
            if (clientlevel != null) {
                float f = clientlevel.getSkyDarken(1.0F);
                float f1;
                if (clientlevel.getSkyFlashTime() > 0) {
                    f1 = 1.0F;
                } else {
                    f1 = f * 0.95F + 0.05F;
                }

                float f2 = this.minecraft.options.darknessEffectScale().get().floatValue();
                float f3 = this.minecraft.player.getEffectBlendFactor(MobEffects.DARKNESS, pPartialTicks) * f2;
                float f4 = this.calculateDarknessScale(this.minecraft.player, f3, pPartialTicks) * f2;
                float f6 = this.minecraft.player.getWaterVision();
                float f5;
                if (this.minecraft.player.hasEffect(MobEffects.NIGHT_VISION)) {
                    f5 = GameRenderer.getNightVisionScale(this.minecraft.player, pPartialTicks);
                } else if (f6 > 0.0F && this.minecraft.player.hasEffect(MobEffects.CONDUIT_POWER)) {
                    f5 = f6;
                } else {
                    f5 = 0.0F;
                }

                Vector3f vector3f = new Vector3f(f, f, 1.0F).lerp(new Vector3f(1.0F, 1.0F, 1.0F), 0.35F);
                float f7 = this.blockLightRedFlicker + 1.5F;
                float f8 = clientlevel.dimensionType().ambientLight();
                boolean flag = clientlevel.effects().forceBrightLightmap();
                float f9 = this.minecraft.options.gamma().get().floatValue();
                RenderSystem.AutoStorageIndexBuffer rendersystem$autostorageindexbuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
                GpuBuffer gpubuffer = rendersystem$autostorageindexbuffer.getBuffer(6);

                try (RenderPass renderpass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(this.texture, OptionalInt.empty())) {
                    renderpass.setPipeline(RenderPipelines.LIGHTMAP);
                    renderpass.setUniform("AmbientLightFactor", f8);
                    renderpass.setUniform("SkyFactor", f1);
                    renderpass.setUniform("BlockFactor", f7);
                    renderpass.setUniform("UseBrightLightmap", flag ? 1 : 0);
                    renderpass.setUniform("SkyLightColor", vector3f.x, vector3f.y, vector3f.z);
                    renderpass.setUniform("NightVisionFactor", f5);
                    renderpass.setUniform("DarknessScale", f4);
                    renderpass.setUniform("DarkenWorldFactor", this.renderer.getDarkenWorldAmount(pPartialTicks));
                    renderpass.setUniform("BrightnessFactor", Math.max(0.0F, f9 - f3));
                    renderpass.setVertexBuffer(0, RenderSystem.getQuadVertexBuffer());
                    renderpass.setIndexBuffer(gpubuffer, rendersystem$autostorageindexbuffer.type());
                    renderpass.drawIndexed(0, 6);
                }

                profilerfiller.pop();
            }
        }
    }

    public static float getBrightness(DimensionType pDimensionType, int pLightLevel) {
        return getBrightness(pDimensionType.ambientLight(), pLightLevel);
    }

    public static float getBrightness(float pAmbientLight, int pLightLevel) {
        float f = pLightLevel / 15.0F;
        float f1 = f / (4.0F - 3.0F * f);
        return Mth.lerp(pAmbientLight, f1, 1.0F);
    }

    public static int pack(int pBlockLight, int pSkyLight) {
        return pBlockLight << 4 | pSkyLight << 20;
    }

    public static int block(int pPackedLight) {
        return (pPackedLight & 0xFFFF) >> 4; // Forge: Fix fullbright quads showing dark artifacts. Reported as MC-169806
    }

    public static int sky(int pPackedLight) {
        return pPackedLight >>> 20 & 15;
    }

    public static int lightCoordsWithEmission(int pPackedLight, int pEmmision) {
        if (pEmmision == 0) {
            return pPackedLight;
        } else {
            int i = Math.max(sky(pPackedLight), pEmmision);
            int j = Math.max(block(pPackedLight), pEmmision);
            return pack(j, i);
        }
    }
}

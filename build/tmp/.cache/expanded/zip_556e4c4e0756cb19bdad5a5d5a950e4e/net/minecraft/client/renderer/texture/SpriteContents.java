package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.resources.metadata.animation.AnimationFrame;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.util.ARGB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class SpriteContents implements Stitcher.Entry, AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final ResourceLocation name;
    final int width;
    final int height;
    private final NativeImage originalImage;
    public NativeImage[] byMipLevel;
    @Nullable
    final SpriteContents.AnimatedTexture animatedTexture;
    private final ResourceMetadata metadata;
    @Nullable
    public final net.minecraftforge.client.textures.ForgeTextureMetadata forgeMeta;

    /**
     * @deprecated Forge: Use the {@linkplain SpriteContents#SpriteContents(ResourceLocation, FrameSize, NativeImage, AnimationMetadataSection, net.minecraftforge.client.textures.ForgeTextureMetadata) overload with Forge metadata parameter} to properly forward custom loaders.
     */
    @Deprecated
    public SpriteContents(ResourceLocation pName, FrameSize pFrameSize, NativeImage pOriginalImage, ResourceMetadata pMetadata) {
        this(pName, pFrameSize, pOriginalImage, pMetadata, null);
    }

    public SpriteContents(ResourceLocation pName, FrameSize pFrameSize, NativeImage pOriginalImage, ResourceMetadata pMetadata, @Nullable net.minecraftforge.client.textures.ForgeTextureMetadata forgeMeta) {
        this.name = pName;
        this.width = pFrameSize.width();
        this.height = pFrameSize.height();
        this.metadata = pMetadata;
        this.animatedTexture = pMetadata.getSection(AnimationMetadataSection.TYPE)
            .map(p_374666_ -> this.createAnimatedTexture(pFrameSize, pOriginalImage.getWidth(), pOriginalImage.getHeight(), p_374666_))
            .orElse(null);
        this.originalImage = pOriginalImage;
        this.byMipLevel = new NativeImage[]{this.originalImage};
        this.forgeMeta = forgeMeta;
    }

    public void increaseMipLevel(int pMipLevel) {
        try {
            this.byMipLevel = MipmapGenerator.generateMipLevels(this.byMipLevel, pMipLevel);
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Generating mipmaps for frame");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Sprite being mipmapped");
            crashreportcategory.setDetail("First frame", () -> {
                StringBuilder stringbuilder = new StringBuilder();
                if (stringbuilder.length() > 0) {
                    stringbuilder.append(", ");
                }

                stringbuilder.append(this.originalImage.getWidth()).append("x").append(this.originalImage.getHeight());
                return stringbuilder.toString();
            });
            CrashReportCategory crashreportcategory1 = crashreport.addCategory("Frame being iterated");
            crashreportcategory1.setDetail("Sprite name", this.name);
            crashreportcategory1.setDetail("Sprite size", () -> this.width + " x " + this.height);
            crashreportcategory1.setDetail("Sprite frames", () -> this.getFrameCount() + " frames");
            crashreportcategory1.setDetail("Mipmap levels", pMipLevel);
            throw new ReportedException(crashreport);
        }
    }

    int getFrameCount() {
        return this.animatedTexture != null ? this.animatedTexture.frames.size() : 1;
    }

    @Nullable
    private SpriteContents.AnimatedTexture createAnimatedTexture(FrameSize pFrameSize, int pWidth, int pHeight, AnimationMetadataSection pMetadata) {
        int i = pWidth / pFrameSize.width();
        int j = pHeight / pFrameSize.height();
        int k = i * j;
        int l = pMetadata.defaultFrameTime();
        List<SpriteContents.FrameInfo> list;
        if (pMetadata.frames().isEmpty()) {
            list = new ArrayList<>(k);

            for (int i1 = 0; i1 < k; i1++) {
                list.add(new SpriteContents.FrameInfo(i1, l));
            }
        } else {
            List<AnimationFrame> list1 = pMetadata.frames().get();
            list = new ArrayList<>(list1.size());

            for (AnimationFrame animationframe : list1) {
                list.add(new SpriteContents.FrameInfo(animationframe.index(), animationframe.timeOr(l)));
            }

            int j1 = 0;
            IntSet intset = new IntOpenHashSet();

            for (Iterator<SpriteContents.FrameInfo> iterator = list.iterator(); iterator.hasNext(); j1++) {
                SpriteContents.FrameInfo spritecontents$frameinfo = iterator.next();
                boolean flag = true;
                if (spritecontents$frameinfo.time <= 0) {
                    LOGGER.warn("Invalid frame duration on sprite {} frame {}: {}", this.name, j1, spritecontents$frameinfo.time);
                    flag = false;
                }

                if (spritecontents$frameinfo.index < 0 || spritecontents$frameinfo.index >= k) {
                    LOGGER.warn("Invalid frame index on sprite {} frame {}: {}", this.name, j1, spritecontents$frameinfo.index);
                    flag = false;
                }

                if (flag) {
                    intset.add(spritecontents$frameinfo.index);
                } else {
                    iterator.remove();
                }
            }

            int[] aint = IntStream.range(0, k).filter(p_251185_ -> !intset.contains(p_251185_)).toArray();
            if (aint.length > 0) {
                LOGGER.warn("Unused frames in sprite {}: {}", this.name, Arrays.toString(aint));
            }
        }

        return list.size() <= 1 ? null : new SpriteContents.AnimatedTexture(List.copyOf(list), i, pMetadata.interpolatedFrames());
    }

    void upload(int pX, int pY, int pSourceX, int pSourceY, NativeImage[] pImages, GpuTexture pTexture) {
        for (int i = 0; i < this.byMipLevel.length; i++) {
            // Forge: Skip uploading if the texture would be made invalid by mip level
            if ((this.width >> i) <= 0 || (this.height >> i) <= 0)
                break;
            RenderSystem.getDevice()
                .createCommandEncoder()
                .writeToTexture(
                    pTexture, pImages[i], i, pX >> i, pY >> i, this.width >> i, this.height >> i, pSourceX >> i, pSourceY >> i
                );
        }
    }

    @Override
    public int width() {
        return this.width;
    }

    @Override
    public int height() {
        return this.height;
    }

    @Override
    public ResourceLocation name() {
        return this.name;
    }

    public NativeImage getOriginalImage() {
        return this.originalImage;
    }

    public IntStream getUniqueFrames() {
        return this.animatedTexture != null ? this.animatedTexture.getUniqueFrames() : IntStream.of(1);
    }

    @Nullable
    public SpriteTicker createTicker() {
        return this.animatedTexture != null ? this.animatedTexture.createTicker() : null;
    }

    public ResourceMetadata metadata() {
        return this.metadata;
    }

    @Override
    public void close() {
        for (NativeImage nativeimage : this.byMipLevel) {
            nativeimage.close();
        }
    }

    @Override
    public String toString() {
        return "SpriteContents{name=" + this.name + ", frameCount=" + this.getFrameCount() + ", height=" + this.height + ", width=" + this.width + "}";
    }

    public boolean isTransparent(int pFrame, int pX, int pY) {
        int i = pX;
        int j = pY;
        if (this.animatedTexture != null) {
            i = pX + this.animatedTexture.getFrameX(pFrame) * this.width;
            j = pY + this.animatedTexture.getFrameY(pFrame) * this.height;
        }

        return ARGB.alpha(this.originalImage.getPixel(i, j)) == 0;
    }

    public void uploadFirstFrame(int pX, int pY, GpuTexture pTexture) {
        if (this.animatedTexture != null) {
            this.animatedTexture.uploadFirstFrame(pX, pY, pTexture);
        } else {
            this.upload(pX, pY, 0, 0, this.byMipLevel, pTexture);
        }
    }

    @OnlyIn(Dist.CLIENT)
    class AnimatedTexture {
        final List<SpriteContents.FrameInfo> frames;
        private final int frameRowSize;
        private final boolean interpolateFrames;

        AnimatedTexture(final List<SpriteContents.FrameInfo> pFrames, final int pFrameRowSize, final boolean pInterpolateFrames) {
            this.frames = pFrames;
            this.frameRowSize = pFrameRowSize;
            this.interpolateFrames = pInterpolateFrames;
        }

        int getFrameX(int pFrameIndex) {
            return pFrameIndex % this.frameRowSize;
        }

        int getFrameY(int pFrameIndex) {
            return pFrameIndex / this.frameRowSize;
        }

        void uploadFrame(int pX, int pY, int pFrameIndex, GpuTexture pTexture) {
            int i = this.getFrameX(pFrameIndex) * SpriteContents.this.width;
            int j = this.getFrameY(pFrameIndex) * SpriteContents.this.height;
            SpriteContents.this.upload(pX, pY, i, j, SpriteContents.this.byMipLevel, pTexture);
        }

        public SpriteTicker createTicker() {
            return SpriteContents.this.new Ticker(this, this.interpolateFrames ? SpriteContents.this.new InterpolationData() : null);
        }

        public void uploadFirstFrame(int pX, int pY, GpuTexture pTexture) {
            this.uploadFrame(pX, pY, this.frames.get(0).index, pTexture);
        }

        public IntStream getUniqueFrames() {
            return this.frames.stream().mapToInt(p_249981_ -> p_249981_.index).distinct();
        }
    }

    @OnlyIn(Dist.CLIENT)
    record FrameInfo(int index, int time) {
    }

    @OnlyIn(Dist.CLIENT)
    final class InterpolationData implements AutoCloseable {
        private final NativeImage[] activeFrame = new NativeImage[SpriteContents.this.byMipLevel.length];

        InterpolationData() {
            for (int i = 0; i < this.activeFrame.length; i++) {
                int j = SpriteContents.this.width >> i;
                int k = SpriteContents.this.height >> i;
                // Forge: Guard against invalid texture size, because we allow generating mipmaps regardless of texture sizes
                this.activeFrame[i] = new NativeImage(Math.max(1, j), Math.max(1, k), false);
            }
        }

        void uploadInterpolatedFrame(int pX, int pY, SpriteContents.Ticker pTicker, GpuTexture pTexture) {
            SpriteContents.AnimatedTexture spritecontents$animatedtexture = pTicker.animationInfo;
            List<SpriteContents.FrameInfo> list = spritecontents$animatedtexture.frames;
            SpriteContents.FrameInfo spritecontents$frameinfo = list.get(pTicker.frame);
            float f = (float)pTicker.subFrame / spritecontents$frameinfo.time;
            int i = spritecontents$frameinfo.index;
            int j = list.get((pTicker.frame + 1) % list.size()).index;
            if (i != j) {
                for (int k = 0; k < this.activeFrame.length; k++) {
                    int l = SpriteContents.this.width >> k;
                    int i1 = SpriteContents.this.height >> k;
                    // Forge: Guard against invalid texture size, because we allow generating mipmaps regardless of texture sizes
                    if (l < 1 || i1 < 1)
                        continue;

                    for (int j1 = 0; j1 < i1; j1++) {
                        for (int k1 = 0; k1 < l; k1++) {
                            int l1 = this.getPixel(spritecontents$animatedtexture, i, k, k1, j1);
                            int i2 = this.getPixel(spritecontents$animatedtexture, j, k, k1, j1);
                            this.activeFrame[k].setPixel(k1, j1, ARGB.lerp(f, l1, i2));
                        }
                    }
                }

                SpriteContents.this.upload(pX, pY, 0, 0, this.activeFrame, pTexture);
            }
        }

        private int getPixel(SpriteContents.AnimatedTexture pAnimatedTexture, int pFrameIndex, int pMipLevel, int pX, int pY) {
            return SpriteContents.this.byMipLevel[pMipLevel]
                .getPixel(
                    pX + (pAnimatedTexture.getFrameX(pFrameIndex) * SpriteContents.this.width >> pMipLevel),
                    pY + (pAnimatedTexture.getFrameY(pFrameIndex) * SpriteContents.this.height >> pMipLevel)
                );
        }

        @Override
        public void close() {
            for (NativeImage nativeimage : this.activeFrame) {
                nativeimage.close();
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    class Ticker implements SpriteTicker {
        int frame;
        int subFrame;
        final SpriteContents.AnimatedTexture animationInfo;
        @Nullable
        private final SpriteContents.InterpolationData interpolationData;

        Ticker(final SpriteContents.AnimatedTexture pAnimationInfo, @Nullable final SpriteContents.InterpolationData pInterpolationData) {
            this.animationInfo = pAnimationInfo;
            this.interpolationData = pInterpolationData;
        }

        @Override
        public void tickAndUpload(int p_249105_, int p_249676_, GpuTexture p_391306_) {
            this.subFrame++;
            SpriteContents.FrameInfo spritecontents$frameinfo = this.animationInfo.frames.get(this.frame);
            if (this.subFrame >= spritecontents$frameinfo.time) {
                int i = spritecontents$frameinfo.index;
                this.frame = (this.frame + 1) % this.animationInfo.frames.size();
                this.subFrame = 0;
                int j = this.animationInfo.frames.get(this.frame).index;
                if (i != j) {
                    this.animationInfo.uploadFrame(p_249105_, p_249676_, j, p_391306_);
                }
            } else if (this.interpolationData != null) {
                this.interpolationData.uploadInterpolatedFrame(p_249105_, p_249676_, this, p_391306_);
            }
        }

        @Override
        public void close() {
            if (this.interpolationData != null) {
                this.interpolationData.close();
            }
        }
    }
}

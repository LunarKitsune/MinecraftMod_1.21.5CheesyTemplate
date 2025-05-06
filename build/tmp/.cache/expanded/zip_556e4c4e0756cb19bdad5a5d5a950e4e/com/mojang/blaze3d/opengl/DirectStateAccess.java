package com.mojang.blaze3d.opengl;

import java.util.Set;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.ARBDirectStateAccess;
import org.lwjgl.opengl.GLCapabilities;

@OnlyIn(Dist.CLIENT)
public abstract class DirectStateAccess {
    public static DirectStateAccess create(GLCapabilities pCapabilities, Set<String> pEnabledExtensions) {
        if (pCapabilities.GL_ARB_direct_state_access && GlDevice.USE_GL_ARB_direct_state_access) {
            pEnabledExtensions.add("GL_ARB_direct_state_access");
            return new DirectStateAccess.Core();
        } else {
            return new DirectStateAccess.Emulated();
        }
    }

    abstract int createFrameBufferObject();

    abstract void bindFrameBufferTextures(int pFrameBuffer, int pColorTexture, int pDepthTexture, int pLevel, int pTarget);

    abstract void blitFrameBuffers(
        int pReadFrameBuffer,
        int pDrawFrameBuffer,
        int pSrcX0,
        int pSrcY0,
        int pSrcX1,
        int pSrcY1,
        int pDestX0,
        int pDestY0,
        int pDestX1,
        int pDestY1,
        int pMask,
        int pFilter
    );

    @OnlyIn(Dist.CLIENT)
    static class Core extends DirectStateAccess {
        @Override
        public int createFrameBufferObject() {
            return ARBDirectStateAccess.glCreateFramebuffers();
        }

        @Override
        public void bindFrameBufferTextures(int p_396835_, int p_394736_, int p_395996_, int p_397932_, int p_396105_) {
            ARBDirectStateAccess.glNamedFramebufferTexture(p_396835_, 36064, p_394736_, p_397932_);
            ARBDirectStateAccess.glNamedFramebufferTexture(p_396835_, 36096, p_395996_, p_397932_);
            if (p_396105_ != 0) {
                GlStateManager._glBindFramebuffer(p_396105_, p_396835_);
            }
        }

        @Override
        public void blitFrameBuffers(
            int p_395353_,
            int p_395149_,
            int p_393964_,
            int p_395294_,
            int p_395276_,
            int p_391710_,
            int p_393525_,
            int p_396971_,
            int p_392279_,
            int p_396123_,
            int p_397974_,
            int p_391707_
        ) {
            ARBDirectStateAccess.glBlitNamedFramebuffer(
                p_395353_, p_395149_, p_393964_, p_395294_, p_395276_, p_391710_, p_393525_, p_396971_, p_392279_, p_396123_, p_397974_, p_391707_
            );
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class Emulated extends DirectStateAccess {
        @Override
        public int createFrameBufferObject() {
            return GlStateManager.glGenFramebuffers();
        }

        @Override
        public void bindFrameBufferTextures(int p_397405_, int p_395460_, int p_393875_, int p_393114_, int p_397703_) {
            int i = p_397703_ == 0 ? '\u8ca9' : p_397703_;
            int j = GlStateManager.getFrameBuffer(i);
            GlStateManager._glBindFramebuffer(i, p_397405_);
            GlStateManager._glFramebufferTexture2D(i, 36064, 3553, p_395460_, p_393114_);
            GlStateManager._glFramebufferTexture2D(i, 36096, 3553, p_393875_, p_393114_);
            if (p_397703_ == 0) {
                GlStateManager._glBindFramebuffer(i, j);
            }
        }

        @Override
        public void blitFrameBuffers(
            int p_396366_,
            int p_393343_,
            int p_397226_,
            int p_396156_,
            int p_397178_,
            int p_396414_,
            int p_397943_,
            int p_396165_,
            int p_394958_,
            int p_393756_,
            int p_393868_,
            int p_394611_
        ) {
            int i = GlStateManager.getFrameBuffer(36008);
            int j = GlStateManager.getFrameBuffer(36009);
            GlStateManager._glBindFramebuffer(36008, p_396366_);
            GlStateManager._glBindFramebuffer(36009, p_393343_);
            GlStateManager._glBlitFrameBuffer(p_397226_, p_396156_, p_397178_, p_396414_, p_397943_, p_396165_, p_394958_, p_393756_, p_393868_, p_394611_);
            GlStateManager._glBindFramebuffer(36008, i);
            GlStateManager._glBindFramebuffer(36009, j);
        }
    }
}
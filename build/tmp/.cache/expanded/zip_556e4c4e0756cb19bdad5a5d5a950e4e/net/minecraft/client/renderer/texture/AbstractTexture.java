package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import javax.annotation.Nullable;
import net.minecraft.util.TriState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractTexture implements AutoCloseable {
    @Nullable
    protected GpuTexture texture;
    protected boolean defaultBlur;

    public void setClamp(boolean pClamp) {
        if (this.texture == null) {
            throw new IllegalStateException("Texture does not exist, can't change its clamp before something initializes it");
        } else {
            this.texture.setAddressMode(pClamp ? AddressMode.CLAMP_TO_EDGE : AddressMode.REPEAT);
        }
    }

    public void setFilter(TriState pBlur, boolean pMipmap) {
        this.setFilter(pBlur.toBoolean(this.defaultBlur), pMipmap);
    }

    public void setFilter(boolean pBlur, boolean pMipmap) {
        if (this.texture == null) {
            throw new IllegalStateException("Texture does not exist, can't get change its filter before something initializes it");
        } else {
            this.blur = pBlur;
            this.mipmap = pMipmap;
            this.texture.setTextureFilter(pBlur ? FilterMode.LINEAR : FilterMode.NEAREST, pMipmap);
        }
    }

    @Override
    public void close() {
        if (this.texture != null) {
            this.texture.close();
            this.texture = null;
        }
    }

    // FORGE: This seems to have been stripped out, but we need it
    private boolean blur, mipmap, lastBlur, lastMipmap;

    public void setBlurMipmap(boolean blur, boolean mipmap) {
        this.lastBlur = this.blur;
        this.lastMipmap = this.mipmap;
        setFilter(blur, mipmap);
    }

    public void restoreLastBlurMipmap() {
        setFilter(this.lastBlur, this.lastMipmap);
    }

    public GpuTexture getTexture() {
        if (this.texture == null) {
            throw new IllegalStateException("Texture does not exist, can't get it before something initializes it");
        } else {
            return this.texture;
        }
    }
}

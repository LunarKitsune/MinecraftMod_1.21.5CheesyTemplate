package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.logging.LogUtils;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class Uniform extends AbstractUniform implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private int location;
    private final UniformType type;
    private final IntBuffer intValues;
    private final FloatBuffer floatValues;
    private final String name;
    private boolean dirty;

    public Uniform(String pName, UniformType pType) {
        this.name = pName;
        this.type = pType;
        if (pType.isIntStorage()) {
            this.intValues = MemoryUtil.memAllocInt(pType.count());
            this.floatValues = null;
        } else {
            this.intValues = null;
            this.floatValues = MemoryUtil.memAllocFloat(pType.count());
        }

        this.location = -1;
    }

    public static int glGetUniformLocation(int pProgram, CharSequence pName) {
        return GlStateManager._glGetUniformLocation(pProgram, pName);
    }

    public static void uploadInteger(int pLocation, int pValue) {
        GlStateManager._glUniform1i(pLocation, pValue);
    }

    @Override
    public void close() {
        if (this.intValues != null) {
            MemoryUtil.memFree(this.intValues);
        }

        if (this.floatValues != null) {
            MemoryUtil.memFree(this.floatValues);
        }
    }

    public void setLocation(int pLocation) {
        this.location = pLocation;
    }

    public String getName() {
        return this.name;
    }

    public UniformType getType() {
        return this.type;
    }

    @Override
    public final void set(float p_392590_) {
        this.floatValues.position(0);
        this.floatValues.put(0, p_392590_);
        this.dirty = true;
    }

    @Override
    public final void set(float p_394021_, float p_396030_) {
        this.floatValues.position(0);
        this.floatValues.put(0, p_394021_);
        this.floatValues.put(1, p_396030_);
        this.dirty = true;
    }

    @Override
    public final void set(float p_396958_, float p_395648_, float p_396717_) {
        this.floatValues.position(0);
        this.floatValues.put(0, p_396958_);
        this.floatValues.put(1, p_395648_);
        this.floatValues.put(2, p_396717_);
        this.dirty = true;
    }

    @Override
    public final void set(Vector3f p_396693_) {
        this.floatValues.position(0);
        p_396693_.get(this.floatValues);
        this.dirty = true;
    }

    @Override
    public final void set(float p_391935_, float p_395223_, float p_395064_, float p_395327_) {
        this.floatValues.position(0);
        this.floatValues.put(p_391935_);
        this.floatValues.put(p_395223_);
        this.floatValues.put(p_395064_);
        this.floatValues.put(p_395327_);
        this.floatValues.flip();
        this.dirty = true;
    }

    @Override
    public final void set(int p_396445_) {
        this.intValues.position(0);
        this.intValues.put(0, p_396445_);
        this.dirty = true;
    }

    @Override
    public final void set(int p_397235_, int p_396890_, int p_391259_) {
        this.intValues.position(0);
        this.intValues.put(0, p_397235_);
        this.intValues.put(1, p_396890_);
        this.intValues.put(2, p_391259_);
        this.dirty = true;
    }

    @Override
    public final void set(float[] p_393131_) {
        if (p_393131_.length < this.type.count()) {
            LOGGER.warn("Uniform.set called with a too-small value array (expected {}, got {}). Ignoring.", this.type.count(), p_393131_.length);
        } else {
            this.floatValues.position(0);
            this.floatValues.put(p_393131_);
            this.floatValues.position(0);
            this.dirty = true;
        }
    }

    @Override
    public final void set(int[] p_394423_) {
        if (p_394423_.length < this.type.count()) {
            LOGGER.warn("Uniform.set called with a too-small value array (expected {}, got {}). Ignoring.", this.type.count(), p_394423_.length);
        } else {
            this.intValues.position(0);
            this.intValues.put(p_394423_);
            this.intValues.position(0);
            this.dirty = true;
        }
    }

    @Override
    public final void set(Matrix4f p_391815_) {
        this.floatValues.position(0);
        p_391815_.get(this.floatValues);
        this.dirty = true;
    }

    public void upload() {
        if (this.dirty) {
            if (this.type.isIntStorage()) {
                switch (this.type) {
                    case INT:
                        GlStateManager._glUniform1(this.location, this.intValues);
                        break;
                    case IVEC3:
                        GlStateManager._glUniform3(this.location, this.intValues);
                }
            } else {
                switch (this.type) {
                    case FLOAT:
                        GlStateManager._glUniform1(this.location, this.floatValues);
                        break;
                    case VEC2:
                        GlStateManager._glUniform2(this.location, this.floatValues);
                        break;
                    case VEC3:
                        GlStateManager._glUniform3(this.location, this.floatValues);
                        break;
                    case VEC4:
                        GlStateManager._glUniform4(this.location, this.floatValues);
                        break;
                    case MATRIX4X4:
                        GlStateManager._glUniformMatrix4(this.location, this.floatValues);
                }
            }

            this.dirty = false;
        }
    }
}
package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.ARBVertexAttribBinding;
import org.lwjgl.opengl.GLCapabilities;

@OnlyIn(Dist.CLIENT)
public abstract class VertexArrayCache {
    public static VertexArrayCache create(GLCapabilities pCapabilities, GlDebugLabel pDebugLabel, Set<String> pEnabledExtensions) {
        if (pCapabilities.GL_ARB_vertex_attrib_binding && GlDevice.USE_GL_ARB_vertex_attrib_binding) {
            pEnabledExtensions.add("GL_ARB_vertex_attrib_binding");
            return new VertexArrayCache.Separate(pDebugLabel);
        } else {
            return new VertexArrayCache.Emulated(pDebugLabel);
        }
    }

    public abstract void bindVertexArray(VertexFormat pFormat, GlBuffer pBuffer);

    @OnlyIn(Dist.CLIENT)
    static class Emulated extends VertexArrayCache {
        private final Map<VertexFormat, VertexArrayCache.VertexArray> cache = new HashMap<>();
        private final GlDebugLabel debugLabels;

        public Emulated(GlDebugLabel pDebugLabels) {
            this.debugLabels = pDebugLabels;
        }

        @Override
        public void bindVertexArray(VertexFormat p_392095_, GlBuffer p_394959_) {
            VertexArrayCache.VertexArray vertexarraycache$vertexarray = this.cache.get(p_392095_);
            if (vertexarraycache$vertexarray == null) {
                int i = GlStateManager._glGenVertexArrays();
                GlStateManager._glBindVertexArray(i);
                GlStateManager._glBindBuffer(34962, p_394959_.handle);
                setupCombinedAttributes(p_392095_, true);
                VertexArrayCache.VertexArray vertexarraycache$vertexarray1 = new VertexArrayCache.VertexArray(i, p_392095_, p_394959_);
                this.debugLabels.applyLabel(vertexarraycache$vertexarray1);
                this.cache.put(p_392095_, vertexarraycache$vertexarray1);
            } else {
                GlStateManager._glBindVertexArray(vertexarraycache$vertexarray.id);
                if (vertexarraycache$vertexarray.lastVertexBuffer != p_394959_) {
                    GlStateManager._glBindBuffer(34962, p_394959_.handle);
                    vertexarraycache$vertexarray.lastVertexBuffer = p_394959_;
                    setupCombinedAttributes(p_392095_, false);
                }
            }
        }

        private static void setupCombinedAttributes(VertexFormat pVertexFormat, boolean pEnabled) {
            int i = pVertexFormat.getVertexSize();
            List<VertexFormatElement> list = pVertexFormat.getElements();

            for (int j = 0; j < list.size(); j++) {
                VertexFormatElement vertexformatelement = list.get(j);
                if (pEnabled) {
                    GlStateManager._enableVertexAttribArray(j);
                }

                switch (vertexformatelement.usage()) {
                    case POSITION:
                    case GENERIC:
                        GlStateManager._vertexAttribPointer(
                            j, vertexformatelement.count(), GlConst.toGl(vertexformatelement.type()), false, i, pVertexFormat.getOffset(vertexformatelement)
                        );
                        break;
                    case NORMAL:
                    case COLOR:
                        GlStateManager._vertexAttribPointer(
                            j, vertexformatelement.count(), GlConst.toGl(vertexformatelement.type()), true, i, pVertexFormat.getOffset(vertexformatelement)
                        );
                        break;
                    case UV:
                        if (vertexformatelement.type() == VertexFormatElement.Type.FLOAT) {
                            GlStateManager._vertexAttribPointer(
                                j, vertexformatelement.count(), GlConst.toGl(vertexformatelement.type()), false, i, pVertexFormat.getOffset(vertexformatelement)
                            );
                        } else {
                            GlStateManager._vertexAttribIPointer(
                                j, vertexformatelement.count(), GlConst.toGl(vertexformatelement.type()), i, pVertexFormat.getOffset(vertexformatelement)
                            );
                        }
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class Separate extends VertexArrayCache {
        private final Map<VertexFormat, VertexArrayCache.VertexArray> cache = new HashMap<>();
        private final GlDebugLabel debugLabels;

        public Separate(GlDebugLabel pDebugLabels) {
            this.debugLabels = pDebugLabels;
        }

        @Override
        public void bindVertexArray(VertexFormat p_391319_, GlBuffer p_391840_) {
            VertexArrayCache.VertexArray vertexarraycache$vertexarray = this.cache.get(p_391319_);
            if (vertexarraycache$vertexarray == null) {
                int i = GlStateManager._glGenVertexArrays();
                GlStateManager._glBindVertexArray(i);
                ARBVertexAttribBinding.glBindVertexBuffer(0, p_391840_.handle, 0L, p_391319_.getVertexSize());
                List<VertexFormatElement> list = p_391319_.getElements();

                for (int j = 0; j < list.size(); j++) {
                    VertexFormatElement vertexformatelement = list.get(j);
                    GlStateManager._enableVertexAttribArray(j);
                    switch (vertexformatelement.usage()) {
                        case POSITION:
                        case GENERIC:
                            ARBVertexAttribBinding.glVertexAttribFormat(
                                j, vertexformatelement.count(), GlConst.toGl(vertexformatelement.type()), false, p_391319_.getOffset(vertexformatelement)
                            );
                            break;
                        case NORMAL:
                        case COLOR:
                            ARBVertexAttribBinding.glVertexAttribFormat(
                                j, vertexformatelement.count(), GlConst.toGl(vertexformatelement.type()), true, p_391319_.getOffset(vertexformatelement)
                            );
                            break;
                        case UV:
                            if (vertexformatelement.type() == VertexFormatElement.Type.FLOAT) {
                                ARBVertexAttribBinding.glVertexAttribFormat(
                                    j, vertexformatelement.count(), GlConst.toGl(vertexformatelement.type()), false, p_391319_.getOffset(vertexformatelement)
                                );
                            } else {
                                ARBVertexAttribBinding.glVertexAttribIFormat(
                                    j, vertexformatelement.count(), GlConst.toGl(vertexformatelement.type()), p_391319_.getOffset(vertexformatelement)
                                );
                            }
                    }

                    ARBVertexAttribBinding.glVertexAttribBinding(j, 0);
                }

                VertexArrayCache.VertexArray vertexarraycache$vertexarray1 = new VertexArrayCache.VertexArray(i, p_391319_, p_391840_);
                this.debugLabels.applyLabel(vertexarraycache$vertexarray1);
                this.cache.put(p_391319_, vertexarraycache$vertexarray1);
            } else {
                GlStateManager._glBindVertexArray(vertexarraycache$vertexarray.id);
                if (vertexarraycache$vertexarray.lastVertexBuffer != p_391840_) {
                    if (vertexarraycache$vertexarray.lastVertexBuffer != null && vertexarraycache$vertexarray.lastVertexBuffer.handle == p_391840_.handle) {
                        ARBVertexAttribBinding.glBindVertexBuffer(0, 0, 0L, 0);
                    }

                    ARBVertexAttribBinding.glBindVertexBuffer(0, p_391840_.handle, 0L, p_391319_.getVertexSize());
                    vertexarraycache$vertexarray.lastVertexBuffer = p_391840_;
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class VertexArray {
        final int id;
        final VertexFormat format;
        @Nullable
        GlBuffer lastVertexBuffer;

        VertexArray(int pId, VertexFormat pFormat, @Nullable GlBuffer pLastVertexBuffer) {
            this.id = pId;
            this.format = pFormat;
            this.lastVertexBuffer = pLastVertexBuffer;
        }
    }
}
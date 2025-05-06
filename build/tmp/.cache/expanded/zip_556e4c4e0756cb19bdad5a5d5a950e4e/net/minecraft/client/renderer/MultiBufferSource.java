package net.minecraft.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectSortedMaps;
import java.util.HashMap;
import java.util.Map;
import java.util.SequencedMap;
import javax.annotation.Nullable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface MultiBufferSource {
    static MultiBufferSource.BufferSource immediate(ByteBufferBuilder pSharedBuffer) {
        return immediateWithBuffers(Object2ObjectSortedMaps.emptyMap(), pSharedBuffer);
    }

    static MultiBufferSource.BufferSource immediateWithBuffers(SequencedMap<RenderType, ByteBufferBuilder> pFixedBuffers, ByteBufferBuilder pSharedBuffer) {
        return new MultiBufferSource.BufferSource(pSharedBuffer, pFixedBuffers);
    }

    VertexConsumer getBuffer(RenderType pRenderType);

    @OnlyIn(Dist.CLIENT)
    public static class BufferSource implements MultiBufferSource {
        protected final ByteBufferBuilder sharedBuffer;
        protected final SequencedMap<RenderType, ByteBufferBuilder> fixedBuffers;
        protected final Map<RenderType, BufferBuilder> startedBuilders = new HashMap<>();
        @Nullable
        protected RenderType lastSharedType;

        protected BufferSource(ByteBufferBuilder pSharedBuffer, SequencedMap<RenderType, ByteBufferBuilder> pFixedBuffers) {
            this.sharedBuffer = pSharedBuffer;
            this.fixedBuffers = pFixedBuffers;
        }

        @Override
        public VertexConsumer getBuffer(RenderType p_109919_) {
            BufferBuilder bufferbuilder = this.startedBuilders.get(p_109919_);
            if (bufferbuilder != null && !p_109919_.canConsolidateConsecutiveGeometry()) {
                this.endBatch(p_109919_, bufferbuilder);
                bufferbuilder = null;
            }

            if (bufferbuilder != null) {
                return bufferbuilder;
            } else {
                ByteBufferBuilder bytebufferbuilder = this.fixedBuffers.get(p_109919_);
                if (bytebufferbuilder != null) {
                    bufferbuilder = new BufferBuilder(bytebufferbuilder, p_109919_.mode(), p_109919_.format());
                } else {
                    if (this.lastSharedType != null) {
                        this.endBatch(this.lastSharedType);
                    }

                    bufferbuilder = new BufferBuilder(this.sharedBuffer, p_109919_.mode(), p_109919_.format());
                    this.lastSharedType = p_109919_;
                }

                this.startedBuilders.put(p_109919_, bufferbuilder);
                return bufferbuilder;
            }
        }

        public void endLastBatch() {
            if (this.lastSharedType != null) {
                this.endBatch(this.lastSharedType);
                this.lastSharedType = null;
            }
        }

        public void endBatch() {
            this.endLastBatch();

            for (RenderType rendertype : this.fixedBuffers.keySet()) {
                this.endBatch(rendertype);
            }
        }

        public void endBatch(RenderType pRenderType) {
            BufferBuilder bufferbuilder = this.startedBuilders.remove(pRenderType);
            if (bufferbuilder != null) {
                this.endBatch(pRenderType, bufferbuilder);
            }
        }

        private void endBatch(RenderType pRenderType, BufferBuilder pBuilder) {
            MeshData meshdata = pBuilder.build();
            if (meshdata != null) {
                if (pRenderType.sortOnUpload()) {
                    ByteBufferBuilder bytebufferbuilder = this.fixedBuffers.getOrDefault(pRenderType, this.sharedBuffer);
                    meshdata.sortQuads(bytebufferbuilder, RenderSystem.getProjectionType().vertexSorting());
                }

                pRenderType.draw(meshdata);
            }

            if (pRenderType.equals(this.lastSharedType)) {
                this.lastSharedType = null;
            }
        }
    }
}
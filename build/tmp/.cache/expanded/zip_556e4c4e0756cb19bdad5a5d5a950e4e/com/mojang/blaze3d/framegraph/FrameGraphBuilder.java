package com.mojang.blaze3d.framegraph;

import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.ResourceDescriptor;
import com.mojang.blaze3d.resource.ResourceHandle;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FrameGraphBuilder {
    private final List<FrameGraphBuilder.InternalVirtualResource<?>> internalResources = new ArrayList<>();
    private final List<FrameGraphBuilder.ExternalResource<?>> externalResources = new ArrayList<>();
    private final List<FrameGraphBuilder.Pass> passes = new ArrayList<>();

    public FramePass addPass(String pName) {
        FrameGraphBuilder.Pass framegraphbuilder$pass = new FrameGraphBuilder.Pass(this.passes.size(), pName);
        this.passes.add(framegraphbuilder$pass);
        return framegraphbuilder$pass;
    }

    public <T> ResourceHandle<T> importExternal(String pName, T pResource) {
        FrameGraphBuilder.ExternalResource<T> externalresource = new FrameGraphBuilder.ExternalResource<>(pName, null, pResource);
        this.externalResources.add(externalresource);
        return externalresource.handle;
    }

    public <T> ResourceHandle<T> createInternal(String pName, ResourceDescriptor<T> pDescriptor) {
        return this.createInternalResource(pName, pDescriptor, null).handle;
    }

    <T> FrameGraphBuilder.InternalVirtualResource<T> createInternalResource(String pName, ResourceDescriptor<T> pDescriptor, @Nullable FrameGraphBuilder.Pass pCreatedBy) {
        int i = this.internalResources.size();
        FrameGraphBuilder.InternalVirtualResource<T> internalvirtualresource = new FrameGraphBuilder.InternalVirtualResource<>(
            i, pName, pCreatedBy, pDescriptor
        );
        this.internalResources.add(internalvirtualresource);
        return internalvirtualresource;
    }

    public void execute(GraphicsResourceAllocator pAllocator) {
        this.execute(pAllocator, FrameGraphBuilder.Inspector.NONE);
    }

    public void execute(GraphicsResourceAllocator pAllocator, FrameGraphBuilder.Inspector pInspector) {
        BitSet bitset = this.identifyPassesToKeep();
        List<FrameGraphBuilder.Pass> list = new ArrayList<>(bitset.cardinality());
        BitSet bitset1 = new BitSet(this.passes.size());

        for (FrameGraphBuilder.Pass framegraphbuilder$pass : this.passes) {
            this.resolvePassOrder(framegraphbuilder$pass, bitset, bitset1, list);
        }

        this.assignResourceLifetimes(list);

        for (FrameGraphBuilder.Pass framegraphbuilder$pass1 : list) {
            for (FrameGraphBuilder.InternalVirtualResource<?> internalvirtualresource : framegraphbuilder$pass1.resourcesToAcquire) {
                pInspector.acquireResource(internalvirtualresource.name);
                internalvirtualresource.acquire(pAllocator);
            }

            pInspector.beforeExecutePass(framegraphbuilder$pass1.name);
            framegraphbuilder$pass1.task.run();
            pInspector.afterExecutePass(framegraphbuilder$pass1.name);

            for (int i = framegraphbuilder$pass1.resourcesToRelease.nextSetBit(0); i >= 0; i = framegraphbuilder$pass1.resourcesToRelease.nextSetBit(i + 1)) {
                FrameGraphBuilder.InternalVirtualResource<?> internalvirtualresource1 = this.internalResources.get(i);
                pInspector.releaseResource(internalvirtualresource1.name);
                internalvirtualresource1.release(pAllocator);
            }
        }
    }

    private BitSet identifyPassesToKeep() {
        Deque<FrameGraphBuilder.Pass> deque = new ArrayDeque<>(this.passes.size());
        BitSet bitset = new BitSet(this.passes.size());

        for (FrameGraphBuilder.VirtualResource<?> virtualresource : this.externalResources) {
            FrameGraphBuilder.Pass framegraphbuilder$pass = virtualresource.handle.createdBy;
            if (framegraphbuilder$pass != null) {
                this.discoverAllRequiredPasses(framegraphbuilder$pass, bitset, deque);
            }
        }

        for (FrameGraphBuilder.Pass framegraphbuilder$pass1 : this.passes) {
            if (framegraphbuilder$pass1.disableCulling) {
                this.discoverAllRequiredPasses(framegraphbuilder$pass1, bitset, deque);
            }
        }

        return bitset;
    }

    private void discoverAllRequiredPasses(FrameGraphBuilder.Pass pPass, BitSet pPassesToKeep, Deque<FrameGraphBuilder.Pass> pOutput) {
        pOutput.add(pPass);

        while (!pOutput.isEmpty()) {
            FrameGraphBuilder.Pass framegraphbuilder$pass = pOutput.poll();
            if (!pPassesToKeep.get(framegraphbuilder$pass.id)) {
                pPassesToKeep.set(framegraphbuilder$pass.id);

                for (int i = framegraphbuilder$pass.requiredPassIds.nextSetBit(0); i >= 0; i = framegraphbuilder$pass.requiredPassIds.nextSetBit(i + 1)) {
                    pOutput.add(this.passes.get(i));
                }
            }
        }
    }

    private void resolvePassOrder(FrameGraphBuilder.Pass pPass, BitSet pPassesToKeep, BitSet pOutput, List<FrameGraphBuilder.Pass> pOrderedPasses) {
        if (pOutput.get(pPass.id)) {
            String s = pOutput.stream().mapToObj(p_368248_ -> this.passes.get(p_368248_).name).collect(Collectors.joining(", "));
            throw new IllegalStateException("Frame graph cycle detected between " + s);
        } else if (pPassesToKeep.get(pPass.id)) {
            pOutput.set(pPass.id);
            pPassesToKeep.clear(pPass.id);

            for (int i = pPass.requiredPassIds.nextSetBit(0); i >= 0; i = pPass.requiredPassIds.nextSetBit(i + 1)) {
                this.resolvePassOrder(this.passes.get(i), pPassesToKeep, pOutput, pOrderedPasses);
            }

            for (FrameGraphBuilder.Handle<?> handle : pPass.writesFrom) {
                for (int j = handle.readBy.nextSetBit(0); j >= 0; j = handle.readBy.nextSetBit(j + 1)) {
                    if (j != pPass.id) {
                        this.resolvePassOrder(this.passes.get(j), pPassesToKeep, pOutput, pOrderedPasses);
                    }
                }
            }

            pOrderedPasses.add(pPass);
            pOutput.clear(pPass.id);
        }
    }

    private void assignResourceLifetimes(Collection<FrameGraphBuilder.Pass> pPasses) {
        FrameGraphBuilder.Pass[] aframegraphbuilder$pass = new FrameGraphBuilder.Pass[this.internalResources.size()];

        for (FrameGraphBuilder.Pass framegraphbuilder$pass : pPasses) {
            for (int i = framegraphbuilder$pass.requiredResourceIds.nextSetBit(0); i >= 0; i = framegraphbuilder$pass.requiredResourceIds.nextSetBit(i + 1)) {
                FrameGraphBuilder.InternalVirtualResource<?> internalvirtualresource = this.internalResources.get(i);
                FrameGraphBuilder.Pass framegraphbuilder$pass1 = aframegraphbuilder$pass[i];
                aframegraphbuilder$pass[i] = framegraphbuilder$pass;
                if (framegraphbuilder$pass1 == null) {
                    framegraphbuilder$pass.resourcesToAcquire.add(internalvirtualresource);
                } else {
                    framegraphbuilder$pass1.resourcesToRelease.clear(i);
                }

                framegraphbuilder$pass.resourcesToRelease.set(i);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class ExternalResource<T> extends FrameGraphBuilder.VirtualResource<T> {
        private final T resource;

        public ExternalResource(String pName, @Nullable FrameGraphBuilder.Pass pCreatedBy, T pResource) {
            super(pName, pCreatedBy);
            this.resource = pResource;
        }

        @Override
        public T get() {
            return this.resource;
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class Handle<T> implements ResourceHandle<T> {
        final FrameGraphBuilder.VirtualResource<T> holder;
        private final int version;
        @Nullable
        final FrameGraphBuilder.Pass createdBy;
        final BitSet readBy = new BitSet();
        @Nullable
        private FrameGraphBuilder.Handle<T> aliasedBy;

        Handle(FrameGraphBuilder.VirtualResource<T> pHolder, int pVersion, @Nullable FrameGraphBuilder.Pass pCreatedBy) {
            this.holder = pHolder;
            this.version = pVersion;
            this.createdBy = pCreatedBy;
        }

        @Override
        public T get() {
            return this.holder.get();
        }

        FrameGraphBuilder.Handle<T> writeAndAlias(FrameGraphBuilder.Pass pAlias) {
            if (this.holder.handle != this) {
                throw new IllegalStateException("Handle " + this + " is no longer valid, as its contents were moved into " + this.aliasedBy);
            } else {
                FrameGraphBuilder.Handle<T> handle = new FrameGraphBuilder.Handle<>(this.holder, this.version + 1, pAlias);
                this.holder.handle = handle;
                this.aliasedBy = handle;
                return handle;
            }
        }

        @Override
        public String toString() {
            return this.createdBy != null ? this.holder + "#" + this.version + " (from " + this.createdBy + ")" : this.holder + "#" + this.version;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public interface Inspector {
        FrameGraphBuilder.Inspector NONE = new FrameGraphBuilder.Inspector() {};

        default void acquireResource(String pName) {
        }

        default void releaseResource(String pName) {
        }

        default void beforeExecutePass(String pName) {
        }

        default void afterExecutePass(String pName) {
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class InternalVirtualResource<T> extends FrameGraphBuilder.VirtualResource<T> {
        final int id;
        private final ResourceDescriptor<T> descriptor;
        @Nullable
        private T physicalResource;

        public InternalVirtualResource(int pId, String pName, @Nullable FrameGraphBuilder.Pass pCreatedBy, ResourceDescriptor<T> pDescriptor) {
            super(pName, pCreatedBy);
            this.id = pId;
            this.descriptor = pDescriptor;
        }

        @Override
        public T get() {
            return Objects.requireNonNull(this.physicalResource, "Resource is not currently available");
        }

        public void acquire(GraphicsResourceAllocator pAllocator) {
            if (this.physicalResource != null) {
                throw new IllegalStateException("Tried to acquire physical resource, but it was already assigned");
            } else {
                this.physicalResource = pAllocator.acquire(this.descriptor);
            }
        }

        public void release(GraphicsResourceAllocator pAllocator) {
            if (this.physicalResource == null) {
                throw new IllegalStateException("Tried to release physical resource that was not allocated");
            } else {
                pAllocator.release(this.descriptor, this.physicalResource);
                this.physicalResource = null;
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    class Pass implements FramePass {
        final int id;
        final String name;
        final List<FrameGraphBuilder.Handle<?>> writesFrom = new ArrayList<>();
        final BitSet requiredResourceIds = new BitSet();
        final BitSet requiredPassIds = new BitSet();
        Runnable task = () -> {};
        final List<FrameGraphBuilder.InternalVirtualResource<?>> resourcesToAcquire = new ArrayList<>();
        final BitSet resourcesToRelease = new BitSet();
        boolean disableCulling;

        public Pass(final int pId, final String pName) {
            this.id = pId;
            this.name = pName;
        }

        private <T> void markResourceRequired(FrameGraphBuilder.Handle<T> pHandle) {
            if (pHandle.holder instanceof FrameGraphBuilder.InternalVirtualResource<?> internalvirtualresource) {
                this.requiredResourceIds.set(internalvirtualresource.id);
            }
        }

        private void markPassRequired(FrameGraphBuilder.Pass pPass) {
            this.requiredPassIds.set(pPass.id);
        }

        @Override
        public <T> ResourceHandle<T> createsInternal(String p_362510_, ResourceDescriptor<T> p_365149_) {
            FrameGraphBuilder.InternalVirtualResource<T> internalvirtualresource = FrameGraphBuilder.this.createInternalResource(p_362510_, p_365149_, this);
            this.requiredResourceIds.set(internalvirtualresource.id);
            return internalvirtualresource.handle;
        }

        @Override
        public <T> void reads(ResourceHandle<T> p_366230_) {
            this._reads((FrameGraphBuilder.Handle<T>)p_366230_);
        }

        private <T> void _reads(FrameGraphBuilder.Handle<T> pHandle) {
            this.markResourceRequired(pHandle);
            if (pHandle.createdBy != null) {
                this.markPassRequired(pHandle.createdBy);
            }

            pHandle.readBy.set(this.id);
        }

        @Override
        public <T> ResourceHandle<T> readsAndWrites(ResourceHandle<T> p_366708_) {
            return this._readsAndWrites((FrameGraphBuilder.Handle<T>)p_366708_);
        }

        @Override
        public void requires(FramePass p_367159_) {
            this.requiredPassIds.set(((FrameGraphBuilder.Pass)p_367159_).id);
        }

        @Override
        public void disableCulling() {
            this.disableCulling = true;
        }

        private <T> FrameGraphBuilder.Handle<T> _readsAndWrites(FrameGraphBuilder.Handle<T> pHandle) {
            this.writesFrom.add(pHandle);
            this._reads(pHandle);
            return pHandle.writeAndAlias(this);
        }

        @Override
        public void executes(Runnable p_366784_) {
            this.task = p_366784_;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    @OnlyIn(Dist.CLIENT)
    abstract static class VirtualResource<T> {
        public final String name;
        public FrameGraphBuilder.Handle<T> handle;

        public VirtualResource(String pName, @Nullable FrameGraphBuilder.Pass pCreatedBy) {
            this.name = pName;
            this.handle = new FrameGraphBuilder.Handle<>(this, 0, pCreatedBy);
        }

        public abstract T get();

        @Override
        public String toString() {
            return this.name;
        }
    }
}
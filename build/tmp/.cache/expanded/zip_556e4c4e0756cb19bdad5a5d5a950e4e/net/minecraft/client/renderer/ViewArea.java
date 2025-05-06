package net.minecraft.client.renderer;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ViewArea {
    protected final LevelRenderer levelRenderer;
    protected final Level level;
    protected int sectionGridSizeY;
    protected int sectionGridSizeX;
    protected int sectionGridSizeZ;
    private int viewDistance;
    private SectionPos cameraSectionPos;
    public SectionRenderDispatcher.RenderSection[] sections;

    public ViewArea(SectionRenderDispatcher pSectionRenderDispatcher, Level pLevel, int pViewDistance, LevelRenderer pLevelRenderer) {
        this.levelRenderer = pLevelRenderer;
        this.level = pLevel;
        this.setViewDistance(pViewDistance);
        this.createSections(pSectionRenderDispatcher);
        this.cameraSectionPos = SectionPos.of(this.viewDistance + 1, 0, this.viewDistance + 1);
    }

    protected void createSections(SectionRenderDispatcher pSectionRenderDispatcher) {
        if (!Minecraft.getInstance().isSameThread()) {
            throw new IllegalStateException("createSections called from wrong thread: " + Thread.currentThread().getName());
        } else {
            int i = this.sectionGridSizeX * this.sectionGridSizeY * this.sectionGridSizeZ;
            this.sections = new SectionRenderDispatcher.RenderSection[i];

            for (int j = 0; j < this.sectionGridSizeX; j++) {
                for (int k = 0; k < this.sectionGridSizeY; k++) {
                    for (int l = 0; l < this.sectionGridSizeZ; l++) {
                        int i1 = this.getSectionIndex(j, k, l);
                        this.sections[i1] = pSectionRenderDispatcher.new RenderSection(i1, SectionPos.asLong(j, k + this.level.getMinSectionY(), l));
                    }
                }
            }
        }
    }

    public void releaseAllBuffers() {
        for (SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection : this.sections) {
            sectionrenderdispatcher$rendersection.reset();
        }
    }

    private int getSectionIndex(int pX, int pY, int pZ) {
        return (pZ * this.sectionGridSizeY + pY) * this.sectionGridSizeX + pX;
    }

    protected void setViewDistance(int pRenderDistanceChunks) {
        int i = pRenderDistanceChunks * 2 + 1;
        this.sectionGridSizeX = i;
        this.sectionGridSizeY = this.level.getSectionsCount();
        this.sectionGridSizeZ = i;
        this.viewDistance = pRenderDistanceChunks;
    }

    public int getViewDistance() {
        return this.viewDistance;
    }

    public LevelHeightAccessor getLevelHeightAccessor() {
        return this.level;
    }

    public void repositionCamera(SectionPos pNewSectionPos) {
        for (int i = 0; i < this.sectionGridSizeX; i++) {
            int j = pNewSectionPos.x() - this.viewDistance;
            int k = j + Math.floorMod(i - j, this.sectionGridSizeX);

            for (int l = 0; l < this.sectionGridSizeZ; l++) {
                int i1 = pNewSectionPos.z() - this.viewDistance;
                int j1 = i1 + Math.floorMod(l - i1, this.sectionGridSizeZ);

                for (int k1 = 0; k1 < this.sectionGridSizeY; k1++) {
                    int l1 = this.level.getMinSectionY() + k1;
                    SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = this.sections[this.getSectionIndex(i, k1, l)];
                    long i2 = sectionrenderdispatcher$rendersection.getSectionNode();
                    if (i2 != SectionPos.asLong(k, l1, j1)) {
                        sectionrenderdispatcher$rendersection.setSectionNode(SectionPos.asLong(k, l1, j1));
                    }
                }
            }
        }

        this.cameraSectionPos = pNewSectionPos;
        this.levelRenderer.getSectionOcclusionGraph().invalidate();
    }

    public SectionPos getCameraSectionPos() {
        return this.cameraSectionPos;
    }

    public void setDirty(int pSectionX, int pSectionY, int pSectionZ, boolean pReRenderOnMainThread) {
        SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = this.getRenderSection(pSectionX, pSectionY, pSectionZ);
        if (sectionrenderdispatcher$rendersection != null) {
            sectionrenderdispatcher$rendersection.setDirty(pReRenderOnMainThread);
        }
    }

    @Nullable
    protected SectionRenderDispatcher.RenderSection getRenderSectionAt(BlockPos pPos) {
        return this.getRenderSection(SectionPos.asLong(pPos));
    }

    @Nullable
    protected SectionRenderDispatcher.RenderSection getRenderSection(long pSectionPos) {
        int i = SectionPos.x(pSectionPos);
        int j = SectionPos.y(pSectionPos);
        int k = SectionPos.z(pSectionPos);
        return this.getRenderSection(i, j, k);
    }

    @Nullable
    private SectionRenderDispatcher.RenderSection getRenderSection(int pX, int pY, int pZ) {
        if (!this.containsSection(pX, pY, pZ)) {
            return null;
        } else {
            int i = pY - this.level.getMinSectionY();
            int j = Math.floorMod(pX, this.sectionGridSizeX);
            int k = Math.floorMod(pZ, this.sectionGridSizeZ);
            return this.sections[this.getSectionIndex(j, i, k)];
        }
    }

    private boolean containsSection(int pX, int pY, int pZ) {
        if (pY >= this.level.getMinSectionY() && pY <= this.level.getMaxSectionY()) {
            return pX < this.cameraSectionPos.x() - this.viewDistance || pX > this.cameraSectionPos.x() + this.viewDistance
                ? false
                : pZ >= this.cameraSectionPos.z() - this.viewDistance && pZ <= this.cameraSectionPos.z() + this.viewDistance;
        } else {
            return false;
        }
    }
}
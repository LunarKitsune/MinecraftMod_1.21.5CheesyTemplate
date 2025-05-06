package net.minecraft.client.renderer;

import javax.annotation.Nullable;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class Octree {
    private final Octree.Branch root;
    final BlockPos cameraSectionCenter;

    public Octree(SectionPos pCameraSectionPos, int pViewDistance, int pSectionGridSizeY, int pMinY) {
        int i = pViewDistance * 2 + 1;
        int j = Mth.smallestEncompassingPowerOfTwo(i);
        int k = pViewDistance * 16;
        BlockPos blockpos = pCameraSectionPos.origin();
        this.cameraSectionCenter = pCameraSectionPos.center();
        int l = blockpos.getX() - k;
        int i1 = l + j * 16 - 1;
        int j1 = j >= pSectionGridSizeY ? pMinY : blockpos.getY() - k;
        int k1 = j1 + j * 16 - 1;
        int l1 = blockpos.getZ() - k;
        int i2 = l1 + j * 16 - 1;
        this.root = new Octree.Branch(new BoundingBox(l, j1, l1, i1, k1, i2));
    }

    public boolean add(SectionRenderDispatcher.RenderSection pSection) {
        return this.root.add(pSection);
    }

    public void visitNodes(Octree.OctreeVisitor pVisitor, Frustum pFrustum, int pNearbyRadius) {
        this.root.visitNodes(pVisitor, false, pFrustum, 0, pNearbyRadius, true);
    }

    boolean isClose(double pMinX, double pMinY, double pMinZ, double pMaxX, double pMaxY, double pMaxZ, int pRadius) {
        int i = this.cameraSectionCenter.getX();
        int j = this.cameraSectionCenter.getY();
        int k = this.cameraSectionCenter.getZ();
        return i > pMinX - pRadius
            && i < pMaxX + pRadius
            && j > pMinY - pRadius
            && j < pMaxY + pRadius
            && k > pMinZ - pRadius
            && k < pMaxZ + pRadius;
    }

    @OnlyIn(Dist.CLIENT)
    static enum AxisSorting {
        XYZ(4, 2, 1),
        XZY(4, 1, 2),
        YXZ(2, 4, 1),
        YZX(1, 4, 2),
        ZXY(2, 1, 4),
        ZYX(1, 2, 4);

        final int xShift;
        final int yShift;
        final int zShift;

        private AxisSorting(final int pXShift, final int pYShift, final int pZShift) {
            this.xShift = pXShift;
            this.yShift = pYShift;
            this.zShift = pZShift;
        }

        public static Octree.AxisSorting getAxisSorting(int pXDiff, int pYDiff, int pZDiff) {
            if (pXDiff > pYDiff && pXDiff > pZDiff) {
                return pYDiff > pZDiff ? XYZ : XZY;
            } else if (pYDiff > pXDiff && pYDiff > pZDiff) {
                return pXDiff > pZDiff ? YXZ : YZX;
            } else {
                return pXDiff > pYDiff ? ZXY : ZYX;
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    class Branch implements Octree.Node {
        private final Octree.Node[] nodes = new Octree.Node[8];
        private final BoundingBox boundingBox;
        private final int bbCenterX;
        private final int bbCenterY;
        private final int bbCenterZ;
        private final Octree.AxisSorting sorting;
        private final boolean cameraXDiffNegative;
        private final boolean cameraYDiffNegative;
        private final boolean cameraZDiffNegative;

        public Branch(final BoundingBox pBoundingBox) {
            this.boundingBox = pBoundingBox;
            this.bbCenterX = this.boundingBox.minX() + this.boundingBox.getXSpan() / 2;
            this.bbCenterY = this.boundingBox.minY() + this.boundingBox.getYSpan() / 2;
            this.bbCenterZ = this.boundingBox.minZ() + this.boundingBox.getZSpan() / 2;
            int i = Octree.this.cameraSectionCenter.getX() - this.bbCenterX;
            int j = Octree.this.cameraSectionCenter.getY() - this.bbCenterY;
            int k = Octree.this.cameraSectionCenter.getZ() - this.bbCenterZ;
            this.sorting = Octree.AxisSorting.getAxisSorting(Math.abs(i), Math.abs(j), Math.abs(k));
            this.cameraXDiffNegative = i < 0;
            this.cameraYDiffNegative = j < 0;
            this.cameraZDiffNegative = k < 0;
        }

        public boolean add(SectionRenderDispatcher.RenderSection pSection) {
            long i = pSection.getSectionNode();
            boolean flag = SectionPos.sectionToBlockCoord(SectionPos.x(i)) - this.bbCenterX < 0;
            boolean flag1 = SectionPos.sectionToBlockCoord(SectionPos.y(i)) - this.bbCenterY < 0;
            boolean flag2 = SectionPos.sectionToBlockCoord(SectionPos.z(i)) - this.bbCenterZ < 0;
            boolean flag3 = flag != this.cameraXDiffNegative;
            boolean flag4 = flag1 != this.cameraYDiffNegative;
            boolean flag5 = flag2 != this.cameraZDiffNegative;
            int j = getNodeIndex(this.sorting, flag3, flag4, flag5);
            if (this.areChildrenLeaves()) {
                boolean flag6 = this.nodes[j] != null;
                this.nodes[j] = Octree.this.new Leaf(pSection);
                return !flag6;
            } else if (this.nodes[j] != null) {
                Octree.Branch octree$branch1 = (Octree.Branch)this.nodes[j];
                return octree$branch1.add(pSection);
            } else {
                BoundingBox boundingbox = this.createChildBoundingBox(flag, flag1, flag2);
                Octree.Branch octree$branch = Octree.this.new Branch(boundingbox);
                this.nodes[j] = octree$branch;
                return octree$branch.add(pSection);
            }
        }

        private static int getNodeIndex(Octree.AxisSorting pSorting, boolean pXDiffNegative, boolean pYDiffNegative, boolean pZDiffNegative) {
            int i = 0;
            if (pXDiffNegative) {
                i += pSorting.xShift;
            }

            if (pYDiffNegative) {
                i += pSorting.yShift;
            }

            if (pZDiffNegative) {
                i += pSorting.zShift;
            }

            return i;
        }

        private boolean areChildrenLeaves() {
            return this.boundingBox.getXSpan() == 32;
        }

        private BoundingBox createChildBoundingBox(boolean pXDiffNegative, boolean pYDiffNegative, boolean pZDiffNegative) {
            int i;
            int j;
            if (pXDiffNegative) {
                i = this.boundingBox.minX();
                j = this.bbCenterX - 1;
            } else {
                i = this.bbCenterX;
                j = this.boundingBox.maxX();
            }

            int k;
            int l;
            if (pYDiffNegative) {
                k = this.boundingBox.minY();
                l = this.bbCenterY - 1;
            } else {
                k = this.bbCenterY;
                l = this.boundingBox.maxY();
            }

            int i1;
            int j1;
            if (pZDiffNegative) {
                i1 = this.boundingBox.minZ();
                j1 = this.bbCenterZ - 1;
            } else {
                i1 = this.bbCenterZ;
                j1 = this.boundingBox.maxZ();
            }

            return new BoundingBox(i, k, i1, j, l, j1);
        }

        @Override
        public void visitNodes(Octree.OctreeVisitor p_369870_, boolean p_363049_, Frustum p_363949_, int p_363158_, int p_368250_, boolean p_369443_) {
            boolean flag = p_363049_;
            if (!p_363049_) {
                int i = p_363949_.cubeInFrustum(this.boundingBox);
                p_363049_ = i == -2;
                flag = i == -2 || i == -1;
            }

            if (flag) {
                p_369443_ = p_369443_
                    && Octree.this.isClose(
                        this.boundingBox.minX(),
                        this.boundingBox.minY(),
                        this.boundingBox.minZ(),
                        this.boundingBox.maxX(),
                        this.boundingBox.maxY(),
                        this.boundingBox.maxZ(),
                        p_368250_
                    );
                p_369870_.visit(this, p_363049_, p_363158_, p_369443_);

                for (Octree.Node octree$node : this.nodes) {
                    if (octree$node != null) {
                        octree$node.visitNodes(p_369870_, p_363049_, p_363949_, p_363158_ + 1, p_368250_, p_369443_);
                    }
                }
            }
        }

        @Nullable
        @Override
        public SectionRenderDispatcher.RenderSection getSection() {
            return null;
        }

        @Override
        public AABB getAABB() {
            return new AABB(
                this.boundingBox.minX(),
                this.boundingBox.minY(),
                this.boundingBox.minZ(),
                this.boundingBox.maxX() + 1,
                this.boundingBox.maxY() + 1,
                this.boundingBox.maxZ() + 1
            );
        }
    }

    @OnlyIn(Dist.CLIENT)
    final class Leaf implements Octree.Node {
        private final SectionRenderDispatcher.RenderSection section;

        Leaf(final SectionRenderDispatcher.RenderSection pSection) {
            this.section = pSection;
        }

        @Override
        public void visitNodes(Octree.OctreeVisitor p_366276_, boolean p_365424_, Frustum p_366156_, int p_361139_, int p_366518_, boolean p_368604_) {
            AABB aabb = this.section.getBoundingBox();
            if (p_365424_ || p_366156_.isVisible(this.getSection().getBoundingBox())) {
                p_368604_ = p_368604_
                    && Octree.this.isClose(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ, p_366518_);
                p_366276_.visit(this, p_365424_, p_361139_, p_368604_);
            }
        }

        @Override
        public SectionRenderDispatcher.RenderSection getSection() {
            return this.section;
        }

        @Override
        public AABB getAABB() {
            return this.section.getBoundingBox();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public interface Node {
        void visitNodes(Octree.OctreeVisitor pVisitor, boolean pIsLeafNode, Frustum pFrustum, int pRecursionDepth, int pNearbyRadius, boolean pIsNearby);

        @Nullable
        SectionRenderDispatcher.RenderSection getSection();

        AABB getAABB();
    }

    @FunctionalInterface
    @OnlyIn(Dist.CLIENT)
    public interface OctreeVisitor {
        void visit(Octree.Node pNode, boolean pIsLeafNode, int pRecursionDepth, boolean pIsNearby);
    }
}
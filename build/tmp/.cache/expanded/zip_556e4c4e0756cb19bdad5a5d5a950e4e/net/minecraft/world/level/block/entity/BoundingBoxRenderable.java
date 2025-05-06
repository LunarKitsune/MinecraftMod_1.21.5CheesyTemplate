package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

public interface BoundingBoxRenderable {
    BoundingBoxRenderable.Mode renderMode();

    BoundingBoxRenderable.RenderableBox getRenderableBox();

    public static enum Mode {
        NONE,
        BOX,
        BOX_AND_INVISIBLE_BLOCKS;
    }

    public record RenderableBox(BlockPos localPos, Vec3i size) {
        public static BoundingBoxRenderable.RenderableBox fromCorners(int pX1, int pY1, int pZ1, int pX2, int pY2, int pZ2) {
            int i = Math.min(pX1, pX2);
            int j = Math.min(pY1, pY2);
            int k = Math.min(pZ1, pZ2);
            return new BoundingBoxRenderable.RenderableBox(
                new BlockPos(i, j, k), new Vec3i(Math.max(pX1, pX2) - i, Math.max(pY1, pY2) - j, Math.max(pZ1, pZ2) - k)
            );
        }
    }
}
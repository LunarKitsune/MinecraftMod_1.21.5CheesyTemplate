package net.minecraft.client.renderer.block;

import java.util.Map;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BlockModelShaper {
    private Map<BlockState, BlockStateModel> modelByStateCache = Map.of();
    private final ModelManager modelManager;

    public BlockModelShaper(ModelManager pModelManager) {
        this.modelManager = pModelManager;
    }

    /** @deprecated Forge: Use {@link #getParticleIcon(BlockState, net.minecraft.world.level.Level, net.minecraft.core.BlockPos) getParticleIcon(BlockState, Level, Pos)} */
    public TextureAtlasSprite getParticleIcon(BlockState pState) {
        return this.getBlockModel(pState).particleIcon(net.minecraftforge.client.model.data.ModelData.EMPTY);
    }

    public TextureAtlasSprite getParticleIcon(BlockState pState, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
       return this.getBlockModel(pState).particleIcon(level.getModelDataManager().getAt(pos));
    }

    public BlockStateModel getBlockModel(BlockState pState) {
        BlockStateModel blockstatemodel = this.modelByStateCache.get(pState);
        if (blockstatemodel == null) {
            blockstatemodel = this.modelManager.getMissingBlockStateModel();
        }

        return blockstatemodel;
    }

    public ModelManager getModelManager() {
        return this.modelManager;
    }

    public void replaceCache(Map<BlockState, BlockStateModel> pModelByStateCache) {
        this.modelByStateCache = pModelByStateCache;
    }
}

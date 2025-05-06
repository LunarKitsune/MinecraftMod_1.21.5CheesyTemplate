package net.minecraft.client.resources.model;

import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ModelGroupCollector {
    static final int SINGLETON_MODEL_GROUP = -1;
    private static final int INVISIBLE_MODEL_GROUP = 0;

    public static Object2IntMap<BlockState> build(BlockColors pBlockColors, BlockStateModelLoader.LoadedModels pLoadedModels) {
        Map<Block, List<Property<?>>> map = new HashMap<>();
        Map<ModelGroupCollector.GroupKey, Set<BlockState>> map1 = new HashMap<>();
        pLoadedModels.models().forEach((p_389610_, p_389611_) -> {
            List<Property<?>> list = map.computeIfAbsent(p_389610_.getBlock(), p_361060_ -> List.copyOf(pBlockColors.getColoringProperties(p_361060_)));
            ModelGroupCollector.GroupKey modelgroupcollector$groupkey = ModelGroupCollector.GroupKey.create(p_389610_, p_389611_, list);
            map1.computeIfAbsent(modelgroupcollector$groupkey, p_367245_ -> Sets.newIdentityHashSet()).add(p_389610_);
        });
        int i = 1;
        Object2IntMap<BlockState> object2intmap = new Object2IntOpenHashMap<>();
        object2intmap.defaultReturnValue(-1);

        for (Set<BlockState> set : map1.values()) {
            Iterator<BlockState> iterator = set.iterator();

            while (iterator.hasNext()) {
                BlockState blockstate = iterator.next();
                if (blockstate.getRenderShape() != RenderShape.MODEL) {
                    iterator.remove();
                    object2intmap.put(blockstate, 0);
                }
            }

            if (set.size() > 1) {
                int j = i++;
                set.forEach(p_362909_ -> object2intmap.put(p_362909_, j));
            }
        }

        return object2intmap;
    }

    @OnlyIn(Dist.CLIENT)
    record GroupKey(Object equalityGroup, List<Object> coloringValues) {
        public static ModelGroupCollector.GroupKey create(BlockState pState, BlockStateModel.UnbakedRoot pRoot, List<Property<?>> pProperties) {
            List<Object> list = getColoringValues(pState, pProperties);
            Object object = pRoot.visualEqualityGroup(pState);
            return new ModelGroupCollector.GroupKey(object, list);
        }

        private static List<Object> getColoringValues(BlockState pState, List<Property<?>> pProperties) {
            Object[] aobject = new Object[pProperties.size()];

            for (int i = 0; i < pProperties.size(); i++) {
                aobject[i] = pState.getValue(pProperties.get(i));
            }

            return List.of(aobject);
        }
    }
}
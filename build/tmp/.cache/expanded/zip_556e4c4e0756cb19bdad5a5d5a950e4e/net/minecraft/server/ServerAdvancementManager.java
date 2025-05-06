package net.minecraft.server;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.advancements.TreeNodePosition;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

public class ServerAdvancementManager extends SimpleJsonResourceReloadListener<Advancement> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private Map<ResourceLocation, AdvancementHolder> advancements = Map.of();
    private AdvancementTree tree = new AdvancementTree();
    private final HolderLookup.Provider registries;

    /** @deprecated Forge: use {@linkplain ServerAdvancementManager#ServerAdvancementManager(net.minecraftforge.common.crafting.conditions.ICondition.IContext) constructor with context}. */
    public ServerAdvancementManager(HolderLookup.Provider pRegistries) {
        this(pRegistries, net.minecraftforge.common.crafting.conditions.ICondition.IContext.EMPTY);
    }

    public ServerAdvancementManager(HolderLookup.Provider pRegistries, net.minecraftforge.common.crafting.conditions.ICondition.IContext context) {
        super(pRegistries, Advancement.CODEC, Registries.ADVANCEMENT);
        this.registries = pRegistries;
    }

    protected void apply(Map<ResourceLocation, Advancement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        Builder<ResourceLocation, AdvancementHolder> builder = ImmutableMap.builder();
        pObject.forEach((p_358541_, p_358542_) -> {
            this.validate(p_358541_, p_358542_);
            builder.put(p_358541_, new AdvancementHolder(p_358541_, p_358542_));
        });
        this.advancements = builder.buildOrThrow();
        AdvancementTree advancementtree = new AdvancementTree();
        advancementtree.addAll(this.advancements.values());

        for (AdvancementNode advancementnode : advancementtree.roots()) {
            if (advancementnode.holder().value().display().isPresent()) {
                TreeNodePosition.run(advancementnode);
            }
        }

        this.tree = advancementtree;
    }

    private void validate(ResourceLocation pLocation, Advancement pAdvancement) {
        ProblemReporter.Collector problemreporter$collector = new ProblemReporter.Collector();
        pAdvancement.validate(problemreporter$collector, this.registries);
        problemreporter$collector.getReport().ifPresent(p_341121_ -> LOGGER.warn("Found validation problems in advancement {}: \n{}", pLocation, p_341121_));
    }

    @Nullable
    public AdvancementHolder get(ResourceLocation pLocation) {
        return this.advancements.get(pLocation);
    }

    public AdvancementTree tree() {
        return this.tree;
    }

    public Collection<AdvancementHolder> getAllAdvancements() {
        return this.advancements.values();
    }
}

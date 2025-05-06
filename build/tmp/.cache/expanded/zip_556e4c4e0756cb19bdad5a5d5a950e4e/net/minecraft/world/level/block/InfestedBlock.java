package net.minecraft.world.level.block;

import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public class InfestedBlock extends Block {
    public static final MapCodec<InfestedBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_360435_ -> p_360435_.group(BuiltInRegistries.BLOCK.byNameCodec().fieldOf("host").forGetter(InfestedBlock::getHostBlock), propertiesCodec())
            .apply(p_360435_, InfestedBlock::new)
    );
    private final Block hostBlock;
    private static final Map<Block, Block> BLOCK_BY_HOST_BLOCK = Maps.newIdentityHashMap();
    private static final Map<BlockState, BlockState> HOST_TO_INFESTED_STATES = Maps.newIdentityHashMap();
    private static final Map<BlockState, BlockState> INFESTED_TO_HOST_STATES = Maps.newIdentityHashMap();

    @Override
    public MapCodec<? extends InfestedBlock> codec() {
        return CODEC;
    }

    public InfestedBlock(Block pHostBlock, BlockBehaviour.Properties pProperties) {
        super(pProperties.destroyTime(pHostBlock.defaultDestroyTime() / 2.0F).explosionResistance(0.75F));
        this.hostBlock = pHostBlock;
        BLOCK_BY_HOST_BLOCK.put(pHostBlock, this);
    }

    public Block getHostBlock() {
        return this.hostBlock;
    }

    public static boolean isCompatibleHostBlock(BlockState pState) {
        return BLOCK_BY_HOST_BLOCK.containsKey(pState.getBlock());
    }

    private void spawnInfestation(ServerLevel pLevel, BlockPos pPos) {
        Silverfish silverfish = EntityType.SILVERFISH.create(pLevel, EntitySpawnReason.TRIGGERED);
        if (silverfish != null) {
            silverfish.snapTo(pPos.getX() + 0.5, pPos.getY(), pPos.getZ() + 0.5, 0.0F, 0.0F);
            pLevel.addFreshEntity(silverfish);
            silverfish.spawnAnim();
        }
    }

    @Override
    protected void spawnAfterBreak(BlockState p_221360_, ServerLevel p_221361_, BlockPos p_221362_, ItemStack p_221363_, boolean p_221364_) {
        super.spawnAfterBreak(p_221360_, p_221361_, p_221362_, p_221363_, p_221364_);
        if (p_221361_.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS) && !EnchantmentHelper.hasTag(p_221363_, EnchantmentTags.PREVENTS_INFESTED_SPAWNS)) {
            this.spawnInfestation(p_221361_, p_221362_);
        }
    }

    public static BlockState infestedStateByHost(BlockState pHost) {
        return getNewStateWithProperties(HOST_TO_INFESTED_STATES, pHost, () -> BLOCK_BY_HOST_BLOCK.get(pHost.getBlock()).defaultBlockState());
    }

    public BlockState hostStateByInfested(BlockState pInfested) {
        return getNewStateWithProperties(INFESTED_TO_HOST_STATES, pInfested, () -> this.getHostBlock().defaultBlockState());
    }

    private static BlockState getNewStateWithProperties(Map<BlockState, BlockState> pStateMap, BlockState pState, Supplier<BlockState> pSupplier) {
        return pStateMap.computeIfAbsent(pState, p_153429_ -> {
            BlockState blockstate = pSupplier.get();

            for (Property property : p_153429_.getProperties()) {
                blockstate = blockstate.hasProperty(property) ? blockstate.setValue(property, p_153429_.getValue(property)) : blockstate;
            }

            return blockstate;
        });
    }
}
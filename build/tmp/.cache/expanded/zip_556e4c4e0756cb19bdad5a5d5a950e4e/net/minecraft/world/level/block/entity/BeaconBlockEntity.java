package net.minecraft.world.level.block.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ARGB;
import net.minecraft.world.LockCode;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeaconBeamBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

public class BeaconBlockEntity extends BlockEntity implements MenuProvider, Nameable, BeaconBeamOwner {
    private static final int MAX_LEVELS = 4;
    public static final List<List<Holder<MobEffect>>> BEACON_EFFECTS = List.of(
        List.of(MobEffects.SPEED, MobEffects.HASTE),
        List.of(MobEffects.RESISTANCE, MobEffects.JUMP_BOOST),
        List.of(MobEffects.STRENGTH),
        List.of(MobEffects.REGENERATION)
    );
    private static final Set<Holder<MobEffect>> VALID_EFFECTS = BEACON_EFFECTS.stream().flatMap(Collection::stream).collect(Collectors.toSet());
    public static final int DATA_LEVELS = 0;
    public static final int DATA_PRIMARY = 1;
    public static final int DATA_SECONDARY = 2;
    public static final int NUM_DATA_VALUES = 3;
    private static final int BLOCKS_CHECK_PER_TICK = 10;
    private static final Component DEFAULT_NAME = Component.translatable("container.beacon");
    private static final String TAG_PRIMARY = "primary_effect";
    private static final String TAG_SECONDARY = "secondary_effect";
    List<BeaconBeamOwner.Section> beamSections = new ArrayList<>();
    private List<BeaconBeamOwner.Section> checkingBeamSections = new ArrayList<>();
    int levels;
    private int lastCheckY;
    @Nullable
    Holder<MobEffect> primaryPower;
    @Nullable
    Holder<MobEffect> secondaryPower;
    @Nullable
    private Component name;
    private LockCode lockKey = LockCode.NO_LOCK;
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int p_58711_) {
            return switch (p_58711_) {
                case 0 -> BeaconBlockEntity.this.levels;
                case 1 -> BeaconMenu.encodeEffect(BeaconBlockEntity.this.primaryPower);
                case 2 -> BeaconMenu.encodeEffect(BeaconBlockEntity.this.secondaryPower);
                default -> 0;
            };
        }

        @Override
        public void set(int p_58713_, int p_58714_) {
            switch (p_58713_) {
                case 0:
                    BeaconBlockEntity.this.levels = p_58714_;
                    break;
                case 1:
                    if (!BeaconBlockEntity.this.level.isClientSide && !BeaconBlockEntity.this.beamSections.isEmpty()) {
                        BeaconBlockEntity.playSound(BeaconBlockEntity.this.level, BeaconBlockEntity.this.worldPosition, SoundEvents.BEACON_POWER_SELECT);
                    }

                    BeaconBlockEntity.this.primaryPower = BeaconBlockEntity.filterEffect(BeaconMenu.decodeEffect(p_58714_));
                    break;
                case 2:
                    BeaconBlockEntity.this.secondaryPower = BeaconBlockEntity.filterEffect(BeaconMenu.decodeEffect(p_58714_));
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    @Nullable
    static Holder<MobEffect> filterEffect(@Nullable Holder<MobEffect> pEffect) {
        return VALID_EFFECTS.contains(pEffect) ? pEffect : null;
    }

    public BeaconBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(BlockEntityType.BEACON, pPos, pBlockState);
    }

    public static void tick(Level pLevel, BlockPos pPos, BlockState pState, BeaconBlockEntity pBlockEntity) {
        int i = pPos.getX();
        int j = pPos.getY();
        int k = pPos.getZ();
        BlockPos blockpos;
        if (pBlockEntity.lastCheckY < j) {
            blockpos = pPos;
            pBlockEntity.checkingBeamSections = Lists.newArrayList();
            pBlockEntity.lastCheckY = pPos.getY() - 1;
        } else {
            blockpos = new BlockPos(i, pBlockEntity.lastCheckY + 1, k);
        }

        BeaconBeamOwner.Section beaconbeamowner$section = pBlockEntity.checkingBeamSections.isEmpty() ? null : pBlockEntity.checkingBeamSections.get(pBlockEntity.checkingBeamSections.size() - 1);
        int l = pLevel.getHeight(Heightmap.Types.WORLD_SURFACE, i, k);

        for (int i1 = 0; i1 < 10 && blockpos.getY() <= l; i1++) {
            BlockState blockstate = pLevel.getBlockState(blockpos);
            int j1 = blockstate.getBeaconColorMultiplier(pLevel, blockpos, pPos);
            if (j1 != -1) {
                if (pBlockEntity.checkingBeamSections.size() <= 1) {
                    beaconbeamowner$section = new BeaconBeamOwner.Section(j1);
                    pBlockEntity.checkingBeamSections.add(beaconbeamowner$section);
                } else if (beaconbeamowner$section != null) {
                    if (j1 == beaconbeamowner$section.getColor()) {
                        beaconbeamowner$section.increaseHeight();
                    } else {
                        beaconbeamowner$section = new BeaconBeamOwner.Section(ARGB.average(beaconbeamowner$section.getColor(), j1));
                        pBlockEntity.checkingBeamSections.add(beaconbeamowner$section);
                    }
                }
            } else {
                if (beaconbeamowner$section == null || blockstate.getLightBlock() >= 15 && !blockstate.is(Blocks.BEDROCK)) {
                    pBlockEntity.checkingBeamSections.clear();
                    pBlockEntity.lastCheckY = l;
                    break;
                }

                beaconbeamowner$section.increaseHeight();
            }

            blockpos = blockpos.above();
            pBlockEntity.lastCheckY++;
        }

        int k1 = pBlockEntity.levels;
        if (pLevel.getGameTime() % 80L == 0L) {
            if (!pBlockEntity.beamSections.isEmpty()) {
                pBlockEntity.levels = updateBase(pLevel, i, j, k);
            }

            if (pBlockEntity.levels > 0 && !pBlockEntity.beamSections.isEmpty()) {
                applyEffects(pLevel, pPos, pBlockEntity.levels, pBlockEntity.primaryPower, pBlockEntity.secondaryPower);
                playSound(pLevel, pPos, SoundEvents.BEACON_AMBIENT);
            }
        }

        if (pBlockEntity.lastCheckY >= l) {
            pBlockEntity.lastCheckY = pLevel.getMinY() - 1;
            boolean flag = k1 > 0;
            pBlockEntity.beamSections = pBlockEntity.checkingBeamSections;
            if (!pLevel.isClientSide) {
                boolean flag1 = pBlockEntity.levels > 0;
                if (!flag && flag1) {
                    playSound(pLevel, pPos, SoundEvents.BEACON_ACTIVATE);

                    for (ServerPlayer serverplayer : pLevel.getEntitiesOfClass(ServerPlayer.class, new AABB(i, j, k, i, j - 4, k).inflate(10.0, 5.0, 10.0))) {
                        CriteriaTriggers.CONSTRUCT_BEACON.trigger(serverplayer, pBlockEntity.levels);
                    }
                } else if (flag && !flag1) {
                    playSound(pLevel, pPos, SoundEvents.BEACON_DEACTIVATE);
                }
            }
        }
    }

    private static int updateBase(Level pLevel, int pX, int pY, int pZ) {
        int i = 0;

        for (int j = 1; j <= 4; i = j++) {
            int k = pY - j;
            if (k < pLevel.getMinY()) {
                break;
            }

            boolean flag = true;

            for (int l = pX - j; l <= pX + j && flag; l++) {
                for (int i1 = pZ - j; i1 <= pZ + j; i1++) {
                    if (!pLevel.getBlockState(new BlockPos(l, k, i1)).is(BlockTags.BEACON_BASE_BLOCKS)) {
                        flag = false;
                        break;
                    }
                }
            }

            if (!flag) {
                break;
            }
        }

        return i;
    }

    @Override
    public void setRemoved() {
        playSound(this.level, this.worldPosition, SoundEvents.BEACON_DEACTIVATE);
        super.setRemoved();
    }

    private static void applyEffects(
        Level pLevel, BlockPos pPos, int pBeaconLevel, @Nullable Holder<MobEffect> pPrimaryEffect, @Nullable Holder<MobEffect> pSecondaryEffect
    ) {
        if (!pLevel.isClientSide && pPrimaryEffect != null) {
            double d0 = pBeaconLevel * 10 + 10;
            int i = 0;
            if (pBeaconLevel >= 4 && Objects.equals(pPrimaryEffect, pSecondaryEffect)) {
                i = 1;
            }

            int j = (9 + pBeaconLevel * 2) * 20;
            AABB aabb = new AABB(pPos).inflate(d0).expandTowards(0.0, pLevel.getHeight(), 0.0);
            List<Player> list = pLevel.getEntitiesOfClass(Player.class, aabb);

            for (Player player : list) {
                player.addEffect(new MobEffectInstance(pPrimaryEffect, j, i, true, true));
            }

            if (pBeaconLevel >= 4 && !Objects.equals(pPrimaryEffect, pSecondaryEffect) && pSecondaryEffect != null) {
                for (Player player1 : list) {
                    player1.addEffect(new MobEffectInstance(pSecondaryEffect, j, 0, true, true));
                }
            }
        }
    }

    public static void playSound(Level pLevel, BlockPos pPos, SoundEvent pSound) {
        pLevel.playSound(null, pPos, pSound, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    @Override
    public List<BeaconBeamOwner.Section> getBeamSections() {
        return (List<BeaconBeamOwner.Section>)(this.levels == 0 ? ImmutableList.of() : this.beamSections);
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider p_333588_) {
        return this.saveCustomOnly(p_333588_);
    }

    private static void storeEffect(CompoundTag pTag, String pKey, @Nullable Holder<MobEffect> pEffect) {
        if (pEffect != null) {
            pEffect.unwrapKey().ifPresent(p_333727_ -> pTag.putString(pKey, p_333727_.location().toString()));
        }
    }

    @Nullable
    private static Holder<MobEffect> loadEffect(CompoundTag pTag, String pKey) {
        return pTag.read(pKey, BuiltInRegistries.MOB_EFFECT.holderByNameCodec()).filter(VALID_EFFECTS::contains).orElse(null);
    }

    @Override
    protected void loadAdditional(CompoundTag p_333194_, HolderLookup.Provider p_333691_) {
        super.loadAdditional(p_333194_, p_333691_);
        this.primaryPower = loadEffect(p_333194_, "primary_effect");
        this.secondaryPower = loadEffect(p_333194_, "secondary_effect");
        this.name = parseCustomNameSafe(p_333194_.get("CustomName"), p_333691_);
        this.lockKey = LockCode.fromTag(p_333194_, p_333691_);
    }

    @Override
    protected void saveAdditional(CompoundTag p_187463_, HolderLookup.Provider p_330516_) {
        super.saveAdditional(p_187463_, p_330516_);
        storeEffect(p_187463_, "primary_effect", this.primaryPower);
        storeEffect(p_187463_, "secondary_effect", this.secondaryPower);
        p_187463_.putInt("Levels", this.levels);
        p_187463_.storeNullable("CustomName", ComponentSerialization.CODEC, p_330516_.createSerializationContext(NbtOps.INSTANCE), this.name);
        this.lockKey.addToTag(p_187463_, p_330516_);
    }

    public void setCustomName(@Nullable Component pName) {
        this.name = pName;
    }

    @Nullable
    @Override
    public Component getCustomName() {
        return this.name;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int p_58696_, Inventory p_58697_, Player p_58698_) {
        return BaseContainerBlockEntity.canUnlock(p_58698_, this.lockKey, this.getDisplayName())
            ? new BeaconMenu(p_58696_, p_58697_, this.dataAccess, ContainerLevelAccess.create(this.level, this.getBlockPos()))
            : null;
    }

    @Override
    public Component getDisplayName() {
        return this.getName();
    }

    @Override
    public Component getName() {
        return this.name != null ? this.name : DEFAULT_NAME;
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter p_396481_) {
        super.applyImplicitComponents(p_396481_);
        this.name = p_396481_.get(DataComponents.CUSTOM_NAME);
        this.lockKey = p_396481_.getOrDefault(DataComponents.LOCK, LockCode.NO_LOCK);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder p_329382_) {
        super.collectImplicitComponents(p_329382_);
        p_329382_.set(DataComponents.CUSTOM_NAME, this.name);
        if (!this.lockKey.equals(LockCode.NO_LOCK)) {
            p_329382_.set(DataComponents.LOCK, this.lockKey);
        }
    }

    @Override
    public void removeComponentsFromTag(CompoundTag p_331794_) {
        p_331794_.remove("CustomName");
        p_331794_.remove("lock");
    }

    @Override
    public void setLevel(Level p_155091_) {
        super.setLevel(p_155091_);
        this.lastCheckY = p_155091_.getMinY() - 1;
    }
}

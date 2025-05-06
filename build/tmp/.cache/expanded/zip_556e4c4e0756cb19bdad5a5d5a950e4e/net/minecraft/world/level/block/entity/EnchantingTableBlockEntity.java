package net.minecraft.world.level.block.entity;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class EnchantingTableBlockEntity extends BlockEntity implements Nameable {
    public int time;
    public float flip;
    public float oFlip;
    public float flipT;
    public float flipA;
    public float open;
    public float oOpen;
    public float rot;
    public float oRot;
    public float tRot;
    private static final RandomSource RANDOM = RandomSource.create();
    @Nullable
    private Component name;

    public EnchantingTableBlockEntity(BlockPos pPos, BlockState pState) {
        super(BlockEntityType.ENCHANTING_TABLE, pPos, pState);
    }

    @Override
    protected void saveAdditional(CompoundTag p_329203_, HolderLookup.Provider p_335261_) {
        super.saveAdditional(p_329203_, p_335261_);
        if (this.hasCustomName()) {
            p_329203_.store("CustomName", ComponentSerialization.CODEC, p_335261_.createSerializationContext(NbtOps.INSTANCE), this.name);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag p_333729_, HolderLookup.Provider p_333480_) {
        super.loadAdditional(p_333729_, p_333480_);
        this.name = parseCustomNameSafe(p_333729_.get("CustomName"), p_333480_);
    }

    public static void bookAnimationTick(Level pLevel, BlockPos pPos, BlockState pState, EnchantingTableBlockEntity pEnchantingTable) {
        pEnchantingTable.oOpen = pEnchantingTable.open;
        pEnchantingTable.oRot = pEnchantingTable.rot;
        Player player = pLevel.getNearestPlayer(pPos.getX() + 0.5, pPos.getY() + 0.5, pPos.getZ() + 0.5, 3.0, false);
        if (player != null) {
            double d0 = player.getX() - (pPos.getX() + 0.5);
            double d1 = player.getZ() - (pPos.getZ() + 0.5);
            pEnchantingTable.tRot = (float)Mth.atan2(d1, d0);
            pEnchantingTable.open += 0.1F;
            if (pEnchantingTable.open < 0.5F || RANDOM.nextInt(40) == 0) {
                float f1 = pEnchantingTable.flipT;

                do {
                    pEnchantingTable.flipT = pEnchantingTable.flipT + (RANDOM.nextInt(4) - RANDOM.nextInt(4));
                } while (f1 == pEnchantingTable.flipT);
            }
        } else {
            pEnchantingTable.tRot += 0.02F;
            pEnchantingTable.open -= 0.1F;
        }

        while (pEnchantingTable.rot >= (float) Math.PI) {
            pEnchantingTable.rot -= (float) (Math.PI * 2);
        }

        while (pEnchantingTable.rot < (float) -Math.PI) {
            pEnchantingTable.rot += (float) (Math.PI * 2);
        }

        while (pEnchantingTable.tRot >= (float) Math.PI) {
            pEnchantingTable.tRot -= (float) (Math.PI * 2);
        }

        while (pEnchantingTable.tRot < (float) -Math.PI) {
            pEnchantingTable.tRot += (float) (Math.PI * 2);
        }

        float f2 = pEnchantingTable.tRot - pEnchantingTable.rot;

        while (f2 >= (float) Math.PI) {
            f2 -= (float) (Math.PI * 2);
        }

        while (f2 < (float) -Math.PI) {
            f2 += (float) (Math.PI * 2);
        }

        pEnchantingTable.rot += f2 * 0.4F;
        pEnchantingTable.open = Mth.clamp(pEnchantingTable.open, 0.0F, 1.0F);
        pEnchantingTable.time++;
        pEnchantingTable.oFlip = pEnchantingTable.flip;
        float f = (pEnchantingTable.flipT - pEnchantingTable.flip) * 0.4F;
        float f3 = 0.2F;
        f = Mth.clamp(f, -0.2F, 0.2F);
        pEnchantingTable.flipA = pEnchantingTable.flipA + (f - pEnchantingTable.flipA) * 0.9F;
        pEnchantingTable.flip = pEnchantingTable.flip + pEnchantingTable.flipA;
    }

    @Override
    public Component getName() {
        return (Component)(this.name != null ? this.name : Component.translatable("container.enchant"));
    }

    public void setCustomName(@Nullable Component pCustomName) {
        this.name = pCustomName;
    }

    @Nullable
    @Override
    public Component getCustomName() {
        return this.name;
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter p_397172_) {
        super.applyImplicitComponents(p_397172_);
        this.name = p_397172_.get(DataComponents.CUSTOM_NAME);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder p_334287_) {
        super.collectImplicitComponents(p_334287_);
        p_334287_.set(DataComponents.CUSTOM_NAME, this.name);
    }

    @Override
    public void removeComponentsFromTag(CompoundTag p_330630_) {
        p_330630_.remove("CustomName");
    }
}
package net.minecraft.core.dispenser;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.AABB;

public class EquipmentDispenseItemBehavior extends DefaultDispenseItemBehavior {
    public static final EquipmentDispenseItemBehavior INSTANCE = new EquipmentDispenseItemBehavior();

    @Override
    protected ItemStack execute(BlockSource p_361136_, ItemStack p_365597_) {
        return dispenseEquipment(p_361136_, p_365597_) ? p_365597_ : super.execute(p_361136_, p_365597_);
    }

    public static boolean dispenseEquipment(BlockSource pBlockSource, ItemStack pItem) {
        BlockPos blockpos = pBlockSource.pos().relative(pBlockSource.state().getValue(DispenserBlock.FACING));
        List<LivingEntity> list = pBlockSource.level().getEntitiesOfClass(LivingEntity.class, new AABB(blockpos), p_368089_ -> p_368089_.canEquipWithDispenser(pItem));
        if (list.isEmpty()) {
            return false;
        } else {
            LivingEntity livingentity = list.getFirst();
            EquipmentSlot equipmentslot = livingentity.getEquipmentSlotForItem(pItem);
            ItemStack itemstack = pItem.split(1);
            livingentity.setItemSlot(equipmentslot, itemstack);
            if (livingentity instanceof Mob mob) {
                mob.setGuaranteedDrop(equipmentslot);
                mob.setPersistenceRequired();
            }

            return true;
        }
    }
}
package net.minecraft.world.inventory;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeAccess;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipePropertySet;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class SmithingMenu extends ItemCombinerMenu {
    public static final int TEMPLATE_SLOT = 0;
    public static final int BASE_SLOT = 1;
    public static final int ADDITIONAL_SLOT = 2;
    public static final int RESULT_SLOT = 3;
    public static final int TEMPLATE_SLOT_X_PLACEMENT = 8;
    public static final int BASE_SLOT_X_PLACEMENT = 26;
    public static final int ADDITIONAL_SLOT_X_PLACEMENT = 44;
    private static final int RESULT_SLOT_X_PLACEMENT = 98;
    public static final int SLOT_Y_PLACEMENT = 48;
    private final Level level;
    private final RecipePropertySet baseItemTest;
    private final RecipePropertySet templateItemTest;
    private final RecipePropertySet additionItemTest;
    private final DataSlot hasRecipeError = DataSlot.standalone();

    public SmithingMenu(int pContainerId, Inventory pPlayerInventory) {
        this(pContainerId, pPlayerInventory, ContainerLevelAccess.NULL);
    }

    public SmithingMenu(int pContainerId, Inventory pPlayerInventory, ContainerLevelAccess pAccess) {
        this(pContainerId, pPlayerInventory, pAccess, pPlayerInventory.player.level());
    }

    private SmithingMenu(int pContainerId, Inventory pPlayerInventory, ContainerLevelAccess pAccess, Level pLevel) {
        super(MenuType.SMITHING, pContainerId, pPlayerInventory, pAccess, createInputSlotDefinitions(pLevel.recipeAccess()));
        this.level = pLevel;
        this.baseItemTest = pLevel.recipeAccess().propertySet(RecipePropertySet.SMITHING_BASE);
        this.templateItemTest = pLevel.recipeAccess().propertySet(RecipePropertySet.SMITHING_TEMPLATE);
        this.additionItemTest = pLevel.recipeAccess().propertySet(RecipePropertySet.SMITHING_ADDITION);
        this.addDataSlot(this.hasRecipeError).set(0);
    }

    private static ItemCombinerMenuSlotDefinition createInputSlotDefinitions(RecipeAccess pAccess) {
        RecipePropertySet recipepropertyset = pAccess.propertySet(RecipePropertySet.SMITHING_BASE);
        RecipePropertySet recipepropertyset1 = pAccess.propertySet(RecipePropertySet.SMITHING_TEMPLATE);
        RecipePropertySet recipepropertyset2 = pAccess.propertySet(RecipePropertySet.SMITHING_ADDITION);
        return ItemCombinerMenuSlotDefinition.create()
            .withSlot(0, 8, 48, recipepropertyset1::test)
            .withSlot(1, 26, 48, recipepropertyset::test)
            .withSlot(2, 44, 48, recipepropertyset2::test)
            .withResultSlot(3, 98, 48)
            .build();
    }

    @Override
    protected boolean isValidBlock(BlockState p_40266_) {
        return p_40266_.is(Blocks.SMITHING_TABLE);
    }

    @Override
    protected void onTake(Player p_150663_, ItemStack p_150664_) {
        p_150664_.onCraftedBy(p_150663_, p_150664_.getCount());
        this.resultSlots.awardUsedRecipes(p_150663_, this.getRelevantItems());
        this.shrinkStackInSlot(0);
        this.shrinkStackInSlot(1);
        this.shrinkStackInSlot(2);
        this.access.execute((p_40263_, p_40264_) -> p_40263_.levelEvent(1044, p_40264_, 0));
    }

    private List<ItemStack> getRelevantItems() {
        return List.of(this.inputSlots.getItem(0), this.inputSlots.getItem(1), this.inputSlots.getItem(2));
    }

    private SmithingRecipeInput createRecipeInput() {
        return new SmithingRecipeInput(this.inputSlots.getItem(0), this.inputSlots.getItem(1), this.inputSlots.getItem(2));
    }

    private void shrinkStackInSlot(int pIndex) {
        ItemStack itemstack = this.inputSlots.getItem(pIndex);
        if (!itemstack.isEmpty()) {
            itemstack.shrink(1);
            this.inputSlots.setItem(pIndex, itemstack);
        }
    }

    @Override
    public void slotsChanged(Container p_369195_) {
        super.slotsChanged(p_369195_);
        if (this.level instanceof ServerLevel) {
            boolean flag = this.getSlot(0).hasItem() && this.getSlot(1).hasItem() && this.getSlot(2).hasItem() && !this.getSlot(this.getResultSlot()).hasItem();
            this.hasRecipeError.set(flag ? 1 : 0);
        }
    }

    @Override
    public void createResult() {
        SmithingRecipeInput smithingrecipeinput = this.createRecipeInput();
        Optional<RecipeHolder<SmithingRecipe>> optional;
        if (this.level instanceof ServerLevel serverlevel) {
            optional = serverlevel.recipeAccess().getRecipeFor(RecipeType.SMITHING, smithingrecipeinput, serverlevel);
        } else {
            optional = Optional.empty();
        }

        optional.ifPresentOrElse(p_359375_ -> {
            ItemStack itemstack = p_359375_.value().assemble(smithingrecipeinput, this.level.registryAccess());
            this.resultSlots.setRecipeUsed((RecipeHolder<?>)p_359375_);
            this.resultSlots.setItem(0, itemstack);
        }, () -> {
            this.resultSlots.setRecipeUsed(null);
            this.resultSlots.setItem(0, ItemStack.EMPTY);
        });
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack pStack, Slot pSlot) {
        return pSlot.container != this.resultSlots && super.canTakeItemForPickAll(pStack, pSlot);
    }

    @Override
    public boolean canMoveIntoInputSlots(ItemStack p_266846_) {
        if (this.templateItemTest.test(p_266846_) && !this.getSlot(0).hasItem()) {
            return true;
        } else {
            return this.baseItemTest.test(p_266846_) && !this.getSlot(1).hasItem()
                ? true
                : this.additionItemTest.test(p_266846_) && !this.getSlot(2).hasItem();
        }
    }

    public boolean hasRecipeError() {
        return this.hasRecipeError.get() > 0;
    }
}
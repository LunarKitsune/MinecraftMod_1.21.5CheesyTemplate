package net.minecraft.world;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuConstructor;

public final class SimpleMenuProvider implements MenuProvider {
    private final Component title;
    private final MenuConstructor menuConstructor;

    public SimpleMenuProvider(MenuConstructor pMenuConstructor, Component pTitle) {
        this.menuConstructor = pMenuConstructor;
        this.title = pTitle;
    }

    @Override
    public Component getDisplayName() {
        return this.title;
    }

    @Override
    public AbstractContainerMenu createMenu(int p_19205_, Inventory p_19206_, Player p_19207_) {
        return this.menuConstructor.createMenu(p_19205_, p_19206_, p_19207_);
    }
}
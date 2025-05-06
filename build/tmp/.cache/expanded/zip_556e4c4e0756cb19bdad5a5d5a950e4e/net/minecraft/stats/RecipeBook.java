package net.minecraft.stats;

import net.minecraft.world.inventory.RecipeBookType;

public class RecipeBook {
    protected final RecipeBookSettings bookSettings = new RecipeBookSettings();

    public boolean isOpen(RecipeBookType pBookType) {
        return this.bookSettings.isOpen(pBookType);
    }

    public void setOpen(RecipeBookType pBookType, boolean pOpen) {
        this.bookSettings.setOpen(pBookType, pOpen);
    }

    public boolean isFiltering(RecipeBookType pBookType) {
        return this.bookSettings.isFiltering(pBookType);
    }

    public void setFiltering(RecipeBookType pBookType, boolean pFiltering) {
        this.bookSettings.setFiltering(pBookType, pFiltering);
    }

    public void setBookSettings(RecipeBookSettings pSettings) {
        this.bookSettings.replaceFrom(pSettings);
    }

    public RecipeBookSettings getBookSettings() {
        return this.bookSettings.copy();
    }

    public void setBookSetting(RecipeBookType pBookType, boolean pOpen, boolean pFiltering) {
        this.bookSettings.setOpen(pBookType, pOpen);
        this.bookSettings.setFiltering(pBookType, pFiltering);
    }
}
package net.minecraft.world.item.crafting.display;

import net.minecraft.core.Registry;

public class SlotDisplays {
    public static SlotDisplay.Type<?> bootstrap(Registry<SlotDisplay.Type<?>> pRegistry) {
        Registry.register(pRegistry, "empty", SlotDisplay.Empty.TYPE);
        Registry.register(pRegistry, "any_fuel", SlotDisplay.AnyFuel.TYPE);
        Registry.register(pRegistry, "item", SlotDisplay.ItemSlotDisplay.TYPE);
        Registry.register(pRegistry, "item_stack", SlotDisplay.ItemStackSlotDisplay.TYPE);
        Registry.register(pRegistry, "tag", SlotDisplay.TagSlotDisplay.TYPE);
        Registry.register(pRegistry, "smithing_trim", SlotDisplay.SmithingTrimDemoSlotDisplay.TYPE);
        Registry.register(pRegistry, "with_remainder", SlotDisplay.WithRemainder.TYPE);
        return Registry.register(pRegistry, "composite", SlotDisplay.Composite.TYPE);
    }
}
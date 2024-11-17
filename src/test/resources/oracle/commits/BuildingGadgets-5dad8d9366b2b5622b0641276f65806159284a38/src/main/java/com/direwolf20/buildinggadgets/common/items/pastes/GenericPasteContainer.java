package com.direwolf20.buildinggadgets.common.items.pastes;

import com.direwolf20.buildinggadgets.common.BuildingGadgets;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public abstract class GenericPasteContainer extends Item {

    public GenericPasteContainer(Properties builder) {
        super(builder.maxStackSize(1));
    }

    /**
     * Helper method. Delegates to {@link GenericPasteContainer#setPasteCount(ItemStack, int)}.
     */
    public static void setPasteAmount(ItemStack stack, int amount) {
        Item item = stack.getItem();
        if (item instanceof GenericPasteContainer) {
            ((GenericPasteContainer) item).setPasteCount(stack, amount);
        } else {
            BuildingGadgets.LOG.warn("Potential abuse of GenericPasteContainer#setPasteAmount(ItemStack, int) where the given ItemStack does not contain a GenericPasteContainer.");
        }
    }

    /**
     * Helper method. Delegates to {@link GenericPasteContainer#getPasteCount(ItemStack)}}.
     */
    public static int getPasteAmount(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof GenericPasteContainer) {
            return ((GenericPasteContainer) item).getPasteCount(stack);
        }
        BuildingGadgets.LOG.warn("Potential abuse of GenericPasteContainer#getPasteAmount(ItemStack) where the given ItemStack does not contain a GenericPasteContainer.");
        return 0;
    }

    /**
     * Set and store the amount of construction pastes in item nbt. Additionally it will clamp the parameter between {@link
     * #getMaxCapacity()} and 0 inclusively.
     */
    public abstract void setPasteCount(ItemStack stack, int amount);

    /**
     * Read and return the amount of construction pastes in item nbt. Always lower or equal to {@link #getMaxCapacity()}.
     */
    public abstract int getPasteCount(ItemStack stack);

    /**
     * @return maximum number of construction paste the container variant can hold.
     */
    public abstract int getMaxCapacity();

}

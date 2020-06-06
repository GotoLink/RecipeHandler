package assets.recipehandler;

import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;

import javax.annotation.Nullable;

/**
 * Represents a compatibility module that can be added to {@link CraftingHandler} for specialized behavior regarding a {@link Container} instance
 */
public interface ICompat {

    /**
     *
     * @return The full-path of the container class (extends {@link Container})
     */
    String getContainer();

    /**
     *
     * @param container The container instance, which class full-path is {@code #getContainer()}
     * @return The inventory crafting space, filled with the content for recipe matching and crafting, or null if crafting isn't possible for current container instance
     */
    @Nullable
    InventoryCrafting getCraftInv(Container container);

    /**
     * The slot where crafting result can be get by {@code Slot#getStack()} and set by {@code Slot#putStack(ItemStack)}
     * {@code Slot#xPos} and {@code Slot#yPos} are read as placement for the crafting switch button on screen
     * It is not necessary for this slot instance to be in {@code Container#inventorySlots}
     * @param container The container instance, which class full-path is {@code #getContainer()}
     * @return The slot that can be accessed for crafting output
     */
    Slot getResultSlot(Container container);
}

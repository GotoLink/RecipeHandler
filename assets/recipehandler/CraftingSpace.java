package assets.recipehandler;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraftforge.items.IItemHandler;

public interface CraftingSpace<E> {
    InventoryCrafting copy(E handler);

    CraftingSpace<IItemHandler> ITEM_HANDLER = new CraftingSpace<IItemHandler>(){

        @Override
        public InventoryCrafting copy(IItemHandler handler) {
            INPUT.clear();
            for(int index = 0; index < 9 && index < handler.getSlots(); index++){
                INPUT.setInventorySlotContents(index, handler.getStackInSlot(index));
            }
            return INPUT;
        }
    };
    CraftingSpace<IInventory> INVENTORY = new CraftingSpace<IInventory>(){

        @Override
        public InventoryCrafting copy(IInventory handler) {
            INPUT.clear();
            for(int index = 0; index < 9 && index < handler.getSizeInventory(); index++){
                INPUT.setInventorySlotContents(index, handler.getStackInSlot(index));
            }
            return INPUT;
        }
    };

    InventoryCrafting INPUT = new InventoryCrafting(new Container() {
            @Override
            public boolean canInteractWith(EntityPlayer playerIn) { return false; }
            @Override
            public void onCraftMatrixChanged(IInventory inventoryIn) { }
        }, 3, 3);
}

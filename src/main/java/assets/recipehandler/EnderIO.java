package assets.recipehandler;

import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;

import java.lang.reflect.Field;

public class EnderIO extends ModCompat {
    private Field grid;
    public EnderIO(String suffix){
        super(9, "crazypants.enderio.machines.machine.crafter.ContainerCrafter" + suffix);
    }

    @Override
    public InventoryCrafting getCraftInv(Container container){
        try {
            if(invGetter == null)
                invGetter = container.getClass().getMethod("getTileEntity");
            Object tile = invGetter.invoke(container);
            if(grid == null) {
                try {
                    grid = tile.getClass().getDeclaredField("craftingGrid");
                }catch (NoSuchFieldException nothing){
                    grid = tile.getClass().getSuperclass().getDeclaredField("craftingGrid");
                }
                grid.setAccessible(true);
            }
            return CraftingSpace.INVENTORY.copy((IInventory) grid.get(tile));
        }catch (Throwable t){
            t.printStackTrace();
        }
        return null;
    }

    @Override
    public Slot getResultSlot(Container container){
        Slot temp = super.getResultSlot(container);
        try {
            temp = new Slot((IInventory)grid.get(invGetter.invoke(container)), temp.slotNumber, temp.xPos, temp.yPos);
        }catch (Throwable t){
            t.printStackTrace();
        }
        return temp;
    }
}

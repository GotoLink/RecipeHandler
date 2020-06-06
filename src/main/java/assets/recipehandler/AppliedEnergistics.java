package assets.recipehandler;

import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraftforge.items.IItemHandler;

import java.lang.reflect.Field;

public class AppliedEnergistics extends ModCompat {
    private Field modeGetter;
    public AppliedEnergistics(String clName){
        super(14, "appeng.container.implementations." + clName);
    }

    @Override
    public InventoryCrafting getCraftInv(Container container){
        try {
            if(invGetter == null) {
                Class<?> containerClass = container.getClass();
                invGetter = containerClass.getMethod("getInventoryByName", String.class);
                try {
                    modeGetter = containerClass.getField("craftingMode");
                }catch (NoSuchFieldException nothing){
                }
            }
            if(modeGetter == null || (Boolean)modeGetter.get(container))
                return CraftingSpace.ITEM_HANDLER.copy((IItemHandler) invGetter.invoke(container, "crafting"));
        }catch (Throwable t){
            t.printStackTrace();
        }
        return null;
    }
}

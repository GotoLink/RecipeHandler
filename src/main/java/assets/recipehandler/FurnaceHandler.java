package assets.recipehandler;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;

import java.util.HashMap;
import java.util.Map;

public class FurnaceHandler {
    private final HashMap<ItemStack,ItemStack> FURNACE;

    public FurnaceHandler(boolean active) {
        if(active)
            FURNACE = new HashMap<ItemStack,ItemStack>(FurnaceRecipes.instance().getSmeltingList());
        else
            FURNACE = null;
    }

    public int compare(){
        if(FURNACE == null)
            return 0;
        int diff = 0;
        for(Map.Entry<ItemStack,ItemStack> entry : FURNACE.entrySet()){
            ItemStack currentOutput = FurnaceRecipes.instance().getSmeltingResult(entry.getKey());
            if(currentOutput != entry.getValue()){//The furnace output changed
                diff++;
            }
        }
        if(diff == 0)
            FURNACE.clear();
        return diff;
    }
}

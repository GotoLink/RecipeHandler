package assets.recipehandler;

import java.lang.reflect.Field;
import java.util.*;

import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;

public class CraftingHandler {
    private static HashMap<String, Field> knownCraftingContainer;
    private static HashSet<String> notCraftingContainer;

    public static void enableGuessing(){
        knownCraftingContainer = new HashMap<String, Field>();
        notCraftingContainer = new HashSet<String>();
    }

    public static InventoryCrafting getCraftingMatrix(Container container){
        if(container == null)
            return null;
        else if (container instanceof ContainerPlayer)
            return ((ContainerPlayer) container).craftMatrix;
        else if (container instanceof ContainerWorkbench)
            return ((ContainerWorkbench) container).craftMatrix;
        else if(notCraftingContainer!=null){
            String name = container.getClass().getName();
            if (!notCraftingContainer.contains(name)) {
                Field f = knownCraftingContainer.get(name);
                if (f == null) {
                    for (Field field : container.getClass().getDeclaredFields()) {
                        if (field!=null && InventoryCrafting.class.isAssignableFrom(field.getClass())) {
                            try {
                                InventoryCrafting craft = InventoryCrafting.class.cast(field.get(container));
                                if(craft!=null){
                                    knownCraftingContainer.put(name, field);
                                    return craft;
                                }
                            } catch (ReflectiveOperationException ref) {
                                continue;
                            }
                        }
                    }
                    notCraftingContainer.add(name);
                } else {
                    try {
                        return InventoryCrafting.class.cast(f.get(container));
                    } catch (ReflectiveOperationException ref) {
                        knownCraftingContainer.put(name, null);
                    }
                }
            }
        }
        return null;
    }

	public static ItemStack findMatchingRecipe(InventoryCrafting craft, World world, int i) {
		if (CraftingManager.getInstance().findMatchingRecipe(craft, world) != null) {
			List<ItemStack> result = getCraftResult(craft, world);
			if (result.size() == 0) {
				return null;
			}
			if (i < 0) {
				int j1 = -i;
				j1 %= result.size();
				j1 = result.size() - j1;
				if (j1 == result.size()) {
					j1 = 0;
				}
				return result.get(j1);
			} else {
				return result.get(i % result.size());
			}
		}
		return null;
	}

	public static List<ItemStack> getCraftResult(InventoryCrafting craft, World world) {
		Iterator<?> recipes = CraftingManager.getInstance().getRecipeList().iterator();
		ArrayList<ItemStack> arraylist = new ArrayList<ItemStack>();
		while (recipes.hasNext()) {
			IRecipe irecipe = (IRecipe) recipes.next();
			if (irecipe.matches(craft, world)) {
				arraylist.add(irecipe.getCraftingResult(craft));
			}
		}
		return arraylist;
	}

    public static IInventory getResultSlot(Container container, int size){
        if(container == null)
            return null;
        else if (container instanceof ContainerPlayer)
            return ((ContainerPlayer) container).craftResult;
        else if (container instanceof ContainerWorkbench)
            return ((ContainerWorkbench) container).craftResult;
        else if(notCraftingContainer!=null){
            for(Field field:container.getClass().getDeclaredFields()){
                if(field != null && IInventory.class.isAssignableFrom(field.getClass())){
                    try {
                        IInventory result = IInventory.class.cast(field.get(container));
                        if (result.getSizeInventory() == size) {
                            return result;
                        }
                    }catch (ReflectiveOperationException ref){}
                }
            }
        }
        return null;
    }

    public static int getNumberOfCraft(Container container, World world){
        InventoryCrafting craft = CraftingHandler.getCraftingMatrix(container);
        if (craft != null)
            return CraftingHandler.getCraftResult(craft, world).size();
        else
            return 0;
    }
}

package assets.recipehandler;

import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;

import java.lang.reflect.Field;
import java.util.*;

public final class CraftingHandler {
    private static HashMap<String, Field> knownCraftingContainer;
    private static HashSet<String> notCraftingContainer;
    private static int previousNumberOfCraft;
    private static int delayTimer = 10;
    private static int recipeIndex;

    public static void enableGuessing(){
        knownCraftingContainer = new HashMap<String, Field>();
        notCraftingContainer = new HashSet<String>();
    }

    public static int getRecipeIndex(){
        return recipeIndex;
    }

    public static void setRecipeIndex(int id){
        if(id>=0){
            recipeIndex = id;
        }
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
                        if (field!=null) {
                            try {
                                field.setAccessible(true);
                                Object craft = field.get(container);
                                if(craft instanceof InventoryCrafting){
                                    knownCraftingContainer.put(name, field);
                                    return (InventoryCrafting) craft;
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    }
                    notCraftingContainer.add(name);
                } else {
                    try {
                        return (InventoryCrafting) f.get(container);
                    } catch (Exception ref) {
                        knownCraftingContainer.put(name, null);
                    }
                }
            }
        }
        return null;
    }

    public static ItemStack findNextMatchingRecipe(InventoryCrafting craft, World world) {
        if (recipeIndex == Integer.MAX_VALUE) {
            recipeIndex = 0;
        } else {
            recipeIndex++;
        }
        return findMatchingRecipe(craft, world);
    }

	public static ItemStack findMatchingRecipe(InventoryCrafting craft, World world) {
		if (CraftingManager.getInstance().findMatchingRecipe(craft, world) != null) {
			List<ItemStack> result = getCraftResult(craft, world);
			if (result.size() == 0) {
				return null;
			}
			if (recipeIndex < 0) {
				int j1 = -recipeIndex;
				j1 %= result.size();
				j1 = result.size() - j1;
				if (j1 == result.size()) {
					j1 = 0;
				}
				return result.get(j1);
			} else {
				return result.get(recipeIndex % result.size());
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
                if(field != null){
                    try {
                        field.setAccessible(true);
                        Object result = field.get(container);
                        if (result instanceof IInventory && ((IInventory) result).getSizeInventory() == size) {
                            return (IInventory) result;
                        }
                    }catch (Exception ignored){}
                }
            }
        }
        return null;
    }

    public static int getNumberOfCraft(Container container, World world){
        if(delayTimer>20) {
            delayTimer = 0;
            InventoryCrafting craft = getCraftingMatrix(container);
            if (craft != null)
                previousNumberOfCraft = getCraftResult(craft, world).size();
            else
                previousNumberOfCraft = 0;
        }else
            delayTimer++;
        return previousNumberOfCraft;
    }
}

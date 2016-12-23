package assets.recipehandler;

import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.*;

public final class CraftingHandler {
    private static HashMap<String, Field> knownCraftingContainer;
    private static HashSet<String> notCraftingContainer;
    private static Field slotCraftInv;
    private static int previousNumberOfCraft;
    private static int delayTimer = 10;
    private static int recipeIndex;

    public static void enableGuessing(){
        knownCraftingContainer = new HashMap<String, Field>();
        notCraftingContainer = new HashSet<String>();
        slotCraftInv = ReflectionHelper.findField(SlotCrafting.class, "field_75239_a", "craftMatrix");
    }

    public static int getRecipeIndex(){
        return recipeIndex;
    }

    public static void setRecipeIndex(int id){
        if(id>=0){
            recipeIndex = id;
        }
    }

    @Nullable
    public static InventoryCrafting getCraftingMatrix(Container container){
        if(container == null)
            return null;
        else if (container instanceof ContainerPlayer)
            return ((ContainerPlayer) container).craftMatrix;
        else if (container instanceof ContainerWorkbench)
            return ((ContainerWorkbench) container).craftMatrix;
        else if(notCraftingContainer!=null){
            for (Slot slot : container.inventorySlots) {
                if (slot!=null && slot.inventory instanceof InventoryCrafting){
                    return (InventoryCrafting) slot.inventory;
                }
            }
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

    @Nonnull
    public static ItemStack findNextMatchingRecipe(InventoryCrafting craft, World world) {
        if (recipeIndex == Integer.MAX_VALUE) {
            recipeIndex = 0;
        } else {
            recipeIndex++;
        }
        return findMatchingRecipe(craft, world);
    }

    @Nonnull
	public static ItemStack findMatchingRecipe(InventoryCrafting craft, World world) {
		if (!CraftingManager.getInstance().findMatchingRecipe(craft, world).isEmpty()) {
			List<ItemStack> result = getCraftResult(craft, world);
			if (result.size() == 0) {
				return ItemStack.EMPTY;
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
		return ItemStack.EMPTY;
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

    @Nullable
    public static Slot getResultSlot(Container container, InventoryCrafting inventory, int index){
        if(container == null)
            return null;
        else if(index < container.inventorySlots.size()){
            Slot slot = container.getSlot(index);
            if(slot instanceof SlotCrafting)
                return slot;
        }
        if(slotCraftInv != null){
            try {
                for (Slot slot : container.inventorySlots) {
                    if (slot instanceof SlotCrafting) {
                        if (inventory == slotCraftInv.get(slot))
                            return slot;
                    }
                }
            }catch (Exception ignored){}
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

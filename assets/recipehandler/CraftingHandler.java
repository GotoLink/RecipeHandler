package assets.recipehandler;

import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.*;

public final class CraftingHandler {
    private static HashMap<String, Field> knownCraftingContainer;
    private static HashSet<String> notCraftingContainer;
    private static Field slotCraftInv;
    private static int previousNumberOfCraft;
    private static long delayTimer = 0;
    private static int recipeIndex;

    /**
     * Enable guessing work over the crafting inventory space
     */
    public static void enableGuessing(){
        knownCraftingContainer = new HashMap<String, Field>();
        notCraftingContainer = new HashSet<String>();
        slotCraftInv = ReflectionHelper.findField(SlotCrafting.class, "field_75239_a", "craftMatrix");
    }

    /**
     * The state of this helper
     * @return The craft index
     */
    public static int getRecipeIndex(){
        return recipeIndex;
    }

    /**
     * Apply new state to this helper
     * @param id The craft index
     */
    public static void setRecipeIndex(int id){
        if(id>=0){
            recipeIndex = id;
        }
    }

    /**
     * Get the crafting space within the given container
     * May be guessing if enabled
     * @param container The searched container
     * @return The crafting space or null if none could be found
     */
    @Nullable
    public static InventoryCrafting getCraftingMatrix(Container container){
        if (container instanceof ContainerPlayer)
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

    /**
     * Get the next craft result
     * @param craft Crafting space
     * @param world Where the craft happens
     * @return The next result of the craft
     */
    public static ItemStack findNextMatchingRecipe(InventoryCrafting craft, World world) {
        if (recipeIndex == Integer.MAX_VALUE) {
            recipeIndex = 0;
        } else {
            recipeIndex++;
        }
        return findMatchingRecipe(craft, world);
    }

    /**
     * The current craft result
     * Based on all the possible results, picked with the current state of this helper
     * @param craft Crafting space
     * @param world Where the craft happens
     * @return The current result of the craft, EMPTY is none could be found
     */
	public static ItemStack findMatchingRecipe(InventoryCrafting craft, World world) {
		if (!CraftingManager.getInstance().findMatchingRecipe(craft, world).isEmpty()) {
			List<ItemStack> result = getCraftResult(craft, world);
			if(previousNumberOfCraft!=result.size()) {
                previousNumberOfCraft = result.size();
                recipeIndex = 0;
            }
            delayTimer = world.getTotalWorldTime();
			if (previousNumberOfCraft == 0) {
				return ItemStack.EMPTY;
			}
			if (recipeIndex < 0) {
				int j1 = -recipeIndex;
				j1 %= previousNumberOfCraft;
				j1 = previousNumberOfCraft - j1;
				if (j1 == previousNumberOfCraft) {
					j1 = 0;
				}
				return result.get(j1);
			} else {
				return result.get(recipeIndex % previousNumberOfCraft);
			}
		}
		return ItemStack.EMPTY;
	}

    /**
     * Get all the results possible from the craft
     * @param craft Crafting space
     * @param world Where the craft happens
     * @return List of all the craft results, empty is none could be found
     */
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

    /**
     * Get the crafting slot where the result should be displayed
     * Can be guessed if the slot number isn't correct
     * @param container Contains the craft space
     * @param inventory Crafting space
     * @param index Possible value for the slot number
     * @return The crafting result slot, or null if none could be found
     */
    @Nullable
    public static Slot getResultSlot(Container container, InventoryCrafting inventory, int index){
        if(index < container.inventorySlots.size()){
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

    /**
     * How many recipes apply in the current container
     * Result may be a cached value
     * @param container To craft into
     * @param world Where the player crafts
     * @return The number of recipes that can be crafted
     */
    public static int getNumberOfCraft(Container container, World world){
        if(world.getTotalWorldTime() - delayTimer > 10) {
            delayTimer = world.getTotalWorldTime();
            InventoryCrafting craft = getCraftingMatrix(container);
            if (craft != null)
                previousNumberOfCraft = getCraftResult(craft, world).size();
            else
                previousNumberOfCraft = 0;
        }
        return previousNumberOfCraft;
    }
}

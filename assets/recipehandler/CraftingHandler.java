package assets.recipehandler;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import net.minecraftforge.common.crafting.IRecipeContainer;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

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
    public static void enableGuessing(List<String> blackList){
        knownCraftingContainer = new HashMap<String, Field>();
        notCraftingContainer = new HashSet<String>(blackList);
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
    public static InventoryCrafting getCraftingMatrix(@Nullable Container container){
        if(container == null)
            return null;
        else if (container instanceof ContainerPlayer)
            return ((ContainerPlayer) container).craftMatrix;
        else if (container instanceof ContainerWorkbench)
            return ((ContainerWorkbench) container).craftMatrix;
        else if (container instanceof IRecipeContainer)
            return ((IRecipeContainer) container).getCraftMatrix();
        else if(notCraftingContainer!=null){
            for (Slot slot : container.inventorySlots) {
                if (slot != null && slot.inventory instanceof InventoryCrafting){
                    return (InventoryCrafting) slot.inventory;
                }
            }
            String name = container.getClass().getName();
            if (!notCraftingContainer.contains(name)) {
                Field f = knownCraftingContainer.get(name);
                if (f == null) {
                    for (Field field : container.getClass().getDeclaredFields()) {
                        if (field != null) {
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
    public static ItemStack findNextMatchingRecipe(InventoryCrafting craft, @Nullable World world) {
        if (recipeIndex == Integer.MAX_VALUE) {
            recipeIndex = 0;
        } else {
            recipeIndex++;
        }
        return findCraftResult(craft, world);
    }

    /**
     * The current craft result
     * Based on all the possible crafts, picked with the current state of this helper
     * @param craft Crafting space
     * @param world Where the craft happens
     * @return The current result of the craft, EMPTY if none could be found
     */
	public static ItemStack findCraftResult(InventoryCrafting craft, @Nullable World world) {
	    IRecipe recipe = findMatchingRecipe(craft, world);
        return recipe != null ? recipe.getCraftingResult(craft) : ItemStack.EMPTY;
	}

    /**
     * The current craft recipe
     * Based on all the possible crafts, picked with the current state of this helper
     * @param craft Crafting space
     * @param world Where the craft happens
     * @return The current craft, null if none could be found
     */
	@Nullable
    public static IRecipe findMatchingRecipe(InventoryCrafting craft, @Nullable World world) {
        if(world == null)
            return null;
        List<IRecipe> result = getCrafts(craft, world);
        if(previousNumberOfCraft!=result.size()) {
            previousNumberOfCraft = result.size();
            recipeIndex = 0;
        }
        delayTimer = world.getTotalWorldTime();
        if (previousNumberOfCraft == 0) {
            return null;
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

    /**
     * Get all the crafts possible
     * @param craft Crafting space
     * @param world Where the craft happens
     * @return List of all the crafts, empty if none could be found
     */
	public static List<IRecipe> getCrafts(InventoryCrafting craft, World world) {
		ArrayList<IRecipe> arraylist = new ArrayList<IRecipe>();
		for (IRecipe irecipe : CraftingManager.REGISTRY) {
			if (irecipe.matches(craft, world)) {
				arraylist.add(irecipe);
			}
		}
		return arraylist;
	}

    /**
     * Set the recipe currently used, for recipe unlock support
     * @param player the crafter
     * @param recipe the recipe to set
     * @return whether the recipe has actually been set
     */
    public static boolean setCraftUsed(EntityPlayerMP player, IRecipe recipe){
        if(recipe.isDynamic() || !player.getEntityWorld().getGameRules().getBoolean("doLimitedCrafting") || player.getRecipeBook().isUnlocked(recipe)){
            InventoryCraftResult resultInv = getResultInv(player.openContainer);
            if(resultInv != null)
                resultInv.setRecipeUsed(recipe);
            return true;
        }
        return false;
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
        InventoryCraftResult resultInv = getResultInv(container);
        if(resultInv != null){
            for (Slot slot : container.inventorySlots) {
                if(slot != null && slot.isHere(resultInv, slot.getSlotIndex()))
                    return slot;
            }
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

    @Nullable
    public static InventoryCraftResult getResultInv(Container container){
        if (container instanceof ContainerPlayer)
            return ((ContainerPlayer) container).craftResult;
        else if (container instanceof ContainerWorkbench)
            return ((ContainerWorkbench) container).craftResult;
        else if (container instanceof IRecipeContainer)
            return ((IRecipeContainer) container).getCraftResult();
        for (Slot slot : container.inventorySlots) {
            if(slot != null && slot.inventory instanceof InventoryCraftResult) {
                return (InventoryCraftResult) slot.inventory;
            }
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
    public static int getNumberOfCraft(@Nullable Container container, @Nullable World world){
        if(world == null)
            return 0;
        if(world.getTotalWorldTime() - delayTimer > 10) {
            delayTimer = world.getTotalWorldTime();
            InventoryCrafting craft = getCraftingMatrix(container);
            if (craft != null)
                previousNumberOfCraft = getCrafts(craft, world).size();
            else
                previousNumberOfCraft = 0;
        }
        return previousNumberOfCraft;
    }
}

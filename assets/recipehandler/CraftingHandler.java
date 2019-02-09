package assets.recipehandler;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.crafting.IRecipeContainer;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.*;

public final class CraftingHandler {
    private HashMap<String, Field> knownCraftingContainer;
    private HashSet<String> notCraftingContainer;
    private HashSet<ICompat> compatibilities = new HashSet<>(4);
    private Field slotCraftInv;
    private ArrayList<IRecipe> crafts = new ArrayList<>(2);
    private int previousNumberOfCraft = 0;
    private long delayTimer = 0;
    private int recipeIndex = 0;

    /**
     * Enable guessing work over the crafting inventory space
     */
    public void enableGuessing(List<String> blackList){
        knownCraftingContainer = new HashMap<String, Field>(10);
        notCraftingContainer = new HashSet<String>(blackList);
        slotCraftInv = ReflectionHelper.findField(SlotCrafting.class, "field_75239_a", "craftMatrix");
    }

    /**
     * Add a compatiblity module instance
     * @param module The module to be added
     * @return true if the module has been added, false otherwise
     */
    public boolean addCompatibility(ICompat module){
        return compatibilities.add(module);
    }

    /**
     * The state of this helper
     * @return The craft index
     */
    public int getRecipeIndex(){
        return recipeIndex;
    }

    /**
     * Apply new state to this helper
     * @param id The craft index
     */
    public void setRecipeIndex(int id){
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
    public InventoryCrafting getCraftingMatrix(@Nullable Container container){
        if(container == null)
            return null;
        else if (container instanceof ContainerPlayer)
            return ((ContainerPlayer) container).craftMatrix;
        else if (container instanceof ContainerWorkbench)
            return ((ContainerWorkbench) container).craftMatrix;
        else if (container instanceof IRecipeContainer)
            return ((IRecipeContainer) container).getCraftMatrix();
        else if(notCraftingContainer != null){
            String name = container.getClass().getName();
            if (!notCraftingContainer.contains(name)) {
                for (Slot slot : container.inventorySlots) {
                    if (slot != null && slot.inventory instanceof InventoryCrafting){
                        return (InventoryCrafting) slot.inventory;
                    }
                }
                for(ICompat mod : compatibilities){
                    if(mod.getContainer().equals(name))
                        return mod.getCraftInv(container);
                }
                Field f = knownCraftingContainer.get(name);
                if (f == null) {
                    for (Field field : container.getClass().getDeclaredFields()) {
                        if (field != null) {
                            try {
                                field.setAccessible(true);
                                InventoryCrafting craft = convert(field.get(container));
                                if(craft != null){
                                    knownCraftingContainer.put(name, field);
                                    return craft;
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    }
                    notCraftingContainer.add(name);
                } else {
                    try {
                        return convert(f.get(container));
                    } catch (Exception ref) {
                        knownCraftingContainer.put(name, null);
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    private InventoryCrafting convert(Object craft){
        if(craft instanceof InventoryCrafting){
            return (InventoryCrafting) craft;
        }else if(craft instanceof IItemHandler && ((IItemHandler) craft).getSlots() == 9){
            return CraftingSpace.ITEM_HANDLER.copy((IItemHandler) craft);
        }else if(craft instanceof IInventory && ((IInventory) craft).getSizeInventory() == 9){
            return CraftingSpace.INVENTORY.copy((IInventory) craft);
        }else if(craft instanceof ICapabilityProvider){
            IItemHandler handler = ((ICapabilityProvider) craft).getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
            if(handler != null && handler.getSlots() == 9)
                return CraftingSpace.ITEM_HANDLER.copy(handler);
        }
        return null;
    }

    /**
     * Get the next craft result
     * @param craft Crafting space
     * @param world Where the craft happens
     * @return The next result of the craft
     */
    public ItemStack findNextMatchingRecipe(InventoryCrafting craft, @Nullable World world) {
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
	public ItemStack findCraftResult(InventoryCrafting craft, @Nullable World world) {
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
    public IRecipe findMatchingRecipe(InventoryCrafting craft, @Nullable World world) {
        if(world == null)
            return null;
        getCrafts(craft, world);
        delayTimer = world.getTotalWorldTime();
        if (previousNumberOfCraft <= 0) {
            recipeIndex = 0;
            return null;
        }
        return crafts.get(recipeIndex % previousNumberOfCraft);
    }

    /**
     * Get all the crafts possible
     * @param craft Crafting space
     * @param world Where the craft happens
     * @return List of all the crafts, empty if none could be found
     */
	public void getCrafts(InventoryCrafting craft, World world) {
	    if(crafts.isEmpty() || !crafts.get(previousNumberOfCraft <= 0 ? 0:recipeIndex % previousNumberOfCraft).matches(craft, world)) {
	        crafts.clear();
            for (IRecipe irecipe : ForgeRegistries.RECIPES) {
                if (irecipe.matches(craft, world)) {
                    crafts.add(irecipe);
                }
            }
        }
        previousNumberOfCraft = crafts.size();
	}

    /**
     * Set the recipe currently used, for recipe unlock support
     * @param player the crafter
     * @param recipe the recipe to set
     * @return whether the recipe has actually been set
     */
    public boolean setCraftUsed(EntityPlayerMP player, IRecipe recipe){
        if(recipe.isDynamic() || !player.getEntityWorld().getGameRules().getBoolean("doLimitedCrafting") || player.getRecipeBook().isUnlocked(recipe)){
            InventoryCraftResult resultInv = getResultInv(player.openContainer);
            if(resultInv != null)
                resultInv.setRecipeUsed(recipe);
            else{
                try{
                    for(Field recF : player.openContainer.getClass().getDeclaredFields()) {
                        if (recF.getType().equals(IRecipe.class)) {
                            recF.setAccessible(true);
                            recF.set(player.openContainer, recipe);
                        }
                    }
                }catch (Throwable ignored){
                }
            }
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
    public Slot getResultSlot(Container container, InventoryCrafting inventory, int index){
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
        }else{
            String type = container.getClass().getName();
            for(ICompat mod : compatibilities){
                if(mod.getContainer().equals(type)){
                    return mod.getResultSlot(container);
                }
            }
        }
        if(slotCraftInv != null){
            try {
                for (Slot slot : container.inventorySlots) {
                    if (slot instanceof SlotCrafting && inventory == slotCraftInv.get(slot))
                        return slot;
                }
            }catch (Exception ignored){}
        }
        return null;
    }

    /**
     * Get the craft result inventory within the given container
     * @param container Contains the craft space
     * @return The crafting result inventory, or null if none could be found
     */
    @Nullable
    public InventoryCraftResult getResultInv(Container container){
        if (container instanceof ContainerPlayer)
            return ((ContainerPlayer) container).craftResult;
        else if (container instanceof ContainerWorkbench)
            return ((ContainerWorkbench) container).craftResult;
        else if (container instanceof IRecipeContainer)
            return ((IRecipeContainer) container).getCraftResult();
        for (Slot slot : container.inventorySlots) {
            if(slot != null && slot.inventory instanceof InventoryCraftResult){
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
    public int getNumberOfCraft(@Nullable Container container, @Nullable World world){
        if(world == null)
            return -1;
        if(world.getTotalWorldTime() - delayTimer > 10) {
            delayTimer = world.getTotalWorldTime();
            InventoryCrafting craft = getCraftingMatrix(container);
            if(craft == null) {
                previousNumberOfCraft = -1;
                return -1;
            }
            if (!craft.isEmpty()) {
                InventoryCraftResult result = getResultInv(container);
                if(result != null && result.isEmpty())
                    reset();
                else
                    getCrafts(craft, world);
            }else {
                reset();
            }
        }
        return previousNumberOfCraft;
    }

    /**
     * Reset the current state of the handler
     */
    private void reset(){
        if(previousNumberOfCraft != 0) {
            previousNumberOfCraft = 0;
            recipeIndex = 0;
        }
    }

    /**
     * Get current list of container names
     * @param isCraft if true return the known crafting containers, if false, return the left-overs
     * @return current list of container names, or null if guessing is disabled
     */
    @Nullable
    public Set<String> getContainers(boolean isCraft){
        return isCraft ? knownCraftingContainer != null ? knownCraftingContainer.keySet() : null : notCraftingContainer;
    }
}

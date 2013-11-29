package assets.recipehandler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;

public class CraftingHandler {
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
				return (ItemStack) result.get(j1);
			} else {
				return (ItemStack) result.get(i % result.size());
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
}

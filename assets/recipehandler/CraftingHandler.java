package assets.recipehandler;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;

public class CraftingHandler {
	public static ItemStack findMatchingRecipe(InventoryCrafting craft, World world, int i) {
		if (CraftingManager.getInstance().findMatchingRecipe(craft, world) != null) {
			List recipes = CraftingManager.getInstance().getRecipeList();
			ArrayList arraylist = new ArrayList();
			for (int i1 = 0; i1 < recipes.size(); i1++) {
				IRecipe irecipe = (IRecipe) recipes.get(i1);
				if (irecipe.matches(craft, world)) {
					arraylist.add(irecipe.getCraftingResult(craft));
				}
			}
			if (arraylist.size() == 0) {
				return null;
			}
			if (i < 0) {
				int j1 = -i;
				j1 %= arraylist.size();
				j1 = arraylist.size() - j1;
				if (j1 == arraylist.size()) {
					j1 = 0;
				}
				return (ItemStack) arraylist.get(j1);
			} else {
				return (ItemStack) arraylist.get(i % arraylist.size());
			}
		} else
			return null;
	}
}

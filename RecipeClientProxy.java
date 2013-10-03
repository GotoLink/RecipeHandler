package assets.recipehandler;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.registry.KeyBindingRegistry;

public class RecipeClientProxy extends RecipeProxy{
	@Override
	public void register()
	{
		KeyBindingRegistry.registerKeyBinding(new RecipeKeyHandler(Keyboard.KEY_ADD));
	}
}

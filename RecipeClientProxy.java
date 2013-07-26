package assets.recipehandler;

import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.packet.Packet250CustomPayload;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.registry.KeyBindingRegistry;

public class RecipeClientProxy extends RecipeProxy{
	@Override
	public void register()
	{
		KeyBindingRegistry.registerKeyBinding(new RecipeKeyHandler(Keyboard.KEY_ADD));
	}
}

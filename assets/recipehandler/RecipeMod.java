package assets.recipehandler;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.registry.KeyBindingRegistry;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@Mod(modid = "recipemod", name = "NoMoreRecipeConflict", version = "0.1")
@NetworkMod(clientSideRequired = true, channels = { PacketHandler.CHANNEL }, packetHandler = PacketHandler.class)
public class RecipeMod {
	private static final boolean debug = false;

	@EventHandler
	public void loading(FMLPreInitializationEvent event) {
		if (debug) {
			GameRegistry.addShapelessRecipe(new ItemStack(Item.appleGold), Block.planks, Item.stick);
			GameRegistry.addShapelessRecipe(new ItemStack(Item.appleRed), Block.planks, Item.stick);
		}
		if (event.getSide().isClient()) {
			registerKey();
		}
	}

	@SideOnly(Side.CLIENT)
	private void registerKey() {
		KeyBindingRegistry.registerKeyBinding(new RecipeKeyHandler(Keyboard.KEY_ADD));
		MinecraftForge.EVENT_BUS.register(new RenderHandler());
	}
}

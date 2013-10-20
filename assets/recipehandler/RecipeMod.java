package assets.recipehandler;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.registry.GameRegistry;

@Mod(modid = Constants.MODID, name = Constants.MOD_NAME, version = Constants.VERSION)
@NetworkMod(clientSideRequired = true, serverSideRequired = false, channels = { Constants.CHANNEL }, packetHandler = PacketHandler.class)
public class RecipeMod {
	@SidedProxy(clientSide = Constants.CLIENT_PROXY, serverSide = Constants.COMMON_PROXY)
	public static RecipeProxy proxy;
	public static final boolean debug = false;

	@EventHandler
	public void loading(FMLPreInitializationEvent event) {
		proxy.register();
		if (debug) {
			GameRegistry.addShapelessRecipe(new ItemStack(Item.appleGold), new Object[] { Block.planks, Item.stick });
			GameRegistry.addShapelessRecipe(new ItemStack(Item.appleRed), new Object[] { Block.planks, Item.stick });
		}
	}
}

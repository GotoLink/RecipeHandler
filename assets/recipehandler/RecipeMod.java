package assets.recipehandler;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@Mod(modid = "recipemod", name = "NoMoreRecipeConflict", version = "0.2")
public class RecipeMod {
	private static final boolean debug = true;
    public static SimpleNetworkWrapper networkWrapper;

	@EventHandler
	public void loading(FMLPreInitializationEvent event) {
		if (debug) {
			GameRegistry.addShapelessRecipe(new ItemStack(Items.golden_apple), Blocks.planks, Items.stick);
			GameRegistry.addShapelessRecipe(new ItemStack(Items.apple), Blocks.planks, Items.stick);
		}
		if (event.getSide().isClient()) {
			registerKey();
		}
        networkWrapper = NetworkRegistry.INSTANCE.newSimpleChannel("recipemod:key");
        networkWrapper.registerMessage(PacketHandler.class, ChangePacket.class, 0, Side.SERVER);
        networkWrapper.registerMessage(PacketHandler.class, ChangePacket.class, 1, Side.CLIENT);
	}

	@SideOnly(Side.CLIENT)
	private void registerKey() {
		new ClientEventHandler().register();
	}
}

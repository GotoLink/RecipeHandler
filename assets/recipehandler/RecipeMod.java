package assets.recipehandler;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.network.FMLEventChannel;
import cpw.mods.fml.common.network.NetworkRegistry;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@Mod(modid = "recipemod", name = "NoMoreRecipeConflict", version = "0.1")
public class RecipeMod {
	private static final boolean debug = false;
    public static FMLEventChannel networkWrapper;

	@EventHandler
	public void loading(FMLPreInitializationEvent event) {
		if (debug) {
			GameRegistry.addShapelessRecipe(new ItemStack(Items.golden_apple), Blocks.planks, Items.stick);
			GameRegistry.addShapelessRecipe(new ItemStack(Items.apple), Blocks.planks, Items.stick);
		}
		if (event.getSide().isClient()) {
			registerKey();
            if(event.getSourceFile().getName().endsWith(".jar")){
                try {
                    Class.forName("mods.mud.ModUpdateDetector").getDeclaredMethod("registerMod", ModContainer.class, String.class, String.class).invoke(null,
                            FMLCommonHandler.instance().findContainerFor(this),
                            "https://raw.github.com/GotoLink/RecipeHandler/master/update.xml",
                            "https://raw.github.com/GotoLink/RecipeHandler/master/changelog.md"
                    );
                } catch (Throwable e) {
                }
            }
		}
        networkWrapper = NetworkRegistry.INSTANCE.newEventDrivenChannel(ChangePacket.CHANNEL);
        networkWrapper.register(new PacketHandler());
	}

	@SideOnly(Side.CLIENT)
	private void registerKey() {
		new ClientEventHandler().register();
	}
}

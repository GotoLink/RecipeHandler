package assets.recipehandler;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.network.FMLEventChannel;
import cpw.mods.fml.common.network.NetworkRegistry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraftforge.common.config.Configuration;

@Mod(modid = "recipehandler", name = "NoMoreRecipeConflict", useMetadata = true)
public final class RecipeMod {
    @SidedProxy(clientSide = "assets.recipehandler.ClientEventHandler", serverSide = "assets.recipehandler.PacketHandler")
    public static IRegister registry;
	private static final boolean debug = false;
    public static FMLEventChannel networkWrapper;
    public static boolean switchKey = false, cycleButton = true, cornerText = false;

	@EventHandler
	public void loading(FMLPreInitializationEvent event) {
		if (debug) {
			GameRegistry.addShapelessRecipe(new ItemStack(Items.golden_apple), Blocks.planks, Items.stick);
			GameRegistry.addShapelessRecipe(new ItemStack(Items.apple), Blocks.planks, Items.stick);
		}
		if (event.getSide().isClient()) {
            if(event.getSourceFile().getName().endsWith(".jar")){
                try {
                    Class.forName("mods.mud.ModUpdateDetector").getDeclaredMethod("registerMod", ModContainer.class, String.class, String.class).invoke(null,
                            FMLCommonHandler.instance().findContainerFor(this),
                            "https://raw.github.com/GotoLink/RecipeHandler/master/update.xml",
                            "https://raw.github.com/GotoLink/RecipeHandler/master/changelog.md"
                    );
                } catch (Throwable ignored) {
                }
            }
		}
        try{
            Configuration config = new Configuration(event.getSuggestedConfigurationFile());
            if(config.get(Configuration.CATEGORY_GENERAL, "Enable Custom Crafting Detection", true, "Tries do detect other crafting systems, disable for less processing").getBoolean())
                CraftingHandler.enableGuessing();
            switchKey = config.get(Configuration.CATEGORY_GENERAL, "Enable Switch Key", switchKey, "Can be modified in controls menu").getBoolean();
            cycleButton = config.get(Configuration.CATEGORY_GENERAL, "Enable Cycle Button", cycleButton, "Rendered in the crafting GUI").getBoolean();
            cornerText = config.get(Configuration.CATEGORY_GENERAL, "Render Text Tooltip", cornerText, "Rendered in the Top Right Corner of the screen").getBoolean();
            config.save();
        }catch (Throwable ignored){}
        registry.register();
        networkWrapper = NetworkRegistry.INSTANCE.newEventDrivenChannel(ChangePacket.CHANNEL);
        networkWrapper.register(new PacketHandler());
	}

    interface IRegister{
        public void register();
        public EntityPlayer getPlayer();
    }
}

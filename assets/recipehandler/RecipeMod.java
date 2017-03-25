package assets.recipehandler;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;

@Mod(modid = "recipehandler", name = "NoMoreRecipeConflict", version = "$version", acceptedMinecraftVersions = "&mcversion")
public final class RecipeMod {
    @SidedProxy(clientSide = "assets.recipehandler.ClientEventHandler", serverSide = "assets.recipehandler.PacketHandler")
    public static IRegister registry;
	private static final boolean debug = false;
    public static FMLEventChannel NETWORK;
    public static boolean switchKey = false, cycleButton = true, cornerText = false, creativeCraft = false;
    public static int xOffset = 0, yOffset = 0;

	@EventHandler
	public void preloading(FMLPreInitializationEvent event) {
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
            if(config.getBoolean("Enable Custom Crafting Detection", Configuration.CATEGORY_GENERAL, true, "Tries do detect other crafting systems, disable for less processing"))
                CraftingHandler.enableGuessing();
            switchKey = config.getBoolean("Enable Switch Key", Configuration.CATEGORY_GENERAL, switchKey, "Can be modified in controls menu");
            cycleButton = config.getBoolean("Enable Cycle Button", Configuration.CATEGORY_GENERAL, cycleButton, "Rendered in the crafting GUI");
            cornerText = config.getBoolean("Render Text Tooltip", Configuration.CATEGORY_GENERAL, cornerText, "Rendered in the Top Right Corner of the screen");
            if(cycleButton){
                Property property = config.get(Configuration.CATEGORY_CLIENT, "Cycle Button Horizontal Offset", 0);
                property.setComment("Offset for button from its default position, negative values to the left, positive to the right [default: 0]");
                xOffset = property.getInt();
                property = config.get(Configuration.CATEGORY_CLIENT, "Cycle Button Vertical Offset", 0);
                property.setComment("Offset for button from its default position, negative values to under, positive to over [default: 0]");
                yOffset = property.getInt();
            }
            creativeCraft = config.getBoolean("Enable Craft In Creative Inventory", Configuration.CATEGORY_CLIENT, creativeCraft, "Shows craft space in creative inventory tab");
            if(config.hasChanged())
                config.save();
        }catch (Throwable ignored){}
        NETWORK = NetworkRegistry.INSTANCE.newEventDrivenChannel(ChangePacket.CHANNEL);
        NETWORK.register(new PacketHandler());
	}

    @EventHandler
    public void loading(FMLInitializationEvent event) {
        registry.register();
        if (debug) {
            GameRegistry.addShapelessRecipe(new ItemStack(Items.GOLDEN_APPLE), Blocks.PLANKS, Items.STICK);
            GameRegistry.addShapelessRecipe(new ItemStack(Items.APPLE), Blocks.PLANKS, Items.STICK);
        }
    }

    interface IRegister{
        void register();
        EntityPlayer getPlayer();
        void scheduleTask(Runnable runner);
    }
}

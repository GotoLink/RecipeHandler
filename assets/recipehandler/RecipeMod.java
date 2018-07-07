package assets.recipehandler;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;

import javax.annotation.Nullable;
import java.util.Arrays;

@Mod(modid = "recipehandler", name = "NoMoreRecipeConflict", version = "$version", acceptedMinecraftVersions = "&mcversion")
public final class RecipeMod {
    @SidedProxy(clientSide = "assets.recipehandler.ClientEventHandler", serverSide = "assets.recipehandler.PacketHandler")
    public static IRegister registry;
	private static final boolean debug = false;
    public static FMLEventChannel NETWORK;
    public static boolean switchKey = false, cycleButton = true, cornerText = false, creativeCraft = false, onlyNecessary = false;
    public static int xOffset = 0, yOffset = 0;

	@EventHandler
	public void preloading(FMLPreInitializationEvent event) {
	    event.getModMetadata().updateJSON = "https://raw.github.com/GotoLink/RecipeHandler/master/update.json";
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
		//Setup the configuration file
        try{
            Configuration config = new Configuration(event.getSuggestedConfigurationFile());
            if(config.getBoolean("Enable Custom Crafting Detection", Configuration.CATEGORY_GENERAL, true, "Tries to detect other crafting systems, disable for less processing")){
                CraftingHandler.enableGuessing(Arrays.asList(config.getStringList("Black List Crafting Container", Configuration.CATEGORY_GENERAL, new String[]{"net.blay09.mods.cookingforblockheads.container.ContainerRecipeBook","morph.avaritia.container.ContainerExtremeCrafting"}, "List of containers to ignore for custom crafting detection")));
            }
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
                onlyNecessary = config.getBoolean("Limit Button To Conflict", Configuration.CATEGORY_CLIENT, onlyNecessary, "Only render button in case of conflict");
            }
            creativeCraft = config.getBoolean("Enable Craft In Creative Inventory", Configuration.CATEGORY_CLIENT, creativeCraft, "Shows craft space in creative inventory tab");
            if(config.getBoolean("Enable Furnace Recipes Tracking", Configuration.CATEGORY_GENERAL, false, "Tracks furnace recipes changes from the mod starting point")){
                //Copy furnace recipes
                FurnaceHandler.init();
            }
            if(config.hasChanged())
                config.save();
        }catch (Throwable ignored){}
        //Setup network for packet handling
        NETWORK = NetworkRegistry.INSTANCE.newEventDrivenChannel(ChangePacket.CHANNEL);
        NETWORK.register(new PacketHandler());
	}

    @EventHandler
    public void loading(FMLInitializationEvent event) {
	    //Register client side event listeners
        registry.register();
        if (debug) {//Conflicting recipes for debugging
            GameRegistry.addShapelessRecipe(new ResourceLocation("recipehandler:debug1"), new ResourceLocation("recipehandler:debug"), new ItemStack(Items.GOLDEN_APPLE), Ingredient.fromItem(Items.STICK));
            GameRegistry.addShapelessRecipe(new ResourceLocation("recipehandler:debug2"), new ResourceLocation("recipehandler:debug"), new ItemStack(Items.APPLE), Ingredient.fromItem(Items.STICK));
        }
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event){
	    event.registerServerCommand(new RecipeCommand());
    }

    /**
     * Cover the client logic
     */
    interface IRegister{
        void register();
        @Nullable
        EntityPlayer getPlayer();
        void scheduleTask(Runnable runner);
        void sendShift(InventoryCrafting crafting, Slot result);
        /**
         * @return The container the player has opened
         */
        @Nullable
        default Container getContainer(){ return getPlayer()!=null ? getPlayer().openContainer : null;}
    }
}

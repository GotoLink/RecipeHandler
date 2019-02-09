package assets.recipehandler;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

@Mod(modid = "recipehandler", name = "NoMoreRecipeConflict", version = "$version", acceptedMinecraftVersions = "&mcversion")
public final class RecipeMod {
    @SidedProxy(modId = "recipehandler", clientSide = "assets.recipehandler.ClientEventHandler", serverSide = "assets.recipehandler.Proxy")
    public static Proxy registry;
	private final boolean debug = false;

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
            registry.setup(config);
            if(config.hasChanged())
                config.save();
        }catch (Throwable ignored){}
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
	    event.registerServerCommand(new RecipeCommand("recipes_server", false));
    }
}

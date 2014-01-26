package assets.recipehandler;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Keyboard;

public class ClientEventHandler {
	public static final Minecraft mc = Minecraft.getMinecraft();
    public static final KeyBinding key = new KeyBinding("RecipeSwitch", Keyboard.KEY_ADD, "special");
    public int recipeIndex;
    private ItemStack oldItem = null;

    public ClientEventHandler() {
        ClientRegistry.registerKeyBinding(key);
    }

	@SubscribeEvent
	public void onRenderGui(RenderGameOverlayEvent.Text event) {
		if (mc != null && mc.thePlayer != null) {
			EntityClientPlayerMP player = mc.thePlayer;
			if (player.openContainer != null) {
				InventoryCrafting craft = null;
				if (player.openContainer instanceof ContainerPlayer) {
					craft = ((ContainerPlayer) player.openContainer).craftMatrix;
				} else if (player.openContainer instanceof ContainerWorkbench) {
					craft = ((ContainerWorkbench) player.openContainer).craftMatrix;
				}
				if (craft != null) {
					int result = CraftingHandler.getCraftResult(craft, mc.theWorld).size();
					if (result > 1) {
						event.right.add(StatCollector.translateToLocal("handler.found.text") + ": " + result);
					}
				}
			}
		}
	}

    public void register(){
        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void keyDown(InputEvent.KeyInputEvent event) {
        if (key.func_151470_d()) {
            if (mc != null && mc.thePlayer != null) {
                EntityClientPlayerMP player = mc.thePlayer;
                if (player.openContainer != null) {
                    InventoryCrafting craft = null;
                    if (player.openContainer instanceof ContainerPlayer) {
                        craft = ((ContainerPlayer) player.openContainer).craftMatrix;
                    } else if (player.openContainer instanceof ContainerWorkbench) {
                        craft = ((ContainerWorkbench) player.openContainer).craftMatrix;
                    }
                    if (craft != null) {
                        if (recipeIndex == Integer.MAX_VALUE) {
                            recipeIndex = 0;
                        } else {
                            recipeIndex++;
                        }
                        ItemStack res = CraftingHandler.findMatchingRecipe(craft, mc.theWorld, recipeIndex);
                        if (res != null && !ItemStack.areItemStacksEqual(res, oldItem)) {
                            RecipeMod.networkWrapper.sendToServer(new ChangePacket(player, res));
                            oldItem = res;
                        }
                    }
                }
            }
        }
    }
}

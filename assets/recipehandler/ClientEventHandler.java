package assets.recipehandler;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Keyboard;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;

public class ClientEventHandler {
	public static final Minecraft mc = Minecraft.getMinecraft();
    public static final KeyBinding key = new KeyBinding("RecipeSwitch", Keyboard.KEY_ADD, "key.categories.gui");
    public int recipeIndex;
    private ItemStack oldItem = null;
    private HashMap<String, Field> knownCraftingContainer = new HashMap<String, Field>();
    private HashSet<String> notCraftingContainer = new HashSet<String>();

    public ClientEventHandler() {
        ClientRegistry.registerKeyBinding(key);
    }

	@SubscribeEvent
	public void onRenderGui(RenderGameOverlayEvent.Text event) {
		if (mc.theWorld != null && mc.thePlayer != null) {
			EntityClientPlayerMP player = mc.thePlayer;
			if (player.openContainer != null) {
				InventoryCrafting craft = getCraftingMatrix(player.openContainer);
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
    public void keyDown(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START && key.getIsKeyPressed()) {
            if (mc.theWorld != null && mc.thePlayer != null) {
                EntityClientPlayerMP player = mc.thePlayer;
                if (player.openContainer != null) {
                    InventoryCrafting craft = getCraftingMatrix(player.openContainer);
                    if (craft != null) {
                        if (recipeIndex == Integer.MAX_VALUE) {
                            recipeIndex = 0;
                        } else {
                            recipeIndex++;
                        }
                        ItemStack res = CraftingHandler.findMatchingRecipe(craft, mc.theWorld, recipeIndex);
                        if (res != null && !ItemStack.areItemStacksEqual(res, oldItem)) {
                            RecipeMod.networkWrapper.sendToServer(new ChangePacket(player, res).toProxy(Side.SERVER));
                            oldItem = res;
                        }
                    }
                }
            }
        }
    }

    private InventoryCrafting getCraftingMatrix(Container container){
        if (container instanceof ContainerPlayer) {
            return ((ContainerPlayer) container).craftMatrix;
        } else if (container instanceof ContainerWorkbench) {
            return ((ContainerWorkbench) container).craftMatrix;
        }else {
            String name = container.getClass().getName();
            if (!notCraftingContainer.contains(name)) {
                Field f = knownCraftingContainer.get(name);
                if (f == null) {
                    for (Field field : container.getClass().getDeclaredFields()) {
                        if (field!=null && InventoryCrafting.class.isAssignableFrom(field.getClass())) {
                            try {
                                InventoryCrafting craft = InventoryCrafting.class.cast(field.get(container));
                                if(craft!=null){
                                    knownCraftingContainer.put(name, field);
                                    return craft;
                                }
                            } catch (ReflectiveOperationException ref) {
                                continue;
                            }
                        }
                    }
                    notCraftingContainer.add(name);
                } else {
                    try {
                        return InventoryCrafting.class.cast(f.get(container));
                    } catch (ReflectiveOperationException ref) {
                        knownCraftingContainer.put(name, null);
                    }
                }
            }
        }
        return null;
    }
}

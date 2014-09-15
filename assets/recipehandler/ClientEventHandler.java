package assets.recipehandler;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

public class ClientEventHandler implements RecipeMod.IRegister{
	private final Minecraft mc;
    private KeyBinding key;
    private int recipeIndex;
    private ItemStack oldItem = null;
    private boolean pressed = false;

    public ClientEventHandler() {
        mc = FMLClientHandler.instance().getClient();
    }

    @Override
    public void register(){
        if(RecipeMod.switchKey) {
            key = new KeyBinding("RecipeSwitch", Keyboard.KEY_ADD, "key.categories.gui");
            ClientRegistry.registerKeyBinding(key);
            FMLCommonHandler.instance().bus().register(this);
        }
        if(RecipeMod.cycleButton)
            MinecraftForge.EVENT_BUS.register(new GuiEventHandler());
        if(RecipeMod.cornerText)
            MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public EntityPlayer getPlayer(){
        return mc.thePlayer;
    }

	@SubscribeEvent
	public void onRenderGui(RenderGameOverlayEvent.Text event) {
		if (mc.theWorld != null && mc.thePlayer != null) {
            int result = CraftingHandler.getNumberOfCraft(mc.thePlayer.openContainer, mc.theWorld);
            if (result > 1) {
                event.right.add(StatCollector.translateToLocal("handler.found.text") + ": " + result);
            }
		}
	}

    @SubscribeEvent
    public void keyDown(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START && mc.theWorld != null && mc.thePlayer != null) {
            if (Keyboard.isKeyDown(key.getKeyCode())) {
                if(!pressed) {
                    pressed = true;
                    pressed();
                }
            }else if (pressed)
                pressed = false;
        }
    }

    public void pressed() {
        InventoryCrafting craft = CraftingHandler.getCraftingMatrix(mc.thePlayer.openContainer);
        if (craft != null) {
            if (recipeIndex == Integer.MAX_VALUE) {
                recipeIndex = 0;
            } else {
                recipeIndex++;
            }
            ItemStack res = CraftingHandler.findMatchingRecipe(craft, mc.theWorld, recipeIndex);
            if (res != null && !ItemStack.areItemStacksEqual(res, oldItem)) {
                RecipeMod.networkWrapper.sendToServer(new ChangePacket(0, res).toProxy(Side.CLIENT));
                oldItem = res;
            }
        }
    }
}

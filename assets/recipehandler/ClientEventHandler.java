package assets.recipehandler;

import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public final class ClientEventHandler implements RecipeMod.IRegister{
    private KeyBinding key;
    private ItemStack oldItem = null;
    private boolean pressed = false;

    @Override
    public void register(){
        if(RecipeMod.switchKey) {
            key = new KeyBinding("RecipeSwitch", KeyConflictContext.GUI, Keyboard.KEY_ADD, "key.categories.gui");
            ClientRegistry.registerKeyBinding(key);
        }
        MinecraftForge.EVENT_BUS.register(this);
        if(RecipeMod.cycleButton)
            MinecraftForge.EVENT_BUS.register(GuiEventHandler.INSTANCE);
    }

    @Override
    public EntityPlayer getPlayer(){
        return FMLClientHandler.instance().getClientPlayerEntity();
    }

    public static World getWorld(){
        return FMLClientHandler.instance().getWorldClient();
    }

	@SubscribeEvent
	public void onRenderGui(RenderGameOverlayEvent.Text event) {
		if (getPlayer() != null && RecipeMod.cornerText) {
			int result = CraftingHandler.getNumberOfCraft(getPlayer().openContainer, getWorld());
			if (result > 1) {
				event.getRight().add(I18n.translateToLocalFormatted("handler.found.text", result));
			}
		}
	}

    @SubscribeEvent
    public void keyDown(TickEvent.ClientTickEvent event) {
        if (getPlayer() != null && FMLClientHandler.instance().getClient().currentScreen != null) {
            if(event.phase == TickEvent.Phase.START && key != null) {
                if (Keyboard.isKeyDown(key.getKeyCode())) {
                    if (!pressed) {
                        pressed = true;
                        pressed();
                    }
                } else if (pressed)
                    pressed = false;
            }
            if(event.phase == TickEvent.Phase.END && Mouse.isButtonDown(0) && GuiScreen.isShiftKeyDown() && oldItem != null){//Shift click
                IInventory result = CraftingHandler.getResultSlot(getPlayer().openContainer, 1);
                if(result != null && !ItemStack.areItemStacksEqual(oldItem, result.getStackInSlot(0))){
					InventoryCrafting craft = CraftingHandler.getCraftingMatrix(getPlayer().openContainer);
					if(craft != null){
						ItemStack res = CraftingHandler.findMatchingRecipe(craft, getWorld());
						if(res != null){
							RecipeMod.networkWrapper.sendToServer(new ChangePacket(0, res, CraftingHandler.getRecipeIndex()).toProxy(Side.SERVER));
						}
					}
                }
            }
        }
    }

    public void pressed() {
        InventoryCrafting craft = CraftingHandler.getCraftingMatrix(getPlayer().openContainer);
        if (craft != null) {
            ItemStack res = CraftingHandler.findNextMatchingRecipe(craft, getWorld());
			if (res == null){
				oldItem = null;
			} else if (!ItemStack.areItemStacksEqual(res, oldItem)) {
                RecipeMod.networkWrapper.sendToServer(new ChangePacket(0, res, CraftingHandler.getRecipeIndex()).toProxy(Side.SERVER));
                oldItem = res;
            }
        }
    }
}

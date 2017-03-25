package assets.recipehandler;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public final class ClientEventHandler implements RecipeMod.IRegister{
    private KeyBinding key;
    private ItemStack oldItem = ItemStack.EMPTY;
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

    @Override
    public void scheduleTask(Runnable runner){
        FMLClientHandler.instance().getClient().addScheduledTask(runner);
    }

    public static World getWorld(){
        return FMLClientHandler.instance().getWorldClient();
    }

	@SubscribeEvent
	public void onRenderGui(RenderGameOverlayEvent.Text event) {
		if (RecipeMod.cornerText && getPlayer() != null) {
			int result = CraftingHandler.getNumberOfCraft(getPlayer().openContainer, getWorld());
			if (result > 1) {
				event.getRight().add(I18n.translateToLocalFormatted("handler.found.text", result));
			}
		}
	}

    @SubscribeEvent
    public void keyDown(InputEvent.MouseInputEvent event) {
        if (FMLClientHandler.instance().getClient().currentScreen != null) {
            if(key != null) {
                if (GameSettings.isKeyDown(key)) {
                    if (!pressed) {
                        pressed = true;
                        pressed();
                    }
                } else if (pressed)
                    pressed = false;
            }
            if(Mouse.isButtonDown(0) && GuiScreen.isShiftKeyDown()){//Shift click
                Slot result = null;
                if(FMLClientHandler.instance().getClient().currentScreen instanceof GuiContainer){
                    Slot slot = ((GuiContainer) FMLClientHandler.instance().getClient().currentScreen).getSlotUnderMouse();
                    if(!(slot instanceof SlotCrafting))
                        return;
                    result = slot;
                }
                InventoryCrafting craft = CraftingHandler.getCraftingMatrix(getPlayer().openContainer);
                if(craft != null){
                    if(result == null) {
                        result = CraftingHandler.getResultSlot(getPlayer().openContainer, craft, 0);
                    }
                    if(result != null){
                        ItemStack res = CraftingHandler.findMatchingRecipe(craft, getWorld());
                        if (res.isEmpty()){
                            oldItem = ItemStack.EMPTY;
                        } else if(!ItemStack.areItemStacksEqual(res, result.getStack())){
                            RecipeMod.NETWORK.sendToServer(new ChangePacket(result.slotNumber, res, CraftingHandler.getRecipeIndex()).toProxy(Side.SERVER));
                            oldItem = res;
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
			if (res.isEmpty()){
				oldItem = ItemStack.EMPTY;
			} else if (!ItemStack.areItemStacksEqual(res, oldItem)) {
			    int index = 0;
                Slot slot = CraftingHandler.getResultSlot(getPlayer().openContainer, craft, index);
                if(slot!= null)
                    index = slot.slotNumber;
                RecipeMod.NETWORK.sendToServer(new ChangePacket(index, res, CraftingHandler.getRecipeIndex()).toProxy(Side.SERVER));
                oldItem = res;
            }
        }
    }
}

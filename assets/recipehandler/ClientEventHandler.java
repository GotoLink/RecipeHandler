package assets.recipehandler;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.*;
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

    /**
     * Register keybind and event handlers
     */
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

    /**
     *
     * @return the client player entity
     */
    @Override
    public EntityPlayer getPlayer(){
        return FMLClientHandler.instance().getClientPlayerEntity();
    }

    /**
     * Send given task to the client main thread scheduler
     * @param runner to be sent
     */
    @Override
    public void scheduleTask(Runnable runner){
        FMLClientHandler.instance().getClient().addScheduledTask(runner);
    }

    /**
     * Called by ChangePacket on the main thread if shift is set
     * Simulates shift clicks, as long as the player keeps shift key down
     * @param crafting the inventory the player craft with
     * @param slot the result slot
     */
    @Override
    public void sendShift(InventoryCrafting crafting, Slot slot){
        if (FMLClientHandler.instance().getClient().currentScreen instanceof GuiContainer && GuiScreen.isShiftKeyDown()) {
            GuiContainer screen = (GuiContainer) FMLClientHandler.instance().getClient().currentScreen;
            //Simulate left click
            ItemStack temp = FMLClientHandler.instance().getClient().playerController.windowClick(screen.inventorySlots.windowId, slot.slotNumber, 0, ClickType.QUICK_MOVE, getPlayer());
            ItemStack result = CraftingHandler.findMatchingRecipe(crafting, getWorld());
            if(!slot.getHasStack() && !result.isEmpty()){
                if (ItemStack.areItemStacksEqual(result, temp)) {
                    //Recipe still match, so bounce it back to server
                    RecipeMod.NETWORK.sendToServer(new ChangePacket(slot.slotNumber, result, CraftingHandler.getRecipeIndex()).setShift().toProxy(Side.SERVER));
                }
            }
        }
    }

    /**
     *
     * @return The world of the client
     */
    public static World getWorld(){
        return FMLClientHandler.instance().getWorldClient();
    }

    /**
     *
     * @return The container the player has opened
     */
    private Container getContainer(){
        return getPlayer().openContainer;
    }

    /**
     * Rendering event for the game overlay, dedicated to text lines
     * @param event the render event
     */
	@SubscribeEvent
	public void onRenderGui(RenderGameOverlayEvent.Text event) {
		if (RecipeMod.cornerText && getPlayer() != null && FMLClientHandler.instance().getClient().currentScreen != null) {
			int result = CraftingHandler.getNumberOfCraft(getContainer(), getWorld());
			if (result > 1) {
				event.getRight().add(I18n.translateToLocalFormatted("handler.found.text", result));
			}
		}
	}

    /**
     * Client tick that happens after all keybinds are processed, in a loaded world
     * @param event the tick event
     */
    @SubscribeEvent
    public void keyDown(InputEvent.MouseInputEvent event) {
        if (FMLClientHandler.instance().getClient().currentScreen != null) {
            if(key != null) {//Switch keybind logic
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
                    if(!(slot instanceof SlotCrafting))//Other inventory event, don't mess it up
                        return;
                    result = slot;
                }
                InventoryCrafting craft = CraftingHandler.getCraftingMatrix(getContainer());
                if(craft != null){
                    if(result == null) {//Shouldn't happen, but who knows
                        result = CraftingHandler.getResultSlot(getContainer(), craft, 0);
                    }
                    if(result != null){
                        ItemStack res = CraftingHandler.findMatchingRecipe(craft, getWorld());
                        if (res.isEmpty()){
                            oldItem = ItemStack.EMPTY;
                        } else if(!ItemStack.areItemStacksEqual(res, result.getStack())){
                            RecipeMod.NETWORK.sendToServer(new ChangePacket(result.slotNumber, res, CraftingHandler.getRecipeIndex()).setShift().toProxy(Side.SERVER));
                            oldItem = res;
                        }
					}
                }
            }
        }
    }

    /**
     * The full switch logic
     * Based on the current open container, find a different recipe, then send it to the server
     */
    public void pressed() {
        InventoryCrafting craft = CraftingHandler.getCraftingMatrix(getContainer());
        if (craft != null) {
            ItemStack res = CraftingHandler.findNextMatchingRecipe(craft, getWorld());
			if (res.isEmpty()){
				oldItem = ItemStack.EMPTY;
			} else if (!ItemStack.areItemStacksEqual(res, oldItem)) {
			    int index = 0;//The default craft result slot index for containers
                Slot slot = CraftingHandler.getResultSlot(getContainer(), craft, index);
                if(slot!= null)
                    index = slot.slotNumber;
                RecipeMod.NETWORK.sendToServer(new ChangePacket(index, res, CraftingHandler.getRecipeIndex()).toProxy(Side.SERVER));
                oldItem = res;
            }
        }
    }
}

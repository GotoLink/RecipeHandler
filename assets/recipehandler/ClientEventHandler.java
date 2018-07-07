package assets.recipehandler;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.client.resources.I18n;
import net.minecraft.world.World;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import javax.annotation.Nullable;

public final class ClientEventHandler implements RecipeMod.IRegister{
    private KeyBinding key;
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
    @Nullable
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
            final GuiContainer screen = (GuiContainer) FMLClientHandler.instance().getClient().currentScreen;
            //Simulate left click
            ItemStack temp = FMLClientHandler.instance().getClient().playerController.windowClick(screen.inventorySlots.windowId, slot.slotNumber, 0, ClickType.QUICK_MOVE, getPlayer());
            ItemStack result = CraftingHandler.findCraftResult(crafting, getWorld());
            if(!slot.getHasStack() && !result.isEmpty() && temp.isEmpty()){
                //Recipe still match, so bounce it back to server
                RecipeMod.NETWORK.sendToServer(new ChangePacket(slot.slotNumber, result, CraftingHandler.getRecipeIndex()).setShift().toProxy(Side.SERVER));
            }
        }
    }

    /**
     *
     * @return The world of the client
     */
    @Nullable
    public static World getWorld(){
        return FMLClientHandler.instance().getWorldClient();
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
				event.getRight().add(I18n.format("handler.found.text", result));
			}
		}
	}

    /**
     * Keyboard event that happens after the keybind is processed in a screen
     * @param event the keyboard event
     */
    @SubscribeEvent
    public void keyDown(GuiScreenEvent.KeyboardInputEvent.Post event) {
        if(key != null && Keyboard.getEventKey() == key.getKeyCode() && getPlayer() != null) {//Switch keybind logic
            if (Keyboard.getEventKeyState()) {
                if (!pressed) {
                    pressed = true;
                    pressed();
                }
            } else if (pressed)
                pressed = false;
        }
    }

    /**
     * Mouse event that happens after the keybind is processed in a screen
     * @param event the mouse event
     */
    @SubscribeEvent
    public void mouseDown(GuiScreenEvent.MouseInputEvent.Post event) {
        if(Mouse.getEventButton() == 0 && Mouse.getEventButtonState() && GuiScreen.isShiftKeyDown()){//Shift click
            if(getPlayer() == null) {
                return;
            }
            Slot result = null;
            if(event.getGui() instanceof GuiContainer){
                Slot slot = ((GuiContainer) event.getGui()).getSlotUnderMouse();
                if(!(slot instanceof SlotCrafting))//Other inventory event, don't mess it up
                    return;
                result = slot;
            }
            if(CraftingHandler.getNumberOfCraft(getContainer(), getWorld()) < 2) {//Not a conflict
                return;
            }
            InventoryCrafting craft = CraftingHandler.getCraftingMatrix(getContainer());
            if(craft != null){
                if(result == null) {//Shouldn't happen, but who knows
                    result = CraftingHandler.getResultSlot(getContainer(), craft, 0);
                }
                if(result != null){
                    ItemStack res = CraftingHandler.findCraftResult(craft, getWorld());
                    if (!res.isEmpty() && !ItemStack.areItemStacksEqual(res, result.getStack())){
                        RecipeMod.NETWORK.sendToServer(new ChangePacket(result.slotNumber, res, CraftingHandler.getRecipeIndex()).setShift().toProxy(Side.SERVER));
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
			if (!res.isEmpty()){
                Slot slot = CraftingHandler.getResultSlot(getContainer(), craft, 0);
                if(slot != null && !ItemStack.areItemStacksEqual(res, slot.getStack())){
                    RecipeMod.NETWORK.sendToServer(new ChangePacket(slot.slotNumber, res, CraftingHandler.getRecipeIndex()).toProxy(Side.SERVER));
                }
            }
        }
    }
}

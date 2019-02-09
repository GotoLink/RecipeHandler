package assets.recipehandler;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.client.resources.I18n;
import net.minecraft.world.World;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import javax.annotation.Nullable;

public final class ClientEventHandler extends Proxy{
    private boolean switchKey = false, cycleButton = true;
    public static boolean cornerText = false, creativeCraft = false, onlyNecessary = false;
    public static int xOffset = 0, yOffset = 0;
    private KeyBinding key;
    private boolean pressed = false;

    @Override
    public void setup(Configuration config) {
        super.setup(config);
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
    }

    /**
     * Register keybind and event handlers
     */
    @Override
    public void register(){
        super.register();
        if(switchKey) {
            key = new KeyBinding("RecipeSwitch", KeyConflictContext.GUI, Keyboard.KEY_ADD, "key.categories.gui");
            ClientRegistry.registerKeyBinding(key);
        }
        MinecraftForge.EVENT_BUS.register(this);
        if(cycleButton)
            MinecraftForge.EVENT_BUS.register(GuiEventHandler.INSTANCE);
        ClientCommandHandler.instance.registerCommand(new RecipeCommand("recipes_client", true));
    }

    /**
     *
     * @return the client player entity
     */
    @Nullable
    public EntityPlayer getPlayer(){
        return FMLClientHandler.instance().getClientPlayerEntity();
    }

    /**
     * @return The container the player has opened
     */
    @Nullable
    Container getContainer(){ return getPlayer()!=null ? getPlayer().openContainer : null;}

    /**
     * Send given task to the client main thread scheduler
     * @param runner to be sent
     */
    @Override
    public void scheduleTask(ChangePacket runner){
        FMLClientHandler.instance().getClient().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                if(!runner.stack().isEmpty()) {
                    Container container = getContainer();
                    InventoryCrafting crafting = craftingHandler.getCraftingMatrix(container);
                    if(crafting != null) {
                        Slot result = craftingHandler.getResultSlot(container, crafting, runner.slot());
                        if (result != null) {
                            result.putStack(runner.stack());
                            if(runner.isShift()){
                                craftingHandler.setRecipeIndex(runner.index());
                                sendShift(crafting, result);
                            }
                        }
                    }
                }
            }
        });
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
            ItemStack result = craftingHandler.findCraftResult(crafting, getWorld());
            if(!slot.getHasStack() && !result.isEmpty() && temp.isEmpty()){
                //Recipe still match, so bounce it back to server
                sendToServer(slot.slotNumber, result, true);
            }
        }
    }

    public void sendToServer(int slot, ItemStack itemStack, boolean shift){
        ChangePacket packet = new ChangePacket(slot, itemStack, craftingHandler.getRecipeIndex());
        if(shift)
            packet.setShift();
        network.sendToServer(packet.toProxy(Side.SERVER));
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
		if (cornerText && getPlayer() != null && FMLClientHandler.instance().getClient().currentScreen != null) {
			int result = craftingHandler.getNumberOfCraft(getContainer(), getWorld());
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
            if(craftingHandler.getNumberOfCraft(getContainer(), getWorld()) < 2) {//Not a conflict
                return;
            }
            InventoryCrafting craft = craftingHandler.getCraftingMatrix(getContainer());
            if(craft != null){
                if(result == null) {//Shouldn't happen, but who knows
                    result = craftingHandler.getResultSlot(getContainer(), craft, 0);
                }
                if(result != null){
                    ItemStack res = craftingHandler.findCraftResult(craft, getWorld());
                    if (!res.isEmpty() && !ItemStack.areItemStacksEqual(res, result.getStack())){
                        sendToServer(result.slotNumber, res, true);
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
        InventoryCrafting craft = craftingHandler.getCraftingMatrix(getContainer());
        if (craft != null) {
            ItemStack res = craftingHandler.findNextMatchingRecipe(craft, getWorld());
			if (!res.isEmpty()){
                Slot slot = craftingHandler.getResultSlot(getContainer(), craft, 0);
                if(slot != null && !ItemStack.areItemStacksEqual(res, slot.getStack())){
                    sendToServer(slot.slotNumber, res, false);
                }
            }
        }
    }
}

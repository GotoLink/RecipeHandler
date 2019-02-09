package assets.recipehandler;

import com.google.common.base.Predicates;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiButtonImage;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.recipebook.IRecipeShownListener;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

/**
 * Deals with all the screen rendering
 */
public final class GuiEventHandler {
    public static final GuiEventHandler INSTANCE = new GuiEventHandler();
    private int deltaX = 0;

    private GuiEventHandler(){}

    /**
     * When potion effects apply, some screen shift
     * Keep record of this event
     * @param potionShift
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onEffectInGui(GuiScreenEvent.PotionShiftEvent potionShift){
        if(potionShift.getGui() instanceof GuiContainer){
            deltaX = 60;//Default shift to the right
        }
    }

    /**
     * After the gui is opened and button list is initialized
     * Add the switch button if a craft space is detected
     * @param event
     */
    @SubscribeEvent
    public void onPostInitGui(GuiScreenEvent.InitGuiEvent.Post event){
        if(event.getGui() instanceof GuiContainer){
            final GuiContainer container = (GuiContainer) event.getGui();
            int xOffset = ClientEventHandler.xOffset;
            int yOffset = ClientEventHandler.yOffset;
            CreativeButton button = null;
            if(container instanceof GuiContainerCreative){//Special handling of creative craft space
                if(!ClientEventHandler.creativeCraft)
                    return;
                xOffset += 166;
                yOffset += 34;
                button = new CreativeButton(event.getButtonList().size() + 2, xOffset, yOffset);
            }
            else {
                InventoryCrafting craft = RecipeMod.registry.craftingHandler.getCraftingMatrix(container.inventorySlots);
                if (craft != null) {
                    Slot slot = RecipeMod.registry.craftingHandler.getResultSlot(container.inventorySlots, craft, 0);
                    if (slot != null) {
                        xOffset += slot.xPos + 2;
                        yOffset += slot.yPos + 22;
                        button = new CreativeButton(event.getButtonList().size(), xOffset, yOffset);
                    }
                }
            }
            if (button != null){
                button.x += deltaX;
                event.getButtonList().add(button);
            }
            deltaX = 0;
        }
    }

    /**
     * After the recipe book button is pressed, screen can shift
     * Shift the button according to new screen values
     * @param event
     */
    @SubscribeEvent
    public void onPostBookToggle(GuiScreenEvent.ActionPerformedEvent.Post event){
        if(event.getButton() instanceof GuiButtonImage && event.getGui() instanceof GuiContainer && event.getGui() instanceof IRecipeShownListener){
            final GuiContainer container = (GuiContainer) event.getGui();
            event.getButtonList().stream().filter(Predicates.instanceOf(CreativeButton.class)).forEach(guiButton -> ((CreativeButton)guiButton).setupXY(container) );
        }
    }

    /**
     * The switch button
     */
    final class CreativeButton extends GuiButton {
        private final ResourceLocation texture = new ResourceLocation("textures/gui/container/villager.png");
        private static final int WIDTH = 12, HEIGHT = 16;
        private final int xOffset, yOffset;
        private boolean firstDraw = true;
        public CreativeButton(int id, int posX, int posY){
            super(id, posX, posY, WIDTH, HEIGHT, "0");
            xOffset = posX;
            yOffset = posY;
        }

        public void setupXY(GuiContainer container){
            this.x = container.getGuiLeft() + xOffset;
            this.y = container.getGuiTop() + yOffset;
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partTicks){
            if(mc.currentScreen instanceof GuiContainerCreative){
                this.visible = ClientEventHandler.creativeCraft && ((GuiContainerCreative) mc.currentScreen).getSelectedTabIndex() == CreativeTabs.INVENTORY.getIndex();
            }
            if (this.visible) {
                if(firstDraw){
                    setupXY((GuiContainer)mc.currentScreen);
                    firstDraw = false;
                }
                //Specific handling for the creative menu inventory tab
                if(mc.currentScreen instanceof GuiContainerCreative){
                    GuiContainerCreative creative = (GuiContainerCreative) mc.currentScreen;
                    Slot slot;
                    for(int i = 0; i < 4; i++) {
                        //Move equip slots away
                        slot = creative.inventorySlots.getSlot(i+5);
                        slot.xPos = (i/2)*54;
                        if(i<2)
                            slot.xPos = 14;
                        //Craft space
                        slot = creative.inventorySlots.getSlot(i+1);
                        slot.xPos = 108 + (i%2)*18;
                        slot.yPos = 6 + (i/2)*18;
                    }
                    //Result slot
                    slot = creative.inventorySlots.getSlot(0);
                    slot.xPos = 164;
                    slot.yPos = 16;
                    GlStateManager.enableRescaleNormal();
                    GlStateManager.enableDepth();
                    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                    //Overlay the equip slots
                    mc.getTextureManager().bindTexture(new ResourceLocation("textures/gui/container/creative_inventory/tab_inventory.png"));
                    creative.drawTexturedModalRect(creative.getGuiLeft() + 13, creative.getGuiTop() + 5, 107, 5, 18, 44);
                    //Render craft space
                    mc.getTextureManager().bindTexture(GuiContainer.INVENTORY_BACKGROUND);
                    creative.drawTexturedModalRect(creative.getGuiLeft() + 106, creative.getGuiTop() + 3, 96, 15, 76, 47);
                    GlStateManager.disableRescaleNormal();
                    GlStateManager.disableDepth();
                }
                //Render craft switch
                int crafts = RecipeMod.registry.craftingHandler.getNumberOfCraft(mc.player.openContainer, mc.player.world);
                if(crafts == -1) {
                    enabled = false;
                    return;
                }
                enabled = crafts > 1;
                if(enabled || !ClientEventHandler.onlyNecessary) {
                    //Render the 'villager choice' arrow
                    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                    mc.getTextureManager().bindTexture(this.texture);
                    int k = 177;
                    if (!this.enabled)
                        k += this.width * 2;
                    else if (super.mousePressed(mc, mouseX, mouseY))
                        k += this.width;
                    this.drawTexturedModalRect(this.x, this.y, k, 2, this.width, this.height);
                    //Render the number of crafts
                    if (!ClientEventHandler.cornerText) {
                        displayString = String.valueOf(crafts);
                        int l = this.enabled ? 0xFFFFFF : 10526880;
                        this.drawCenteredString(mc.fontRenderer, this.displayString, this.x, this.y + this.height / 2, l);
                    }
                }
            }
        }

        @Override
        public boolean mousePressed(Minecraft mc, int mouseX, int mouseY){
            boolean onButton = super.mousePressed(mc, mouseX, mouseY);
            if (onButton){
                ((ClientEventHandler)RecipeMod.registry).pressed();
            }
            return onButton;
        }
    }
}

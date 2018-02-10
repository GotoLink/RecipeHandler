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
            InventoryCrafting craft = CraftingHandler.getCraftingMatrix(container.inventorySlots);
            if (craft != null || (RecipeMod.creativeCraft && container instanceof GuiContainerCreative)){
                int guiTop = (container.height) / 2 + RecipeMod.yOffset;
                CreativeButton button = new CreativeButton(event.getButtonList().size() + 2, 0, guiTop);
                button.setupX(container);
                button.x += deltaX;
                event.getButtonList().add(button);
                if(!RecipeMod.creativeCraft && container instanceof GuiContainerCreative){
                    event.getButtonList().remove(button);
                }
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
            event.getButtonList().stream().filter(Predicates.instanceOf(CreativeButton.class)).forEach(guiButton -> ((CreativeButton)guiButton).setupX(container) );
        }
    }

    /**
     * The switch button
     */
    final class CreativeButton extends GuiButton {
        private final ResourceLocation texture = new ResourceLocation("textures/gui/container/villager.png");
        private static final int WIDTH = 12, HEIGHT = WIDTH + 7;
        public CreativeButton(int id, int posX, int posY){
            super(id, posX-WIDTH, posY-2*HEIGHT+3, WIDTH, HEIGHT, "0");
        }

        public void setupX(GuiContainer container){
            this.x = container.getGuiLeft() + container.getXSize() + RecipeMod.xOffset - this.width - 3;
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partTicks){
            if(mc.currentScreen instanceof GuiContainerCreative){
                this.visible = RecipeMod.creativeCraft && ((GuiContainerCreative) mc.currentScreen).getSelectedTabIndex() == CreativeTabs.INVENTORY.getTabIndex();
            }
            if (this.visible) {
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
                int crafts = CraftingHandler.getNumberOfCraft(mc.player.openContainer, mc.player.world);
                enabled = crafts > 1;
                if(enabled || !RecipeMod.onlyNecessary) {
                    //Render the 'villager choice' arrow
                    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                    mc.getTextureManager().bindTexture(this.texture);
                    int k = 176;
                    if (!this.enabled)
                        k += this.width * 2;
                    else if (super.mousePressed(mc, mouseX, mouseY))
                        k += this.width;
                    this.drawTexturedModalRect(this.x, this.y, k, 0, this.width, this.height);
                    //Render the number of crafts
                    if (!RecipeMod.cornerText) {
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

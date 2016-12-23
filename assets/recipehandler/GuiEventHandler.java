package assets.recipehandler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public final class GuiEventHandler {
    public static final GuiEventHandler INSTANCE = new GuiEventHandler();
    private int deltaX = 0;

    private GuiEventHandler(){}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onEffectInGui(GuiScreenEvent.PotionShiftEvent potionShift){
        if(potionShift.getGui() instanceof GuiContainer){
            deltaX = 60;
        }
    }

    @SubscribeEvent
    public void onPostInitGui(GuiScreenEvent.InitGuiEvent.Post event){
        if(event.getGui() instanceof GuiContainer){
            InventoryCrafting craft = CraftingHandler.getCraftingMatrix(((GuiContainer) event.getGui()).inventorySlots);
            if (craft != null){
                int guiLeft = (event.getGui().width + ((GuiContainer) event.getGui()).getXSize()) / 2 + deltaX;
                int guiTop = (event.getGui().height) / 2;
                event.getButtonList().add(new CreativeButton(event.getButtonList().size() + 2, guiLeft + RecipeMod.xOffset, guiTop + RecipeMod.yOffset));
            }
            deltaX = 0;
        }
    }

    public final class CreativeButton extends GuiButton {
        private final ResourceLocation texture = new ResourceLocation("textures/gui/container/villager.png");
        private static final int WIDTH = 12, HEIGHT = WIDTH + 7;
        public CreativeButton(int id, int posX, int posY){
            super(id, posX-WIDTH-3, posY-2*HEIGHT, WIDTH, HEIGHT, "0");
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY){
            if (this.visible) {
                int crafts = CraftingHandler.getNumberOfCraft(mc.player.openContainer, mc.world);
                displayString = String.valueOf(crafts);
                enabled = crafts > 1;
                GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                mc.renderEngine.bindTexture(this.texture);
                int k = 176;
                if (!this.enabled)
                    k += this.width * 2;
                else if(super.mousePressed(mc, mouseX, mouseY))
                    k += this.width;
                this.drawTexturedModalRect(this.xPosition, this.yPosition, k, 0, this.width, this.height);
                if(!RecipeMod.cornerText) {
                    int l = this.enabled ? 0xFFFFFF : 10526880;
                    this.drawCenteredString(mc.fontRendererObj, this.displayString, this.xPosition, this.yPosition + this.height / 2, l);
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

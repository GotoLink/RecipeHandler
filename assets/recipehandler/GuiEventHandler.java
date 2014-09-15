package assets.recipehandler;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiScreenEvent;
import org.lwjgl.opengl.GL11;

/**
 * Created by Olivier on 15/09/2014.
 */
public class GuiEventHandler {
    @SubscribeEvent
    public void onPostInitGui(GuiScreenEvent.InitGuiEvent.Post event){
        if(event.gui instanceof GuiContainer){
            InventoryCrafting craft = CraftingHandler.getCraftingMatrix(((GuiContainer) event.gui).inventorySlots);
            if (craft != null){
                int guiLeft = (event.gui.width + 176) / 2;
                int guiTop = (event.gui.height) / 2;
                event.buttonList.add(new CreativeButton(event.buttonList.size()+2, guiLeft, guiTop));
            }
        }
    }

    public static class CreativeButton extends GuiButton {
        private final ResourceLocation texture = new ResourceLocation("textures/gui/container/villager.png");
        private static final int WIDTH = 12, HEIGHT = WIDTH + 7;
        private int timer = 0;
        public CreativeButton(int id, int posX, int posY){
            super(id, posX-WIDTH-3, posY-2*HEIGHT, WIDTH, HEIGHT, "0");
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY){
            if (this.visible) {
                if(timer>20){
                    timer = 0;
                    displayString = String.valueOf(CraftingHandler.getNumberOfCraft(mc.thePlayer.openContainer, mc.theWorld));
                    enabled = !("0".equals(displayString));
                }else
                    timer++;
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
                    this.drawCenteredString(mc.fontRenderer, this.displayString, this.xPosition, this.yPosition + this.height / 2, l);
                }
            }
        }

        @Override
        public boolean mousePressed (Minecraft mc, int mouseX, int mouseY){
            boolean onButton = super.mousePressed(mc, mouseX, mouseY);
            if (onButton){
                ((ClientEventHandler)RecipeMod.registry).pressed();
            }
            return onButton;
        }
    }
}

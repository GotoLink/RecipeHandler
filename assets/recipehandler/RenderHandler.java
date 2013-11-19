package assets.recipehandler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.ForgeSubscribe;

public class RenderHandler {
	public static final Minecraft mc = Minecraft.getMinecraft();

	@ForgeSubscribe
	public void onRenderGui(RenderGameOverlayEvent.Text event) {
		if (mc != null && mc.thePlayer != null) {
			EntityClientPlayerMP player = mc.thePlayer;
			if (player.openContainer != null) {
				InventoryCrafting craft = null;
				if (player.openContainer instanceof ContainerPlayer) {
					craft = ((ContainerPlayer) player.openContainer).craftMatrix;
				} else if (player.openContainer instanceof ContainerWorkbench) {
					craft = ((ContainerWorkbench) player.openContainer).craftMatrix;
				}
				if (craft != null) {
					int result = CraftingHandler.getCraftResult(craft, mc.theWorld).size();
					if (result > 1) {
						event.right.add(StatCollector.translateToLocal("handler.found.text") + ": " + result);
					}
				}
			}
		}
	}
}

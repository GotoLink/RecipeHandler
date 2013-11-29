package assets.recipehandler;

import java.util.EnumSet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import cpw.mods.fml.client.registry.KeyBindingRegistry.KeyHandler;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.network.PacketDispatcher;

public class RecipeKeyHandler extends KeyHandler {
	public static final String KEY_STRING = "RecipeSwitch";
	public static final Minecraft mc = Minecraft.getMinecraft();
	public int recipeIndex;
	private ItemStack oldItem = null;

	public RecipeKeyHandler(int key) {
		super(new KeyBinding[] { new KeyBinding(KEY_STRING, key) }, new boolean[] { true });
	}

	@Override
	public String getLabel() {
		return KEY_STRING;
	}

	@Override
	public void keyDown(EnumSet<TickType> types, KeyBinding kb, boolean tickEnd, boolean isRepeat) {
		if (kb.keyDescription.equals(KEY_STRING) && tickEnd) {
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
						if (recipeIndex == Integer.MAX_VALUE) {
							recipeIndex = 0;
						} else {
							recipeIndex++;
						}
						ItemStack res = CraftingHandler.findMatchingRecipe(craft, mc.theWorld, recipeIndex);
						if (res != null && !ItemStack.areItemStacksEqual(res, oldItem)) {
							PacketDispatcher.sendPacketToServer(PacketHandler.getPacket(player.entityId, res));
							oldItem = res;
						}
					}
				}
			}
		}
	}

	@Override
	public void keyUp(EnumSet<TickType> types, KeyBinding kb, boolean tickEnd) {
	}

	@Override
	public EnumSet<TickType> ticks() {
		return EnumSet.of(TickType.CLIENT);
	}
}

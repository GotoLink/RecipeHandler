package assets.recipehandler;

import java.util.EnumSet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import cpw.mods.fml.client.registry.KeyBindingRegistry.KeyHandler;
import cpw.mods.fml.common.TickType;

public class RecipeKeyHandler extends KeyHandler{

	public static final String KEY_STRING = "RecipeSwitch";
	public int recipeIndex;
	private ItemStack oldItem = null;
	private boolean keyPressed;
	public RecipeKeyHandler(int key) 
	{
		super(new KeyBinding[]{new KeyBinding(KEY_STRING,key)}, new boolean[]{true});
	}

	@Override
	public String getLabel() 
	{
		return KEY_STRING;
	}

	@Override
	public void keyDown(EnumSet<TickType> types, KeyBinding kb, boolean tickEnd, boolean isRepeat) 
	{
		if(kb.keyDescription == KEY_STRING && tickEnd && !keyPressed)
		{
			keyPressed = true;
			if(Minecraft.getMinecraft()!=null && Minecraft.getMinecraft().thePlayer!=null)
			{
				EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
				if(Minecraft.getMinecraft().currentScreen instanceof GuiContainer && player.openContainer!=null)
				{
					InventoryCrafting craft = null;
					IInventory result = null;
					if(player.openContainer instanceof ContainerPlayer)
					{
						craft = ((ContainerPlayer)player.openContainer).craftMatrix;
						result = ((ContainerPlayer)player.openContainer).craftResult;
					}
					else if(player.openContainer instanceof ContainerWorkbench)
					{
						craft = ((ContainerWorkbench)player.openContainer).craftMatrix;
						result = ((ContainerWorkbench)player.openContainer).craftResult;
					}
					if(result!=null)
					{
						recipeIndex++;
						ItemStack res = CraftingHandler.findMatchingRecipe(craft,Minecraft.getMinecraft().theWorld, recipeIndex);
						if(res!=null && res!=oldItem)
						{
							player.sendQueue.addToSendQueue(PacketHandler.getPacket(player.entityId, res.itemID, res.stackSize, res.getItemDamage()));
							oldItem = res;
						}
					}
				}
			}
		}
	}

	@Override
	public void keyUp(EnumSet<TickType> types, KeyBinding kb, boolean tickEnd) 
	{
		if(kb.keyDescription == KEY_STRING && tickEnd)
			keyPressed = false;
	}

	@Override
	public EnumSet<TickType> ticks() 
	{
		return EnumSet.of(TickType.CLIENT);
	}
}

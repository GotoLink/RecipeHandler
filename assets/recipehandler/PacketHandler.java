package assets.recipehandler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.Packet250CustomPayload;
import cpw.mods.fml.common.network.IPacketHandler;
import cpw.mods.fml.common.network.Player;

public class PacketHandler implements IPacketHandler {
	public static final String CHANNEL = "RecipeModChanel";

	@Override
	public void onPacketData(INetworkManager manager, Packet250CustomPayload packet, Player player) {
		if (packet.channel.equals(CHANNEL)) {
			handle(packet, (EntityPlayer) player);
		}
	}

	private void handle(Packet250CustomPayload packet, EntityPlayer player) {
		DataInputStream inStream = new DataInputStream(new ByteArrayInputStream(packet.data));
		int data;
		ItemStack stack;
		try {
			data = inStream.readInt();
			stack = Packet.readItemStack(inStream);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		IInventory result = null;
		EntityPlayer ePlayer = (EntityPlayer) player.worldObj.getEntityByID(data);
		if (ePlayer.openContainer instanceof ContainerPlayer) {
			result = ((ContainerPlayer) ePlayer.openContainer).craftResult;
		} else if (ePlayer.openContainer instanceof ContainerWorkbench) {
			result = ((ContainerWorkbench) ePlayer.openContainer).craftResult;
		}
		if (result != null) {
			result.setInventorySlotContents(0, stack.copy());
			if (ePlayer instanceof EntityPlayerMP) {
				((EntityPlayerMP) ePlayer).playerNetServerHandler.sendPacketToPlayer(packet);
			}
		}
	}

	public static Packet getPacket(int d, ItemStack stack) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream outputStream = new DataOutputStream(bos);
		try {
			outputStream.writeInt(d);
			Packet.writeItemStack(stack, outputStream);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		Packet250CustomPayload packet = new Packet250CustomPayload();
		packet.channel = CHANNEL;
		packet.data = bos.toByteArray();
		packet.length = bos.size();
		return packet;
	}
}

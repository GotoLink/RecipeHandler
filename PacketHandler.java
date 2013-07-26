package assets.recipehandler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet250CustomPayload;
import cpw.mods.fml.common.network.IPacketHandler;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.common.network.Player;

public class PacketHandler implements IPacketHandler{

	@Override
	public void onPacketData(INetworkManager manager, Packet250CustomPayload packet, Player player) 
	{
		if(packet.channel.equals(Constants.CHANNEL))
		{
			handle(packet, (EntityPlayer)player);
		}
	}

	private void handle(Packet250CustomPayload packet, EntityPlayer player) 
	{
		DataInputStream inStream = new DataInputStream(new ByteArrayInputStream(packet.data));
		String name;
		int[] data = new int[3];
		try {
			for(int id = 0; id < data.length; id++)
				data[id] = inStream.readInt();
			name = inStream.readUTF();
		} catch (IOException e) {
            e.printStackTrace();
            return;
		}
		IInventory result = null;
		if(player.username.equals(name))
		{
			if(player.openContainer instanceof ContainerPlayer)
			{
				result = ((ContainerPlayer)player.openContainer).craftResult;
			}
			else if(player.openContainer instanceof ContainerWorkbench)
			{
				result = ((ContainerWorkbench)player.openContainer).craftResult;
			}
		}
		if(result != null)
		{
			result.setInventorySlotContents(0, new ItemStack(data[0],data[1],data[2]));
		}
	}
	
	public static void sendData(String name, int... data)
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream(4*data.length+2*name.length());
		DataOutputStream outputStream = new DataOutputStream(bos);
		try 
		{
			for(int d:data)
				outputStream.writeInt(d);
			outputStream.writeUTF(name);
		} 
		catch (IOException ex) 
		{
			ex.printStackTrace();
		}
		Packet250CustomPayload packet = new Packet250CustomPayload();
		packet.channel = Constants.CHANNEL;
		packet.data = bos.toByteArray();
		packet.length = bos.size();
		sendPacket(packet);
	}

	protected static void sendPacket(Packet250CustomPayload packet)
	{
		PacketDispatcher.sendPacketToServer(packet);
	}
}

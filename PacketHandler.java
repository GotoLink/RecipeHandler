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
		int[] data = new int[4];
		try {
			for(int id = 0; id < data.length; id++)
				data[id] = inStream.readInt();
		} catch (IOException e) {
            e.printStackTrace();
            return;
		}
		IInventory result = null;
		if(player.entityId==data[0])
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
			result.setInventorySlotContents(0, new ItemStack(data[1],data[2],data[3]));
			if(player instanceof EntityPlayerMP)
			{
				((EntityPlayerMP)player).playerNetServerHandler.sendPacketToPlayer(packet);
			}
		}
		
	}
	
	public static Packet getPacket(int... data)
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream(4*data.length);
		DataOutputStream outputStream = new DataOutputStream(bos);
		try 
		{
			for(int d:data)
				outputStream.writeInt(d);
		} 
		catch (IOException ex) 
		{
			ex.printStackTrace();
		}
		Packet250CustomPayload packet = new Packet250CustomPayload();
		packet.channel = Constants.CHANNEL;
		packet.data = bos.toByteArray();
		packet.length = bos.size();
		return packet;
	}
}

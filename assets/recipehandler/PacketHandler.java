package assets.recipehandler;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetHandlerPlayServer;

public class PacketHandler{

    @SideOnly(Side.CLIENT)
    private void handle(ChangePacket packet) {
        Entity ent = Minecraft.getMinecraft().theWorld.getEntityByID(packet.entityId);
        if(ent instanceof EntityPlayer){
            IInventory result = null;
            if (((EntityPlayer) ent).openContainer instanceof ContainerPlayer) {
                result = ((ContainerPlayer) ((EntityPlayer) ent).openContainer).craftResult;
            } else if (((EntityPlayer) ent).openContainer instanceof ContainerWorkbench) {
                result = ((ContainerWorkbench) ((EntityPlayer) ent).openContainer).craftResult;
            }
            if (result != null) {
                result.setInventorySlotContents(0, packet.itemstack.copy());
            }
        }
    }

    private FMLProxyPacket handle(Entity ent, ItemStack stack) {
        if(ent instanceof EntityPlayer){
            IInventory result = null;
            if (((EntityPlayer) ent).openContainer instanceof ContainerPlayer) {
                result = ((ContainerPlayer) ((EntityPlayer) ent).openContainer).craftResult;
            } else if (((EntityPlayer) ent).openContainer instanceof ContainerWorkbench) {
                result = ((ContainerWorkbench) ((EntityPlayer) ent).openContainer).craftResult;
            }
            if (result != null) {
                result.setInventorySlotContents(0, stack.copy());
                return new ChangePacket(ent, stack).toProxy(Side.CLIENT);
            }
        }
        return null;
	}

    @SubscribeEvent
    public void onServerPacket(FMLNetworkEvent.ServerCustomPacketEvent event) {
        ChangePacket pkt = new ChangePacket();
        pkt.fromBytes(event.packet.payload());
        FMLProxyPacket packet = handle(((NetHandlerPlayServer) event.handler).playerEntity.worldObj.getEntityByID(pkt.entityId), pkt.itemstack);
        if(packet!=null){
            packet.setDispatcher(event.packet.getDispatcher());
            event.reply = packet;
        }
    }

    @SubscribeEvent
    public void onClientPacket(FMLNetworkEvent.ClientCustomPacketEvent event) {
        ChangePacket pkt = new ChangePacket();
        pkt.fromBytes(event.packet.payload());
        handle(pkt);
    }
}

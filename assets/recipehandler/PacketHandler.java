package assets.recipehandler;

import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class PacketHandler implements IMessageHandler<ChangePacket, ChangePacket>{

    @Override
    public ChangePacket onMessage(ChangePacket message, MessageContext ctx) {
        if(ctx.side.isServer()){
            return handle(ctx.getServerHandler().field_147369_b.worldObj.getEntityByID(message.entityId), message.itemstack);
        }else{
            handle(message);
            return null;
        }
    }

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

    private ChangePacket handle(Entity ent, ItemStack stack) {
        if(ent instanceof EntityPlayer){
            IInventory result = null;
            if (((EntityPlayer) ent).openContainer instanceof ContainerPlayer) {
                result = ((ContainerPlayer) ((EntityPlayer) ent).openContainer).craftResult;
            } else if (((EntityPlayer) ent).openContainer instanceof ContainerWorkbench) {
                result = ((ContainerWorkbench) ((EntityPlayer) ent).openContainer).craftResult;
            }
            if (result != null) {
                result.setInventorySlotContents(0, stack.copy());
                return new ChangePacket(ent, stack);
            }
        }
        return null;
	}

}

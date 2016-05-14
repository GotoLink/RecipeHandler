package assets.recipehandler;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.network.NetHandlerPlayServer;

import java.util.Iterator;

public final class PacketHandler implements RecipeMod.IRegister {
    @Override
    public void register(){
    }

    @Override
    public EntityPlayer getPlayer(){
        return null;
    }

    @SubscribeEvent
    public void onClientPacket(FMLNetworkEvent.ClientCustomPacketEvent event){
        ChangePacket message = new ChangePacket().fromBytes(event.getPacket().payload());
        IInventory result = CraftingHandler.getResultSlot(RecipeMod.registry.getPlayer().openContainer, message.slot+1);
        if (result != null) {
            result.setInventorySlotContents(message.slot, message.itemstack.copy());
        }
    }

    @SubscribeEvent
    public void onServerPacket(FMLNetworkEvent.ServerCustomPacketEvent event){
        ChangePacket reply = new ChangePacket().fromBytes(event.getPacket().payload()).handle(((NetHandlerPlayServer) event.getHandler()).playerEntity);
        if(reply != null) {
            event.setReply(reply.toProxy(Side.CLIENT));
            event.getReply().setDispatcher(event.getPacket().getDispatcher());
        }
    }
}

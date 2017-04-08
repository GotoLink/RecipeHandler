package assets.recipehandler;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Receives packets on both sides and process them
 */
public final class PacketHandler implements RecipeMod.IRegister {
    @Override
    public void register(){
    }

    @Override
    public EntityPlayer getPlayer(){
        return null;
    }

    @Override
    public void scheduleTask(Runnable runner){

    }

    @Override
    public void sendShift(InventoryCrafting crafting, Slot result) {

    }

    /**
     * The event when a server packet is received on client side
     * Push it on client main thread
     * @param event
     */
    @SubscribeEvent
    public void onClientPacket(FMLNetworkEvent.ClientCustomPacketEvent event){
        final ChangePacket message = new ChangePacket().fromBytes(event.getPacket().payload());
        RecipeMod.registry.scheduleTask(message.getRun());
    }

    /**
     * The event when a client packet is received on server side
     * Sends a reply for the client side when appropriate
     * @param event
     */
    @SubscribeEvent
    public void onServerPacket(FMLNetworkEvent.ServerCustomPacketEvent event){
        ChangePacket reply = new ChangePacket().fromBytes(event.getPacket().payload()).handle(((NetHandlerPlayServer) event.getHandler()).player);
        if(reply != null) {
            event.setReply(reply.toProxy(Side.CLIENT));
            event.getReply().setDispatcher(event.getPacket().getDispatcher());
        }
    }
}

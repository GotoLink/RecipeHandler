package assets.recipehandler;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;

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

    @SubscribeEvent
    public void onClientPacket(FMLNetworkEvent.ClientCustomPacketEvent event){
        final ChangePacket message = new ChangePacket().fromBytes(event.getPacket().payload());
        RecipeMod.registry.scheduleTask(message.getRun());
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

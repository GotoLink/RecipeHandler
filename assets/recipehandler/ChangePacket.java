package assets.recipehandler;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;

public class ChangePacket implements IMessage {
    public final static String CHANNEL = "recipemod:key";
    public ItemStack itemstack;
    Entity entity;
    public int entityId;
    public ChangePacket(){}
    public ChangePacket(Entity entity, ItemStack stack) {
        this.entity = entity;
        this.itemstack = stack;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        entityId = buf.readInt();
        itemstack = ByteBufUtils.readItemStack(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(entity.getEntityId());
        ByteBufUtils.writeItemStack(buf, itemstack);
    }

    public FMLProxyPacket toProxy(Side side){
        ByteBuf buf = Unpooled.buffer();
        toBytes(buf);
        FMLProxyPacket proxy = new FMLProxyPacket(buf, CHANNEL);
        proxy.setTarget(side);
        return proxy;
    }
}

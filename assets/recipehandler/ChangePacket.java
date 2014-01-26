package assets.recipehandler;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;

public class ChangePacket implements IMessage {
    public ItemStack itemstack;
    Entity entity;
    public int entityId;
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
        buf.writeInt(entity.func_145782_y());
        ByteBufUtils.writeItemStack(buf, itemstack);
    }
}

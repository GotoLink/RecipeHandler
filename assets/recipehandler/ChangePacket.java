package assets.recipehandler;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;

import java.util.Iterator;

public final class ChangePacket {
    public final static String CHANNEL = "recipemod:key";
    public ItemStack itemstack;
    public int slot;
    public ChangePacket(){}
    public ChangePacket(int slot, ItemStack stack) {
        this.slot = slot;
        this.itemstack = stack;
    }

    public ChangePacket fromBytes(ByteBuf buf) {
        slot = buf.readInt();
        itemstack = ByteBufUtils.readItemStack(buf);
        return this;
    }

    public void toBytes(ByteBuf buf) {
        buf.writeInt(slot);
        ByteBufUtils.writeItemStack(buf, itemstack);
    }

    ChangePacket handle(EntityPlayer player) {
        if(itemstack!=null && slot>=0) {
            InventoryCrafting crafting = CraftingHandler.getCraftingMatrix(player.openContainer);
            if(crafting!=null) {
                Iterator<ItemStack> itr = CraftingHandler.getCraftResult(crafting, player.worldObj).iterator();
                while(itr.hasNext()){
                    if(ItemStack.areItemStacksEqual(itr.next(), itemstack)) {
                        IInventory result = CraftingHandler.getResultSlot(player.openContainer, slot+1);
                        if (result != null) {
                            result.setInventorySlotContents(slot, itemstack.copy());
                            return new ChangePacket(slot, itemstack);
                        }
                        break;
                    }
                }
            }
        }
        return null;
    }

    public FMLProxyPacket toProxy(Side side){
        ByteBuf buf = Unpooled.buffer();
        toBytes(buf);
        FMLProxyPacket proxy = new FMLProxyPacket(buf, CHANNEL);
        proxy.setTarget(side);
        return proxy;
    }
}

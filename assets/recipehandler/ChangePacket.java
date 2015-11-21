package assets.recipehandler;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.Side;
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
    private int index;
    public ChangePacket(){}
    public ChangePacket(int slot, ItemStack stack, int recipeIndex) {
        this.slot = slot;
        this.itemstack = stack;
        this.index = recipeIndex;
    }

    public ChangePacket fromBytes(ByteBuf buf) {
        slot = buf.readInt();
        itemstack = ByteBufUtils.readItemStack(buf);
        index = buf.readInt();
        return this;
    }

    public void toBytes(ByteBuf buf) {
        buf.writeInt(slot);
        ByteBufUtils.writeItemStack(buf, itemstack);
        buf.writeInt(index);
    }

    ChangePacket handle(EntityPlayer player) {
        if(itemstack != null && slot >= 0 && index >= 0) {
            InventoryCrafting crafting = CraftingHandler.getCraftingMatrix(player.openContainer);
            if(crafting!=null) {
                CraftingHandler.setRecipeIndex(index);
                ItemStack itr = CraftingHandler.findMatchingRecipe(crafting, player.worldObj);
                if(ItemStack.areItemStacksEqual(itr, itemstack)) {
                    IInventory result = CraftingHandler.getResultSlot(player.openContainer, slot+1);
                    if (result != null) {
                        result.setInventorySlotContents(slot, itemstack.copy());
                        return new ChangePacket(slot, itemstack, index);
                    }
                }
            }
        }
        return null;
    }

    public FMLProxyPacket toProxy(Side side){
        PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        toBytes(buf);
        FMLProxyPacket proxy = new FMLProxyPacket(buf, CHANNEL);
        proxy.setTarget(side);
        return proxy;
    }
}

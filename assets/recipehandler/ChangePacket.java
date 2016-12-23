package assets.recipehandler;

import com.google.common.util.concurrent.ListenableFuture;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public final class ChangePacket {
    public final static String CHANNEL = "recipemod:key";
    private ItemStack itemstack = ItemStack.EMPTY;
    private int slot;
    private int index;
    public ChangePacket(){}
    public ChangePacket(int slot, @Nonnull ItemStack stack, int recipeIndex) {
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

    @Nullable
    ChangePacket handle(final EntityPlayerMP player) {
        if(!itemstack.isEmpty() && slot >= 0 && index >= 0) {
            ListenableFuture<ChangePacket> future = player.mcServer.callFromMainThread(new Callable<ChangePacket>() {
                 @Override
                 public ChangePacket call() {
                     InventoryCrafting crafting = CraftingHandler.getCraftingMatrix(player.openContainer);
                     if(crafting!=null) {
                         CraftingHandler.setRecipeIndex(index);
                         ItemStack itr = CraftingHandler.findMatchingRecipe(crafting, player.getEntityWorld());
                         if(ItemStack.areItemStacksEqual(itr, itemstack)) {
                             Slot result = CraftingHandler.getResultSlot(player.openContainer, crafting, slot);
                             if (result != null) {
                                 result.putStack(itemstack.copy());
                                 return new ChangePacket(slot, itemstack, index);
                             }
                         }
                     }
                     return null;
                 }
            });
            try {
                return future.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
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

    public Runnable getRun(){
        return new Runnable() {
            @Override
            public void run() {
                Container container = RecipeMod.registry.getPlayer().openContainer;
                InventoryCrafting crafting = CraftingHandler.getCraftingMatrix(container);
                if(crafting!=null) {
                    Slot result = CraftingHandler.getResultSlot(container, crafting, slot);
                    if (result != null) {
                        result.putStack(itemstack);
                    }
                }
            }
        };
    }
}

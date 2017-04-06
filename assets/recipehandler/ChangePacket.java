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

/**
 * Packet to set the craft result of a switch
 * Handled on both sides
 */
public final class ChangePacket {
    public final static String CHANNEL = "recipemod:key";
    private ItemStack itemstack = ItemStack.EMPTY;
    private int slot;
    private int index;
    private boolean shift;

    /**
     * The serializer helper constructor
     */
    public ChangePacket(){}

    /**
     * The constructor for operation
     * @param slot The slot number where the result of craft applies
     * @param stack The result of craft
     * @param recipeIndex The current state of the CraftingHandler
     */
    public ChangePacket(int slot, @Nonnull ItemStack stack, int recipeIndex) {
        this.slot = slot;
        this.itemstack = stack;
        this.index = recipeIndex;
    }

    /**
     * Set the shift flag
     * @return this packet
     */
    public ChangePacket setShift(){
        shift = true;
        return this;
    }

    /**
     * Deserialize from the given buffer
     * @param buf the buffer
     * @return this packet
     */
    public ChangePacket fromBytes(ByteBuf buf) {
        slot = buf.readInt();
        itemstack = ByteBufUtils.readItemStack(buf);
        index = buf.readInt();
        shift = buf.readBoolean();
        return this;
    }

    /**
     * Serialize to the given buffer
     * @param buf the buffer
     */
    public void toBytes(ByteBuf buf) {
        buf.writeInt(slot);
        ByteBufUtils.writeItemStack(buf, itemstack);
        buf.writeInt(index);
        buf.writeBoolean(shift);
    }

    @Nullable
    ChangePacket handle(final EntityPlayerMP player) {
        //Check the packet data is correct
        if(!itemstack.isEmpty() && slot >= 0 && index >= 0) {
            //Send it to the server main thread
            ListenableFuture<ChangePacket> future = player.mcServer.callFromMainThread(new Callable<ChangePacket>() {
                 @Nullable
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
                                 if(shift){
                                     return new ChangePacket(slot, itemstack, index).setShift();
                                 }
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

    /**
     * Create the FML packet to be sent to the given side
     * @param side receiving side
     * @return the FML packet
     */
    public FMLProxyPacket toProxy(Side side){
        PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        toBytes(buf);
        FMLProxyPacket proxy = new FMLProxyPacket(buf, CHANNEL);
        proxy.setTarget(side);
        return proxy;
    }

    /**
     *
     * @return The runnable for client side
     */
    public Runnable getRun(){
        return new Runnable() {
            @Override
            public void run() {
                if(!itemstack.isEmpty()) {
                    Container container = RecipeMod.registry.getPlayer().openContainer;
                    InventoryCrafting crafting = CraftingHandler.getCraftingMatrix(container);
                    if(crafting!=null) {
                        Slot result = CraftingHandler.getResultSlot(container, crafting, slot);
                        if (result != null) {
                            result.putStack(itemstack);
                            if(shift){
                                CraftingHandler.setRecipeIndex(index);
                                RecipeMod.registry.sendShift(crafting, result);
                            }
                        }
                    }
                }
            }
        };
    }
}

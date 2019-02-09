package assets.recipehandler;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import javax.annotation.Nullable;
import java.util.Arrays;

public class Proxy {
    public FMLEventChannel network;
    public CraftingHandler craftingHandler = new CraftingHandler();
    public FurnaceHandler furnaceHandler;

    public void setup(Configuration config) {
        //Setup network for packet handling
        network = NetworkRegistry.INSTANCE.newEventDrivenChannel(ChangePacket.CHANNEL);
        network.register(new PacketHandler());
        if(config.getBoolean("Enable Custom Crafting Detection", Configuration.CATEGORY_GENERAL, true, "Tries to detect other crafting systems, disable for less processing")){
            craftingHandler.enableGuessing(Arrays.asList(config.getStringList("Black List Crafting Container", Configuration.CATEGORY_GENERAL, new String[]{"net.blay09.mods.cookingforblockheads.container.ContainerRecipeBook","morph.avaritia.container.ContainerExtremeCrafting", "slimeknights.tconstruct.tools.common.inventory.ContainerPartBuilder", "slimeknights.tconstruct.tools.common.inventory.ContainerStencilTable", "jds.bibliocraft.containers.ContainerDiscRack"}, "List of containers to ignore for custom crafting detection")));
        }
        furnaceHandler = new FurnaceHandler(config.getBoolean("Enable Furnace Recipes Tracking", Configuration.CATEGORY_GENERAL, false, "Tracks furnace recipes changes from the mod starting point"));//Copy furnace recipes

    }

    public void register(){
        if(Loader.isModLoaded("appliedenergistics2")){
            craftingHandler.addCompatibility(new AppliedEnergistics("ContainerCraftingTerm"));
            craftingHandler.addCompatibility(new AppliedEnergistics("ContainerPatternTerm"));
        }
        if(Loader.isModLoaded("enderiomachines")){
            craftingHandler.addCompatibility(new EnderIO("$Simple"));
            craftingHandler.addCompatibility(new EnderIO("$Normal"));
        }
    }

    public void scheduleTask(ChangePacket runner){

    }

    public void sendShift(InventoryCrafting crafting, Slot result) {

    }

    @Nullable
    public ChangePacket getAnswer(EntityPlayerMP player, ChangePacket packet){
        InventoryCrafting crafting = craftingHandler.getCraftingMatrix(player.openContainer);
        if(crafting != null) {
            craftingHandler.setRecipeIndex(packet.index());
            IRecipe recipe = craftingHandler.findMatchingRecipe(crafting, player.getEntityWorld());
            if(recipe != null) {
                ItemStack itr = recipe.getCraftingResult(crafting);
                if (ItemStack.areItemsEqual(itr, packet.stack()) && itr.getCount() == packet.stack().getCount() && compareNBT(itr, packet.stack())) {
                    Slot result = craftingHandler.getResultSlot(player.openContainer, crafting, packet.slot());
                    if (result != null && craftingHandler.setCraftUsed(player, recipe)) {
                        result.putStack(packet.stack().copy());
                        return new ChangePacket(packet);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Use "Network-Aware" NBT Stack comparison
     * @param stackA first stack
     * @param stackB second stack
     * @return true if the nbt are close enough
     */
    private boolean compareNBT(ItemStack stackA, ItemStack stackB){
        NBTTagCompound shareTagA = stackA.getItem().getNBTShareTag(stackA);
        NBTTagCompound shareTagB = stackB.getItem().getNBTShareTag(stackB);
        if (shareTagA == null || !hasData(shareTagA)) {
            return shareTagB == null || !hasData(shareTagB);
        }else
            return shareTagA.equals(shareTagB);
    }

    /**
     * Check first level of NBT for data
     * @param tag the nbt to check
     * @return true if at least one key of the first level isn't empty
     */
    private boolean hasData(NBTTagCompound tag){
        if (!tag.isEmpty()) {
            for (String key : tag.getKeySet()) {
                NBTBase nbt = tag.getTag(key);
                if (nbt != null && !nbt.isEmpty())
                    return true;
            }
        }
        return false;
    }
}

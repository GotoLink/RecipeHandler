package assets.recipehandler;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.client.util.RecipeItemHelper;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.crafting.IShapedRecipe;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.*;

public class RecipeCommand extends CommandBase {
    private static final RecipeItemHelper HELPER = new RecipeItemHelper();

    @Override
    public String getName() {
        return "recipes";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.recipes.usage";
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if("get".equals(args[0])) {
            if(args.length == 2){
                return getListOfStringsMatchingLastWord(args, "by_name", "by_item", "by_held");
            }
            if(args.length == 3) {
                if ("by_name".equals(args[1]))
                    return getListOfStringsMatchingLastWord(args, ForgeRegistries.RECIPES.getKeys());
                if ("by_item".equals(args[1]))
                    return getListOfStringsMatchingLastWord(args, ForgeRegistries.ITEMS.getKeys());
            }
        }
        if("check".equals(args[0]))
            return getListOfStringsMatchingLastWord(args, "result", "ingredients", "space");
        if("container".equals(args[0]))
            return getListOfStringsMatchingLastWord(args, "true", "false");
        return args.length == 1 ? getListOfStringsMatchingLastWord(args, "check", "conflict", "get", "dump", "container", "furnace") : super.getTabCompletions(server, sender, args, targetPos);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if(args.length < 1)
            throw new WrongUsageException(getUsage(sender));
        if("conflict".equals(args[0])){//Return all conflicting recipes based on their ingredients requirements
            Set<ResourceLocation> done = Sets.newHashSetWithExpectedSize(1000);
            IntArrayList leftA = new IntArrayList();
            IntArrayList leftB = new IntArrayList();
            for(IRecipe recipeA : ForgeRegistries.RECIPES){
                for(IRecipe recipeB : ForgeRegistries.RECIPES){
                    if(!areSameGroup(recipeA, recipeB) && !done.contains(recipeB.getRegistryName()) && !ItemStack.areItemStacksEqual(recipeA.getRecipeOutput(), recipeB.getRecipeOutput())) {
                        HELPER.clear();
                        recipeA.getIngredients().forEach(RecipeCommand::accountStacks);
                        leftB.clear();
                        if(HELPER.canCraft(recipeB, leftB)){
                            HELPER.clear();
                            recipeB.getIngredients().forEach(RecipeCommand::accountStacks);
                            leftA.clear();
                            if(HELPER.canCraft(recipeA, leftA) && leftA.equals(leftB) && canFit(recipeA, recipeB)) {//Both recipes use same ingredients
                                sender.sendMessage(new TextComponentTranslation("commands.recipes.conflict", recipeA.getRegistryName(), recipeB.getRegistryName()));
                            }
                        }
                    }
                }
                done.add(recipeA.getRegistryName());
            }
        }else if("dump".equals(args[0])){//Display the full list of recipes
            List<ITextComponent> texts = Lists.newArrayListWithCapacity(1000);
            for (ResourceLocation key : ForgeRegistries.RECIPES.getKeys()) {
                texts.add(new TextComponentString(key.toString() + " | "));
            }
            sendMessages(sender, texts, 5);
            texts.clear();
        }else if("get".equals(args[0]) && args.length >= 2){//Display a recipe...
            ItemStack stack = ItemStack.EMPTY;
            if("by_held".equals(args[1]) && sender.getCommandSenderEntity() != null){//...with similar output as the item being held
                for(ItemStack equiped : sender.getCommandSenderEntity().getHeldEquipment()){
                    if(!equiped.isEmpty()){
                        stack = equiped;
                        break;
                    }
                }
            }
            if("by_item".equals(args[1]) && args.length == 3) {//...with similar output as the given item name
                stack = new ItemStack(getItemByText(sender, args[2]));
            }
            if(!stack.isEmpty()){
                for (IRecipe recipeReg : ForgeRegistries.RECIPES) {
                    if(ItemStack.areItemsEqualIgnoreDurability(recipeReg.getRecipeOutput(), stack)){
                        displayRecipe(sender, recipeReg);
                    }
                }
            }
            if("by_name".equals(args[1]) && args.length == 3){//...with given registry name
                IRecipe recipe = ForgeRegistries.RECIPES.getValue(new ResourceLocation(args[2]));
                if(recipe != null)
                    displayRecipe(sender, recipe);
                else
                    throw new CommandException("commands.recipes.notFound", args[2]);
            }
        }else if("check".equals(args[0])){//Return all recipes that can be displayed in the recipe book...
            List<ITextComponent> messages = Lists.newArrayListWithCapacity(20);
            if("result".equals(args[1])) {//...but no output
                for (IRecipe recipe : ForgeRegistries.RECIPES) {
                    if (!recipe.isDynamic() && recipe.getRecipeOutput().isEmpty()) {
                        messages.add(new TextComponentTranslation("commands.recipes.empty_output", recipe.getRegistryName()));
                    }
                }
            }else if("ingredients".equals(args[1])) {//...but no ingredients
                for (IRecipe recipe : ForgeRegistries.RECIPES) {
                    if (!recipe.isDynamic()) {
                        if (emptyIngredients(recipe.getIngredients())) {
                            messages.add(new TextComponentTranslation("commands.recipes.empty_ingredients", recipe.getRegistryName()));
                        }
                    }
                }
            }else if("space".equals(args[1]) && args.length == 4){//...but can't fit, or match empty space of given size
                int width = parseInt(args[2], 1);
                int height = parseInt(args[3], 1);
                InventoryCrafting inv = new InventoryCrafting(new Container(){
                    @Override
                    public boolean canInteractWith(EntityPlayer playerIn) {
                        return true;
                    }
                }, width, height);
                for (IRecipe recipe : ForgeRegistries.RECIPES) {
                    if (!recipe.isDynamic()){
                        if(!recipe.canFit(width, height))
                            messages.add(new TextComponentTranslation("commands.recipes.bigger_space", recipe.getRegistryName(), width, height));
                        if(recipe.matches(inv, sender.getEntityWorld()))
                            messages.add(new TextComponentTranslation("commands.recipes.match_empty", recipe.getRegistryName(), width, height));
                    }
                }
            }else{
                throw new WrongUsageException(getUsage(sender));
            }
            sendMessages(sender, messages, 1);
            messages.clear();
        }else if("container".equals(args[0]) && args.length == 2){//Return all listed containers
            boolean flag = parseBoolean(args[1]);
            Set<String> cont = CraftingHandler.getContainers(flag);
            if(cont != null){
                sender.sendMessage(new TextComponentTranslation("commands.recipes.container." + flag, cont));
            }
        }else if("furnace".equals(args[0])){
            sender.sendMessage(new TextComponentTranslation("commands.recipes.furnace", FurnaceHandler.compare()));
        }
    }

    /**
     * Check whether two recipes could conflict
     * @param recipeA
     * @param recipeB
     * @return true if the two recipes are too similar to conflict
     */
    private static boolean areSameGroup(IRecipe recipeA, IRecipe recipeB){
        return recipeA.getRegistryName().getResourceDomain().equals(recipeB.getRegistryName().getResourceDomain()) || emptyIngredients(recipeA.getIngredients()) || emptyIngredients(recipeB.getIngredients());
    }

    /**
     * Check whether all ingredients in the collection are empty
     * @param ingredients the collection to check
     * @return true if the ingredients collection is empty or all of its elements are empty
     */
    private static boolean emptyIngredients(Collection<Ingredient> ingredients){
        return ingredients.isEmpty() || ingredients.stream().allMatch(ingredient -> ingredient == Ingredient.EMPTY || ingredient.apply(ItemStack.EMPTY) || Arrays.stream(ingredient.getMatchingStacks()).allMatch(ItemStack::isEmpty));
    }

    /**
     * Display the recipe to the sender using translated chat messages
     * @param sender the sender asking for the recipe
     * @param recipe the recipe to display
     */
    private static void displayRecipe(ICommandSender sender, IRecipe recipe){
        ITextComponent message = new TextComponentTranslation("commands.recipes.found", recipe.getRegistryName());
        sender.sendMessage(message);
        if (!recipe.getRecipeOutput().isEmpty())
            sender.sendMessage(recipe.getRecipeOutput().getTextComponent());
        else
            sender.sendMessage(new TextComponentTranslation("commands.recipes.empty_output", recipe.getRegistryName()));
        int max = 0;
        for (Ingredient ingredient : recipe.getIngredients()) {
            max = max < ingredient.getMatchingStacks().length ? ingredient.getMatchingStacks().length : max;
        }
        List<ITextComponent> texts = Lists.newArrayListWithCapacity(max*recipe.getIngredients().size());
        for (int index = 0; index < max; index++) {
            message = new TextComponentTranslation(index > 0 ? "commands.recipes.orMadeFrom" : "commands.recipes.madeFrom");
            sender.sendMessage(message);
            for (Ingredient ingredient : recipe.getIngredients()) {
                int length = ingredient.getMatchingStacks().length;
                texts.add(length > 0 ? ingredient.getMatchingStacks()[index < length ? index : 0].getTextComponent() : ItemStack.EMPTY.getTextComponent());
            }
            sendMessages(sender, texts, recipe instanceof IShapedRecipe ? ((IShapedRecipe) recipe).getRecipeWidth() : texts.size() > 1 ? texts.size() - 1 : 1);
            texts.clear();
        }
        if (max == 0)
            sender.sendMessage(new TextComponentTranslation("commands.recipes.empty_ingredients", recipe.getRegistryName()));
    }

    /**
     * "Pretty print" the message list to the command user
     * @param sender the command user
     * @param texts the message list
     * @param width the max amount of messages of a single message line
     */
    private static void sendMessages(ICommandSender sender, Collection<ITextComponent> texts, int width){
        ITextComponent message;
        for (int divider = width; divider > 0; divider--) {
            if (texts.size() % divider == 0 || width != texts.size() - 1) {
                int i = 0;
                Iterator<ITextComponent> itr = texts.iterator();
                while (i < texts.size()) {
                    message = itr.next();
                    for (int j = i + 1; j < i + divider && j < texts.size(); j++)
                        message.appendSibling(itr.next());
                    sender.sendMessage(message);
                    i = i + divider;
                }
                break;
            }
        }
    }

    /**
     * Check if both recipes have same fitting space
     * @param recipeA the first recipe
     * @param recipeB the second recipe
     * @return false if both recipes can't fit the same space
     */
    private static boolean canFit(IRecipe recipeA, IRecipe recipeB){
        if(recipeA instanceof IShapedRecipe){
            if(recipeB instanceof IShapedRecipe){
                return ((IShapedRecipe) recipeA).getRecipeWidth() == ((IShapedRecipe) recipeB).getRecipeWidth() && ((IShapedRecipe) recipeA).getRecipeHeight() == ((IShapedRecipe) recipeB).getRecipeHeight();
            }
            if(!recipeB.canFit(((IShapedRecipe) recipeA).getRecipeWidth(), ((IShapedRecipe) recipeA).getRecipeHeight())){
                return false;
            }
        }
        if(recipeB instanceof IShapedRecipe && !recipeA.canFit(((IShapedRecipe) recipeB).getRecipeWidth(), ((IShapedRecipe) recipeB).getRecipeHeight())){
            return false;
        }
        for(int i = 1; i < 10; i++){
            for(int j = 1; j < 10; j++){
                if(recipeA.canFit(i, j) != recipeB.canFit(i, j))
                    return false;
            }
        }
        return true;
    }

    /**
     * Fill helper with stacks from given ingredient
     * @param ingredient the ingredient from which matching stacks are taken
     */
    private static void accountStacks(Ingredient ingredient){
        for(ItemStack stack : ingredient.getMatchingStacks()){
            HELPER.accountStack(stack);
        }
    }
}

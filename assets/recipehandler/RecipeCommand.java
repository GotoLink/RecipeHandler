package assets.recipehandler;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import joptsimple.internal.Strings;
import net.minecraft.client.util.RecipeItemHelper;
import net.minecraft.command.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.common.crafting.IShapedRecipe;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.*;

public class RecipeCommand extends CommandBase {
    private static final RecipeItemHelper HELPER = new RecipeItemHelper();
    private Thread conclictCheck;
    private List<ITextComponent> dump = Lists.newArrayListWithCapacity(1000);
    private static int PAGE_SIZE = 30;
    private final String NAME;
    private final boolean CLIENT;

    public RecipeCommand(String title, boolean isClient){
        this.NAME = title;
        this.CLIENT = isClient;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender){
        return CLIENT || super.checkPermission(server, sender);
    }

    @Override
    public int getRequiredPermissionLevel()
    {
        return 2;
    }

    @Override
    public List<String> getAliases(){
        return Lists.newArrayList("recipes");
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
        if("dump".equals(args[0])){//Display the full list of recipes
            if(dump.isEmpty()) {
                ITextComponent separator = new TextComponentString(" | ").setStyle(new Style().setUnderlined(false).setHoverEvent(null).setClickEvent(null));
                for (ResourceLocation key : ForgeRegistries.RECIPES.getKeys()) {
                    dump.add(textHoverAndDisplay(key).appendSibling(separator));
                }
            }
            int page = 1;
            int max = 1 + dump.size()/PAGE_SIZE;
            if(args.length > 1){
                page = parseInt(args[1], 1, max);
            }
            sendMessages(sender, dump.subList((page-1)*PAGE_SIZE, Math.min(dump.size(), page*PAGE_SIZE)), PAGE_SIZE);
            sender.setCommandStat(CommandResultStats.Type.QUERY_RESULT, dump.size());
            ITextComponent next = new TextComponentTranslation("commands.recipes.dump.page", page, max);
            int nextPage = page == max ? 1 : page + 1;
            next.getStyle().setColor(TextFormatting.DARK_GREEN)
                    .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/recipes dump " + nextPage))
                    .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentTranslation("commands.recipes.dump.next", nextPage)));
            sender.sendMessage(next);
        }else {
            dump.clear();
            if ("conflict".equals(args[0])) {//Return all conflicting recipes based on their ingredients requirements
                if (conclictCheck != null && conclictCheck.isAlive())
                    return;
                final Collection<IRecipe> recipes = ForgeRegistries.RECIPES.getValuesCollection();
                conclictCheck = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Set<ResourceLocation> done = Sets.newHashSetWithExpectedSize(recipes.size());
                        IntArrayList leftA = new IntArrayList();
                        IntArrayList leftB = new IntArrayList();
                        for (IRecipe recipeA : recipes) {
                            for (IRecipe recipeB : recipes) {
                                if (!done.contains(recipeB.getRegistryName()) && !areSameGroup(recipeA, recipeB) && !ItemStack.areItemStacksEqual(recipeA.getRecipeOutput(), recipeB.getRecipeOutput()) && canFit(recipeA, recipeB)) {
                                    HELPER.clear();
                                    recipeA.getIngredients().forEach(RecipeCommand::accountStacks);
                                    leftB.clear();
                                    if (HELPER.canCraft(recipeB, leftB)) {
                                        HELPER.clear();
                                        recipeB.getIngredients().forEach(RecipeCommand::accountStacks);
                                        leftA.clear();
                                        if (HELPER.canCraft(recipeA, leftA) && leftA.equals(leftB)) {//Both recipes use same ingredients
                                            ITextComponent message = new TextComponentTranslation("commands.recipes.conflict", textHoverAndDisplay(recipeA.getRegistryName()), textHoverAndDisplay(recipeB.getRegistryName()));
                                            server.addScheduledTask(
                                                    new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            sender.sendMessage(message);
                                                        }
                                                    });

                                        }
                                    }
                                }
                            }
                            done.add(recipeA.getRegistryName());
                        }
                    }
                }, "Recipes Conflict");
                conclictCheck.setDaemon(true);
                conclictCheck.start();
            } else if ("get".equals(args[0])) {//Display a recipe...
                if (args.length < 2)
                    throw new WrongUsageException("commands.recipes.get.usage");
                ItemStack stack = ItemStack.EMPTY;
                if ("by_held".equals(args[1]) && sender.getCommandSenderEntity() != null) {//...with similar output as the item being held
                    for (ItemStack equiped : sender.getCommandSenderEntity().getHeldEquipment()) {
                        if (!equiped.isEmpty()) {
                            stack = equiped;
                            break;
                        }
                    }
                } else if ("by_item".equals(args[1]) && args.length == 3) {//...with similar output as the given item name
                    stack = new ItemStack(getItemByText(sender, args[2]));
                }
                if (!stack.isEmpty()) {
                    List<ITextComponent> messages = Lists.newArrayListWithCapacity(20);
                    for (IRecipe recipeReg : ForgeRegistries.RECIPES) {
                        if (ItemStack.areItemsEqualIgnoreDurability(recipeReg.getRecipeOutput(), stack)) {
                            messages.add(textHoverAndDisplay(recipeReg.getRegistryName()));
                        }
                    }
                    messages.add(0, new TextComponentTranslation("commands.recipes.get.item." + (messages.size() > 0), messages.size(), stack.getTextComponent()));
                    sendMessages(sender, messages, 1);
                } else if ("by_name".equals(args[1]) && args.length >= 3) {//...with given registry name
                    String[] texts = new String[args.length - 2];
                    System.arraycopy(args, 2, texts, 0, texts.length);
                    String arg = Strings.join(texts, " ");
                    IRecipe recipe = ForgeRegistries.RECIPES.getValue(new ResourceLocation(arg));
                    if (recipe != null)//...exact match
                        displayRecipe(sender, recipe);
                    else {//...or the closest found
                        int minlv = -1;
                        ResourceLocation suggest = null;
                        for (ResourceLocation key : ForgeRegistries.RECIPES.getKeys()) {
                            String test = pathName(key);
                            int lv = StringUtils.getLevenshteinDistance(arg, test, Math.abs(test.length() - arg.length()));
                            if (lv != -1) {
                                if (minlv == -1 || lv < minlv) {
                                    minlv = lv;
                                    suggest = key;
                                }
                            }
                        }
                        if (suggest != null)
                            sender.sendMessage(new TextComponentTranslation("commands.recipes.suggest", arg, textHoverAndDisplay(suggest)));
                        else
                            throw new CommandException("commands.recipes.notFound", arg);
                    }
                }
            } else if ("check".equals(args[0])) {//Return all recipes that can be displayed in the recipe book...
                List<ITextComponent> messages = Lists.newArrayListWithCapacity(20);
                if (args.length == 2) {
                    if ("result".equals(args[1])) {//...but no output
                        for (IRecipe recipe : ForgeRegistries.RECIPES) {
                            if (!recipe.isDynamic() && recipe.getRecipeOutput().isEmpty()) {
                                messages.add(translateAndDisplay("commands.recipes.empty_output", recipe.getRegistryName()));
                            }
                        }
                    } else if ("ingredients".equals(args[1])) {//...but no ingredients
                        for (IRecipe recipe : ForgeRegistries.RECIPES) {
                            if (!recipe.isDynamic() && emptyIngredients(recipe.getIngredients())) {
                                messages.add(translateAndDisplay("commands.recipes.empty_ingredients", recipe.getRegistryName()));
                            }
                        }
                    }
                } else if (args.length == 4 && "space".equals(args[1])) {//...but can't fit, or match empty space of given size
                    int width = parseInt(args[2], 1);
                    int height = parseInt(args[3], 1);
                    InventoryCrafting inv = new InventoryCrafting(new Container() {
                        @Override
                        public boolean canInteractWith(EntityPlayer playerIn) {
                            return true;
                        }
                    }, width, height);
                    messages.add(new TextComponentTranslation("commands.recipes.check.space", width, height));
                    for (IRecipe recipe : ForgeRegistries.RECIPES) {
                        if (!recipe.isDynamic()) {
                            if (!recipe.canFit(width, height))
                                messages.add(translateAndDisplay("commands.recipes.bigger_space", recipe.getRegistryName()));
                            try {
                                if (recipe.matches(inv, sender.getEntityWorld()))
                                    messages.add(translateAndDisplay("commands.recipes.match_empty", recipe.getRegistryName()));
                            } catch (Exception error) {
                                messages.add(translateAndDisplay("commands.recipes.match_error", recipe.getRegistryName()));
                            }
                        }
                    }
                } else {
                    throw new WrongUsageException("commands.recipes.check.usage");
                }
                sendMessages(sender, messages, 1);
                messages.clear();
            } else if ("container".equals(args[0])) {//Return all listed containers
                try {
                    boolean flag = parseBoolean(args.length == 2 ? args[1] : "");
                    Set<String> cont = RecipeMod.registry.craftingHandler.getContainers(flag);
                    if (cont != null) {
                        sender.sendMessage(new TextComponentTranslation("commands.recipes.container." + flag, cont.size()));
                        sender.setCommandStat(CommandResultStats.Type.QUERY_RESULT, cont.size());
                        for(String key : cont) {
                            String temp = pathName(key.replace('.', '/'));
                            sender.sendMessage(new TextComponentString(temp).setStyle(hover(getModAndPath(key.substring(0, key.length() - temp.length() - 1)))));
                        }
                    }
                } catch (CommandException flagError) {
                    throw new WrongUsageException("commands.recipes.container.usage");
                }
            } else if ("furnace".equals(args[0])) {
                int compare = RecipeMod.registry.furnaceHandler.compare();
                sender.sendMessage(new TextComponentTranslation("commands.recipes.furnace", compare));
                sender.setCommandStat(CommandResultStats.Type.QUERY_RESULT, compare);
            }
        }
    }

    /**
     * Try to match given class path with a mod class path
     * @param temp a class path
     * @return the component to send
     */
    private static ITextComponent getModAndPath(String temp){
        TextComponentString text = new TextComponentString(temp.replace('.', '/'));
        if(text.getText().startsWith("net/minecraft/"))
            return text.appendText("\n").appendText("Minecraft");
        for(ModContainer mod : Loader.instance().getActiveModList()){
            for(String pack : mod.getOwnedPackages()){
                if(pack.contains(temp)){
                    return text.appendText("\n").appendText(mod.getName());
                }
            }
        }
        return text;
    }

    /**
     * Create simple text with clickable recipe display command and hover text
     * @param recipeKey to display
     * @return the component to send
     */
    private static ITextComponent textHoverAndDisplay(ResourceLocation recipeKey){
        return new TextComponentString(pathName(recipeKey)).setStyle(display(recipeKey).setHoverEvent(hover(recipeKey).getHoverEvent()));
    }

    /**
     * Create localizable text with clickable recipe display command and hover text
     * @param text to translate
     * @param recipeKey to display
     * @return the component to send
     */
    private static ITextComponent translateAndDisplay(String text, ResourceLocation recipeKey){
        return new TextComponentTranslation(text, textHoverAndDisplay(recipeKey));
    }

    /**
     * Create clickable recipe display style
     * @param recipeKey the recipe
     * @return the style to apply
     */
    private static Style display(ResourceLocation recipeKey){
        return new Style().setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/recipes get by_name " + recipeKey)).setUnderlined(true);
    }

    /**
     * Create localizable text with hover text
     * @param text to translate
     * @param recipeKey to hover
     * @return the component to send
     */
    private static ITextComponent translateAndHover(String text, ResourceLocation recipeKey){
        return new TextComponentTranslation(text, new TextComponentString(pathName(recipeKey)).setStyle(hover(recipeKey)));
    }

    /**
     * Create hover text style
     * @param recipeKey the resource which namespace is used as text
     * @return the style to apply
     */
    private static Style hover(ResourceLocation recipeKey){
        String simple = pathName(recipeKey);
        int diff = recipeKey.getPath().length() - simple.length();
        ModContainer mod = Loader.instance().getIndexedModList().get(recipeKey.getNamespace());
        return hover(new TextComponentString((diff > 0 ? (recipeKey.getPath().substring(0, diff) + "\n") : "") + (mod != null ? mod.getName() : recipeKey.getNamespace())));
    }

    /**
     * Create hover text style
     * @param hoverText the componenr which is used as text
     * @return the style to apply
     */
    private static Style hover(ITextComponent hoverText){
        return new Style().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText)).setColor(TextFormatting.BLUE);
    }

    /**
     * Extract simple name out of resource path
     * @param  recipeKey the resource
     * @return simple name
     */
    private static String pathName(ResourceLocation recipeKey){
        return pathName(recipeKey.getPath());
    }

    /**
     * Extract simple name out of resource path
     * @param  recipeKey the resource path
     * @return simple name
     */
    private static String pathName(String recipeKey){
        int pos = recipeKey.lastIndexOf("/") + 1;
        return pos > 0 ? recipeKey.substring(pos) : recipeKey;
    }

    /**
     * Check whether two recipes could conflict
     * @param recipeA first recipe
     * @param recipeB second recipe
     * @return true if the two recipes are too similar to conflict
     */
    private static boolean areSameGroup(IRecipe recipeA, IRecipe recipeB){
        return recipeA.getRegistryName().getNamespace().equals(recipeB.getRegistryName().getNamespace()) || emptyIngredients(recipeA.getIngredients()) || emptyIngredients(recipeB.getIngredients());
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
        sender.sendMessage(translateAndHover(recipe instanceof IShapedRecipe ? "commands.recipes.found_shaped" : "commands.recipes.found_shapeless", recipe.getRegistryName()));
        if (!recipe.getRecipeOutput().isEmpty())
            sender.sendMessage(recipe.getRecipeOutput().getTextComponent());
        else
            sender.sendMessage(translateAndHover("commands.recipes.empty_output", recipe.getRegistryName()));
        int max = 0;
        for (Ingredient ingredient : recipe.getIngredients()) {
            max = max < ingredient.getMatchingStacks().length ? ingredient.getMatchingStacks().length : max;
        }
        if (max == 0) {
            sender.sendMessage(translateAndHover("commands.recipes.empty_ingredients", recipe.getRegistryName()));
            return;
        }
        List<ITextComponent> texts = Lists.newArrayListWithCapacity(recipe.getIngredients().size());
        ITextComponent AIR = ItemStack.EMPTY.getTextComponent();
        ITextComponent MADE_FROM = new TextComponentTranslation("commands.recipes.orMadeFrom").setStyle(new Style().setItalic(true));
        for (int index = 0; index < max; index++) {
            sender.sendMessage(index > 0 ? MADE_FROM : new TextComponentTranslation( "commands.recipes.madeFrom").setStyle(MADE_FROM.getStyle()));
            for (Ingredient ingredient : recipe.getIngredients()) {
                int length = ingredient.getMatchingStacks().length;
                texts.add(length > 0 ? ingredient.getMatchingStacks()[index < length ? index : 0].getTextComponent() : AIR);
            }
            sendMessages(sender, texts, recipe instanceof IShapedRecipe ? ((IShapedRecipe) recipe).getRecipeWidth() : (int) Math.ceil(Math.sqrt((double)texts.size())));
            texts.clear();
        }
    }

    /**
     * "Pretty print" the message list to the command user
     * @param sender the command user
     * @param texts the message list
     * @param width the max amount of messages of a single message line
     */
    private static void sendMessages(ICommandSender sender, List<ITextComponent> texts, int width){
        ITextComponent message;
        Iterator<ITextComponent> itr;
        for (int divider = 0; divider < texts.size();) {
            itr = texts.subList(divider, Math.min(texts.size(), divider + width)).iterator();
            message = itr.next();
            while (itr.hasNext())
                message.appendSibling(itr.next());
            sender.sendMessage(message);
            divider += width;
        }
    }

    /**
     * Check if both recipes have same fitting space
     * @param recipeA the first recipe
     * @param recipeB the second recipe
     * @return false if both recipes can't fit the same space
     */
    private static boolean canFit(IRecipe recipeA, IRecipe recipeB){
        int maxW = 10;
        int maxH = 10;
        if(recipeA instanceof IShapedRecipe){
            maxW = ((IShapedRecipe) recipeA).getRecipeWidth();
            maxH = ((IShapedRecipe) recipeA).getRecipeHeight();
            if(recipeB instanceof IShapedRecipe){//Simple compare
                return maxW == ((IShapedRecipe) recipeB).getRecipeWidth() && maxH == ((IShapedRecipe) recipeB).getRecipeHeight();
            }
            return recipeB.canFit(maxW, maxH) && !recipeB.canFit(maxW - 1, maxH) && !recipeB.canFit(maxW, maxH - 1);//Not any smaller
        }
        else if(recipeB instanceof IShapedRecipe){
            maxW = ((IShapedRecipe) recipeB).getRecipeWidth();
            maxH = ((IShapedRecipe) recipeB).getRecipeHeight();
            return recipeA.canFit(maxW, maxH) && !recipeA.canFit(maxW - 1, maxH) && !recipeA.canFit(maxW, maxH - 1);//Not any smaller
        }
        //Both recipes are shapeless
        for(int i = 1; i < maxW; i++){
            for(int j = 1; j < maxH; j++){
                if(recipeA.canFit(i, j) != recipeB.canFit(i, j))
                    return false;
                if(recipeA.canFit(i, j))
                    return true;
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

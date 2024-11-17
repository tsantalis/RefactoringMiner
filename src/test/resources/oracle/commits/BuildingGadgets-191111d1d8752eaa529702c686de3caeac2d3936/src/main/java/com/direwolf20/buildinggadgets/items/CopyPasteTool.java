package com.direwolf20.buildinggadgets.items;

import com.direwolf20.buildinggadgets.BuildingGadgets;
import com.direwolf20.buildinggadgets.Config;
import com.direwolf20.buildinggadgets.ModItems;
import com.direwolf20.buildinggadgets.blocks.ConstructionBlock;
import com.direwolf20.buildinggadgets.blocks.ConstructionBlockTileEntity;
import com.direwolf20.buildinggadgets.entities.BlockBuildEntity;
import com.direwolf20.buildinggadgets.gui.GuiProxy;
import com.direwolf20.buildinggadgets.network.PacketBlockMap;
import com.direwolf20.buildinggadgets.network.PacketHandler;
import com.direwolf20.buildinggadgets.tools.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import java.util.*;

import static com.direwolf20.buildinggadgets.tools.GadgetUtils.useEnergy;
import static com.direwolf20.buildinggadgets.tools.GadgetUtils.withSuffix;

public class CopyPasteTool extends GenericGadget {

    public enum toolModes {
        Copy, Paste;
        private static toolModes[] vals = values();

        public toolModes next() {
            return vals[(this.ordinal() + 1) % vals.length];
        }
    }

    public CopyPasteTool() {
        setRegistryName("copypastetool");        // The unique name (within your mod) that identifies this item
        setUnlocalizedName(BuildingGadgets.MODID + ".copypastetool");     // Used for localization (en_US.lang)
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.TOOLS);
    }

    public static void setAnchor(ItemStack stack, BlockPos anchorPos) {
        GadgetUtils.writePOSToNBT(stack, anchorPos, "anchor");
    }

    public static BlockPos getAnchor(ItemStack stack) {
        return GadgetUtils.getPOSFromNBT(stack, "anchor");
    }

    public static String getUUID(ItemStack stack) {
        NBTTagCompound tagCompound = stack.getTagCompound();
        if (tagCompound == null) {
            return null;
        }
        String uuid = tagCompound.getString("UUID");
        if (tagCompound == null || uuid == "") {
            if (getStartPos(stack) == null && getEndPos(stack) == null) {
                return null;
            }
            UUID uid = UUID.randomUUID();
            tagCompound.setString("UUID", uid.toString());
            stack.setTagCompound(tagCompound);
            uuid = uid.toString();
        }
        return uuid;
    }

    public static String getOwner(ItemStack stack) {
        return stack.getTagCompound().getString("owner");
    }

    public static void setOwner(ItemStack stack, String owner) {
        NBTTagCompound tagCompound = stack.getTagCompound();
        tagCompound.setString("owner", owner);
        stack.setTagCompound(tagCompound);
    }

    public static Integer getCopyCounter(ItemStack stack) {
        return stack.getTagCompound().getInteger("copycounter");
    }

    public static void setCopyCounter(ItemStack stack, int counter) {
        NBTTagCompound tagCompound = stack.getTagCompound();
        tagCompound.setInteger("copycounter", counter);
        stack.setTagCompound(tagCompound);
    }

    public static void incrementCopyCounter(ItemStack stack) {
        NBTTagCompound tagCompound = stack.getTagCompound();
        tagCompound.setInteger("copycounter", tagCompound.getInteger("copycounter") + 1);
        stack.setTagCompound(tagCompound);
    }

    public static void setLastBuild(ItemStack stack, BlockPos anchorPos, Integer dim) {
        GadgetUtils.writePOSToNBT(stack, anchorPos, "lastBuild", dim);
    }

    public static BlockPos getLastBuild(ItemStack stack) {
        return GadgetUtils.getPOSFromNBT(stack, "lastBuild");
    }

    public static Integer getLastBuildDim(ItemStack stack) {
        return GadgetUtils.getDIMFromNBT(stack, "lastBuild");
    }

    public static void setItemCountMap(ItemStack stack, Map<UniqueItem, Integer> tagMap) {
        NBTTagCompound tagCompound = stack.getTagCompound();
        if (tagCompound == null) {
            tagCompound = new NBTTagCompound();
        }
        NBTTagList tagList = GadgetUtils.itemCountToNBT(tagMap);
        tagCompound.setTag("itemcountmap", tagList);
        stack.setTagCompound(tagCompound);
    }

    public static Map<UniqueItem, Integer> getItemCountMap(ItemStack stack) {
        NBTTagCompound tagCompound = stack.getTagCompound();
        if (tagCompound == null) {
            return null;
        }

        Map<UniqueItem, Integer> tagMap = GadgetUtils.nbtToItemCount((NBTTagList) tagCompound.getTag("itemcountmap"));
        return tagMap;
    }

    public static void setStartPos(ItemStack stack, BlockPos startPos) {
        GadgetUtils.writePOSToNBT(stack, startPos, "startPos");
    }

    public static BlockPos getStartPos(ItemStack stack) {
        return GadgetUtils.getPOSFromNBT(stack, "startPos");
    }

    public static void setEndPos(ItemStack stack, BlockPos startPos) {
        GadgetUtils.writePOSToNBT(stack, startPos, "endPos");
    }

    public static BlockPos getEndPos(ItemStack stack) {
        return GadgetUtils.getPOSFromNBT(stack, "endPos");
    }

    public static ArrayList<BlockMap> getBlockMapList(NBTTagCompound tagCompound) {
        ArrayList<BlockMap> blockMap = new ArrayList<BlockMap>();
        BlockPos startBlock = GadgetUtils.getPOSFromNBT(tagCompound, "startPos");
        if (tagCompound == null) {
            tagCompound = new NBTTagCompound();
        }
        NBTTagList MapIntStateTag = (NBTTagList) tagCompound.getTag("mapIntState");
        if (MapIntStateTag == null) {
            MapIntStateTag = new NBTTagList();
        }
        BlockMapIntState MapIntState = new BlockMapIntState();
        MapIntState.getIntStateMapFromNBT(MapIntStateTag);
        int[] posIntArray = tagCompound.getIntArray("posIntArray");
        int[] stateIntArray = tagCompound.getIntArray("stateIntArray");
        for (int i = 0; i < posIntArray.length; i++) {
            int p = posIntArray[i];
            BlockPos pos = GadgetUtils.relIntToPos(startBlock, p);
            short IntState = (short) stateIntArray[i];
            blockMap.add(new BlockMap(pos, MapIntState.getStateFromSlot(IntState), (int) (byte) ((p & 0xff0000) >> 16), (int) (byte) ((p & 0x00ff00) >> 8), (int) (byte) (p & 0x0000ff)));
        }
        return blockMap;
    }

    public static ArrayList<BlockMap> getBlockMapList(NBTTagCompound tagCompound, BlockPos startBlock) {
        ArrayList<BlockMap> blockMap = new ArrayList<BlockMap>();
        if (tagCompound == null) {
            tagCompound = new NBTTagCompound();
        }
        NBTTagList MapIntStateTag = (NBTTagList) tagCompound.getTag("mapIntState");
        if (MapIntStateTag == null) {
            MapIntStateTag = new NBTTagList();
        }
        BlockMapIntState MapIntState = new BlockMapIntState();
        MapIntState.getIntStateMapFromNBT(MapIntStateTag);
        int[] posIntArray = tagCompound.getIntArray("posIntArray");
        int[] stateIntArray = tagCompound.getIntArray("stateIntArray");
        for (int i = 0; i < posIntArray.length; i++) {
            int p = posIntArray[i];
            BlockPos pos = GadgetUtils.relIntToPos(startBlock, p);
            short IntState = (short) stateIntArray[i];
            blockMap.add(new BlockMap(pos, MapIntState.getStateFromSlot(IntState), (int) (byte) ((p & 0xff0000) >> 16), (int) (byte) ((p & 0x00ff00) >> 8), (int) (byte) (p & 0x0000ff)));
        }
        return blockMap;
    }

    public static BlockMapIntState getBlockMapIntState(NBTTagCompound tagCompound) {
        if (tagCompound == null) {
            tagCompound = new NBTTagCompound();
        }
        NBTTagList MapIntStateTag = (NBTTagList) tagCompound.getTag("mapIntState");
        if (MapIntStateTag == null) {
            MapIntStateTag = new NBTTagList();
        }
        NBTTagList MapIntStackTag = (NBTTagList) tagCompound.getTag("mapIntStack");
        if (MapIntStackTag == null) {
            MapIntStackTag = new NBTTagList();
        }
        BlockMapIntState MapIntState = new BlockMapIntState();
        MapIntState.getIntStateMapFromNBT(MapIntStateTag);
        MapIntState.getIntStackMapFromNBT(MapIntStackTag);
        return MapIntState;
    }

    public static void setToolMode(ItemStack stack, toolModes mode) {
        NBTTagCompound tagCompound = stack.getTagCompound();
        if (tagCompound == null) {
            tagCompound = new NBTTagCompound();
        }
        tagCompound.setString("mode", mode.name());
        stack.setTagCompound(tagCompound);
    }

    public static toolModes getToolMode(ItemStack stack) {
        NBTTagCompound tagCompound = stack.getTagCompound();
        toolModes mode = toolModes.Copy;
        if (tagCompound == null) {
            setToolMode(stack, mode);
            return mode;
        }
        try {
            mode = toolModes.valueOf(tagCompound.getString("mode"));
        } catch (Exception e) {
            setToolMode(stack, mode);
        }
        return mode;
    }

    @Override
    public void addInformation(ItemStack stack, World player, List<String> list, ITooltipFlag b) {
        super.addInformation(stack, player, list, b);
        list.add(TextFormatting.AQUA + I18n.format("tooltip.gadget.mode") + ": " + getToolMode(stack));
        if (Config.poweredByFE) {
            IEnergyStorage energy = stack.getCapability(CapabilityEnergy.ENERGY, null);
            list.add(TextFormatting.WHITE + I18n.format("tooltip.gadget.energy") + ": " + withSuffix(energy.getEnergyStored()) + "/" + withSuffix(energy.getMaxEnergyStored()));
        }
    }

    public void setMode(EntityPlayer player, ItemStack heldItem, int modeInt) {
        //Called when we specify a mode with the radial menu
        toolModes mode = toolModes.values()[modeInt];
        setToolMode(heldItem, mode);
        player.sendStatusMessage(new TextComponentString(TextFormatting.AQUA + new TextComponentTranslation("message.gadget.toolmode").getUnformattedComponentText() + ": " + mode.name()), true);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        ItemStack stack = player.getHeldItem(hand);
        player.setActiveHand(hand);
        if (!world.isRemote) {
            if (getToolMode(stack) == toolModes.Copy) {
                if (player.isSneaking()) {
                    if (getStartPos(stack) != null) {
                        copyBlocks(stack, player, world, getStartPos(stack), pos);
                    } else {
                        setEndPos(stack, pos);
                    }
                } else {
                    //setStartPos(stack, pos);
                    if (getEndPos(stack) != null) {
                        copyBlocks(stack, player, world, pos, getEndPos(stack));
                    } else {
                        setStartPos(stack, pos);
                    }
                }

            } else if (getToolMode(stack) == toolModes.Paste) {
                buildBlockMap(world, pos.up(), stack, player);
            }
            NBTTagCompound tagCompound = stack.getTagCompound();
            ByteBuf buf = Unpooled.buffer(16);
            ByteBufUtils.writeTag(buf, tagCompound);
            //System.out.println(buf.readableBytes());
        }

        return EnumActionResult.SUCCESS;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        player.setActiveHand(hand);
        if (!world.isRemote) {
            if (getToolMode(stack) == toolModes.Copy) {
                BlockPos pos = VectorTools.getPosLookingAt(player);
                if (pos == null) {
                    //setStartPos(stack, null);
                    //setEndPos(stack, null);
                    //player.sendStatusMessage(new TextComponentString(TextFormatting.AQUA + new TextComponentTranslation("message.gadget.areareset").getUnformattedComponentText()), true);
                    return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
                }
                if (player.isSneaking()) {
                    if (getStartPos(stack) != null) {
                        copyBlocks(stack, player, world, getStartPos(stack), pos);
                    } else {
                        setEndPos(stack, pos);
                    }
                } else {
                    if (getEndPos(stack) != null) {
                        copyBlocks(stack, player, world, pos, getEndPos(stack));
                    } else {
                        setStartPos(stack, pos);
                    }
                }
            } else if (getToolMode(stack) == toolModes.Paste) {
                if (getAnchor(stack) == null) {
                    BlockPos pos = VectorTools.getPosLookingAt(player);
                    if (pos == null) return new ActionResult<ItemStack>(EnumActionResult.FAIL, stack);
                    buildBlockMap(world, pos.up(), stack, player);
                } else {
                    buildBlockMap(world, getAnchor(stack).up(), stack, player);
                }
            }
        } else {
            if (getToolMode(stack) == toolModes.Copy) {
                BlockPos pos = VectorTools.getPosLookingAt(player);
                if (pos == null) {
                    if (player.isSneaking()) {
                        player.openGui(BuildingGadgets.instance, GuiProxy.CopyPasteID, world, hand.ordinal(), 0, 0);
                        return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
                    }
                }
            }
        }
        return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
    }

    public void rotateBlocks(ItemStack stack, EntityPlayer player) {
        if (!(getToolMode(stack) == toolModes.Paste)) return;
        if (player.world.isRemote) {
            return;
        }
        ArrayList<BlockMap> blockMapList = new ArrayList<BlockMap>();
        BlockMapWorldSave worldSave = BlockMapWorldSave.get(player.world);
        NBTTagCompound tagCompound = worldSave.getCompoundFromUUID(getUUID(stack));
        BlockPos startPos = getStartPos(stack);
        blockMapList = getBlockMapList(tagCompound);
        ArrayList<Integer> posIntArrayList = new ArrayList<Integer>();
        ArrayList<Integer> stateIntArrayList = new ArrayList<Integer>();
        BlockMapIntState blockMapIntState = new BlockMapIntState();

        for (BlockMap blockMap : blockMapList) {
            BlockPos tempPos = blockMap.pos;

            int px = (tempPos.getX() - startPos.getX());
            int pz = (tempPos.getZ() - startPos.getZ());

            int nx = -pz;
            int nz = px;

            BlockPos newPos = new BlockPos(startPos.getX() + nx, tempPos.getY(), startPos.getZ() + nz);
            IBlockState rotatedState = blockMap.state.withRotation(Rotation.CLOCKWISE_90);
            posIntArrayList.add(GadgetUtils.relPosToInt(startPos, newPos));
            blockMapIntState.addToMap(rotatedState);
            stateIntArrayList.add((int) blockMapIntState.findSlot(rotatedState));
            UniqueItem uniqueItem = BlockMapIntState.blockStateToUniqueItem(rotatedState, player, tempPos);
            blockMapIntState.addToStackMap(uniqueItem, rotatedState);
        }
        int[] posIntArray = posIntArrayList.stream().mapToInt(i -> i).toArray();
        int[] stateIntArray = stateIntArrayList.stream().mapToInt(i -> i).toArray();
        tagCompound.setTag("mapIntState", blockMapIntState.putIntStateMapIntoNBT());
        tagCompound.setTag("mapIntStack", blockMapIntState.putIntStackMapIntoNBT());
        tagCompound.setIntArray("posIntArray", posIntArray);
        tagCompound.setIntArray("stateIntArray", stateIntArray);
        incrementCopyCounter(stack);
        tagCompound.setInteger("copycounter", getCopyCounter(stack));
        worldSave.addToMap(getUUID(stack), tagCompound);
        worldSave.markForSaving();
        PacketHandler.INSTANCE.sendTo(new PacketBlockMap(tagCompound), (EntityPlayerMP) player);
        player.sendStatusMessage(new TextComponentString(TextFormatting.AQUA + new TextComponentTranslation("message.gadget.rotated").getUnformattedComponentText()), true);
    }

    public static void copyBlocks(ItemStack stack, EntityPlayer player, World world, BlockPos startPos, BlockPos endPos) {
        if (startPos != null && endPos != null) {
            if (findBlocks(world, startPos, endPos, stack, player)) {
                setStartPos(stack, startPos);
                setEndPos(stack, endPos);
                player.sendStatusMessage(new TextComponentString(TextFormatting.AQUA + new TextComponentTranslation("message.gadget.copied").getUnformattedComponentText()), true);
            }
        }
    }

    public static boolean findBlocks(World world, BlockPos start, BlockPos end, ItemStack stack, EntityPlayer player) {
        setLastBuild(stack, null, 0);

        int startX = start.getX();
        int startY = start.getY();
        int startZ = start.getZ();

        int endX = end.getX();
        int endY = end.getY();
        int endZ = end.getZ();

        if (Math.abs(startX - endX) >= 125 || Math.abs(startY - endY) >= 125 || Math.abs(startZ - endZ) >= 125) {
            player.sendStatusMessage(new TextComponentString(TextFormatting.RED + new TextComponentTranslation("message.gadget.toobigarea").getUnformattedComponentText()), true);
            return false;
        }

        int iStartX = startX < endX ? startX : endX;
        int iStartY = startY < endY ? startY : endY;
        int iStartZ = startZ < endZ ? startZ : endZ;
        int iEndX = startX < endX ? endX : startX;
        int iEndY = startY < endY ? endY : startY;
        int iEndZ = startZ < endZ ? endZ : startZ;
        BlockMapWorldSave worldSave = BlockMapWorldSave.get(world);
        NBTTagCompound tagCompound = new NBTTagCompound();
        ArrayList<Integer> posIntArrayList = new ArrayList<Integer>();
        ArrayList<Integer> stateIntArrayList = new ArrayList<Integer>();
        BlockMapIntState blockMapIntState = new BlockMapIntState();
        Map<UniqueItem, Integer> itemCountMap = new HashMap<UniqueItem, Integer>();

        int blockCount = 0;

        for (int x = iStartX; x <= iEndX; x++) {
            for (int y = iStartY; y <= iEndY; y++) {
                for (int z = iStartZ; z <= iEndZ; z++) {
                    BlockPos tempPos = new BlockPos(x, y, z);
                    IBlockState tempState = world.getBlockState(tempPos);
                    if (tempState != Blocks.AIR.getDefaultState() && (world.getTileEntity(tempPos) == null || world.getTileEntity(tempPos) instanceof ConstructionBlockTileEntity) && !tempState.getBlock().getMaterial(tempState).isLiquid() && !BlacklistBlocks.checkBlacklist(tempState.getBlock())) {
                        TileEntity te = world.getTileEntity(tempPos);
                        IBlockState assignState = InventoryManipulation.getSpecificStates(tempState, world, player, tempPos, stack);
                        IBlockState actualState = assignState.getBlock().getActualState(assignState, world, tempPos);
                        if (te instanceof ConstructionBlockTileEntity) {
                            actualState = ((ConstructionBlockTileEntity) te).getActualBlockState();
                        }
                        posIntArrayList.add(GadgetUtils.relPosToInt(start, tempPos));
                        blockMapIntState.addToMap(actualState);
                        stateIntArrayList.add((int) blockMapIntState.findSlot(actualState));
                        UniqueItem uniqueItem = BlockMapIntState.blockStateToUniqueItem(actualState, player, tempPos);
                        blockMapIntState.addToStackMap(uniqueItem, actualState);
                        blockCount++;
                        if (blockCount > 32768) {
                            player.sendStatusMessage(new TextComponentString(TextFormatting.RED + new TextComponentTranslation("message.gadget.toomanyblocks").getUnformattedComponentText()), true);
                            return false;
                        }
                        NonNullList<ItemStack> drops = NonNullList.create();
                        actualState.getBlock().getDrops(drops, world, new BlockPos(0, 0, 0), actualState, 0);
                        int neededItems = 0;
                        for (ItemStack drop : drops) {
                            if (drop.getItem().equals(uniqueItem.item)) {
                                neededItems++;
                            }
                        }
                        if (neededItems == 0) {
                            neededItems = 1;
                        }
                        if (uniqueItem.item != Items.AIR) {
                            boolean found = false;
                            for (Map.Entry<UniqueItem, Integer> entry : itemCountMap.entrySet()) {
                                if (entry.getKey().equals(uniqueItem)) {
                                    itemCountMap.put(entry.getKey(), itemCountMap.get(entry.getKey()) + neededItems);
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                itemCountMap.put(uniqueItem, neededItems);
                            }
                        }
                    }
                }
            }
        }
        setItemCountMap(stack, itemCountMap);
        tagCompound.setTag("mapIntState", blockMapIntState.putIntStateMapIntoNBT());
        tagCompound.setTag("mapIntStack", blockMapIntState.putIntStackMapIntoNBT());
        int[] posIntArray = posIntArrayList.stream().mapToInt(i -> i).toArray();
        int[] stateIntArray = stateIntArrayList.stream().mapToInt(i -> i).toArray();
        tagCompound.setIntArray("posIntArray", posIntArray);
        tagCompound.setIntArray("stateIntArray", stateIntArray);

        tagCompound.setTag("startPos", NBTUtil.createPosTag(start));
        tagCompound.setTag("endPos", NBTUtil.createPosTag(end));
        tagCompound.setInteger("dim", player.dimension);
        tagCompound.setString("UUID", getUUID(stack));
        tagCompound.setString("owner", player.getName());
        incrementCopyCounter(stack);
        tagCompound.setInteger("copycounter", getCopyCounter(stack));

        worldSave.addToMap(getUUID(stack), tagCompound);
        worldSave.markForSaving();
        PacketHandler.INSTANCE.sendTo(new PacketBlockMap(tagCompound), (EntityPlayerMP) player);
        return true;
    }

    public void buildBlockMap(World world, BlockPos startPos, ItemStack stack, EntityPlayer player) {
        long time = System.nanoTime();
        BlockPos anchorPos = getAnchor(stack);
        ArrayList<BlockMap> blockMapList = new ArrayList<BlockMap>();
        BlockMapWorldSave worldSave = BlockMapWorldSave.get(world);
        NBTTagCompound tagCompound = worldSave.getCompoundFromUUID(getUUID(stack));
        Map<IBlockState, UniqueItem> IntStackMap = getBlockMapIntState(tagCompound).getIntStackMap();
        if (anchorPos == null) {
            blockMapList = getBlockMapList(tagCompound, startPos);
            setLastBuild(stack, startPos, player.dimension);
        } else {
            blockMapList = getBlockMapList(tagCompound, anchorPos);
            setLastBuild(stack, anchorPos, player.dimension);
        }
        for (BlockMap blockMap : blockMapList) {
            placeBlock(world, blockMap.pos, player, blockMap.state, IntStackMap);
        }
        setAnchor(stack, null);
        //System.out.printf("Built %d Blocks in %.2f ms%n", blockMapList.size(), (System.nanoTime() - time) * 1e-6);
    }

    public static void placeBlock(World world, BlockPos pos, EntityPlayer player, IBlockState state, Map<IBlockState, UniqueItem> IntStackMap) {
        if (!world.getBlockState(pos).getBlock().isReplaceable(world, pos)) return;
        if (state.equals(Blocks.AIR.getDefaultState())) return;
        if (!player.isAllowEdit()) {
            return;
        }
        ItemStack heldItem = player.getHeldItemMainhand();
        if (!(heldItem.getItem() instanceof CopyPasteTool)) {
            heldItem = player.getHeldItemOffhand();
            if (!(heldItem.getItem() instanceof CopyPasteTool)) {
                return;
            }
        }
        UniqueItem uniqueItem = IntStackMap.get(state);
        if (uniqueItem == null) return; //This shouldn't happen I hope!
        ItemStack itemStack = new ItemStack(uniqueItem.item, 1, uniqueItem.meta);
        NonNullList<ItemStack> drops = NonNullList.create();
        state.getBlock().getDrops(drops, world, pos, state, 0);
        int neededItems = 0;
        for (ItemStack drop : drops) {
            if (drop.getItem().equals(itemStack.getItem())) {
                neededItems++;
            }
        }
        if (neededItems == 0) {
            neededItems = 1;
        }
        if (!world.isBlockModifiable(player, pos)) {
            return;
        }
        BlockSnapshot blockSnapshot = BlockSnapshot.getBlockSnapshot(world, pos);
        if (ForgeEventFactory.onPlayerBlockPlace(player, blockSnapshot, EnumFacing.UP, EnumHand.MAIN_HAND).isCanceled()) {
            return;
        }
        ItemStack constructionPaste = new ItemStack(ModItems.constructionPaste);
        boolean useConstructionPaste = false;
        if (InventoryManipulation.countItem(itemStack, player) < neededItems) {
            if (InventoryManipulation.countPaste(player) < neededItems) {
                return;
            } else {
                itemStack = constructionPaste.copy();
                useConstructionPaste = true;
            }
        }
        if (Config.poweredByFE) {
            if (!useEnergy(heldItem, Config.energyCostBuilder, player)) {
                return;
            }
        } else {
            if (heldItem.getItemDamage() >= heldItem.getMaxDamage()) {
                if (heldItem.isItemStackDamageable()) {
                    return;
                }
            } else {
                heldItem.damageItem(1, player);
            }
        }
        boolean useItemSuccess;
        if (useConstructionPaste) {
            useItemSuccess = InventoryManipulation.usePaste(player, 1);
        } else {
            useItemSuccess = InventoryManipulation.useItem(itemStack, player, neededItems);
        }
        if (useItemSuccess) {
            world.spawnEntity(new BlockBuildEntity(world, pos, player, state, 1, state, useConstructionPaste));
        }

    }

    public static void anchorBlocks(EntityPlayer player, ItemStack stack) {
        BlockPos currentAnchor = getAnchor(stack);
        if (currentAnchor == null) {
            RayTraceResult lookingAt = VectorTools.getLookingAt(player);
            if (lookingAt == null) {
                return;
            }
            currentAnchor = lookingAt.getBlockPos().up();
            setAnchor(stack, currentAnchor);
            player.sendStatusMessage(new TextComponentString(TextFormatting.AQUA + new TextComponentTranslation("message.gadget.anchorrender").getUnformattedComponentText()), true);
        } else {
            setAnchor(stack, null);
            player.sendStatusMessage(new TextComponentString(TextFormatting.AQUA + new TextComponentTranslation("message.gadget.anchorremove").getUnformattedComponentText()), true);
        }
    }

    public static void undoBuild(EntityPlayer player, ItemStack heldItem) {
        long time = System.nanoTime();
        BlockMapWorldSave worldSave = BlockMapWorldSave.get(player.world);
        NBTTagCompound tagCompound = worldSave.getCompoundFromUUID(getUUID(heldItem));
        World world = player.world;
        if (world.isRemote) {
            return;
        }
        BlockPos startPos = getLastBuild(heldItem);
        if (startPos == null) {
            return;
        }
        Integer dimension = getLastBuildDim(heldItem);
        ItemStack silkTool = heldItem.copy(); //Setup a Silk Touch version of the tool so we can return stone instead of cobblestone, etc.
        silkTool.addEnchantment(Enchantments.SILK_TOUCH, 1);
        ArrayList<BlockMap> blockMapList = getBlockMapList(tagCompound, startPos);
        boolean success = true;
        for (BlockMap blockMap : blockMapList) {
            double distance = blockMap.pos.getDistance(player.getPosition().getX(), player.getPosition().getY(), player.getPosition().getZ());
            boolean sameDim = (player.dimension == dimension);
            IBlockState currentBlock = world.getBlockState(blockMap.pos);
            BlockEvent.BreakEvent e = new BlockEvent.BreakEvent(world, blockMap.pos, currentBlock, player);
            boolean cancelled = MinecraftForge.EVENT_BUS.post(e);
            if (distance < 256 && !cancelled && sameDim) { //Don't allow us to undo a block while its still being placed or too far away
                if (currentBlock.getBlock() == blockMap.state.getBlock() || currentBlock.getBlock() instanceof ConstructionBlock) {
                    if (currentBlock.getBlock().getBlockHardness(currentBlock, world, blockMap.pos) >= 0) {
                        currentBlock.getBlock().harvestBlock(world, player, blockMap.pos, currentBlock, world.getTileEntity(blockMap.pos), silkTool);
                        world.spawnEntity(new BlockBuildEntity(world, blockMap.pos, player, currentBlock, 2, currentBlock, false));
                    }
                }
            } else {
                player.sendStatusMessage(new TextComponentString(TextFormatting.RED + new TextComponentTranslation("message.gadget.undofailed").getUnformattedComponentText()), true);
                success = false;
            }
            //System.out.printf("Undid %d Blocks in %.2f ms%n", blockMapList.size(), (System.nanoTime() - time) * 1e-6);
        }
        if (success) setLastBuild(heldItem, null, 0);
    }
}
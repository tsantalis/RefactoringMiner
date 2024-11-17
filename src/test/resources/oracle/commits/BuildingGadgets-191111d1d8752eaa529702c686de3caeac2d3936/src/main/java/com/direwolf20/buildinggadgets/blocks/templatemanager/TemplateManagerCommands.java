package com.direwolf20.buildinggadgets.blocks.templatemanager;

import com.direwolf20.buildinggadgets.ModItems;
import com.direwolf20.buildinggadgets.items.CopyPasteTool;
import com.direwolf20.buildinggadgets.items.Template;
import com.direwolf20.buildinggadgets.network.PacketBlockMap;
import com.direwolf20.buildinggadgets.network.PacketHandler;
import com.direwolf20.buildinggadgets.tools.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.minecraft.client.gui.GuiScreen.setClipboardString;

public class TemplateManagerCommands {
    private static final Set<Item> allowedItemsLeft = Stream.of(ModItems.copyPasteTool, ModItems.template).collect(Collectors.toSet());
    private static final Set<Item> allowedItemsRight = Stream.of(Items.PAPER, ModItems.template).collect(Collectors.toSet());

    public static void loadTemplate(TemplateManagerContainer container, EntityPlayer player) {
        ItemStack itemStack0 = container.getSlot(0).getStack();
        ItemStack itemStack1 = container.getSlot(1).getStack();
        if (!(allowedItemsLeft.contains(itemStack0.getItem())) || !(allowedItemsRight.contains(itemStack1.getItem()))) {
            return;
        }
        if (itemStack1.getItem().equals(Items.PAPER)) return;
        World world = player.world;

        BlockPos startPos = Template.getStartPos(itemStack1);
        BlockPos endPos = Template.getEndPos(itemStack1);
        Map<UniqueItem, Integer> tagMap = Template.getItemCountMap(itemStack1);
        String UUIDTemplate = Template.getUUID(itemStack1);
        if (UUIDTemplate == null || UUIDTemplate.equals("")) return;

        BlockMapWorldSave worldSave = BlockMapWorldSave.get(world);
        TemplateWorldSave templateWorldSave = TemplateWorldSave.get(world);
        NBTTagCompound tagCompound;

        if (itemStack0.getItem().equals(ModItems.copyPasteTool)) {
            CopyPasteTool.setStartPos(itemStack0, startPos);
            CopyPasteTool.setEndPos(itemStack0, endPos);
            CopyPasteTool.setItemCountMap(itemStack0, tagMap);
            String UUID = CopyPasteTool.getUUID(itemStack0);

            if (UUID == null || UUID.equals("")) return;

            NBTTagCompound templateTagCompound = templateWorldSave.getCompoundFromUUID(UUIDTemplate);
            tagCompound = templateTagCompound.copy();
            CopyPasteTool.incrementCopyCounter(itemStack0);
            tagCompound.setInteger("copycounter", CopyPasteTool.getCopyCounter(itemStack0));
            tagCompound.setString("UUID", CopyPasteTool.getUUID(itemStack0));
            tagCompound.setString("owner", player.getName());
            worldSave.addToMap(UUID, tagCompound);
        } else {
            Template.setStartPos(itemStack0, startPos);
            Template.setEndPos(itemStack0, endPos);
            Template.setItemCountMap(itemStack0, tagMap);
            String UUID = Template.getUUID(itemStack0);

            if (UUID == null || UUID.equals("")) return;

            NBTTagCompound templateTagCompound = templateWorldSave.getCompoundFromUUID(UUIDTemplate);
            tagCompound = templateTagCompound.copy();
            Template.incrementCopyCounter(itemStack0);
            tagCompound.setInteger("copycounter", Template.getCopyCounter(itemStack0));
            tagCompound.setString("UUID", Template.getUUID(itemStack0));
            tagCompound.setString("owner", player.getName());
            templateWorldSave.addToMap(UUID, tagCompound);
            Template.setName(itemStack0, Template.getName(itemStack1));
        }
        container.putStackInSlot(0, itemStack0);
        PacketHandler.INSTANCE.sendTo(new PacketBlockMap(tagCompound), (EntityPlayerMP) player);
    }

    public static void saveTemplate(TemplateManagerContainer container, EntityPlayer player, String templateName) {
        ItemStack itemStack0 = container.getSlot(0).getStack();
        ItemStack itemStack1 = container.getSlot(1).getStack();

        if (itemStack0.isEmpty() && itemStack1.getItem() instanceof Template && !templateName.isEmpty()) {
            Template.setName(itemStack1, templateName);
            container.putStackInSlot(1, itemStack1);
            return;
        }


        if (!(allowedItemsLeft.contains(itemStack0.getItem())) || !(allowedItemsRight.contains(itemStack1.getItem()))) {
            return;
        }

        World world = player.world;
        ItemStack templateStack;
        if (itemStack1.getItem().equals(Items.PAPER)) {
            templateStack = new ItemStack(ModItems.template, 1);
            container.putStackInSlot(1, templateStack);
        }
        if (!(container.getSlot(1).getStack().getItem().equals(ModItems.template))) return;
        templateStack = container.getSlot(1).getStack();
        BlockMapWorldSave worldSave = BlockMapWorldSave.get(world);
        TemplateWorldSave templateWorldSave = TemplateWorldSave.get(world);
        NBTTagCompound templateTagCompound;

        if (itemStack0.getItem().equals(ModItems.copyPasteTool)) {
            String UUID = CopyPasteTool.getUUID(itemStack0);
            String UUIDTemplate = Template.getUUID(templateStack);
            if (UUID == null || UUID.equals("")) return;
            if (UUIDTemplate == null || UUIDTemplate.equals("")) return;

            NBTTagCompound tagCompound = worldSave.getCompoundFromUUID(UUID);
            templateTagCompound = tagCompound.copy();
            Template.incrementCopyCounter(templateStack);
            templateTagCompound.setInteger("copycounter", Template.getCopyCounter(templateStack));
            templateTagCompound.setString("UUID", Template.getUUID(templateStack));

            templateWorldSave.addToMap(UUIDTemplate, templateTagCompound);
            BlockPos startPos = CopyPasteTool.getStartPos(itemStack0);
            BlockPos endPos = CopyPasteTool.getEndPos(itemStack0);
            Map<UniqueItem, Integer> tagMap = CopyPasteTool.getItemCountMap(itemStack0);
            Template.setStartPos(templateStack, startPos);
            Template.setEndPos(templateStack, endPos);
            Template.setItemCountMap(templateStack, tagMap);
            Template.setName(templateStack, templateName);
            container.putStackInSlot(1, templateStack);
            PacketHandler.INSTANCE.sendTo(new PacketBlockMap(templateTagCompound), (EntityPlayerMP) player);
        } else {
            String UUID = Template.getUUID(itemStack0);
            String UUIDTemplate = Template.getUUID(templateStack);
            if (UUID == null || UUID.equals("")) return;
            if (UUIDTemplate == null || UUIDTemplate.equals("")) return;

            NBTTagCompound tagCompound = templateWorldSave.getCompoundFromUUID(UUID);
            templateTagCompound = tagCompound.copy();
            Template.incrementCopyCounter(templateStack);
            templateTagCompound.setInteger("copycounter", Template.getCopyCounter(templateStack));
            templateTagCompound.setString("UUID", Template.getUUID(templateStack));

            templateWorldSave.addToMap(UUIDTemplate, templateTagCompound);
            BlockPos startPos = Template.getStartPos(itemStack0);
            BlockPos endPos = Template.getEndPos(itemStack0);
            Map<UniqueItem, Integer> tagMap = Template.getItemCountMap(itemStack0);
            Template.setStartPos(templateStack, startPos);
            Template.setEndPos(templateStack, endPos);
            Template.setItemCountMap(templateStack, tagMap);
            if (templateName.equals("")) {
                Template.setName(templateStack, Template.getName(itemStack0));
            } else {
                Template.setName(templateStack, templateName);
            }
            container.putStackInSlot(1, templateStack);
            PacketHandler.INSTANCE.sendTo(new PacketBlockMap(templateTagCompound), (EntityPlayerMP) player);
        }
    }

    public static void PasteTemplate(TemplateManagerContainer container, EntityPlayer player, NBTTagCompound sentTagCompound, String templateName) {
        ItemStack itemStack1 = container.getSlot(1).getStack();

        if (!(allowedItemsRight.contains(itemStack1.getItem()))) {
            return;
        }

        World world = player.world;
        ItemStack templateStack;
        if (itemStack1.getItem().equals(Items.PAPER)) {
            templateStack = new ItemStack(ModItems.template, 1);
            container.putStackInSlot(1, templateStack);
        }
        if (!(container.getSlot(1).getStack().getItem().equals(ModItems.template))) return;
        templateStack = container.getSlot(1).getStack();

        TemplateWorldSave templateWorldSave = TemplateWorldSave.get(world);

        String UUIDTemplate = Template.getUUID(templateStack);
        if (UUIDTemplate == null || UUIDTemplate.equals("")) return;

        NBTTagCompound templateTagCompound;

        templateTagCompound = sentTagCompound.copy();
        BlockPos startPos = GadgetUtils.getPOSFromNBT(templateTagCompound, "startPos");
        BlockPos endPos = GadgetUtils.getPOSFromNBT(templateTagCompound, "endPos");
        Template.incrementCopyCounter(templateStack);
        templateTagCompound.setInteger("copycounter", Template.getCopyCounter(templateStack));
        templateTagCompound.setString("UUID", Template.getUUID(templateStack));
        //GadgetUtils.writePOSToNBT(templateTagCompound, startPos, "startPos", 0);
        //GadgetUtils.writePOSToNBT(templateTagCompound, endPos, "startPos", 0);
        //Map<UniqueItem, Integer> tagMap = GadgetUtils.nbtToItemCount((NBTTagList) templateTagCompound.getTag("itemcountmap"));
        //templateTagCompound.removeTag("itemcountmap");

        NBTTagList MapIntStateTag = (NBTTagList) templateTagCompound.getTag("mapIntState");

        BlockMapIntState MapIntState = new BlockMapIntState();
        MapIntState.getIntStateMapFromNBT(MapIntStateTag);
        MapIntState.makeStackMapFromStateMap(player);
        templateTagCompound.setTag("mapIntStack", MapIntState.putIntStackMapIntoNBT());
        templateTagCompound.setString("owner", player.getName());

        Map<UniqueItem, Integer> itemCountMap = new HashMap<UniqueItem, Integer>();
        Map<IBlockState, UniqueItem> IntStackMap = MapIntState.IntStackMap;
        ArrayList<BlockMap> blockMapList = CopyPasteTool.getBlockMapList(templateTagCompound);
        for (BlockMap blockMap : blockMapList) {
            UniqueItem uniqueItem = IntStackMap.get(blockMap.state);
            NonNullList<ItemStack> drops = NonNullList.create();
            blockMap.state.getBlock().getDrops(drops, world, new BlockPos(0, 0, 0), blockMap.state, 0);
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

        templateWorldSave.addToMap(UUIDTemplate, templateTagCompound);


        Template.setStartPos(templateStack, startPos);
        Template.setEndPos(templateStack, endPos);
        //Template.setItemCountMap(templateStack, tagMap);
        Template.setItemCountMap(templateStack, itemCountMap);
        Template.setName(templateStack, templateName);
        container.putStackInSlot(1, templateStack);
        PacketHandler.INSTANCE.sendTo(new PacketBlockMap(templateTagCompound), (EntityPlayerMP) player);
    }

    public static void CopyTemplate(TemplateManagerContainer container) {
        ItemStack itemStack0 = container.getSlot(0).getStack();
        if (itemStack0.getItem() instanceof CopyPasteTool) {
            NBTTagCompound tagCompound = PasteToolBufferBuilder.getTagFromUUID(CopyPasteTool.getUUID(itemStack0));
            if (tagCompound == null) {
                Minecraft.getMinecraft().player.sendStatusMessage(new TextComponentString(TextFormatting.RED + new TextComponentTranslation("message.gadget.copyfailed").getUnformattedComponentText()), false);
                return;
            }
            NBTTagCompound newCompound = new NBTTagCompound();
            newCompound.setIntArray("stateIntArray", tagCompound.getIntArray("stateIntArray"));
            newCompound.setIntArray("posIntArray", tagCompound.getIntArray("posIntArray"));
            newCompound.setTag("mapIntState", tagCompound.getTag("mapIntState"));
            GadgetUtils.writePOSToNBT(newCompound, GadgetUtils.getPOSFromNBT(tagCompound, "startPos"), "startPos", 0);
            GadgetUtils.writePOSToNBT(newCompound, GadgetUtils.getPOSFromNBT(tagCompound, "endPos"), "endPos", 0);
            //Map<UniqueItem, Integer> tagMap = CopyPasteTool.getItemCountMap(itemStack0);
            //NBTTagList tagList = GadgetUtils.itemCountToNBT(tagMap);
            //newCompound.setTag("itemcountmap", tagList);
            String jsonTag = newCompound.toString();
            setClipboardString(jsonTag);
        }
    }
}

package com.direwolf20.buildinggadgets.items;

import java.util.Map;

import javax.annotation.Nullable;

import com.direwolf20.buildinggadgets.tools.GadgetUtils;
import com.direwolf20.buildinggadgets.tools.UniqueItem;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;

public interface ITemplate {

    @Nullable
    String getUUID(ItemStack stack);

    default void setItemCountMap(ItemStack stack, Map<UniqueItem, Integer> tagMap) {
        NBTTagCompound tagCompound = stack.getTagCompound();
        if (tagCompound == null) {
            tagCompound = new NBTTagCompound();
        }
        NBTTagList tagList = GadgetUtils.itemCountToNBT(tagMap);
        tagCompound.setTag("itemcountmap", tagList);
        stack.setTagCompound(tagCompound);
    }

    default Map<UniqueItem, Integer> getItemCountMap(ItemStack stack) {
        NBTTagCompound tagCompound = stack.getTagCompound();
        if (tagCompound == null) {
            return null;
        }

        Map<UniqueItem, Integer> tagMap = GadgetUtils.nbtToItemCount((NBTTagList) tagCompound.getTag("itemcountmap"));
        return tagMap;
    }

    default int getCopyCounter(ItemStack stack) {
        return stack.getTagCompound().getInteger("copycounter");
    }

    default void setCopyCounter(ItemStack stack, int counter) {
        NBTTagCompound tagCompound = stack.getTagCompound();
        tagCompound.setInteger("copycounter", counter);
        stack.setTagCompound(tagCompound);
    }

    default void incrementCopyCounter(ItemStack stack) {
        NBTTagCompound tagCompound = stack.getTagCompound();
        tagCompound.setInteger("copycounter", tagCompound.getInteger("copycounter") + 1);
        stack.setTagCompound(tagCompound);
    }

    default void setStartPos(ItemStack stack, BlockPos startPos) {
        GadgetUtils.writePOSToNBT(stack, startPos, "startPos");
    }

    default BlockPos getStartPos(ItemStack stack) {
        return GadgetUtils.getPOSFromNBT(stack, "startPos");
    }

    default void setEndPos(ItemStack stack, BlockPos startPos) {
        GadgetUtils.writePOSToNBT(stack, startPos, "endPos");
    }

    default BlockPos getEndPos(ItemStack stack) {
        return GadgetUtils.getPOSFromNBT(stack, "endPos");
    }

}

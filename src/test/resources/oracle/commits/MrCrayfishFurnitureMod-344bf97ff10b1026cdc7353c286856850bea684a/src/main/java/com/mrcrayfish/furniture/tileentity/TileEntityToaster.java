/**
 * MrCrayfish's Furniture Mod
 * Copyright (C) 2016  MrCrayfish (http://www.mrcrayfish.com/)
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.mrcrayfish.furniture.tileentity;

import com.mrcrayfish.furniture.api.RecipeAPI;
import com.mrcrayfish.furniture.api.RecipeData;
import com.mrcrayfish.furniture.gui.inventory.ISimpleInventory;
import com.mrcrayfish.furniture.init.FurnitureSounds;
import com.mrcrayfish.furniture.util.TileEntityUtil;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;

public class TileEntityToaster extends TileEntity implements ITickable, ISimpleInventory
{
    public ItemStack[] slots = new ItemStack[2];

    private int toastingTime = 0;
    private boolean toasting = false;

    public boolean addSlice(ItemStack item)
    {
        for(int i = 0; i < slots.length; i++)
        {
            if(slots[i] == null)
            {
                slots[i] = item.copy();
                return true;
            }
        }
        return false;
    }

    public void removeSlice()
    {
        for(int i = 0; i < slots.length; i++)
        {
            if(slots[i] != null)
            {
                if(!world.isRemote)
                {
                    EntityItem entityItem = new EntityItem(world, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, slots[i]);
                    world.spawnEntity(entityItem);
                }
                slots[i] = null;
                TileEntityUtil.markBlockForUpdate(world, pos);
                return;
            }
        }
    }

    public void startToasting()
    {
        this.toasting = true;
        TileEntityUtil.markBlockForUpdate(world, pos);
    }

    public boolean isToasting()
    {
        return toasting;
    }

    public ItemStack getSlice(int slot)
    {
        return slots[slot];
    }

    @Override
    public void update()
    {
        if(toasting)
        {
            if(toastingTime == 200)
            {
                for(int i = 0; i < slots.length; i++)
                {
                    if(slots[i] != null)
                    {
                        if(!world.isRemote)
                        {
                            RecipeData data = RecipeAPI.getToasterRecipeFromInput(slots[i]);
                            EntityItem entityItem = new EntityItem(world, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, data.getOutput().copy());
                            world.spawnEntity(entityItem);
                        }
                        slots[i] = null;
                    }
                }
                if(!world.isRemote)
                {
                    world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, FurnitureSounds.toaster_down, SoundCategory.BLOCKS, 0.75F, 1.0F);

                }
                toastingTime = 0;
                toasting = false;
                TileEntityUtil.markBlockForUpdate(world, pos);
                world.updateComparatorOutputLevel(pos, blockType);
            }
            else
            {
                toastingTime++;
            }
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tagCompound)
    {
        super.readFromNBT(tagCompound);
        if(tagCompound.hasKey("Items", 9))
        {
            NBTTagList tagList = (NBTTagList) tagCompound.getTag("Items");
            this.slots = new ItemStack[2];
            for(int i = 0; i < tagList.tagCount(); ++i)
            {
                NBTTagCompound itemTag = (NBTTagCompound) tagList.getCompoundTagAt(i);
                byte slot = itemTag.getByte("Slot");

                if(slot >= 0 && slot < this.slots.length)
                {
                    this.slots[slot] = new ItemStack(itemTag);
                }
            }
        }
        this.toastingTime = tagCompound.getInteger("ToastTime");
        this.toasting = tagCompound.getBoolean("Toasting");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tagCompound)
    {
        super.writeToNBT(tagCompound);
        NBTTagList tagList = new NBTTagList();

        for(int slot = 0; slot < this.slots.length; ++slot)
        {
            if(this.slots[slot] != null)
            {
                NBTTagCompound itemTag = new NBTTagCompound();
                itemTag.setByte("Slot", (byte) slot);
                this.slots[slot].writeToNBT(itemTag);
                tagList.appendTag(itemTag);
            }
        }
        tagCompound.setTag("Items", tagList);
        tagCompound.setInteger("ToastTime", this.toastingTime);
        tagCompound.setBoolean("Toasting", toasting);
        return tagCompound;
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt)
    {
        this.readFromNBT(pkt.getNbtCompound());
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket()
    {
        return new SPacketUpdateTileEntity(pos, getBlockMetadata(), this.writeToNBT(new NBTTagCompound()));
    }

    @Override
    public NBTTagCompound getUpdateTag()
    {
        return this.writeToNBT(new NBTTagCompound());
    }

    @Override
    public int getSize()
    {
        return 2;
    }

    @Override
    public ItemStack getItem(int i)
    {
        return slots[i];
    }

    @Override
    public void clear()
    {
        for(int i = 0; i < slots.length; i++)
        {
            slots[i] = null;
        }

    }
}

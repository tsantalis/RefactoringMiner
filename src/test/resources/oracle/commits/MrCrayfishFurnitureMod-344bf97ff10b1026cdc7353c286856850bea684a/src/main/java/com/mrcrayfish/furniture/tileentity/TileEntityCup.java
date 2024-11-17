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

import com.mrcrayfish.furniture.gui.inventory.ISimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

public class TileEntityCup extends TileEntity implements ISimpleInventory
{
    private ItemStack item = null;
    public int red, green, blue;

    public void setItem(ItemStack item)
    {
        this.item = item.copy();
    }

    public void setColour(int[] rgb)
    {
        this.red = rgb[0];
        this.green = rgb[1];
        this.blue = rgb[2];
    }

    public ItemStack getDrink()
    {
        return item;
    }

    @Override
    public void readFromNBT(NBTTagCompound tagCompound)
    {
        super.readFromNBT(tagCompound);
        if(tagCompound.hasKey("Item", 9))
        {
            NBTTagList tagList = (NBTTagList) tagCompound.getTag("Item");
            if(tagList.tagCount() > 0)
            {
                this.item = new ItemStack(tagList.getCompoundTagAt(0));
            }
        }
        this.red = tagCompound.getInteger("Red");
        this.green = tagCompound.getInteger("Green");
        this.blue = tagCompound.getInteger("Blue");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tagCompound)
    {
        super.writeToNBT(tagCompound);
        NBTTagList tagList = new NBTTagList();
        NBTTagCompound nbt = new NBTTagCompound();
        if(item != null)
        {
            item.writeToNBT(nbt);
            tagList.appendTag(nbt);
        }
        tagCompound.setTag("Item", tagList);
        tagCompound.setInteger("Red", red);
        tagCompound.setInteger("Green", green);
        tagCompound.setInteger("Blue", blue);
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
        return 1;
    }

    @Override
    public ItemStack getItem(int i)
    {
        return getDrink();
    }

    @Override
    public void clear()
    {
        red = 0;
        green = 0;
        blue = 0;
        item = null;
    }
}

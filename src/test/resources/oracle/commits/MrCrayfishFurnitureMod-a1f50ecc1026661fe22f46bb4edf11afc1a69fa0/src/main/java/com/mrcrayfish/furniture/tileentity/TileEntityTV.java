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

import com.mrcrayfish.furniture.blocks.tv.Channels;
import com.mrcrayfish.furniture.util.TileEntityUtil;
import net.minecraft.nbt.NBTTagCompound;

public class TileEntityTV extends TileEntitySyncClient
{
    private int channel = 0;

    @Override
    public void readFromNBT(NBTTagCompound tagCompound)
    {
        super.readFromNBT(tagCompound);
        if(tagCompound.hasKey("Channel", 3))
        {
            this.channel = tagCompound.getInteger("Channel");
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tagCompound)
    {
        super.writeToNBT(tagCompound);
        tagCompound.setInteger("Channel", this.getChannel());
        return tagCompound;
    }

    public int getChannel()
    {
        return this.channel;
    }

    public void setChannel(int channel)
    {
        this.channel = channel;
    }

    public void reloadChannel()
    {
        markDirty();
        TileEntityUtil.markBlockForUpdate(world, pos);
    }

    public void nextChannel()
    {
        int nextChannel = 0;
        if(channel < Channels.getChannelCount() - 1)
        {
            nextChannel = channel + 1;
        }
        setChannel(nextChannel);
        markDirty();
        TileEntityUtil.markBlockForUpdate(world, pos);
    }
}

package com.mrcrayfish.furniture.tileentity;

import net.minecraft.nbt.NBTTagCompound;

public class TileEntityDoorMat extends TileEntitySyncClient
{
    private String message = null;

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    @Override
    public void readFromNBT(NBTTagCompound tagCompound)
    {
        super.readFromNBT(tagCompound);
        this.message = tagCompound.getString("message");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tagCompound)
    {
        super.writeToNBT(tagCompound);
        if(this.message != null)
        {
            tagCompound.setString("message", this.message);
        }
        return tagCompound;
    }
}

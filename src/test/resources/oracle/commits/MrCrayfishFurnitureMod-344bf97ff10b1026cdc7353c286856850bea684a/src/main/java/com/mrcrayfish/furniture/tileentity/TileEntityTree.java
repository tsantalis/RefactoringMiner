package com.mrcrayfish.furniture.tileentity;

import com.mrcrayfish.furniture.gui.inventory.ISimpleInventory;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;

public class TileEntityTree extends TileEntity implements ITickable, ISimpleInventory
{
    private ItemStack[] ornaments = new ItemStack[4];

    @Override
    public int getSize()
    {
        return ornaments.length;
    }

    @Override
    public ItemStack getItem(int i)
    {
        return ornaments[i];
    }

    @Override
    public void clear()
    {
        for(int i = 0; i < ornaments.length; i++)
        {
            ornaments[i] = null;
        }
    }

    @Override
    public void update()
    {
    }

    public void addOrnament(EnumFacing facing, ItemStack item)
    {
        ItemStack temp = ornaments[facing.getHorizontalIndex()];
        if(temp != null)
        {
            if(!world.isRemote)
            {
                EntityItem entityItem = new EntityItem(world, pos.getX() + 0.5, pos.getY() + 1.0D, pos.getZ() + 0.5, temp);
                world.spawnEntity(entityItem);
            }
            ornaments[facing.getHorizontalIndex()] = null;
        }
        if(item != null)
        {
            ornaments[facing.getHorizontalIndex()] = item.copy().splitStack(1);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tagCompound)
    {
        super.readFromNBT(tagCompound);

        if(tagCompound.hasKey("Items", 9))
        {
            NBTTagList tagList = (NBTTagList) tagCompound.getTag("Items");
            this.ornaments = new ItemStack[this.getSize()];

            for(int i = 0; i < tagList.tagCount(); ++i)
            {
                NBTTagCompound itemTag = (NBTTagCompound) tagList.getCompoundTagAt(i);
                int slot = itemTag.getByte("Slot") & 255;

                if(slot >= 0 && slot < this.ornaments.length)
                {
                    this.ornaments[slot] = new ItemStack(itemTag);
                }
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tagCompound)
    {
        super.writeToNBT(tagCompound);
        NBTTagList tagList = new NBTTagList();

        for(int slot = 0; slot < this.ornaments.length; ++slot)
        {
            if(this.ornaments[slot] != null)
            {
                NBTTagCompound itemTag = new NBTTagCompound();
                itemTag.setByte("Slot", (byte) slot);
                this.ornaments[slot].writeToNBT(itemTag);
                tagList.appendTag(itemTag);
            }
        }

        tagCompound.setTag("Items", tagList);
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
}

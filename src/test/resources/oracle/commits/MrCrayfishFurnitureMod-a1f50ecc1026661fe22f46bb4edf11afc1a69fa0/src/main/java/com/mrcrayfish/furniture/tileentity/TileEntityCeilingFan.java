package com.mrcrayfish.furniture.tileentity;

import com.mrcrayfish.furniture.util.TileEntityUtil;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Author: MrCrayfish
 */
public class TileEntityCeilingFan extends TileEntitySyncClient implements ITickable
{
    private boolean powered = false;
    private float maxSpeed = 30F;
    private float acceleration = 0.25F;

    @SideOnly(Side.CLIENT)
    public float prevFanRotation;
    @SideOnly(Side.CLIENT)
    public float fanRotation;
    @SideOnly(Side.CLIENT)
    private float currentSpeed;

    public void setPowered(boolean powered)
    {
        this.powered = powered;
        this.markDirty();
        TileEntityUtil.syncToClient(this);
    }

    public boolean isPowered()
    {
        return powered;
    }

    @Override
    public void update()
    {
        if(world.isRemote)
        {
            prevFanRotation = fanRotation;

            if(powered)
            {
                currentSpeed += acceleration;
                if(currentSpeed > maxSpeed)
                {
                    currentSpeed = maxSpeed;
                }
            }
            else
            {
                currentSpeed *= 0.95F;
            }

           fanRotation += currentSpeed;
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound)
    {
        super.readFromNBT(compound);
        if(compound.hasKey("maxSpeed", Constants.NBT.TAG_FLOAT))
        {
            maxSpeed = compound.getFloat("maxSpeed");
        }
        if(compound.hasKey("acceleration", Constants.NBT.TAG_FLOAT))
        {
            acceleration = compound.getFloat("acceleration");
        }
        if(compound.hasKey("powered", Constants.NBT.TAG_BYTE))
        {
            this.setPowered(compound.getBoolean("powered"));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound)
    {
        super.writeToNBT(compound);
        compound.setBoolean("powered", powered);
        compound.setFloat("maxSpeed", maxSpeed);
        compound.setFloat("acceleration", acceleration);
        return compound;
    }

    @Override
    public double getMaxRenderDistanceSquared()
    {
        return 16384;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox()
    {
        return INFINITE_EXTENT_AABB;
    }
}

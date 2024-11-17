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
import com.mrcrayfish.furniture.gui.containers.ContainerMicrowave;
import com.mrcrayfish.furniture.init.FurnitureSounds;
import com.mrcrayfish.furniture.util.ParticleSpawner;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;

import java.util.Random;

public class TileEntityMicrowave extends TileEntityFurniture implements ISidedInventory, ITickable
{
    private Random rand = new Random();

    private static final int[] slot = new int[]{0};

    private String customName;
    private boolean cooking = false;
    public int progress = 0;
    private int timer = 0;

    public TileEntityMicrowave()
    {
        super("microwave", 1);
    }

    public ItemStack getItem()
    {
        return getStackInSlot(0);
    }

    public void startCooking()
    {
        if(!getStackInSlot(0).isEmpty())
        {
            RecipeData data = RecipeAPI.getMicrowaveRecipeFromIngredients(getStackInSlot(0));
            if(data != null)
            {
                cooking = true;
                world.updateComparatorOutputLevel(pos, blockType);
            }
        }
    }

    public void stopCooking()
    {
        this.cooking = false;
        this.progress = 0;
        world.updateComparatorOutputLevel(pos, blockType);
    }

    public boolean isCooking()
    {
        return cooking;
    }

    @Override
    public void update()
    {
        if(cooking)
        {
            if(this.world.isRemote)
            {
                double posX = pos.getX() + 0.35D + (rand.nextDouble() / 3);
                double posZ = pos.getZ() + 0.35D + (rand.nextDouble() / 3);
                ParticleSpawner.spawnParticle("smoke", posX, pos.getY() + 0.065D, posZ);
            }

            progress++;
            if(progress >= 40)
            {
                if(!getStackInSlot(0).isEmpty())
                {
                    RecipeData data = RecipeAPI.getMicrowaveRecipeFromIngredients(getStackInSlot(0));
                    if(data != null)
                    {
                        this.setInventorySlotContents(0, data.getOutput().copy());
                    }
                }
                if(world.isRemote)
                {
                    world.playSound(pos.getX(), pos.getY(), pos.getZ(), FurnitureSounds.microwave_finish, SoundCategory.BLOCKS, 0.75F, 1.0F, true);
                }
                timer = 0;
                progress = 0;
                cooking = false;
                world.updateComparatorOutputLevel(pos, blockType);
            }
            else
            {
                if(timer == 20)
                {
                    timer = 0;
                }
                if(timer == 0)
                {
                    world.playSound(pos.getX(), pos.getY(), pos.getZ(), FurnitureSounds.microwave_running, SoundCategory.BLOCKS, 0.75F, 1.0F, true);
                }
                timer++;
            }
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tagCompound)
    {
        super.readFromNBT(tagCompound);
        this.cooking = tagCompound.getBoolean("Coooking");
        this.progress = tagCompound.getInteger("Progress");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tagCompound)
    {
        super.writeToNBT(tagCompound);
        tagCompound.setBoolean("Coooking", cooking);
        tagCompound.setInteger("Progress", progress);
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
    public int getInventoryStackLimit()
    {
        return 1;
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack)
    {
        return RecipeAPI.getMicrowaveRecipeFromIngredients(stack) != null;
    }

    @Override
    public int[] getSlotsForFace(EnumFacing side)
    {
        return slot;
    }

    @Override
    public boolean canInsertItem(int index, ItemStack stack, EnumFacing direction)
    {
        return RecipeAPI.getMicrowaveRecipeFromIngredients(stack) != null;
    }

    @Override
    public boolean canExtractItem(int index, ItemStack stack, EnumFacing direction)
    {
        return RecipeAPI.getMicrowaveRecipeFromIngredients(stack) == null;
    }

    @Override
    public Container createContainer(InventoryPlayer playerInventory, EntityPlayer playerIn)
    {
        this.fillWithLoot(playerIn);
        return new ContainerMicrowave(playerInventory, this);
    }
}

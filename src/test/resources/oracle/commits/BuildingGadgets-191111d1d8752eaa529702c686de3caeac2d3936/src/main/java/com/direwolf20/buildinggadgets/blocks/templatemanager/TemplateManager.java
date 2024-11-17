package com.direwolf20.buildinggadgets.blocks.templatemanager;

import com.direwolf20.buildinggadgets.BuildingGadgets;
import com.direwolf20.buildinggadgets.items.CopyPasteTool;
import com.direwolf20.buildinggadgets.items.Template;
import com.direwolf20.buildinggadgets.network.PacketBlockMap;
import com.direwolf20.buildinggadgets.network.PacketHandler;
import com.direwolf20.buildinggadgets.tools.BlockMapWorldSave;
import com.direwolf20.buildinggadgets.tools.TemplateWorldSave;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TemplateManager extends Block {
    public static final int GUI_ID = 1;

    public TemplateManager() {
        super(Material.ROCK);
        setHardness(2.0f);
        setUnlocalizedName(BuildingGadgets.MODID + ".templatemanager");
        setRegistryName("templatemanager");
    }

    @SideOnly(Side.CLIENT)
    public void initModel() {
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(this), 0, new ModelResourceLocation(getRegistryName(), "inventory"));
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World worldin, IBlockState state) {
        return new TemplateManagerTileEntity();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        // Only execute on the server
        if (world.isRemote) {
            return true;
        }
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TemplateManagerTileEntity)) {
            return false;
        }
        TemplateManagerContainer container = ((TemplateManagerTileEntity) te).getContainer(player);
        for (int i = 0; i <= 1; i++) {
            ItemStack itemStack = container.getSlot(i).getStack();
            if (itemStack.getItem() instanceof CopyPasteTool) {
                String UUID = CopyPasteTool.getUUID(itemStack);
                if (!(UUID == null)) {
                    BlockMapWorldSave worldSave = BlockMapWorldSave.get(world);
                    NBTTagCompound tagCompound = worldSave.getCompoundFromUUID(UUID);
                    if (tagCompound != null) {
                        PacketHandler.INSTANCE.sendTo(new PacketBlockMap(tagCompound), (EntityPlayerMP) player);
                    }
                }
            } else if (itemStack.getItem() instanceof Template) {
                String UUID = Template.getUUID(itemStack);
                if (!(UUID == null)) {
                    TemplateWorldSave worldSave = TemplateWorldSave.get(world);
                    NBTTagCompound tagCompound = worldSave.getCompoundFromUUID(UUID);
                    if (tagCompound != null) {
                        PacketHandler.INSTANCE.sendTo(new PacketBlockMap(tagCompound), (EntityPlayerMP) player);
                    }
                }
            }
        }
        player.openGui(BuildingGadgets.instance, GUI_ID, world, pos.getX(), pos.getY(), pos.getZ());
        return true;
    }
}

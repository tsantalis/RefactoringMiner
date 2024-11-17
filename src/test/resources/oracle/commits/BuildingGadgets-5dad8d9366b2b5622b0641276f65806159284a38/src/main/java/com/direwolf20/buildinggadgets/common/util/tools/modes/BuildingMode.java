package com.direwolf20.buildinggadgets.common.util.tools.modes;

import com.direwolf20.buildinggadgets.api.building.IBuildingMode;
import com.direwolf20.buildinggadgets.common.config.Config;
import com.direwolf20.buildinggadgets.common.util.GadgetUtils;
import com.direwolf20.buildinggadgets.common.util.blocks.BlockMap;
import com.direwolf20.buildinggadgets.common.util.helpers.NBTHelper;
import com.direwolf20.buildinggadgets.common.util.ref.Reference;
import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.doubles.Double2ObjectArrayMap;
import it.unimi.dsi.fastutil.doubles.Double2ObjectMap;
import it.unimi.dsi.fastutil.doubles.DoubleRBTreeSet;
import it.unimi.dsi.fastutil.doubles.DoubleSortedSet;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;
import java.util.function.BiPredicate;

public enum BuildingMode {
    TARGETED_AXIS_CHASING("build_to_me.png", new BuildToMeMode(BuildingMode::combineTester)),
    VERTICAL_COLUMN("vertical_column.png", new BuildingVerticalColumnMode(BuildingMode::combineTester)),
    HORIZONTAL_COLUMN("horizontal_column.png", new BuildingHorizontalColumnMode(BuildingMode::combineTester)),
    VERTICAL_WALL("vertical_wall.png", new VerticalWallMode(BuildingMode::combineTester)),
    HORIZONTAL_WALL("horizontal_wall.png", new HorizontalWallMode(BuildingMode::combineTester)),
    STAIR("stairs.png", new StairMode(BuildingMode::combineTester)),
    GRID("grid.png", new GridMode(BuildingMode::combineTester)),
    SURFACE("surface.png", new BuildingSurfaceMode(BuildingMode::combineTester));
    private static final BuildingMode[] VALUES = values();
    private final ResourceLocation icon;
    private final IBuildingMode modeImpl;

    BuildingMode(String iconFile, IBuildingMode modeImpl) {
        this.icon = new ResourceLocation(Reference.MODID, "textures/gui/mode/" + iconFile);
        this.modeImpl = modeImpl;
    }

    public ResourceLocation getIcon() {
        return icon;
    }

    public IBuildingMode getModeImplementation() {
        return modeImpl;
    }

    public String getRegistryName() {
        return getModeImplementation().getRegistryName().toString() + "/BuildingGadget";
    }

    @Override
    public String toString() {
        return getModeImplementation().getLocalized();
    }

    public BuildingMode next() {
        return VALUES[(this.ordinal() + 1) % VALUES.length];
    }

    public static List<BlockPos> collectPlacementPos(World world, EntityPlayer player, BlockPos hit, EnumFacing sideHit, ItemStack tool, BlockPos initial) {
        IBuildingMode mode = byName(NBTHelper.getOrNewTag(tool).getString("mode")).getModeImplementation();
        return mode.createExecutionContext(player, hit, sideHit, tool)
                .collectFilteredSequence(world, tool, player, initial);
    }

    public static BuildingMode byName(String name) {
        return Arrays.stream(VALUES)
                .filter(mode -> mode.getRegistryName().equals(name))
                .findFirst()
                .orElse(TARGETED_AXIS_CHASING);
    }

    private static final ImmutableList<ResourceLocation> ICONS = Arrays.stream(values())
            .map(BuildingMode::getIcon)
            .collect(ImmutableList.toImmutableList());

    public static ImmutableList<ResourceLocation> getIcons() {
        return ICONS;
    }

    public static BiPredicate<BlockPos, IBlockState> combineTester(World world, ItemStack tool, EntityPlayer player, BlockPos initial) {
        IBlockState target = GadgetUtils.getToolBlock(tool);
        return (pos, state) -> {
            IBlockState current = world.getBlockState(pos);
            BlockItemUseContext context = new BlockItemUseContext(world,player,tool,initial,EnumFacing.DOWN,0,0,0);
            if (!target.isReplaceable(context))
                return false;
            if (pos.getY() < 0)
                return false;
            if (Config.GENERAL.allowOverwriteBlocks.get())
                return current.isReplaceable(context);
            return current.getBlock().isAir(current, world, pos);
        };
    }

    //  TODO: Fix this
    private static boolean isReplaceable(World world, EntityPlayer player, BlockPos pos, IBlockState setBlock) {
        if (! setBlock.isValidPosition(world, pos)) {
            return false;
        }
        if (pos.getY() < 0) {
            return false;
        }
        if (Config.GENERAL.allowOverwriteBlocks.get() && !world.getBlockState(pos).isReplaceable(
                new BlockItemUseContext(world, player, new ItemStack(setBlock.getBlock()), pos, EnumFacing.UP, 0.5F, 0.0F, 0.5F))) {
            return false;
        }
        return true;
    }

    public static List<BlockMap> sortMapByDistance(List<BlockMap> unSortedMap, EntityPlayer player) {//TODO unused
        List<BlockPos> unSortedList = new ArrayList<>();
        Map<BlockPos, IBlockState> PosToStateMap = new HashMap<>();
        Map<BlockPos, Integer> PosToX = new HashMap<>();
        Map<BlockPos, Integer> PosToY = new HashMap<>();
        Map<BlockPos, Integer> PosToZ = new HashMap<>();
        for (BlockMap blockMap : unSortedMap) {
            PosToStateMap.put(blockMap.pos, blockMap.state);
            PosToX.put(blockMap.pos, blockMap.xOffset);
            PosToY.put(blockMap.pos, blockMap.yOffset);
            PosToZ.put(blockMap.pos, blockMap.zOffset);
            unSortedList.add(blockMap.pos);
        }
        List<BlockMap> sortedMap = new ArrayList<BlockMap>();
        Double2ObjectMap<BlockPos> rangeMap = new Double2ObjectArrayMap<>(unSortedList.size());
        DoubleSortedSet distances = new DoubleRBTreeSet();
        double x = player.posX;
        double y = player.posY + player.getEyeHeight();
        double z = player.posZ;
        for (BlockPos pos : unSortedList) {
            double distance = pos.distanceSqToCenter(x, y, z);
            rangeMap.put(distance, pos);
            distances.add(distance);
        }
        for (double dist : distances) {
            //System.out.println(dist);
            BlockPos pos = new BlockPos(rangeMap.get(dist));
            sortedMap.add(new BlockMap(pos, PosToStateMap.get(pos), PosToX.get(pos), PosToY.get(pos), PosToZ.get(pos)));
        }
        //System.out.println(unSortedList);
        //System.out.println(sortedList);
        return sortedMap;
    }

}

package crazypants.enderio.conduit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import crazypants.enderio.base.Log;
import crazypants.enderio.base.conduit.ConduitUtil;
import crazypants.enderio.base.conduit.IConduit;
import crazypants.enderio.base.conduit.IConduitBundle;
import crazypants.enderio.base.diagnostics.ConduitNeighborUpdateTracker;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;

//I=base type, I is the base class of the implementations accepted by the network 
public abstract class AbstractConduitNetwork<T extends IConduit, I extends T> {

  private final List<I> conduits = new ArrayList<I>();

  protected final Class<I> implClass;
  protected final Class<T> baseConduitClass;

  protected AbstractConduitNetwork(Class<I> implClass, Class<T> baseConduitClass) {
    this.implClass = implClass;
    this.baseConduitClass = baseConduitClass;
  }

  public void init(IConduitBundle tile, Collection<I> connections, World world) {

    if(world.isRemote) {
      throw new UnsupportedOperationException();
    }

    // Destroy all existing networks around this block
    for (I con : connections) {
      AbstractConduitNetwork<?, ?> network = con.getNetwork();
      if(network != null) {
        network.destroyNetwork();
      }
    }
    setNetwork(world, tile);
  }

  public final Class<T> getBaseConduitType() {
    return baseConduitClass;
  }

  protected void setNetwork(World world, IConduitBundle tile) {

    T conduit = tile.getConduit(getBaseConduitType());

    if(conduit != null && implClass.isAssignableFrom(conduit.getClass()) && conduit.setNetwork(this)) {
      addConduit(implClass.cast(conduit));
      TileEntity te = tile.getEntity();
      Collection<T> connections = ConduitUtil.getConnectedConduits(world, te.getPos(), getBaseConduitType());
      for (T con : connections) {
        if(con.getNetwork() == null) {
          setNetwork(world, con.getBundle());
        } else if(con.getNetwork() != this) {
          con.getNetwork().destroyNetwork();
          setNetwork(world, con.getBundle());
        }
      }
    }
  }

  public void addConduit(I newconduit) {
    if (conduits.isEmpty()) {
      ConduitNetworkTickHandler.instance.registerNetwork(this);
    }
    BlockPos newpos = null;
    boolean error = false;
    // Step 1: Is the new conduit attached to a TE that is valid?
    final IConduitBundle newbundle = newconduit.getBundle();
    if (newbundle == null) {
      Log.info("Tried to add invalid conduit to network: ", newconduit);
      error = true;
    } else {
      final TileEntity newte = newbundle.getEntity();
      if (!newte.hasworld()) {
        Log.info("Tried to add invalid (no world) conduit to network: ", newconduit);
        error = true;
      }
      if (newte.isInvalid()) {
        Log.info("Tried to add invalid (invalidated) conduit to network: ", newconduit);
        error = true;
      }
      newpos = newte.getPos();
      final World newworld = newte.getWorld();
      if (!newworld.isBlockLoaded(newpos)) {
        Log.info("Tried to add invalid (unloaded) conduit to network: ", newconduit);
        error = true;
      }
      if (newworld.getTileEntity(newte.getPos()) != newte) {
        Log.info("Tried to add invalid (world disagrees) conduit to network: ", newconduit);
        error = true;
      }
    }
    if (error) {
      new Exception("trace for message above").printStackTrace();
      return;
    }
    // Step 2: Check for duplicates and other errors
    List<I> old = new ArrayList<I>(conduits);
    conduits.clear();
    for (I oldconduit : old) {
      // Step 2.1: Fast skip if we have a real dupe
      if (newconduit == oldconduit) {
        continue;
      }
      // Step 2.2: Check if the old conduit's TE is valid
      final IConduitBundle oldbundle = oldconduit.getBundle();
      final TileEntity oldte = oldbundle.getEntity();
      if (oldte == null || oldte.isInvalid() || !oldte.hasworld()) {
        oldconduit.setNetwork(null);
        continue; // bad conduit, skip it
      }
      // Step 2.2b: Check if the target position is loaded
      final World oldworld = oldbundle.getBundleworld();
      final BlockPos oldpos = oldte.getPos();
      if (!oldworld.isBlockLoaded(oldpos)) {
        Log.info("Removed unloaded but valid conduit from network: " + oldconduit);
        oldconduit.setNetwork(null);
        continue; // bad conduit, skip it
      }
      // Step 2.3: Check if the old conduit's TE matches what its world has
      if (oldworld.getTileEntity(oldpos) != oldte) {
        oldconduit.setNetwork(null);
        continue; // bad conduit, skip it
      }
      // Step 2.4: Check if the new conduit is for the same position as the old. This should not happen, as the new conduit should have been gotten from the
      // world and the old conduit already was checked against the world...
      if (newpos != null && newpos.equals(oldpos)) {
        Log.info("Tried to add invalid conduit to network! Old conduit: ", oldconduit, "/", oldbundle, " New conduit: ", newconduit, "/", oldbundle,
            " World says: ", oldworld.getTileEntity(newpos));
        newconduit = null;
      }
      conduits.add(oldconduit);
    }
    // Step 3: Add the new conduit
    if (newconduit != null) {
      conduits.add(newconduit);
    }
  }

  public void destroyNetwork() {
    for (I con : conduits) {
      con.setNetwork(null);
    }
    conduits.clear();
    ConduitNetworkTickHandler.instance.unregisterNetwork(this);
  }

  public List<I> getConduits() {
    return conduits;
  }

  private static final EnumFacing[] WEDUNS = new EnumFacing[] { EnumFacing.WEST, EnumFacing.EAST, EnumFacing.DOWN, EnumFacing.UP, EnumFacing.NORTH,
      EnumFacing.SOUTH };

  public void sendBlockUpdatesForEntireNetwork() {
    ConduitNeighborUpdateTracker tracker = null;
    Set<BlockPos> notified = new HashSet<BlockPos>();
    for (I con : conduits) {
      TileEntity te = con.getBundle().getEntity();
      if (con.hasExternalConnections()) {
        final BlockPos pos = te.getPos();
        final Block blockType = te.getBlockType();
        final World world = te.getWorld();
        if (world.isBlockLoaded(pos)) {
          IBlockState bs = world.getBlockState(pos);
          if (tracker == null) {
            tracker = new ConduitNeighborUpdateTracker("Conduit network " + this.getClass() + " was interrupted while notifying neighbors of changes");
          }
          tracker.start("World.notifyBlockUpdate() at " + pos);
          world.notifyBlockUpdate(pos, bs, bs, 3);
          tracker.stop();

          // the following is a fancy version of world.notifyNeighborsOfStateChange(pos, blockType);

          // don't notify other conduits and don't notify the same block twice
          EnumSet<EnumFacing> sidesToNotify = EnumSet.noneOf(EnumFacing.class);
          for (EnumFacing side : WEDUNS) {
            final BlockPos offset = pos.offset(side);
            if (con.containsExternalConnection(side) && !notified.contains(offset) && world.isBlockLoaded(offset)) {
              IBlockState blockState = world.getBlockState(offset);
              if (blockState.getBlock() != blockType && blockState.getBlock() != Blocks.AIR) {
                sidesToNotify.add(side);
                notified.add(offset);
              }
            }
          }

          if (!sidesToNotify.isEmpty()) {
            tracker.start("ForgeEventFactory.onNeighborNotify() at " + pos);
            boolean canceled = ForgeEventFactory.onNeighborNotify(world, pos, bs, sidesToNotify).isCanceled();
            tracker.stop();

            if (!canceled) {
              for (EnumFacing side : WEDUNS) {
                if (sidesToNotify.contains(side)) {
                  final BlockPos offset = pos.offset(side);
                  tracker.start("World.notifyNeighborsOfStateChange() from " + pos + " to " + offset + " (" + world.getBlockState(offset) + ")");
                  world.notifyBlockOfStateChange(offset, blockType);
                  tracker.stop();
                }
              }
            }
          }
        }
      }
    }
    if (tracker != null) {
      tracker.discard();
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (IConduit con : conduits) {
      sb.append(con.getLocation());
      sb.append(", ");
    }
    return "AbstractConduitNetwork@" + Integer.toHexString(hashCode()) + " [conduits=" + sb.toString() + "]";
  }

  public void doNetworkTick(Profiler theProfiler) {
  }
}

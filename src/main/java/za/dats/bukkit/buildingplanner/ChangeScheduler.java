package za.dats.bukkit.buildingplanner;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.World;
import org.bukkit.block.Block;

import za.dats.bukkit.buildingplanner.model.BlockHelper;

/**
 * Schedule sets of changes to the world.
 * 
 * Not thread safe.
 * 
 * @author cmdrdats
 */
public class ChangeScheduler {
    private final int CHUNK_SIZE = 50;

    private static class Change {
	World world;
	int x, y, z;
	int state;

	public Change(World world, int x, int y, int z, int newState) {
	    this.world = world;
	    this.x = x;
	    this.y = y;
	    this.z = z;
	    state = newState;
	}

	public void apply() {
	    Block block = world.getBlockAt(x, y, z);
	    block.setType(BlockHelper.getMaterial(state));
	    block.setData(BlockHelper.getData(state));
	}
    }

    private static class ChangeExecutor implements Runnable {
	private final List<Change> changes;

	ChangeExecutor(List<Change> changes) {
	    this.changes = changes;
	}

	public void run() {
	    for (Change change : changes) {
		change.apply();
	    }
	}
    }

    List<Change> currentChunk = new ArrayList<Change>(CHUNK_SIZE);

    /**
     * Schedule a new change
     * 
     * @param world
     * @param x
     * @param y
     * @param z
     * @param newState
     * @return true if changes were committed
     */
    public void scheduleChange(World world, int x, int y, int z, int newState) {
	Change change = new Change(world, x, y, z, newState);
	currentChunk.add(change);

	if (currentChunk.size() >= CHUNK_SIZE) {
	    commit();
	}
    }

    public void commit() {
	if (currentChunk.size() == 0) {
	    return;
	}
	                               
	BuildingPlanner.plugin.getServer().getScheduler()
		.scheduleAsyncDelayedTask(BuildingPlanner.plugin, new ChangeExecutor(currentChunk), 3);
	currentChunk = new ArrayList<Change>(CHUNK_SIZE);

    }
}

package za.dats.bukkit.buildingplanner;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.World;

import za.dats.bukkit.buildingplanner.model.Change;
import za.dats.bukkit.buildingplanner.model.ChangeSet;


/**
 * Schedule sets of changes to the world.
 * 
 * Not thread safe.
 * 
 * @author cmdrdats
 */
public class ChangeScheduler {
    private final int CHUNK_SIZE = 50;

    ChangeSet changeSet = new ChangeSet(CHUNK_SIZE);

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
	changeSet.add(change);

	if (changeSet.size() >= CHUNK_SIZE) {
	    commit();
	}
    }

    public void commit() {
	if (changeSet.size() == 0) {
	    return;
	}

	changeSet.commit();
	changeSet = new ChangeSet(CHUNK_SIZE);
    }
}

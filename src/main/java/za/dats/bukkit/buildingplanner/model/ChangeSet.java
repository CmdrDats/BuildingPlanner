package za.dats.bukkit.buildingplanner.model;

import java.util.ArrayList;
import java.util.List;

import za.dats.bukkit.buildingplanner.BuildingPlanner;

public class ChangeSet implements Runnable {
    List<Change> changes;

    public ChangeSet() {
	this(10);
    }

    public ChangeSet(int initialSize) {
	changes = new ArrayList<Change>(initialSize);
    }

    public void add(Change change) {
	changes.add(change);
    }

    public int size() {
	return changes.size();
    }

    public void run() {
	for (Change change : changes) {
	    change.apply();
	}
    }

    public void commit() {
	BuildingPlanner.plugin.getServer().getScheduler().scheduleSyncDelayedTask(BuildingPlanner.plugin, this, 3);
    }
    
    public void undo() {
	BuildingPlanner.plugin.getServer().getScheduler()
	.scheduleSyncDelayedTask(BuildingPlanner.plugin, new Runnable() {
	    public void run() {
		undoChangeSet();
	    }
	}, 3);
	
    }

    protected void undoChangeSet() {
	for (Change change : changes) {
	    change.undo();
	}
    }
}

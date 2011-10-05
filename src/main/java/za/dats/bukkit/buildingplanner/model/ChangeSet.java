package za.dats.bukkit.buildingplanner.model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import za.dats.bukkit.buildingplanner.BuildingPlanner;
import za.dats.bukkit.buildingplanner.listeners.ChangeSetCompleteListener;

public class ChangeSet implements Runnable {
    Queue<Change> changes;
    private int taskId;
    ChangeSetCompleteListener listener;

    public ChangeSet(ChangeSetCompleteListener listener) {
	this.listener = listener;
	changes = new LinkedList<Change>();
    }

    public void add(Change change) {
	changes.offer(change);
    }

    public int size() {
	return changes.size();
    }

    public void run() {
	Change poll = changes.poll();
	if (poll == null) {
	    BuildingPlanner.plugin.getServer().getScheduler().cancelTask(taskId);
	    listener.complete();
	    return;
	}
	poll.apply();
    }

    public void commit() {
	if (changes.size() == 0) {
	    listener.complete();
	    return;
	}
	taskId = BuildingPlanner.plugin.getServer().getScheduler().scheduleSyncRepeatingTask(BuildingPlanner.plugin, this, 1, 1);
    }
}

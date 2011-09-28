package za.dats.bukkit.buildingplanner.listeners;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.SignChangeEvent;

import za.dats.bukkit.buildingplanner.BuildingPlanner;
import za.dats.bukkit.buildingplanner.model.PlanArea;

public class PlanAreaDestroyListener extends BlockListener {
    ArrayList<PlanAreaListener> listeners = new ArrayList<PlanAreaListener>();
    private final List<PlanArea> planAreas;

    public PlanAreaDestroyListener(List<PlanArea> planAreas) {
	this.planAreas = planAreas;
    }

    @Override
    public void onBlockBreak(BlockBreakEvent event) {
	if (event.isCancelled()) {
	    return;
	}
	List<PlanArea> areas = new ArrayList<PlanArea>(planAreas);
	for (PlanArea area : areas) {
	    if (area.isFloorBlock(event.getBlock())) {
		event.setCancelled(true);
		return;
	    }
	    
	    if (area.fenceContains(event.getBlock()) || event.getBlock().equals(area.getSignBlock())) {
		if (!event.getPlayer().hasPermission("buildingplanner.destroy")) {
		    event.getPlayer().sendMessage("You do not have permission to destroy a planning area");
		    event.setCancelled(true);
		    return;
		}
		
		BuildingPlanner.info("Destroying Plan");
		area.restoreOriginalBlocks(false);
		
		for (PlanAreaListener listener : listeners) {
		    listener.destroy(area);
		}
		continue;
	    }
	}
    }
    
    public void addListener(PlanAreaListener listener) {
	listeners.add(listener);
    }
}

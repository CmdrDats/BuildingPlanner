package za.dats.bukkit.buildingplanner.listeners;

import org.bukkit.event.entity.EntityListener;
import org.bukkit.event.entity.ItemSpawnEvent;

import za.dats.bukkit.buildingplanner.BuildingPlanner;
import za.dats.bukkit.buildingplanner.model.PlanArea;

public class PlanItemListener extends EntityListener {
    @Override
    public void onItemSpawn(ItemSpawnEvent event) {
	PlanArea area = BuildingPlanner.plugin.areaManager.getAffectedArea(event.getLocation().getBlock());
	if (area == null) {
	    return;
	}

	// No restrictions if the area is committed.
	if (area.isCommitted()) {
	    return;
	}
	
	if (!area.isPlannedBlock(event.getLocation().getBlock())) {
	    return;
	}
	
	
	// Disable all item spawns in plan blocks
	event.setCancelled(true);
	//System.out.println("Spawning item: "+event.getType());
        super.onItemSpawn(event);
    }
}

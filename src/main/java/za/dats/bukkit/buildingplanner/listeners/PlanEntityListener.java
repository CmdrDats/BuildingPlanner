package za.dats.bukkit.buildingplanner.listeners;

import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import za.dats.bukkit.buildingplanner.BuildingPlanner;
import za.dats.bukkit.buildingplanner.Config;
import za.dats.bukkit.buildingplanner.model.PlanArea;
import za.dats.bukkit.buildingplanner.model.PlanArea.OpType;

public class PlanEntityListener extends EntityListener {
    @Override
    public void onEntityDamage(EntityDamageEvent event) {
	if (!Config.isProtectedFromFalling()) {
	    return;
	}
	if (!(event.getEntity() instanceof Player)) {
	    return;
	}
	Player player = (Player) event.getEntity();

	if (!event.getCause().equals(DamageCause.FALL)) {
	    return;
	}

	PlanArea area = BuildingPlanner.plugin.areaManager.getAffectedArea(event.getEntity().getLocation().getBlock());
	if (area == null) {
	    return;
	}

	if (area.isCommitted()) {
	    return;
	}

	if (!area.checkPermission(player, OpType.MODIFY)) {
	    event.setCancelled(true);
	    return;
	}

	event.setCancelled(true);
    }

    @Override
    public void onEntityExplode(EntityExplodeEvent event) {
	List<Block> blockList = event.blockList();

	for (Block block : blockList) {
	    PlanArea area = BuildingPlanner.plugin.areaManager.getAffectedArea(block);
	    if (area == null) {
		continue;
	    }
	    
	    if (area.isCommitted()) {
		continue;
	    }
	    
	    if (area.isPlannedBlock(block)) {
		event.setCancelled(true);
	    }
	}
	super.onEntityExplode(event);
    }
}

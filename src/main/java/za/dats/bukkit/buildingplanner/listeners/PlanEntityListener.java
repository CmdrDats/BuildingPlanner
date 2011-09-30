package za.dats.bukkit.buildingplanner.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import za.dats.bukkit.buildingplanner.BuildingPlanner;
import za.dats.bukkit.buildingplanner.Config;
import za.dats.bukkit.buildingplanner.model.PlanArea;

public class PlanEntityListener extends EntityListener {
    @Override
    public void onEntityDamage(EntityDamageEvent event) {
	if (!Config.isProtectedFromFalling()) {
	    return;
	}
	if (!(event.getEntity() instanceof Player)) {
	    return;
	}

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

	event.setCancelled(true);
    }
}

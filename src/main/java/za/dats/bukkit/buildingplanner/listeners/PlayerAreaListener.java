package za.dats.bukkit.buildingplanner.listeners;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Type;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

import za.dats.bukkit.buildingplanner.BuildingPlanner;
import za.dats.bukkit.buildingplanner.model.PlanArea;

public class PlayerAreaListener extends PlayerListener {
    private final List<PlanArea> planAreas;

    public PlayerAreaListener(List<PlanArea> planAreas) {
	this.planAreas = planAreas;
    }

    private PlanArea getAffectedArea(Location location) {
	for (PlanArea area : planAreas) {

	    if (area.isInside(location)) {
		return area;
	    }
	}

	return null;
    }

    @Override
    public void onPlayerInteract(PlayerInteractEvent event) {
	if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
	    return;
	}

	PlanArea area = getAffectedArea(event.getClickedBlock().getLocation());
	if (area == null) {
	    return;
	}
	if (area.getSignBlock() != event.getClickedBlock()) {
	    return;
	}

	Date clickDate = new Date();
	if (area.isCommitted()) {
	    if (area.getCommitAttemptTime() != null
		    && (clickDate.getTime() - area.getCommitAttemptTime().getTime() < 5000)
		    && area.getCommitPlayer() == event.getPlayer()) {
		area.setCommitAttemptTime(null);
		area.setCommitPlayer(null);
		area.unCommit();
		return;
	    }

	    area.reportPlan(event.getPlayer());
	    if (event.getPlayer().hasPermission("buildingplanner.uncommit")) {
		area.setCommitAttemptTime(clickDate);
		area.setCommitPlayer(event.getPlayer());
		event.getPlayer().sendMessage("To view this plan, right click the sign again within 5 seconds.");
	    }
	    return;
	}

	if (area.getCommitAttemptTime() != null && (clickDate.getTime() - area.getCommitAttemptTime().getTime() < 5000)
		&& area.getCommitPlayer() == event.getPlayer()) {
	    area.commit();
	    area.setCommitAttemptTime(null);
	    area.setCommitPlayer(null);
	    event.getPlayer().sendMessage("Area committed. Materials in supply chest will be used to build plan!");
	    return;
	}

	area.reportPlan(event.getPlayer());

	if (event.getPlayer().hasPermission("buildingplanner.commit")) {
	    area.setCommitAttemptTime(clickDate);
	    area.setCommitPlayer(event.getPlayer());
	    event.getPlayer().sendMessage("To commit this plan, right click the sign again within 5 seconds.");
	}
    }

}

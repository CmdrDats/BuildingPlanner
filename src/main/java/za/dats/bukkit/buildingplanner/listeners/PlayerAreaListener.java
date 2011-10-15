package za.dats.bukkit.buildingplanner.listeners;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.GameMode;
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
import za.dats.bukkit.buildingplanner.Config;
import za.dats.bukkit.buildingplanner.model.PlanArea;
import za.dats.bukkit.buildingplanner.model.PlanArea.OpType;

public class PlayerAreaListener extends PlayerListener {
    @Override
    public void onPlayerInteract(PlayerInteractEvent event) {
	if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
	    return;
	}

	PlanArea area = BuildingPlanner.plugin.areaManager.getAffectedArea(event.getClickedBlock());
	if (area == null) {
	    return;
	}

	ItemStack item = event.getItem();
	if (item != null) {
	    boolean allowed = true;
	    if (item.getType().equals(Material.LAVA_BUCKET)
		    && !BuildingPlanner.plugin.areaManager.validBlockType(Material.LAVA)) {
		allowed = false;
	    }

	    if (item.getType().equals(Material.WATER_BUCKET)
		    && !BuildingPlanner.plugin.areaManager.validBlockType(Material.WATER)) {
		allowed = false;
	    }
	    if (!allowed) {
		event.getPlayer().sendMessage("Cannot use this item in a planning area.");
		event.setCancelled(true);
		return;
	    }
	}

	switch (event.getClickedBlock().getType()) {
	case CAKE_BLOCK:
	case BED_BLOCK:
	case CHEST:
	case FURNACE:
	case BURNING_FURNACE:
	case LOCKED_CHEST:
	case WORKBENCH:
	    if (!area.isCommitted() && area.isPlannedBlock(event.getClickedBlock())) {
		event.setCancelled(true);
		return;
	    }
	}

	if (area.getSignBlock() != event.getClickedBlock()) {
	    return;
	}

	if (area.isLocked()) {
	    event.getPlayer().sendMessage("Area is locked: " + area.getLockReason());
	    event.setCancelled(true);
	    return;
	}

	event.setCancelled(true);
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
	    if (area.checkPermission(event.getPlayer(), OpType.UNCOMMIT, true)) {
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

	if (area.checkPermission(event.getPlayer(), OpType.COMMIT, true)) {
	    area.setCommitAttemptTime(clickDate);
	    area.setCommitPlayer(event.getPlayer());
	    event.getPlayer().sendMessage("To commit this plan, right click the sign again within 5 seconds.");
	}
    }

    public void onPlayerMove(PlayerMoveEvent event) {
	if (event.isCancelled()) {
	    return;
	}
	if (!Config.isCreativeModeEnabled()) {
	    return;
	}
	PlanArea fromArea = BuildingPlanner.plugin.areaManager.getAffectedArea(event.getFrom());

	PlanArea toArea = BuildingPlanner.plugin.areaManager.getAffectedArea(event.getTo());

	if (fromArea == null && toArea != null) {
	    event.getPlayer().setGameMode(GameMode.CREATIVE);
	} else if (fromArea != null && toArea == null) {
	    event.getPlayer().setGameMode(GameMode.SURVIVAL);

	}
    };

}

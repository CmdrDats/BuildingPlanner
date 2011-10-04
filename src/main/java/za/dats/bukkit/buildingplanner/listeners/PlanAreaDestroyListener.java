package za.dats.bukkit.buildingplanner.listeners;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.SignChangeEvent;

import za.dats.bukkit.buildingplanner.BuildingPlanner;
import za.dats.bukkit.buildingplanner.Config;
import za.dats.bukkit.buildingplanner.model.PlanArea;
import za.dats.bukkit.buildingplanner.model.PlanArea.OpType;

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
	    if (Config.isFloorInvincible() && area.isFloorBlock(event.getBlock())) {
		event.setCancelled(true);
		return;
	    }

	    if (area.fenceContains(event.getBlock()) || event.getBlock().equals(area.getSignBlock())) {
		if (!area.checkPermission(event.getPlayer(), OpType.DESTROY)) {
		    event.setCancelled(true);
		    return;
		}

		if (area.isLocked()) {
		    event.getPlayer().sendMessage("Area is locked: " + area.getLockReason());
		    event.setCancelled(true);
		    return;
		}

		BuildingPlanner.info("Destroying Plan");
		final PlanArea destroyArea = area;
		Thread planDestroyThread = new Thread("Plan destroy thread") {
		    public void run() {
			destroyArea.destroyArea();

			for (PlanAreaListener listener : listeners) {
			    listener.destroy(destroyArea);
			}
		    };
		};
		planDestroyThread.setDaemon(true);
		planDestroyThread.start();
		continue;
	    }
	}
    }

    public void addListener(PlanAreaListener listener) {
	listeners.add(listener);
    }
}

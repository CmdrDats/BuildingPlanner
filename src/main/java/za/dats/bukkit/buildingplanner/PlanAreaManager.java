package za.dats.bukkit.buildingplanner;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;

import za.dats.bukkit.buildingplanner.listeners.PlanAreaCreationListener;
import za.dats.bukkit.buildingplanner.listeners.PlanAreaDestroyListener;
import za.dats.bukkit.buildingplanner.listeners.PlanAreaListener;
import za.dats.bukkit.buildingplanner.listeners.PlanAreaModificationListener;
import za.dats.bukkit.buildingplanner.listeners.PlanEntityListener;
import za.dats.bukkit.buildingplanner.listeners.PlanItemListener;
import za.dats.bukkit.buildingplanner.listeners.PlayerAreaListener;
import za.dats.bukkit.buildingplanner.model.PlanArea;

public class PlanAreaManager implements PlanAreaListener {
    List<PlanArea> planAreas = new ArrayList<PlanArea>();

    public void init() {
	PlanAreaCreationListener areaCreationListener = new PlanAreaCreationListener();
	areaCreationListener.addListener(this);
	BuildingPlanner.pm.registerEvent(Type.SIGN_CHANGE, areaCreationListener, Priority.Normal,
		BuildingPlanner.plugin);

	PlanAreaDestroyListener areaDestroyListener = new PlanAreaDestroyListener(planAreas);
	areaDestroyListener.addListener(this);
	BuildingPlanner.pm
		.registerEvent(Type.BLOCK_BREAK, areaDestroyListener, Priority.Normal, BuildingPlanner.plugin);

	PlanAreaModificationListener areaModifyListener = new PlanAreaModificationListener();
	BuildingPlanner.pm.registerEvent(Type.BLOCK_BREAK, areaModifyListener, Priority.Normal, BuildingPlanner.plugin);
	BuildingPlanner.pm.registerEvent(Type.BLOCK_IGNITE, areaModifyListener, Priority.Normal, BuildingPlanner.plugin);
	BuildingPlanner.pm.registerEvent(Type.BLOCK_PHYSICS, areaModifyListener, Priority.Normal,
		BuildingPlanner.plugin);
	BuildingPlanner.pm
		.registerEvent(Type.BLOCK_DAMAGE, areaModifyListener, Priority.Normal, BuildingPlanner.plugin);
	BuildingPlanner.pm.registerEvent(Type.BLOCK_PLACE, areaModifyListener, Priority.Normal, BuildingPlanner.plugin);

	
	PlayerAreaListener movementListener = new PlayerAreaListener();
	BuildingPlanner.pm.registerEvent(Type.PLAYER_INTERACT, movementListener, Priority.Normal,
		BuildingPlanner.plugin);
	BuildingPlanner.pm.registerEvent(Type.ITEM_SPAWN, new PlanItemListener(), Priority.Normal,
		BuildingPlanner.plugin);

	BuildingPlanner.pm.registerEvent(Type.ENTITY_DAMAGE, new PlanEntityListener(), Priority.Normal,
		BuildingPlanner.plugin);
	BuildingPlanner.pm.registerEvent(Type.ENTITY_EXPLODE, new PlanEntityListener(), Priority.Normal,
		BuildingPlanner.plugin);

	Thread supplyCheckThread = new Thread("PlanAreaSupplyThread") {
	    @Override
	    public void run() {
		while (BuildingPlanner.plugin.isEnabled()) {
		    try {
			if (checkAreas()) {
			    // Try build again sooner if block has been built
			    Thread.sleep(500);
			} else {
			    Thread.sleep(5000);
			}
			// Catch all throwables so that the thread doesn't die if something breaks.
		    } catch (Throwable e) {
			e.printStackTrace();
		    }

		    try {
			saveAreas();
			// Catch all throwables so that the thread doesn't die if something breaks.
		    } catch (Throwable e) {
			e.printStackTrace();
		    }

		}
	    }
	};
	supplyCheckThread.setDaemon(true);
	supplyCheckThread.start();

	Thread saveThread = new Thread("PlanAreaSave") {
	    @Override
	    public void run() {
		while (BuildingPlanner.plugin.isEnabled()) {

		    try {
			saveAreas();
			Thread.sleep(1000);
			// Catch all throwables so that the thread doesn't die if something breaks.
		    } catch (Throwable e) {
			e.printStackTrace();
		    }

		}
	    }
	};
	saveThread.setDaemon(true);
	saveThread.start();

	File[] areaList = BuildingPlanner.plugin.getDataFolder().listFiles(new FilenameFilter() {

	    public boolean accept(File dir, String name) {
		if (name.endsWith(".area")) {
		    return true;
		}
		return false;
	    }
	});

	for (File file : areaList) {
	    PlanArea area = new PlanArea();
	    area.loadArea(file);
	    planAreas.add(area);
	}
    }

    protected void saveAreas() {
	for (PlanArea area : planAreas) {
	    try {
		area.trySave();
		// If one area breaks, we don't want the rest to not save..
	    } catch (Throwable e) {
		e.printStackTrace();
	    }
	}
    }

    protected boolean checkAreas() {
	boolean result = false;
	for (PlanArea area : planAreas) {
	    if (!area.isCommitted()) {
		continue;
	    }

	    if (area.buildFromSupply(1)) {
		result = true;
	    }
	}

	return result;
    }

    public void create(PlanArea area) {
	planAreas.add(area);
	area.saveArea();
    }

    public void destroy(PlanArea area) {
	planAreas.remove(area);
	area.deleteArea();
    }

    public PlanArea getAffectedArea(Block block) {
	for (PlanArea area : planAreas) {
	    /*
	     * if (includeSignAndFence && (area.fenceContains(block) || block.equals(area.getSignBlock()))) { return
	     * null; }
	     */

	    if (area.isInside(block)) {
		return area;
	    }
	}

	return null;
    }

    public PlanArea getAreaByName(String areaName) {
	for (PlanArea area : planAreas) {
	    if (areaName.equals(area.getName())) {
		return area;
	    }
	}
	return null;
    }

    public List<PlanArea> getPlanList() {
	return Collections.unmodifiableList(planAreas);
    }
}

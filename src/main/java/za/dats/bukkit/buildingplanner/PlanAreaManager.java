package za.dats.bukkit.buildingplanner;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;

import za.dats.bukkit.buildingplanner.listeners.PlanAreaCreationListener;
import za.dats.bukkit.buildingplanner.listeners.PlanAreaDestroyListener;
import za.dats.bukkit.buildingplanner.listeners.PlanAreaListener;
import za.dats.bukkit.buildingplanner.listeners.PlanAreaModificationListener;
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

	PlanAreaModificationListener areaModifyListener = new PlanAreaModificationListener(planAreas);
	BuildingPlanner.pm.registerEvent(Type.BLOCK_BREAK, areaModifyListener, Priority.Normal, BuildingPlanner.plugin);
	BuildingPlanner.pm.registerEvent(Type.BLOCK_PHYSICS, areaModifyListener, Priority.Normal,BuildingPlanner.plugin);
	BuildingPlanner.pm
		.registerEvent(Type.BLOCK_DAMAGE, areaModifyListener, Priority.Normal, BuildingPlanner.plugin);
	BuildingPlanner.pm.registerEvent(Type.BLOCK_PLACE, areaModifyListener, Priority.Normal, BuildingPlanner.plugin);

	PlayerAreaListener movementListener = new PlayerAreaListener(planAreas);
	BuildingPlanner.pm.registerEvent(Type.PLAYER_INTERACT, movementListener, Priority.Normal,
		BuildingPlanner.plugin);

	Thread supplyCheckThread = new Thread("PlanAreaSupplyChestCheck") {
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
		}
	    }
	};
	supplyCheckThread.setDaemon(true);
	supplyCheckThread.start();
	
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
}

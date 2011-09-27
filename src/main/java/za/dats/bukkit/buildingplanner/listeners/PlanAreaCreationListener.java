package za.dats.bukkit.buildingplanner.listeners;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.SignChangeEvent;

import za.dats.bukkit.buildingplanner.BuildingPlanner;
import za.dats.bukkit.buildingplanner.Config;
import za.dats.bukkit.buildingplanner.model.PlanArea;

public class PlanAreaCreationListener extends BlockListener {
    ArrayList<PlanAreaListener> listeners = new ArrayList<PlanAreaListener>();

    public BlockFace getRotated(BlockFace face, boolean left) {
	BlockFace result = null;

	switch (face) {
	case EAST_NORTH_EAST:
	case EAST_SOUTH_EAST:
	case EAST:
	    result = BlockFace.SOUTH;
	    break;

	case WEST_NORTH_WEST:
	case WEST_SOUTH_WEST:
	case WEST:
	    result = BlockFace.NORTH;
	    break;

	case NORTH_EAST:
	case NORTH_WEST:
	case NORTH_NORTH_EAST:
	case NORTH_NORTH_WEST:
	case NORTH:
	    result = BlockFace.EAST;
	    break;

	case SOUTH_EAST:
	case SOUTH_WEST:
	case SOUTH_SOUTH_EAST:
	case SOUTH_SOUTH_WEST:
	case SOUTH:
	    result = BlockFace.WEST;
	    break;

	default:
	    return null;

	}

	if (left) {
	    return result.getOppositeFace();
	}

	return result;
    }

    private void traverse(PlanArea area, Block block) {
	if (!Material.FENCE.equals(block.getType())) {
	    return;
	}

	if (area.fenceContains(block)) {
	    return;
	}

	area.add(block);

	traverse(area, block.getRelative(BlockFace.EAST));
	traverse(area, block.getRelative(BlockFace.WEST));
	traverse(area, block.getRelative(BlockFace.NORTH));
	traverse(area, block.getRelative(BlockFace.SOUTH));
	traverse(area, block.getRelative(BlockFace.UP));
	traverse(area, block.getRelative(BlockFace.DOWN));
    }

    @Override
    public void onSignChange(SignChangeEvent event) {
	BuildingPlanner.debug("Processing sign change event");
	if (event.isCancelled()) {
	    return;
	}

	BuildingPlanner.debug("Handling non cancelled");
	if (!(event.getBlock().getState() instanceof Sign)) {
	    return;
	}
	Sign sign = (Sign) event.getBlock().getState();
	if (!"[Plan]".equals(event.getLine(0))) {
	    return;
	}

	if (!event.getPlayer().hasPermission("buildingplanner.create")) {
	    event.getPlayer().sendMessage("You do not have permission to create a planning area");
	    event.setCancelled(true);
	    return;
	}

	org.bukkit.material.Sign aSign = (org.bukkit.material.Sign) sign.getData();
	BlockFace left = getRotated(aSign.getFacing(), true);
	BlockFace right = getRotated(aSign.getFacing(), false);

	// Ignore if sign not placed correctly somehow.
	if (left == null || right == null) {
	    sign.setLine(1, BuildingPlanner.color("&CIncorrect Boundaries"));
	    sign.update(true);
	    return;
	}

	PlanArea area = new PlanArea();
	area.set(event.getBlock());
	area.setSignBlock(event.getBlock());

	Block leftBlock = sign.getBlock().getRelative(left);
	Block rightBlock = sign.getBlock().getRelative(right);

	if (!(Material.FENCE.equals(leftBlock.getType()) && Material.FENCE.equals(rightBlock.getType()))) {
	    event.setLine(1, BuildingPlanner.color("&CNeed fence to"));
	    event.setLine(2, BuildingPlanner.color("&Cleft and right"));
	    return;
	}
	traverse(area, leftBlock);

	if (!area.fenceContains(rightBlock)) {
	    event.setLine(1, BuildingPlanner.color("&CBoundaries not closed"));
	    return;
	}

	try {
	    area.setMaxY(area.getMinY() + Integer.parseInt(event.getLine(1)));
	} catch (NumberFormatException e) {
	    if (Config.getDefaultHeight() != 0) {
		area.setMaxY(Config.getDefaultHeight());
	    } else {
		// Add the middle amount between the z and x size as an extra y size
		// Seems like a good height measure
		int extra = area.getMaxX() - area.getMinX() + area.getMaxZ() - area.getMinZ();
		area.setMaxY(area.getMaxY() + (extra / 2));
	    }
	}

	area.init();

	event.setLine(2, BuildingPlanner.color("&EOK"));

	for (PlanAreaListener listener : listeners) {
	    listener.create(area);
	}
    }

    public void addListener(PlanAreaListener listener) {
	listeners.add(listener);
    }
}

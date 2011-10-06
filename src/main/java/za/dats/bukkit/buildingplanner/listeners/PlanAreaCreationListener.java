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

	area.add(block, true);

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
	area.setName(event.getLine(1));
	area.set(event.getBlock());
	area.setSignBlock(event.getBlock());
	area.setOwner(event.getPlayer().getName());

	Block leftBlock = sign.getBlock().getRelative(left);
	Block rightBlock = sign.getBlock().getRelative(right);

	if (!setSizeFromLine(area, event.getLine(2), left, right, aSign.getFacing().getOppositeFace())) {
	    if (!(Material.FENCE.equals(leftBlock.getType()) && Material.FENCE.equals(rightBlock.getType()))) {
		event.setLine(1, BuildingPlanner.color("Need fence to"));
		event.setLine(2, BuildingPlanner.color("left and right"));
		return;
	    }
	    traverse(area, leftBlock);

	    if (!area.fenceContains(rightBlock)) {
		event.setLine(1, BuildingPlanner.color("&CBoundaries"));
		event.setLine(2, BuildingPlanner.color("&Cnot closed"));
		return;
	    }

	    int maxY = area.getMaxY();
	    try {
		maxY = area.getMinY() + Integer.parseInt(event.getLine(2));
	    } catch (NumberFormatException e) {
		maxY = getDefaultHeight(area);
	    }

	    area.setMaxY(maxY);
	}

	if ((area.getMaxX() - area.getMinX() > Config.getMaxSize())
		|| (area.getMaxZ() - area.getMinZ() > Config.getMaxSize())
		|| (area.getMaxY() - area.getMinY() > Config.getMaxHeight())) {

	    event.setLine(1, BuildingPlanner.color("&CArea too"));
	    event.setLine(2, BuildingPlanner.color("&Clarge"));
	    return;
	}

	area.init();

	event.setLine(2, BuildingPlanner.color("&EOK"));

	for (PlanAreaListener listener : listeners) {
	    listener.create(area);
	}
    }

    private int getDefaultHeight(PlanArea area) {
	int maxY;
	if (Config.getDefaultHeight() != 0) {
	    maxY = Math.max(area.getMinY() + 1 + Config.getDefaultHeight(), area.getMaxY());
	} else {
	    // Add the middle amount between the z and x size as an extra y size
	    // Seems like a good height measure
	    int extra = area.getMaxX() - area.getMinX() + area.getMaxZ() - area.getMinZ();
	    maxY = area.getMaxY() + (extra / 2);
	}
	return maxY;
    }

    private boolean setSizeFromLine(PlanArea area, String line, BlockFace leftFace, BlockFace rightFace,
	    BlockFace backFace) {
	if (!Config.isSetSizeFromSign()) {
	    return false;
	}
	String[] parts = line.split("x");
	if (parts != null && parts.length >= 2) {
	    try {
		int sizeX = Integer.parseInt(parts[0].trim());
		int sizeZ = Integer.parseInt(parts[1].trim());
		int sizeY = (Config.getDefaultHeight() == 0) ? (sizeX + sizeZ) / 2 : Config.getDefaultHeight();

		if (parts.length == 3) {
		    sizeY = Integer.parseInt(parts[2].trim());
		}

		int leftPart = sizeX / 2;
		int rightPart = sizeX - leftPart;

		if (area.getSignBlock().getLocation().getBlockY() + sizeY > area.getSignBlock().getWorld()
			.getMaxHeight()) {
		    sizeY = area.getSignBlock().getWorld().getMaxHeight()
			    - area.getSignBlock().getLocation().getBlockY() - 1;
		}

		Block supplyBlock = area.getSignBlock().getRelative(leftFace);
		Block leftBlock = area.getSignBlock().getRelative(leftFace, leftPart);
		Block rightBlock = area.getSignBlock().getRelative(rightFace, rightPart-1);
		Block backBlock = area.getSignBlock().getRelative(backFace, sizeZ-1);
		Block upBlock = area.getSignBlock().getRelative(BlockFace.UP, sizeY-1);
		area.add(supplyBlock, false);
		area.add(leftBlock, false);
		area.add(rightBlock, false);
		area.add(backBlock, false);
		area.add(upBlock, false);
		return true;
	    } catch (NumberFormatException e) {
		return false;
	    }
	}
	return false;
    }

    public void addListener(PlanAreaListener listener) {
	listeners.add(listener);
    }
}

package za.dats.bukkit.buildingplanner.model;

import java.io.File;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.material.MaterialData;
import org.bukkit.util.config.Configuration;
import org.bukkit.util.config.ConfigurationNode;

import com.sun.java_cup.internal.runtime.Scanner;

import za.dats.bukkit.buildingplanner.BuildingPlanner;

/**
 * This is purely to read pre 0.3.0 areas in.
 * 
 * @author cmdrdats
 *
 */
public class OldReader {
    public static Location convertCoord(World world, String coord) {
	String[] split = coord.split("x");
	Location result = new Location(world, Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2]));
	return result;
    }
    public static String getCoord(int x, int y, int z) {
	return String.format("%1$08dx%3$08dx%2$08d", x, y, z);
    }

    public static String getCoord(Location loc) {
	return getCoord(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public static String locationToString(Location location) {
	return location.getWorld().getUID() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":"
		+ location.getBlockZ() + ":" + location.getPitch() + ":" + location.getYaw();
    }

    public static Location stringToLocation(String string) {
	String[] split = string.split(":");
	World world = BuildingPlanner.plugin.getServer().getWorld(UUID.fromString(split[0]));

	Location location = new Location(world, Double.parseDouble(split[1]), Double.parseDouble(split[2]),
		Double.parseDouble(split[3]), Float.parseFloat(split[4]), Float.parseFloat(split[5]));
	return location;
    }
    
    public static HashMap<String, Object> blockStateToConfig(BlockState blockState) {
	HashMap<String, Object> result = new HashMap<String, Object>();

	result.put("location", locationToString(blockState.getBlock().getLocation()));
	result.put("type", blockState.getType().toString());
	result.put("dataByte", (int) blockState.getRawData());
	return result;
    }

    public static BlockState configToBlockState(ConfigurationNode node) {
	Location l = stringToLocation(node.getString("location"));
	BlockState result = l.getBlock().getState();

	Material m = Material.valueOf(node.getString("type"));
	result.setType(m);

	if (m.getData() != null) {
	    MaterialData newData = m.getNewData((byte) node.getInt("dataByte", 0));
	    result.setData(newData);
	}

	return result;
    }
}

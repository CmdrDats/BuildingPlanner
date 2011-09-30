package za.dats.bukkit.buildingplanner;

import java.io.File;
import java.util.HashMap;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

public class Config {
    private final static String configFile = "configuration.yml";
    private static Configuration conf;

    
    public static void init(JavaPlugin plugin) {
	File file = new File(plugin.getDataFolder(), configFile);
	conf = new Configuration(file);
	if (file.exists()) {
	    conf.load();
	}
	
	// Make sure we add new configuration options.
	boolean changed = setDefaults();
	if (!file.exists() || changed) {
	    conf.save();
	}
    }

    private static boolean setDefaults() {
	HashMap<String, Object> defaults = new HashMap<String, Object>();
	defaults.put("enabled", true);
	defaults.put("defaultHeight", 0);
	defaults.put("floorColour", "RED");
	defaults.put("gridColour", "WHITE");
	defaults.put("liftFences", true); 
	defaults.put("setSizeFromSign", true); 
	defaults.put("floorInvincible", true);
	defaults.put("maxHeight", 64);
	defaults.put("maxSize", 128);
	defaults.put("protectedFromFalling", true);
	/*
	defaults.put("lang.createConfirm", "&EBuilt <name>!");
	defaults.put("lang.destroyed", "&EDestroyed Memory Stone Structure!");
	defaults.put("lang.signAdded", "&EMemory Stone created.");
	defaults.put("lang.destroyForgotten", "Memory stone: <name> has been destroyed and forgotten.");
	defaults.put("lang.memorize", "Memorized: <name>");
	defaults.put("lang.alreadymemorized", "You have already memorized: <name>");
	defaults.put("lang.notfound", "<name> could not be found");
	defaults.put("lang.cooldown", "Teleport cooling down (<left>s)");
	defaults.put("lang.startrecall", "Starting recall to <name>");
	defaults.put("lang.cancelled", "Recall cancelled");
	defaults.put("lang.chargesleft", "You have <numcharges> left on your <material>");
	defaults.put("lang.consumed", "You have worn your <material> out!");
	defaults.put("lang.teleportingother", "Teleporting <name> to <destination>");
	defaults.put("lang.teleportedbyother", "<name> is teleporting you to <destination>");
	defaults.put("lang.teleporting", "Teleporting to <destination>");
	defaults.put("lang.noteleportzone", "You are in a no teleport zone. Cannot teleport out.");
	defaults.put("lang.teleportitemnotfound", "You need to have a <material> to teleport");
	*/
	boolean changed = false;
	for (String key : defaults.keySet()) {
	    if (conf.getProperty(key) == null) {
		changed = true;
		conf.setProperty(key, defaults.get(key));
	    }
	}
	return changed;
    }
    
    public static String getLang(String key) {
	return conf.getString("lang."+key);
    }
    
    public static String getColorLang(String langKey, String ... keyMap) {
	String result = getLang(langKey);
	if (result == null) {
	    result = langKey;
	}
	for (int i = 0; i < keyMap.length; i += 2) {
	    if (i+1 >= keyMap.length) {
		break;
	    }
	    
	    String key = keyMap[i];
	    String value = keyMap[i+1];
	    
	    result = result.replaceAll("[<]"+key+"[>]", value);
	}
	
	return BuildingPlanner.color(result);
    }

    public static boolean isEnabled() {
	return conf.getBoolean("enabled", true);
    }
    
    public static int getDefaultHeight() {
	return conf.getInt("defaultHeight", 0);
    }
    
    public static DyeColor getFloorColour() {
	return DyeColor.valueOf(conf.getString("floorColour", "RED"));
    }
    
    public static DyeColor getGridColour() {
	return DyeColor.valueOf(conf.getString("gridColour", "WHITE"));
    }
    
    public static boolean isFenceLiftEnabled() {
	return conf.getBoolean("liftFences", true);
    }

    public static boolean isSetSizeFromSign() {
	return conf.getBoolean("setSizeFromSign", true);
    }

    public static boolean isFloorInvincible() {
	return conf.getBoolean("floorInvincible", true);
    }
    
    public static int getMaxHeight() {
	return conf.getInt("maxHeight", 64);
    }
    
    public static int getMaxSize() {
	return conf.getInt("maxSize", 128);
    }

    public static boolean isProtectedFromFalling() {
	return conf.getBoolean("protectedFromFalling", true);
    }
                                            
}

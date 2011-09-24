package za.dats.bukkit.buildingplanner.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.TreeSpecies;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Tree;
import org.bukkit.material.Wool;
import org.bukkit.util.config.Configuration;
import org.bukkit.util.config.ConfigurationNode;

import za.dats.bukkit.buildingplanner.BuildingPlanner;

public class PlanArea {
    private Block signBlock;
    private Block supplyBlock;

    private int minX, minY, minZ;
    private int maxX, maxY, maxZ;

    private HashSet<Block> fenceBlocks = new HashSet<Block>();
    // private HashSet<BlockState> originalBlocks = new HashSet<BlockState>();
    private HashMap<String, BlockState> originalBlocks = new HashMap<String, BlockState>();
    private TreeMap<String, BlockState> planBlocks = new TreeMap<String, BlockState>();

    private Player commitPlayer;
    private Date commitAttemptTime;
    private boolean committed;

    public Block getSignBlock() {
	return signBlock;
    }

    public void setSignBlock(Block signBlock) {
	this.signBlock = signBlock;
    }

    public Block getSupplyBlock() {
	return supplyBlock;
    }

    public int getMinX() {
	return minX;
    }

    public void setMinX(int minX) {
	this.minX = minX;
    }

    public int getMinY() {
	return minY;
    }

    public void setMinY(int minY) {
	this.minY = minY;
    }

    public int getMinZ() {
	return minZ;
    }

    public void setMinZ(int minZ) {
	this.minZ = minZ;
    }

    public int getMaxX() {
	return maxX;
    }

    public void setMaxX(int maxX) {
	this.maxX = maxX;
    }

    public int getMaxY() {
	return maxY;
    }

    public void setMaxY(int maxY) {
	this.maxY = maxY;
    }

    public int getMaxZ() {
	return maxZ;
    }

    public void setMaxZ(int maxZ) {
	this.maxZ = maxZ;
    }

    public HashSet<Block> getFenceBlocks() {
	return fenceBlocks;
    }

    public void setFenceBlocks(HashSet<Block> fenceBlocks) {
	this.fenceBlocks = fenceBlocks;
    }

    public boolean isCommitted() {
	return committed;
    }

    public Player getCommitPlayer() {
	return commitPlayer;
    }

    public void setCommitPlayer(Player commitPlayer) {
	this.commitPlayer = commitPlayer;
    }

    public Date getCommitAttemptTime() {
	return commitAttemptTime;
    }

    public void setCommitAttemptTime(Date commitAttemptTime) {
	this.commitAttemptTime = commitAttemptTime;
    }

    public void set(Block block) {
	Location blockLocation = block.getLocation();
	minX = blockLocation.getBlockX();
	minY = blockLocation.getBlockY();
	minZ = blockLocation.getBlockZ();

	maxX = blockLocation.getBlockX();
	maxY = blockLocation.getBlockY();
	maxZ = blockLocation.getBlockZ();

	fenceBlocks.add(block);
    }

    public void add(Block block) {
	if (supplyBlock == null) {
	    supplyBlock = block;
	}

	Location blockLocation = block.getLocation();
	minX = Math.min(minX, blockLocation.getBlockX());
	minY = Math.min(minY, blockLocation.getBlockY());
	minZ = Math.min(minZ, blockLocation.getBlockZ());

	maxX = Math.max(maxX, blockLocation.getBlockX());
	maxY = Math.max(maxY, blockLocation.getBlockY());
	maxZ = Math.max(maxZ, blockLocation.getBlockZ());

	fenceBlocks.add(block);
    }

    public boolean fenceContains(Block block) {
	return fenceBlocks.contains(block);
    }

    public void updateOriginalBlocks() {
	for (int x = minX; x <= maxX; x++) {
	    for (int y = minY - 1; y <= maxY; y++) {
		for (int z = minZ; z <= maxZ; z++) {
		    originalBlocks.put(getCoord(x, y, z), signBlock.getWorld().getBlockAt(x, y, z).getState());
		}
	    }
	}

	saveArea();
    }

    public void init() {
	updateOriginalBlocks();
	placeBottomZone();
	placeSupplyChest();
    }

    private void placeSupplyChest() {
	if (supplyBlock != null) {
	    if (!Material.CHEST.equals(supplyBlock.getType())) {
		supplyBlock.setType(Material.CHEST);
	    }
	}
    }

    private static String getCoord(int x, int y, int z) {
	return String.format("%1$08dx%3$08dx%2$08d", x, y, z);
    }

    private String getCoord(Location loc) {
	return getCoord(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    private void placeBottomZone() {
	for (int x = minX; x <= maxX; x++) {
	    for (int z = minZ; z <= maxZ; z++) {
		int xOffset = x - minX;
		int zOffset = z - minZ;

		signBlock.getWorld().getBlockAt(x, minY - 1, z).setType(Material.WOOL);
		// Make white wool grid every 5 blocks.
		if (xOffset % 5 != 0 && zOffset % 5 != 0) {
		    signBlock.getWorld().getBlockAt(x, minY - 1, z).setData((byte) 14);
		}

	    }
	}

    }

    public void restoreOriginalBlocks(boolean ignoreSupplyChest) {
	for (int x = minX; x <= maxX; x++) {
	    for (int y = minY - 1; y <= maxY; y++) {
		for (int z = minZ; z <= maxZ; z++) {
		    if (ignoreSupplyChest) {
			Block block = signBlock.getWorld().getBlockAt(x, y, z);
			if (block == supplyBlock) {
			    continue;
			}
		    }
		    BlockState state = originalBlocks.get(getCoord(x, y, z));
		    state.update(true);
		    // signBlock.getWorld().getBlockAt(x, y, z).setType(state.getType());
		    // signBlock.getWorld().getBlockAt(x, y, z).setData(state.getRawData());
		}
	    }
	}
    }

    public void restorePlanBlocks() {
	for (int x = minX; x <= maxX; x++) {
	    for (int y = minY - 1; y <= maxY; y++) {
		for (int z = minZ; z <= maxZ; z++) {
		    BlockState state = planBlocks.get(getCoord(x, y, z));
		    if (state == null) {
			continue;
		    }

		    Block block = signBlock.getWorld().getBlockAt(x, y, z);
		    if (block == supplyBlock) {
			continue;
		    }

		    state.update(true);
		    // signBlock.getWorld().getBlockAt(x, y, z).setType(state.getType());
		    // signBlock.getWorld().getBlockAt(x, y, z).setData(state.getRawData());
		}
	    }
	}
    }

    public void commit() {
	restoreOriginalBlocks(true);
	committed = true;
	saveArea();
    }

    public void unCommit() {
	restorePlanBlocks();
	committed = false;
	saveArea();
    }

    public boolean isFloorBlock(Block block) {
	Location loc = block.getLocation();
	if (loc.getBlockY() != minY - 1) {
	    return false;
	}

	if (loc.getBlockX() < minX || loc.getBlockX() > maxX) {
	    return false;
	}

	if (loc.getBlockZ() < minZ || loc.getBlockZ() > maxZ) {
	    return false;
	}

	return true;
    }

    public boolean isInside(Block block) {
	Location loc = block.getLocation();
	return isInside(loc);
    }

    public boolean isInside(Location loc) {
	if (loc.getBlockX() < minX || loc.getBlockX() > maxX) {
	    return false;
	}

	if (loc.getBlockY() < minY || loc.getBlockY() > maxY) {
	    return false;
	}

	if (loc.getBlockZ() < minZ || loc.getBlockZ() > maxZ) {
	    return false;
	}
	return true;
    }

    public void addOriginalBlock(Block block, boolean save) {
	Location loc = block.getLocation();
	planBlocks.remove(getCoord(loc));
	originalBlocks.put(getCoord(loc), block.getState());

	if (save) {
	    saveArea();
	}
    }

    public void addPlanBlock(Block block) {
	Location loc = block.getLocation();
	planBlocks.put(getCoord(loc), block.getState());
	saveArea();
    }

    public void removePlannedBlock(Block block, boolean save) {
	planBlocks.remove(getCoord(block.getLocation()));
	if (save) {
	    saveArea();
	}
    }

    public boolean isPlannedBlock(Block block) {
	Location location = block.getLocation();
	String coord = getCoord(location);
	if (planBlocks.containsKey(coord)) {
	    return true;
	}

	return false;
    }

    public void addOriginalBlock(Location loc, BlockState state) {
	planBlocks.remove(getCoord(loc));
	originalBlocks.put(getCoord(loc), state);
	saveArea();
    }

    public void reportPlan(Player player) {
	HashMap<String, AtomicLong> counts = getMaterialCount();

	if (counts.size() == 0) {
	    player.sendMessage("This plan has no required materials yet.");
	    return;
	}

	player.sendMessage("This plan needs the following: ");
	for (String item : counts.keySet()) {
	    AtomicLong count = counts.get(item);
	    player.sendMessage(" " + count + "x " + item);
	}
    }

    private String getMaterialDataString(MaterialData data) {
	String result = null;
	Material type = data.getItemType();
	// Switch out the special item based blocks to their primitive state
	switch (type) {
	case REDSTONE_WIRE:
	    result = Material.REDSTONE.toString();
	}

	if (result == null) {
	    result = data.toString().replaceAll("[(].*", "");
	}

	return result;

    }

    private HashMap<String, AtomicLong> getMaterialCount() {
	HashMap<String, AtomicLong> counts = new HashMap<String, AtomicLong>();
	for (BlockState block : planBlocks.values()) {
	    String key = getMaterialDataString(block.getData());

	    AtomicLong count = counts.get(key);
	    if (count == null) {
		count = new AtomicLong();
		counts.put(key, count);
	    }
	    count.incrementAndGet();
	}

	return counts;
    }

    private boolean isItemForPlanBlock(ItemStack stack, BlockState block) {
	Material itemMaterial = stack.getType();
	Material blockMaterial = block.getType();
	if (itemMaterial.equals(Material.REDSTONE) && blockMaterial.equals(Material.REDSTONE_WIRE)) {
	    return true;
	}

	if (itemMaterial.equals(blockMaterial) && stack.getDurability() == block.getData().getData()) {
	    return true;
	}

	return false;
    }

    public boolean buildFromSupply(int maxItems) {
	int count = 0;
	if (!(supplyBlock.getState() instanceof Chest)) {
	    return false;
	}
	Chest chest = (Chest) supplyBlock.getState();
	ItemStack[] contents = chest.getInventory().getContents();
	List<String> buildBlockLocations = new ArrayList<String>();

	for (int i = 0; i < contents.length; i++) {
	    ItemStack itemStack = contents[i];
	    if (itemStack == null) {
		continue;
	    }

	    for (String location : planBlocks.keySet()) {
		BlockState planBlock = planBlocks.get(location);
		// Ignore fulfilled blocks.
		if (buildBlockLocations.contains(location)) {
		    continue;
		}

		if (isItemForPlanBlock(itemStack, planBlock)) {
		    int amount = itemStack.getAmount();
		    amount--;
		    count++;

		    buildBlockLocations.add(location);

		    if (amount == 0) {
			chest.getInventory().setItem(i, null);
		    } else {
			itemStack.setAmount(amount);
		    }

		    // Stop looking when count is reached
		    if (count >= maxItems || amount == 0) {
			break;
		    }
		}
	    }

	    // Stop looking when count is reached
	    if (count >= maxItems) {
		break;
	    }

	}

	for (String key : buildBlockLocations) {
	    BlockState planBlock = planBlocks.get(key);
	    planBlock.update(true);
	    // planBlock.getBlock().setType(planBlock.getType());
	    // planBlock.getBlock().setData(planBlock.getRawData());
	    addOriginalBlock(planBlock.getBlock(), false);
	}

	if (buildBlockLocations.size() > 0) {
	    saveArea();
	    return true;
	}

	return false;
    }

    public File getAreaFile() {
	String fileName = getCoord(signBlock.getLocation()) + ".area";
	File areaFile = new File(BuildingPlanner.plugin.getDataFolder(), fileName);
	return areaFile;
    }

    public void deleteArea() {
	File areaFile = getAreaFile();
	if (areaFile.exists()) {
	    areaFile.delete();
	}
    }

    public void saveArea() {
	Configuration area = new Configuration(getAreaFile());
	area.setProperty("committed", committed);
	area.setProperty("signLocation", locationToString(signBlock.getLocation()));
	area.setProperty("supplyLocation", locationToString(supplyBlock.getLocation()));
	area.setProperty("minLocation", locationToString(new Location(signBlock.getWorld(), minX, minY, minZ)));
	area.setProperty("maxLocation", locationToString(new Location(signBlock.getWorld(), maxX, maxY, maxZ)));

	int fenceCount = 0;
	for (Block fence : fenceBlocks) {
	    area.setProperty("fenceBlock." + fenceCount, locationToString(fence.getLocation()));
	}

	for (String key : originalBlocks.keySet()) {
	    area.setProperty("originalBlocks." + key, blockStateToConfig(originalBlocks.get(key)));
	}

	for (String key : planBlocks.keySet()) {
	    area.setProperty("planBlocks." + key, blockStateToConfig(planBlocks.get(key)));
	}

	area.save();
    }

    private HashMap<String, Object> blockStateToConfig(BlockState blockState) {
	HashMap<String, Object> result = new HashMap<String, Object>();

	result.put("location", locationToString(blockState.getBlock().getLocation()));
	result.put("type", blockState.getType().toString());
	result.put("dataByte", (int)blockState.getRawData());
	return result;
    }

    private BlockState configToBlockState(ConfigurationNode node) {
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

    public void loadArea(File areaFile) {
	Configuration config = new Configuration(areaFile);
	config.load();
	
	committed = config.getBoolean("committed", false);
	Location signLocation = stringToLocation(config.getString("signLocation", ""));
	signBlock = signLocation.getBlock();

	Location supplyLocation = stringToLocation(config.getString("supplyLocation", ""));
	supplyBlock = supplyLocation.getBlock();

	Location minLocation = stringToLocation(config.getString("minLocation", ""));
	minX = minLocation.getBlockX();
	minY = minLocation.getBlockY();
	minZ = minLocation.getBlockZ();

	Location maxLocation = stringToLocation(config.getString("maxLocation", ""));
	maxX = maxLocation.getBlockX();
	maxY = maxLocation.getBlockY();
	maxZ = maxLocation.getBlockZ();

	List<String> originalKeys = config.getKeys("originalBlocks");
	if (originalKeys != null) {
	    originalBlocks.clear();
	    for (String key : originalKeys) {
		originalBlocks.put(key, (BlockState)configToBlockState(config.getNode("originalBlocks."+key)));
	    }
	}

	List<String> planKeys = config.getKeys("planBlocks");
	planBlocks.clear();
	if (planKeys != null) {
	    for (String key : planKeys) {
		planBlocks.put(key, (BlockState)configToBlockState(config.getNode("planBlocks."+key)));
	    }
	}
    }

    public String locationToString(Location location) {
	return location.getWorld().getUID() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":"
		+ location.getBlockZ() + ":" + location.getPitch() + ":" + location.getYaw();
    }

    public Location stringToLocation(String string) {
	String[] split = string.split(":");
	World world = BuildingPlanner.plugin.getServer().getWorld(UUID.fromString(split[0]));

	Location location = new Location(world, Double.parseDouble(split[1]), Double.parseDouble(split[2]),
		Double.parseDouble(split[3]), Float.parseFloat(split[4]), Float.parseFloat(split[5]));
	return location;
    }
}

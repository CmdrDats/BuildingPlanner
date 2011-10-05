package za.dats.bukkit.buildingplanner.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Bed;
import org.bukkit.material.Door;
import org.bukkit.material.MaterialData;
import org.bukkit.material.SimpleAttachableMaterialData;
import org.bukkit.util.config.Configuration;
import org.bukkit.util.config.ConfigurationNode;

import za.dats.bukkit.buildingplanner.BuildingPlanner;
import za.dats.bukkit.buildingplanner.Config;
import za.dats.bukkit.buildingplanner.listeners.ChangeSetCompleteListener;
import za.dats.bukkit.buildingplanner.model.PlanArea.OpType;

public class PlanArea {
    private static final int THREAD_SLEEP_INTERVAL = 20;

    public enum OpType {
	CREATE("buildingplanner.create", false), DESTROY("buildingplanner.destroy", false), MODIFY(
		"buildingplanner.use", true), COMMIT("buildingplanner.commit", false), UNCOMMIT(
		"buildingplanner.uncommit", false), INVITE("buildingplanner.invite", false), GIVE(
		"buildingplanner.give", false);

	final String permissionValue;
	final boolean allowCollaborators;

	private OpType(String permissionValue, boolean allowCollaborators) {
	    this.permissionValue = permissionValue;
	    this.allowCollaborators = allowCollaborators;
	}
    }

    String name;
    String owner;
    Block signBlock;
    Block supplyBlock;

    int minX, minY, minZ;
    int maxX, maxY, maxZ;
    int sizeX, sizeY, sizeZ;

    private HashSet<Block> fenceBlocks = new HashSet<Block>();
    private List<String> collaborators = new ArrayList<String>();

    private int[][][] originalBlocks;
    private int[][][] planBlocks;

    private Player commitPlayer;
    private Date commitAttemptTime;
    private boolean committed;
    private boolean locked;
    private String lockReason;

    private Date lastChange;
    volatile boolean dirty;
    private volatile boolean destroyed;

    public String getName() {
	return name;
    }

    public void setName(String name) {
	this.name = name;
    }

    public String getOwner() {
	return owner;
    }

    public void setOwner(String owner) {
	this.owner = owner;
    }

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

    public int getMinY() {
	return minY;
    }

    public int getMinZ() {
	return minZ;
    }

    public int getMaxX() {
	return maxX;
    }

    public int getMaxY() {
	return maxY;
    }

    public int getMaxZ() {
	return maxZ;
    }

    public void setMaxY(int maxY) {
	this.maxY = maxY;
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

    public void lock(String reason) {
	locked = true;
	lockReason = reason;
    }

    public void unlock() {
	locked = false;
    }

    public boolean isLocked() {
	return locked;
    }

    public String getLockReason() {
	return lockReason;
    }

    public void set(Block block) {
	Location blockLocation = block.getLocation();
	minX = blockLocation.getBlockX();
	minY = blockLocation.getBlockY();
	minZ = blockLocation.getBlockZ();

	maxX = blockLocation.getBlockX();
	maxY = blockLocation.getBlockY();
	maxZ = blockLocation.getBlockZ();

	// fenceBlocks.add(block);
    }

    public void add(Block block, boolean fence) {
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

	if (fence) {
	    fenceBlocks.add(block);
	}
    }

    public boolean fenceContains(Block block) {
	return fenceBlocks.contains(block);
    }

    public void updateOriginalBlocks() {
	for (int x = 0; x < sizeX; x++) {
	    for (int y = 0; y < sizeY; y++) {
		for (int z = 0; z < sizeZ; z++) {
		    Block blockAt = signBlock.getWorld().getBlockAt(x + minX, y + minY, z + minZ);
		    BlockState state = blockAt.getState();

		    originalBlocks[x][y][z] = BlockHelper.getSate(state);
		}
	    }
	}

	saveArea();
    }

    public void init() {
	minY -= 1;
	this.sizeX = maxX - minX + 1;
	this.sizeY = maxY - minY + 1;
	this.sizeZ = maxZ - minZ + 1;

	originalBlocks = new int[sizeX][sizeY][sizeZ];
	planBlocks = new int[sizeX][sizeY][sizeZ];

	if (Config.isFenceLiftEnabled()) {
	    liftFences();
	}

	updateOriginalBlocks();
	Thread setupThread = new Thread("Plan setup thread") {
	    @Override
	    public void run() {
		lock("Setting up area");
		placeBottomZone();

		placeSupplyChest();
		unlock();
	    }
	};

	setupThread.setDaemon(true);
	setupThread.start();
    }

    private void liftFences() {
	for (Block fenceBlock : fenceBlocks) {
	    fenceBlock.setType(Material.AIR);
	}
    }

    private void placeSupplyChest() {
	if (supplyBlock != null) {
	    int fenceSize = fenceBlocks.size();
	    if (!Material.FENCE.equals(supplyBlock.getType())) {
		fenceSize--;
	    }

	    if (!Material.CHEST.equals(supplyBlock.getType())) {
		supplyBlock.setType(Material.CHEST);
	    }

	    if (Config.isFenceLiftEnabled() && fenceSize > 0) {
		ItemStack stack = new ItemStack(Material.FENCE, fenceBlocks.size());
		Chest chest = (Chest) supplyBlock.getState();
		chest.getInventory().addItem(stack);
		fenceBlocks.clear();
	    }
	}
    }

    private void placeBottomZone() {
	ChangeSet scheduler = new ChangeSet(new ChangeSetCompleteListener() {
	    public void complete() {
	    }
	});
	
	World world = signBlock.getWorld();
	int floorState = BlockHelper.getState(Material.WOOL, (byte) Config.getFloorColour().getData());
	int gridState = BlockHelper.getState(Material.WOOL, (byte) Config.getGridColour().getData());

	for (int x = minX; x <= maxX; x++) {
	    for (int z = minZ; z <= maxZ; z++) {
		final int xOffset = x - minX;
		final int zOffset = z - minZ;

		int planState = planBlocks[x - minX][0][z - minZ];
		Material material = BlockHelper.getMaterial(planState);
		if (material != Material.AIR) {
		    continue;
		}

		int state = floorState;
		if (xOffset % 5 != 0 && zOffset % 5 != 0) {
		    state = floorState;
		    continue;
		} else {
		    state = gridState;
		}

		scheduler.add(new Change(world, x, minY, z, state));
	    }
	}

	scheduler.commit();

    }

    private interface AreaCycler {
	/**
	 * @return false if cycling should stop.
	 */
	boolean cycle(int x, int y, int z);
    }

    public void cycleArea(AreaCycler cycler, boolean reverse) {
	// There is probably a cleaner way of doing this... oh well.

	if (reverse) {
	    for (int y = sizeY - 1; y >= 0; y--) {
		for (int x = sizeX - 1; x >= 0; x--) {
		    for (int z = sizeZ - 1; z >= 0; z--) {
			if (!cycler.cycle(x, y, z)) {
			    return;
			}
		    }
		}
	    }
	    return;
	}

	for (int y = 0; y < sizeY; y++) {
	    for (int x = 0; x < sizeX; x++) {
		for (int z = 0; z < sizeZ; z++) {
		    if (!cycler.cycle(x, y, z)) {
			return;
		    }
		}
	    }
	}
    }

    private void restoreBlock(ChangeSet changeSet, final int[][][] blockMap, final int x, final int y, final int z,
	    boolean ignoreSupplyChest, boolean restoreAir, int mode) {
	if (ignoreSupplyChest) {
	    Location supplyLocation = supplyBlock.getLocation();
	    if (supplyLocation.getBlockX() == x + minX && supplyLocation.getBlockY() == y + minY
		    && supplyLocation.getBlockZ() == z + minZ) {
		return;
	    }
	}

	final int state = blockMap[x][y][z];

	if ((planBlocks[x][y][z] == 0) && (!isFloorBlock(x, y, z))) {
	    return;
	}

	
	if (mode == 1) {
	    final Block block = signBlock.getWorld().getBlockAt(x + minX, y + minY, z + minZ);
	    if (!(block.getState().getData() instanceof SimpleAttachableMaterialData)) {
		return;
	    }
	} else if (mode == 2) {
	    if (BlockHelper.isAttachable(state)) {
		return;
	    }
	    if (!restoreAir && BlockHelper.isAir(state)) {
		return;
	    }
	} else if (mode == 3) {
	    if (!BlockHelper.isAttachable(state)) {
		return;
	    }

	    if (!restoreAir && BlockHelper.isAir(state)) {
		return;
	    }

	}
	changeSet.add(new Change(signBlock.getWorld(), x + minX, y + minY, z + minZ, state));

	return;
    }

    private void restoreBlockPlan(final ChangeSet changeSet, final int[][][] blockMap, final boolean ignoreSupplyChest,
	    final boolean restoreAir) {
	// Overwrite attachables first.
	AreaCycler removeAttachablesCycler = new AreaCycler() {
	    public boolean cycle(final int x, final int y, final int z) {
		restoreBlock(changeSet, blockMap, x, y, z, ignoreSupplyChest, restoreAir, 1);
		return true;
	    }
	};

	// Restore basic blocks
	AreaCycler solidBlockCycler = new AreaCycler() {
	    public boolean cycle(int x, int y, int z) {
		restoreBlock(changeSet, blockMap, x, y, z, ignoreSupplyChest, restoreAir, 2);
		return true;
	    }
	};

	// Restore Attachables
	AreaCycler attachableCycler = new AreaCycler() {
	    public boolean cycle(int x, int y, int z) {
		restoreBlock(changeSet, blockMap, x, y, z, ignoreSupplyChest, restoreAir, 3);
		return true;
	    }
	};

	cycleArea(removeAttachablesCycler, restoreAir);
	cycleArea(solidBlockCycler, restoreAir);
	cycleArea(attachableCycler, restoreAir);
	changeSet.commit();
    }

    public void destroyArea() {
	BuildingPlanner.info("Starting destroy: " + getAreaName());
	lock("Destroying plan area");
	ChangeSet set = new ChangeSet(new ChangeSetCompleteListener() {
	    public void complete() {
		committed = true;
		destroyed = true;
		unlock();
		deleteArea();
		BuildingPlanner.info("Finished destroy: " + getAreaName());
	    }
	});
	restoreBlockPlan(set, originalBlocks, false, true);
    }

    public void commit() {
	BuildingPlanner.info("Starting commit: " + getAreaName());
	lock("Committing plan area");
	ChangeSet set = new ChangeSet(new ChangeSetCompleteListener() {

	    public void complete() {
		committed = true;
		saveArea();
		unlock();
		BuildingPlanner.info("Finished commit: " + getAreaName());
	    }

	});

	restoreBlockPlan(set, originalBlocks, true, true);
    }

    public void unCommit() {
	BuildingPlanner.info("Starting uncommit: " + getAreaName());
	lock("Uncommitting plan area");
	placeBottomZone();
	ChangeSet set = new ChangeSet(new ChangeSetCompleteListener() {
	    public void complete() {
		committed = false;
		saveArea();
		unlock();
		BuildingPlanner.info("Finished uncommit: " + getAreaName());
	    }
	});
	restoreBlockPlan(set, planBlocks, true, false);
    }
    
    

    public boolean isFloorBlock(Block block) {
	Location loc = block.getLocation();
	if (loc.getBlockY() != minY) {
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

    public boolean isFloorBlock(int x, int y, int z) {
	if (y != 0) {
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

	updateBlock(planBlocks, loc, 0);
	updateBlock(originalBlocks, loc, BlockHelper.getSate(block.getState()));

	if (save) {
	    saveArea();
	}
    }

    private void updateBlock(int[][][] blockMap, Location loc, int state) {
	int x = loc.getBlockX() - minX;
	int y = loc.getBlockY() - minY;
	int z = loc.getBlockZ() - minZ;

	// Check bounds.
	if ((x < 0 || x >= sizeX) || (y < 0 || y >= sizeY) || (z < 0 || z >= sizeZ)) {
	    return;
	}

	blockMap[x][y][z] = state;
    }

    private int getBlock(int[][][] blockMap, Location loc) {
	int x = loc.getBlockX() - minX;
	int y = loc.getBlockY() - minY;
	int z = loc.getBlockZ() - minZ;

	// Check bounds.
	if ((x < 0 || x >= sizeX) || (y < 0 || y >= sizeY) || (z < 0 || z >= sizeZ)) {
	    return 0;
	}

	return blockMap[x][y][z];
    }

    public void addOriginalBlock(Location loc, BlockState state) {
	updateBlock(planBlocks, loc, 0);
	updateBlock(originalBlocks, loc, BlockHelper.getSate(state));
	saveArea();
    }

    public void addPlanBlock(Block block) {
	Location loc = block.getLocation();
	BlockState state = block.getState();

	updateBlock(planBlocks, loc, BlockHelper.getSate(state));
	saveArea();
    }

    public void removePlannedBlock(Block block, boolean save) {
	updateBlock(planBlocks, block.getLocation(), 0);

	if (save) {
	    saveArea();
	}
    }

    public boolean isPlannedBlock(Block block) {
	Location location = block.getLocation();

	int state = getBlock(planBlocks, location);
	if (BlockHelper.isAir(state)) {
	    return false;
	}

	return true;
    }

    public void reportPlan(Player player) {
	HashMap<String, AtomicLong> counts = getMaterialCount();

	String planName = name == null ? "The plan" : name;

	if (counts.size() == 0) {
	    player.sendMessage(planName + " has no required materials yet.");
	    return;
	}

	player.sendMessage(planName + " needs the following: ");
	for (String item : counts.keySet()) {
	    AtomicLong count = counts.get(item);
	    player.sendMessage(" " + count + "x " + item);
	}
    }

    private String getMaterialDataString(int state) {
	String result = null;
	Material type = BlockHelper.getMaterial(state);
	MaterialData data = BlockHelper.getMaterialData(state);
	if (data instanceof Door) {
	    Door door = (Door) data;
	    if (door.isTopHalf()) {
		return null;
	    }
	} else if (data instanceof Bed) {
	    Bed bed = (Bed) data;
	    if (bed.isHeadOfBed()) {
		return null;
	    }
	}

	// Switch out the special item based blocks to their primitive state
	switch (type) {
	case REDSTONE_WIRE:
	    result = Material.REDSTONE.toString();
	    break;
	case WOODEN_DOOR:
	    result = Material.WOOD_DOOR.toString();
	    break;
	case IRON_DOOR_BLOCK:
	    result = Material.IRON_DOOR.toString();
	    break;
	case DOUBLE_STEP:
	    data = Material.STEP.getNewData(BlockHelper.getData(state));
	    break;
	case BED_BLOCK:
	    result = Material.BED.toString();
	    break;
	}

	if (result == null) {
	    if (data != null) {
		result = data.toString().replaceAll("[(].*", "");
	    } else {
		result = type.toString();
	    }
	}

	return result;

    }

    private HashMap<String, AtomicLong> getMaterialCount() {
	final HashMap<String, AtomicLong> counts = new HashMap<String, AtomicLong>();

	cycleArea(new AreaCycler() {
	    public boolean cycle(int x, int y, int z) {
		int state = planBlocks[x][y][z];

		if (BlockHelper.isAir(state)) {
		    return true;
		}

		String key = getMaterialDataString(state);
		if (key == null) {
		    return true;
		}

		AtomicLong count = counts.get(key);
		if (count == null) {
		    count = new AtomicLong();
		    counts.put(key, count);
		}
		if (BlockHelper.getMaterial(state).equals(Material.DOUBLE_STEP)) {
		    count.incrementAndGet();
		}

		count.incrementAndGet();

		return true;
	    }
	}, false);

	return counts;
    }

    private boolean isItemForPlanBlock(ItemStack stack, int state, MaterialData data) {
	// Don't place top half of door block.
	if (data instanceof Door) {
	    Door door = (Door) data;
	    if (door.isTopHalf()) {
		return false;
	    }
	}

	// Don't place end of bed.
	if (data instanceof Bed) {
	    Bed bed = (Bed) data;
	    if (!bed.isHeadOfBed()) {
		return false;
	    }
	}

	Material itemMaterial = stack.getType();
	Material blockMaterial = BlockHelper.getMaterial(state);
	if (itemMaterial.equals(Material.REDSTONE) && blockMaterial.equals(Material.REDSTONE_WIRE)) {
	    return true;
	}

	if (itemMaterial.equals(Material.WOOD_DOOR) && blockMaterial.equals(Material.WOODEN_DOOR)) {
	    return true;
	}

	if (itemMaterial.equals(Material.STEP) && blockMaterial.equals(Material.DOUBLE_STEP)
		&& stack.getDurability() == BlockHelper.getData(state) && stack.getAmount() >= 2) {
	    return true;
	}

	switch (itemMaterial) {
	case REDSTONE:
	    return blockMaterial.equals(Material.REDSTONE_WIRE);
	case WOOD_DOOR:
	    return blockMaterial.equals(Material.WOODEN_DOOR);
	case IRON_DOOR:
	    return blockMaterial.equals(Material.IRON_DOOR_BLOCK);
	case BED:
	    return blockMaterial.equals(Material.BED_BLOCK);
	case CAKE:
	    return blockMaterial.equals(Material.CAKE_BLOCK);
	case SIGN:
	    return blockMaterial.equals(Material.WALL_SIGN) || blockMaterial.equals(Material.SIGN_POST);
	case WOOL:
	case LOG:
	case STEP:
	    return itemMaterial.equals(blockMaterial) && stack.getDurability() == BlockHelper.getData(state);
	}

	// Every other normal block can do a simple type comparison.
	if (itemMaterial.equals(blockMaterial)) {
	    return true;
	}

	return false;
    }

    public boolean buildFromSupply(final int maxItems) {
	if (!(supplyBlock.getState() instanceof Chest)) {
	    return false;
	}
	final Chest chest = (Chest) supplyBlock.getState();
	final ItemStack[] contents = chest.getInventory().getContents();
	final AtomicInteger count = new AtomicInteger();

	cycleArea(new AreaCycler() {
	    public boolean cycle(int x, int y, int z) {
		for (int i = 0; i < contents.length; i++) {
		    final ItemStack itemStack = contents[i];
		    if (itemStack == null) {
			continue;
		    }

		    int state = planBlocks[x][y][z];
		    MaterialData data = BlockHelper.getMaterialData(planBlocks[x][y][z]);
		    if (isItemForPlanBlock(itemStack, state, data)) {
			int amount = itemStack.getAmount();
			amount--;

			if (BlockHelper.getMaterial(state).equals(Material.DOUBLE_STEP)) {
			    amount--;
			}

			if (amount <= 0) {
			    chest.getInventory().setItem(i, null);
			} else {
			    itemStack.setAmount(amount);
			}

			count.incrementAndGet();

			Block planBlock = signBlock.getWorld().getBlockAt(x + minX, y + minY, z + minZ);
			// Add the top of the door
			if (data instanceof Door) {
			    Door door = (Door) data;
			    if (!door.isTopHalf()) {
				buildBlock(x, y + 1, z);
			    }
			}

			// Add the end of the bed.
			if (data instanceof Bed) {
			    Bed bed = (Bed) data;
			    if (bed.isHeadOfBed()) {
				Block relative = planBlock.getRelative(bed.getFacing().getOppositeFace());
				Location loc = relative.getLocation();
				buildBlock(loc.getBlockX() - minX, loc.getBlockY() - minY, loc.getBlockZ() - minZ);
			    }
			}

			buildBlock(x, y, z);

			// Stop looking when count is reached
			if (count.get() >= maxItems || amount == 0) {
			    return false;
			}
		    }

		    // Stop looking when count is reached
		    if (count.get() >= maxItems) {
			return false;
		    }

		}

		return true;
	    }
	}, false);

	if (count.get() > 0) {
	    saveArea();
	    return true;
	}

	return false;
    }

    protected void buildBlock(int x, int y, int z) {
	int state = planBlocks[x][y][z];
	Block block = signBlock.getWorld().getBlockAt(x + minX, y + minY, z + minZ);
	block.setType(BlockHelper.getMaterial(state));
	block.setData(BlockHelper.getData(state));
	addOriginalBlock(block, false);
	planBlocks[x][y][z] = 0;
    }

    public File getAreaDataFile() {
	String fileName = OldReader.getCoord(signBlock.getLocation()) + ".data";
	File areaFile = new File(BuildingPlanner.plugin.getDataFolder(), fileName);
	return areaFile;
    }

    public File getAreaDataFileTmp() {
	String fileName = OldReader.getCoord(signBlock.getLocation()) + ".datatmp";
	File areaFile = new File(BuildingPlanner.plugin.getDataFolder(), fileName);
	return areaFile;
    }

    public File getAreaDescriptorFile() {
	String fileName = OldReader.getCoord(signBlock.getLocation()) + ".area";
	File areaFile = new File(BuildingPlanner.plugin.getDataFolder(), fileName);
	return areaFile;
    }

    public void deleteArea() {
	File areaFile = getAreaDataFile();
	if (areaFile.exists()) {
	    areaFile.delete();
	}
	File areaDescFile = getAreaDescriptorFile();
	if (areaDescFile.exists()) {
	    areaDescFile.delete();
	}
    }

    public void saveArea() {
	dirty = true;
	lastChange = new Date();
    }

    public void loadArea(File areaFile) {
	Configuration config = new Configuration(areaFile);
	config.load();

	name = config.getString("name", "");
	owner = config.getString("owner", "");
	committed = config.getBoolean("committed", false);
	boolean newFormat = config.getBoolean("newFormat", false);
	List<Object> collab = config.getList("collaborators");
	if (collab != null) {
	    for (Object colName : collab) {
		collaborators.add(colName.toString());
	    }
	}
	Location signLocation = OldReader.stringToLocation(config.getString("signLocation", ""));
	signBlock = signLocation.getBlock();

	Location supplyLocation = OldReader.stringToLocation(config.getString("supplyLocation", ""));
	supplyBlock = supplyLocation.getBlock();

	Location minLocation = OldReader.stringToLocation(config.getString("minLocation", ""));
	minX = minLocation.getBlockX();
	minY = minLocation.getBlockY();
	minZ = minLocation.getBlockZ();

	// Pull the minY down a notch on old format files.
	if (!newFormat) {
	    minY--;
	}

	Location maxLocation = OldReader.stringToLocation(config.getString("maxLocation", ""));
	maxX = maxLocation.getBlockX();
	maxY = maxLocation.getBlockY();
	maxZ = maxLocation.getBlockZ();

	sizeX = maxX - minX + 1;
	sizeY = maxY - minY + 1;
	sizeZ = maxZ - minZ + 1;

	originalBlocks = new int[sizeX][sizeY][sizeZ];
	planBlocks = new int[sizeX][sizeY][sizeZ];

	File areaDataFile = getAreaDataFile();
	try {
	    if (areaDataFile.exists()) {
		FileInputStream in = new FileInputStream(areaDataFile);

		DataInputStream inStream = new DataInputStream(in);

		for (int x = 0; x < sizeX; x++) {
		    for (int y = 0; y < sizeY; y++) {
			for (int z = 0; z < sizeZ; z++) {
			    originalBlocks[x][y][z] = inStream.readInt();
			}
		    }
		}
		for (int x = 0; x < sizeX; x++) {
		    for (int y = 0; y < sizeY; y++) {
			for (int z = 0; z < sizeZ; z++) {
			    planBlocks[x][y][z] = inStream.readInt();
			}
		    }
		}

		inStream.close();
		in.close();
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}

	// Load areas generated in the old version of the plugin and then immediately resave to the newer format.
	boolean save = false;
	List<String> originalKeys = config.getKeys("originalBlocks");
	if (originalKeys != null) {

	    for (String key : originalKeys) {
		save = true;
		Location loc = OldReader.stringToLocation(config.getString("originalBlocks." + key + ".location"));
		BlockState blockState = OldReader.configToBlockState(config.getNode("originalBlocks." + key));
		updateBlock(originalBlocks, loc, BlockHelper.getSate(blockState));
	    }
	}

	List<String> planKeys = config.getKeys("planBlocks");
	if (planKeys != null) {
	    for (String key : planKeys) {
		save = true;
		Location loc = OldReader.stringToLocation(config.getString("planBlocks." + key + ".location"));
		BlockState blockState = OldReader.configToBlockState(config.getNode("planBlocks." + key));
		// Location convertCoord = OldReader.convertCoord(signLocation.getWorld(), key);
		updateBlock(planBlocks, loc, BlockHelper.getSate(blockState));
	    }
	}

	if (save) {
	    saveArea();
	}
    }

    public void trySave() {
	if ((!dirty) || lastChange == null) {
	    return;
	}

	Date now = new Date();
	if (now.getTime() - lastChange.getTime() < 5000) {
	    return;
	}

	if (destroyed) {
	    BuildingPlanner.info("Area destroyed. No point in saving");
	}
	if (locked) {
	    BuildingPlanner.info("Area " + name + " Locked: " + lockReason + " will save when unlocked..");
	    return;
	}
	dirty = false;
	BuildingPlanner.info("Saving area: " + getAreaName());
	saveProps();

	try {
	    File areaData = getAreaDataFileTmp();
	    final FileOutputStream out = new FileOutputStream(areaData);
	    DataOutputStream outStream = new DataOutputStream(out);

	    for (int x = 0; x < sizeX; x++) {
		for (int y = 0; y < sizeY; y++) {
		    for (int z = 0; z < sizeZ; z++) {
			outStream.writeInt(originalBlocks[x][y][z]);
		    }
		}
	    }
	    for (int x = 0; x < sizeX; x++) {
		for (int y = 0; y < sizeY; y++) {
		    for (int z = 0; z < sizeZ; z++) {
			outStream.writeInt(planBlocks[x][y][z]);
		    }
		}
	    }

	    outStream.close();
	    out.close();

	    File areaDataFile = getAreaDataFile();
	    if (areaDataFile.exists()) {
		areaDataFile.delete();
	    }
	    areaData.renameTo(areaDataFile);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    private void saveProps() {
	Configuration area = new Configuration(getAreaDescriptorFile());
	area.setProperty("name", name);
	area.setProperty("owner", owner);
	area.setProperty("committed", committed);
	area.setProperty("newFormat", true);
	area.setProperty("signLocation", OldReader.locationToString(signBlock.getLocation()));
	area.setProperty("supplyLocation", OldReader.locationToString(supplyBlock.getLocation()));
	area.setProperty("minLocation",
		OldReader.locationToString(new Location(signBlock.getWorld(), minX, minY, minZ)));
	area.setProperty("maxLocation",
		OldReader.locationToString(new Location(signBlock.getWorld(), maxX, maxY, maxZ)));

	int fenceCount = 0;
	for (Block fence : fenceBlocks) {
	    area.setProperty("fenceBlock." + fenceCount, OldReader.locationToString(fence.getLocation()));
	}

	area.setProperty("collaborators", collaborators);
	area.save();
    }

    public String getAreaName() {
	StringBuilder result = new StringBuilder();

	if (name != null && name.length() > 0) {
	    result.append(name);
	} else {
	    result.append("Unnamed: " + sizeX + "x" + sizeZ + "x" + sizeY + " area");
	}

	result.append(committed ? ", committed" : ", planning");

	if (owner != null && owner.length() > 0) {
	    result.append(", owned by " + owner);
	} else {
	    result.append(", unowned");
	}

	result.append(", at x:" + signBlock.getX() + ", z:" + signBlock.getZ() + ", y:" + signBlock.getY());

	return result.toString();
    }

    public boolean checkPermission(Player player, OpType op) {
	return checkPermission(player, op, false);
    }

    public List<String> getCollaborators() {
	return Collections.unmodifiableList(collaborators);
    }

    public void addCollaborator(String name) {
	if (collaborators.contains(name)) {
	    return;
	}

	collaborators.add(name);
	saveProps();
    }

    public void removeCollaborator(String name) {
	collaborators.remove(name);
	saveProps();
    }

    public boolean checkPermission(Player player, OpType op, boolean silent) {
	if (!player.hasPermission(op.permissionValue)) {
	    if (!silent) {
		player.sendMessage("You do not have permission to " + op.toString().toLowerCase() + " a planning area");
	    }
	    return false;
	}

	if (!player.hasPermission("buildingplanner.anyarea") && Config.isOwnerEditOnly()) {
	    if (owner == null) {
		return true;
	    }

	    if (owner.equals(player.getName())) {
		return true;
	    }

	    if (op.allowCollaborators && collaborators.contains(player.getName())) {
		return true;
	    }

	    if (!silent) {
		if (op.allowCollaborators) {
		    player.sendMessage("You are not an owner or collaborator for the area");
		} else {
		    player.sendMessage("You are not an owner for the area");
		}
	    }
	    return false;
	}

	return true;
    }

    public void changeOwner(String newOwner) {
	setOwner(newOwner);
	saveProps();
    }
}

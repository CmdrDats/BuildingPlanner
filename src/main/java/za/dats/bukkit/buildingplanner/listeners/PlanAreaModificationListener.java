package za.dats.bukkit.buildingplanner.listeners;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Bed;
import org.bukkit.material.Door;

import za.dats.bukkit.buildingplanner.BuildingPlanner;
import za.dats.bukkit.buildingplanner.Config;
import za.dats.bukkit.buildingplanner.model.PlanArea;
import za.dats.bukkit.buildingplanner.model.PlanArea.OpType;

public class PlanAreaModificationListener extends BlockListener {
    @Override
    public void onBlockIgnite(BlockIgniteEvent event) {
	if (event.isCancelled()) {
	    return;
	}
	
	PlanArea area = BuildingPlanner.plugin.areaManager.getAffectedArea(event.getBlock());
	if (area == null) {
	    return;
	}
	
	if (!area.isCommitted() && area.isPlannedBlock(event.getBlock())) {
	    event.setCancelled(true);
	    return;
	}
        super.onBlockIgnite(event);
    }

    @Override
    public void onBlockFromTo(BlockFromToEvent event) {
	if (event.isCancelled()) {
	    return;
	}
	PlanArea area = BuildingPlanner.plugin.areaManager.getAffectedArea(event.getBlock());
	if (area == null) {
	    return;
	}
	
	event.setCancelled(true);
    }
    
    @Override
    public void onBlockDamage(BlockDamageEvent event) {
	if (event.isCancelled()) {
	    return;
	}
	
	PlanArea area = BuildingPlanner.plugin.areaManager.getAffectedArea(event.getBlock());
	if (area == null) {
	    return;
	}

	if (!area.checkPermission(event.getPlayer(), OpType.MODIFY)) {
	    event.setCancelled(true);
	    return;
	}
	
	if (area.isLocked()) {
	    event.getPlayer().sendMessage("Area is locked: "+area.getLockReason());
	    event.setCancelled(true);
	    return;
	}

	if (area.isPlannedBlock(event.getBlock())) {
	    BlockFace relativeDirection = null;
	    Block block = event.getBlock();
	    BlockState state = block.getState();

	    if (state.getData() instanceof Door) {
		Door door = (Door) state.getData();
		relativeDirection = door.isTopHalf() ? BlockFace.DOWN : BlockFace.UP;
	    } else if (state.getData() instanceof Bed) {
		Bed bed = (Bed) block.getState().getData();
		relativeDirection = bed.isHeadOfBed() ? bed.getFacing().getOppositeFace() : bed.getFacing();
	    }

	    if (relativeDirection != null) {
		Block relative = block.getRelative(relativeDirection);
		area.removePlannedBlock(relative, true);
		relative.setType(Material.AIR);
	    }

	    area.removePlannedBlock(block, true);
	    block.setType(Material.AIR);
	}

	super.onBlockDamage(event);
    }

    @Override
    public void onBlockBreak(BlockBreakEvent event) {
	if (event.isCancelled()) {
	    return;
	}
	
	Block block = event.getBlock();
	PlanArea area = BuildingPlanner.plugin.areaManager.getAffectedArea(block);
	if (area == null) {
	    return;
	}

	if (!area.checkPermission(event.getPlayer(), OpType.MODIFY)) {
	    event.setCancelled(true);
	    return;
	}

	if (area.isLocked()) {
	    event.getPlayer().sendMessage("Area is locked: "+area.getLockReason());
	    event.setCancelled(true);
	    return;
	}
	
	if (block == area.getSupplyBlock()) {
	    event.setCancelled(true);
	    event.getPlayer().sendMessage("This is the supply chest for plan, to get rid of it, break the plan sign");
	    return;
	}

	
	if (!area.isPlannedBlock(block)) {
	    final BlockState state = block.getState();

	    BlockFace relativeDirection = null;
	    if (state.getData() instanceof Door) {
		Door door = (Door) state.getData();
		relativeDirection = door.isTopHalf() ? BlockFace.DOWN : BlockFace.UP;
	    } else if (state.getData() instanceof Bed) {
		Bed bed = (Bed) block.getState().getData();
		relativeDirection = bed.isHeadOfBed() ? bed.getFacing().getOppositeFace() : bed.getFacing();
	    }

	    if (relativeDirection != null) {
		Block relative = block.getRelative(relativeDirection);
		relative.getState().setType(Material.AIR);
		area.addOriginalBlock(relative.getLocation(), relative.getState());
	    }

	    state.setType(Material.AIR);
	    area.addOriginalBlock(block.getLocation(), state);
	}
    }

    @Override
    public void onBlockPlace(BlockPlaceEvent event) {
	final Block block = event.getBlock();
	final Player player = event.getPlayer();
	final PlanArea area = BuildingPlanner.plugin.areaManager.getAffectedArea(block);
	if (area == null) {
	    return;
	}

	if (!area.checkPermission(event.getPlayer(), OpType.MODIFY)) {
	    event.setCancelled(true);
	    return;
	}

	if (area.isLocked()) {
	    event.getPlayer().sendMessage("Area is locked: "+area.getLockReason());
	    event.setCancelled(true);
	    return;
	}

	if (!validBlock(event.getBlockPlaced())) {
	    event.getPlayer().sendMessage("This block is not allowed to be placed in planning area");
	    event.setCancelled(true);
	    return;
	}
	
	final ItemStack itemInHand = player.getItemInHand();
	final Material type = itemInHand.getType();
	
	final short durability = itemInHand.getDurability();
	int task = BuildingPlanner.plugin.getServer().getScheduler()
		.scheduleSyncDelayedTask(BuildingPlanner.plugin, new Runnable() {
		    public void run() {
			addBlockToArea(block, player, area, type, durability);

			// Special extendable blocks.
			if (block.getState().getData() instanceof Door) {
			    addBlockToArea(block.getRelative(BlockFace.UP), player, area, null, (short) 0);
			} else if (block.getState().getData() instanceof Bed) {
			    Bed bed = (Bed) block.getState().getData();
			    addBlockToArea(block.getRelative(bed.getFacing()), player, area, null, (short) 0);
			}
		    }

		});

	super.onBlockPlace(event);
    }
    
    private boolean validBlock(Block blockPlaced) {
	Material type = blockPlaced.getType();
	return BuildingPlanner.plugin.areaManager.validBlockType(type);
    }

    protected void addBlockToArea(final Block block, final Player player, final PlanArea area, final Material type,
	    final short durability) {
	if (area.isCommitted()) {
	    // If committed area, means that we're changing the structure underlying the plan, so
	    // replace the original.
	    area.addOriginalBlock(block, true);
	} else {
	    if (type != null) {
		// 'Refund' the item when placed in a planning area.
		ItemStack newItem = new ItemStack(type, 1, durability);
		player.getInventory().addItem(newItem);
	    }

	    area.addPlanBlock(block);
	}
    }

}

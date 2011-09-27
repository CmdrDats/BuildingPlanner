package za.dats.bukkit.buildingplanner.listeners;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Bed;
import org.bukkit.material.Door;

import za.dats.bukkit.buildingplanner.BuildingPlanner;
import za.dats.bukkit.buildingplanner.model.PlanArea;

public class PlanAreaModificationListener extends BlockListener {

    private final List<PlanArea> planAreas;

    public PlanAreaModificationListener(List<PlanArea> planAreas) {
	this.planAreas = planAreas;
    }

    @Override
    public void onBlockDamage(BlockDamageEvent event) {
	PlanArea area = getAffectedArea(event.getBlock());
	if (area == null) {
	    return;
	}

	if (!event.getPlayer().hasPermission("buildingplanner.use")) {
	    return;
	}

	System.out.println("Block damage: " + event.getBlock().getState().getData());
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
    public void onBlockPhysics(BlockPhysicsEvent event) {
	PlanArea area = getAffectedArea(event.getBlock());
	if (area == null) {
	    return;
	}

	if (area.isPlannedBlock(event.getBlock())) {
	    area.removePlannedBlock(event.getBlock(), true);
	}
	System.out.println("Cancelling physics: "+event.getBlock().getState().getData()+" - "+event.getEventName());
	event.getBlock().setType(Material.AIR);
	event.setCancelled(true);
	super.onBlockPhysics(event);
    }

    @Override
    public void onBlockBreak(BlockBreakEvent event) {
	Block block = event.getBlock();
	PlanArea area = getAffectedArea(block);
	if (area == null) {
	    return;
	}

	if (!event.getPlayer().hasPermission("buildingplanner.use")) {
	    event.getPlayer().sendMessage("You're not allowed to modify planning areas");
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
	final PlanArea area = getAffectedArea(block);
	if (area == null) {
	    return;
	}

	if (!player.hasPermission("buildingplanner.use")) {
	    player.sendMessage("You're not allowed to build in planning areas");
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

    private PlanArea getAffectedArea(Block block) {
	for (PlanArea area : planAreas) {
	    if (area.fenceContains(block) || block.equals(area.getSignBlock())) {
		return null;
	    }

	    if (area.isInside(block)) {
		return area;
	    }
	}

	return null;
    }
}

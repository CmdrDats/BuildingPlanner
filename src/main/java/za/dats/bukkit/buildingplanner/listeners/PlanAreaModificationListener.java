package za.dats.bukkit.buildingplanner.listeners;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

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

	if (area.isPlannedBlock(event.getBlock())) {
	    area.removePlannedBlock(event.getBlock(), true);
	    event.getBlock().setType(Material.AIR);
	}
    }

    @Override
    public void onBlockPhysics(BlockPhysicsEvent event) {
	PlanArea area = getAffectedArea(event.getBlock());
	if (area == null) {
	    return;
	}
	event.setCancelled(true);
        super.onBlockPhysics(event);
    }
    
    @Override
    public void onBlockBreak(BlockBreakEvent event) {
	PlanArea area = getAffectedArea(event.getBlock());
	if (area == null) {
	    return;
	}

	if (event.getBlock() == area.getSupplyBlock()) {
	    event.setCancelled(true);
	    event.getPlayer().sendMessage(
		    "This is the supply chest for plan, to get rid of it, break the plan sign");
	}

	if (!area.isPlannedBlock(event.getBlock())) {
	    BlockState state = event.getBlock().getState();
	    state.setType(Material.AIR);
	    area.addOriginalBlock(event.getBlock().getLocation(), state);
	}
    }

    @Override
    public void onBlockPlace(BlockPlaceEvent event) {
	PlanArea area = getAffectedArea(event.getBlock());
	if (area == null) {
	    return;
	}

	if (area.isCommitted()) {
	    // If committed area, means that we're changing the structure underlying the plan, so replace the original.
	    area.addOriginalBlock(event.getBlock(), true);
	} else {
	    // 'Refund' the item when placed in a planning area.
	    ItemStack newItem = new ItemStack(event.getPlayer().getItemInHand().getType(), 1, event.getPlayer()
		    .getItemInHand().getDurability());
	    event.getPlayer().getInventory().addItem(newItem);

	    area.addPlanBlock(event.getBlock());
	}
	super.onBlockPlace(event);
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

package za.dats.bukkit.buildingplanner.model;

import org.bukkit.World;
import org.bukkit.block.Block;


public class Change {
    World world;
    int x, y, z;
    int state;
    int oldState;

    public Change(World world, int x, int y, int z, int newState) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        state = newState;
    }

    public void apply() {
        Block block = world.getBlockAt(x, y, z);
        oldState = BlockHelper.getSate(block.getState());
        
        block.setType(BlockHelper.getMaterial(state));
        block.setData(BlockHelper.getData(state));
    }
    
    public void undo() {
        Block block = world.getBlockAt(x, y, z);
	
        block.setType(BlockHelper.getMaterial(oldState));
        block.setData(BlockHelper.getData(oldState));
    }

}
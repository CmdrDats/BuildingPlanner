package za.dats.bukkit.buildingplanner.model;

import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.material.MaterialData;

public class BlockHelper {
    // Block format is 0xRRDDMMMM
    // Where R = reserved, D = data, M = material ID
    public static int getState(Material material, byte data) {
        int result = material.getId() & 0xFFFF;
        result += data << 16;
        return result;
    }

    public static Material getMaterial(int state) {
        short matType = (short) (state & 0xFFFF);
        return Material.getMaterial(matType);
    }

    public static byte getData(int state) {
        byte data = (byte) (state >> 16 & 0xFF);
        return data;
    }

    public static int getSate(BlockState state) {
        return getState(state.getType(), state.getRawData());
    }

    public static boolean isAttachable(int state) {
        short matType = (short) (state & 0xFFFF);
        if (matType == 50
    	    || // Torch
    	    matType == 75
    	    || matType == 76
    	    || // Redstone torch
    	    matType == 55
    	    || // Redstone wire
    	    matType == 63
    	    || // Sign Post
    	    matType == 64
    	    || matType == 71
    	    || // Doors
    	    matType == 66 || matType == 68
    	    || matType == 6
    	    || // Sapling
    	    matType == 27 || matType == 28
    	    || matType == 66
    	    || // Rails
    	    matType == 30 || matType == 31
    	    || matType == 32
    	    || // Cobwebs, grass
    	    matType == 37 || matType == 38 || matType == 39 || matType == 40 || matType == 59 || matType == 65
    	    || matType == 70 || matType == 72 || matType == 77 || matType == 83 || matType == 93
    	    || matType == 94 || matType == 96) {
    	return true;
        }
        return false;
    }

    public static MaterialData getMaterialData(int state) {
        MaterialData newData = getMaterial(state).getNewData(getData(state));
        return newData;
    }

    public static boolean isAir(int state) {
        return (state & 0xFFFF) == 0;
    }
}
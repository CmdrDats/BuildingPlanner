package za.dats.bukkit.buildingplanner.model;

import org.bukkit.Material;
import org.bukkit.material.MaterialData;

public class PlanItemMaterial extends MaterialData {
    public PlanItemMaterial(int type) {
	super(type);
    }
    
    public PlanItemMaterial(int type, byte data) {
	super(type, data);
    }
    
    public PlanItemMaterial(Material material) {
	super(material);
    }

    public PlanItemMaterial(Material material, byte data) {
	super(material, data);
    }
}

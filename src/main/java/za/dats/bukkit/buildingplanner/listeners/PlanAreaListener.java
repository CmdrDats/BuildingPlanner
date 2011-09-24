package za.dats.bukkit.buildingplanner.listeners;

import za.dats.bukkit.buildingplanner.model.PlanArea;

public interface PlanAreaListener {
    void create(PlanArea area);
    void destroy(PlanArea area);
}

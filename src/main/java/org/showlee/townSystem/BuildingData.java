package org.showlee.townSystem;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class BuildingData {
    private final String buildingId;
    private int level;
    private final UUID owner;
    private final Location location;
    private final List<String> unlocked;

    // Основной конструктор
    public BuildingData(@Nullable String buildingId, int level, @NotNull UUID owner,
                        @NotNull Location location) {
        this(buildingId, level, owner, location, new ArrayList<>());
    }

    // Конструктор с unlocked
    public BuildingData(@Nullable String buildingId, int level, @NotNull UUID owner,
                        @NotNull Location location, @NotNull List<String> unlocked) {
        this.buildingId = buildingId;
        this.level = level;
        this.owner = owner;
        this.location = location;
        this.unlocked = new ArrayList<>(unlocked);
    }

    // Геттеры
    public @Nullable String getBuildingId() { return buildingId; }
    public int getLevel() { return level; }
    public @NotNull UUID getOwner() { return owner; }
    public @NotNull Location getLocation() { return location; }
    public @NotNull List<String> getUnlocked() { return unlocked; }

    // Сеттеры
    public void setLevel(int level) { this.level = level; }
    public void addUnlocked(String buildingId) { unlocked.add(buildingId); }

    // Для сериализации
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("building-id", buildingId);
        data.put("level", level);
        data.put("owner", owner.toString());
        data.put("unlocked", new ArrayList<>(unlocked));
        return data;
    }

    public static @NotNull BuildingData deserialize(@NotNull Map<String, Object> data, @NotNull Location location) {
        return new BuildingData(
                (String) data.get("building-id"),
                (int) data.get("level"),
                UUID.fromString((String) data.get("owner")),
                location,
                (List<String>) data.getOrDefault("unlocked", new ArrayList<>())
        );
    }
}

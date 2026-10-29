package hu.donut.core;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public final class DataManager {
    private final DonutCore plugin;
    private final File file;
    public final Map<UUID, PlayerData> players = new HashMap<>();
    public final List<Listing> listings = new ArrayList<>();
    public final Map<String, Location> warps = new TreeMap<>();
    public Location spawn;

    public DataManager(DonutCore plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    public PlayerData get(UUID id) {
        return players.computeIfAbsent(id, PlayerData::new);
    }

    public PlayerData findByName(String name) {
        for (PlayerData d : players.values()) {
            if (d.name.equalsIgnoreCase(name)) return d;
        }
        return null;
    }

    public void load() {
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        spawn = y.getLocation("spawn");

        ConfigurationSection w = y.getConfigurationSection("warps");
        if (w != null) {
            for (String k : w.getKeys(false)) {
                Location l = w.getLocation(k);
                if (l != null) warps.put(k, l);
            }
        }

        ConfigurationSection p = y.getConfigurationSection("players");
        if (p != null) {
            for (String k : p.getKeys(false)) {
                ConfigurationSection s = p.getConfigurationSection(k);
                if (s == null) continue;
                try {
                    UUID id = UUID.fromString(k);
                    players.put(id, PlayerData.load(id, s));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        ConfigurationSection l = y.getConfigurationSection("listings");
        if (l != null) {
            for (String k : l.getKeys(false)) {
                ConfigurationSection s = l.getConfigurationSection(k);
                if (s == null) continue;
                ItemStack item = s.getItemStack("item");
                String seller = s.getString("seller");
                if (item == null || seller == null) continue;
                try {
                    listings.add(new Listing(UUID.fromString(k), UUID.fromString(seller),
                            s.getString("sellerName", "?"), item, s.getDouble("price"),
                            s.getLong("created"), s.getLong("expires")));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    public void save() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("spawn", spawn);
        for (Map.Entry<String, Location> e : warps.entrySet()) y.set("warps." + e.getKey(), e.getValue());
        for (PlayerData d : players.values()) d.save(y.createSection("players." + d.id));
        for (Listing li : listings) {
            ConfigurationSection s = y.createSection("listings." + li.id);
            s.set("seller", li.seller.toString());
            s.set("sellerName", li.sellerName);
            s.set("item", li.item);
            s.set("price", li.price);
            s.set("created", li.created);
            s.set("expires", li.expires);
        }
        try {
            plugin.getDataFolder().mkdirs();
            y.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not save data.yml: " + ex.getMessage());
        }
    }
}

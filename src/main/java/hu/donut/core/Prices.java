package hu.donut.core;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Prices {
    public record Entry(double buy, double sell) {}

    public record Sale(int amount, double money) {}

    private final DonutCore plugin;
    private final Map<Material, Entry> entries = new LinkedHashMap<>();

    public Prices(DonutCore plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        entries.clear();
        ConfigurationSection s = plugin.getConfig().getConfigurationSection("shop");
        if (s == null) return;
        for (String key : s.getKeys(false)) {
            Material m = Material.matchMaterial(key);
            if (m == null || !m.isItem()) {
                plugin.getLogger().warning("Unknown shop material: " + key);
                continue;
            }
            entries.put(m, new Entry(s.getDouble(key + ".buy", -1), s.getDouble(key + ".sell", -1)));
        }
    }

    public Map<Material, Entry> entries() {
        return entries;
    }

    public Entry get(Material m) {
        return entries.get(m);
    }

    /** Sells plain (no item meta) items from the player's inventory. only == null sells everything sellable. */
    public Sale sell(Player p, Material only, int max) {
        int total = 0;
        double earned = 0;
        ItemStack[] contents = p.getInventory().getStorageContents();
        for (int i = 0; i < contents.length && total < max; i++) {
            ItemStack it = contents[i];
            if (it == null || it.hasItemMeta()) continue;
            if (only != null && it.getType() != only) continue;
            Entry e = entries.get(it.getType());
            if (e == null || e.sell() <= 0) continue;
            int take = Math.min(it.getAmount(), max - total);
            total += take;
            earned += take * e.sell();
            if (take == it.getAmount()) contents[i] = null;
            else it.setAmount(it.getAmount() - take);
        }
        p.getInventory().setStorageContents(contents);
        return new Sale(total, earned);
    }
}

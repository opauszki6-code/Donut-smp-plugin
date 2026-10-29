package hu.donut.core;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public abstract class Menu implements InventoryHolder {
    protected final Inventory inv;
    private final Map<Integer, Consumer<InventoryClickEvent>> actions = new HashMap<>();

    protected Menu(int rows, String title) {
        Component t = Util.c(title);
        this.inv = Bukkit.createInventory(this, rows * 9, t);
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    protected void set(int slot, ItemStack item, Consumer<InventoryClickEvent> action) {
        inv.setItem(slot, item);
        if (action != null) actions.put(slot, action);
        else actions.remove(slot);
    }

    protected void clear() {
        inv.clear();
        actions.clear();
    }

    protected void fillBottomRow(ItemStack pane) {
        int start = inv.getSize() - 9;
        for (int i = start; i < inv.getSize(); i++) set(i, pane, null);
    }

    public void click(InventoryClickEvent e) {
        Consumer<InventoryClickEvent> a = actions.get(e.getRawSlot());
        if (a != null) a.accept(e);
    }

    public void open(Player p) {
        p.openInventory(inv);
    }
}

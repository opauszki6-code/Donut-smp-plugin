package hu.donut.core;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public final class Listing {
    public final UUID id;
    public final UUID seller;
    public final String sellerName;
    public final ItemStack item;
    public final double price;
    public final long created;
    public final long expires;

    public Listing(UUID id, UUID seller, String sellerName, ItemStack item, double price, long created, long expires) {
        this.id = id;
        this.seller = seller;
        this.sellerName = sellerName;
        this.item = item;
        this.price = price;
        this.created = created;
        this.expires = expires;
    }

    public boolean expired() {
        return System.currentTimeMillis() > expires;
    }
}

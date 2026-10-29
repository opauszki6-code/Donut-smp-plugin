package hu.donut.core;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public final class AuctionService {
    private final DonutCore pl;

    public AuctionService(DonutCore pl) {
        this.pl = pl;
    }

    public boolean list(Player p, ItemStack item, double price) {
        var cfg = pl.getConfig();
        double min = cfg.getDouble("ah.min-price", 1);
        if (price < min) {
            Util.msg(p, "<red>Minimum price is <white>$" + Util.fmt(min) + "<red>.");
            return false;
        }
        long own = pl.data().listings.stream().filter(l -> l.seller.equals(p.getUniqueId())).count();
        int max = cfg.getInt("ah.max-listings", 10);
        if (own >= max) {
            Util.msg(p, "<red>You can only have <white>" + max + "<red> listings at once.");
            return false;
        }
        long now = System.currentTimeMillis();
        long dur = cfg.getLong("ah.duration-hours", 48) * 3600_000L;
        pl.data().listings.add(new Listing(UUID.randomUUID(), p.getUniqueId(), p.getName(),
                item.clone(), price, now, now + dur));
        return true;
    }

    public void buy(Player buyer, Listing l) {
        if (!pl.data().listings.contains(l)) {
            Util.msg(buyer, "<red>That listing is no longer available.");
            return;
        }
        if (l.expired()) {
            Util.msg(buyer, "<red>That listing has expired.");
            return;
        }
        if (l.seller.equals(buyer.getUniqueId())) {
            Util.msg(buyer, "<red>You can't buy your own listing. Click it in <white>/ah mine <red>to cancel.");
            return;
        }
        PlayerData b = pl.data().get(buyer.getUniqueId());
        if (b.money < l.price) {
            Util.msg(buyer, "<red>You need <white>$" + Util.fmt(l.price - b.money) + "<red> more.");
            return;
        }
        double tax = pl.getConfig().getDouble("ah.tax-percent", 5);
        double payout = l.price * (1 - tax / 100.0);
        b.money -= l.price;
        PlayerData s = pl.data().get(l.seller);
        s.money += payout;
        pl.data().listings.remove(l);
        Util.give(buyer, l.item.clone());
        Util.msg(buyer, "<green>Bought <white>" + Util.itemLabel(l.item) + "<green> for <white>$" + Util.fmt(l.price) + "<green>.");
        Player seller = Bukkit.getPlayer(l.seller);
        if (seller != null && s.is(Setting.AUCTION_ALERTS)) {
            Util.msg(seller, "<green>" + buyer.getName() + " bought your <white>" + Util.itemLabel(l.item)
                    + "<green>. You received <white>$" + Util.fmt(payout) + "<green>.");
        }
    }

    public void cancel(Player p, Listing l) {
        if (!l.seller.equals(p.getUniqueId())) return;
        if (!pl.data().listings.remove(l)) return;
        Util.give(p, l.item.clone());
        Util.msg(p, "<yellow>" + (l.expired() ? "Claimed expired listing: " : "Cancelled listing: ")
                + "<white>" + Util.itemLabel(l.item) + "<yellow>.");
    }
}

package hu.donut.core;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public final class TeleportService {
    private static final Set<Material> UNSAFE = EnumSet.of(Material.LAVA, Material.WATER, Material.FIRE,
            Material.SOUL_FIRE, Material.MAGMA_BLOCK, Material.CACTUS, Material.CAMPFIRE,
            Material.SOUL_CAMPFIRE, Material.POWDER_SNOW, Material.SWEET_BERRY_BUSH, Material.COBWEB);

    private final DonutCore pl;
    private final Map<UUID, BukkitTask> pending = new HashMap<>();
    private final Map<UUID, Long> rtpCooldown = new HashMap<>();

    public TeleportService(DonutCore pl) {
        this.pl = pl;
    }

    public boolean isPending(Player p) {
        return pending.containsKey(p.getUniqueId());
    }

    public void cancel(Player p, boolean notify) {
        BukkitTask t = pending.remove(p.getUniqueId());
        if (t == null) return;
        t.cancel();
        if (notify) Util.msg(p, "<red>Teleport cancelled.");
    }

    /** Teleports after the warmup; moving or taking damage cancels it. */
    public void teleport(Player p, Location dest) {
        int secs = pl.getConfig().getInt("teleport.warmup-seconds", 3);
        if (secs <= 0 || p.hasPermission("donutcore.bypass.warmup")) {
            go(p, dest);
            return;
        }
        cancel(p, false);
        int[] left = {secs};
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!p.isOnline()) {
                    pending.remove(p.getUniqueId());
                    cancel();
                    return;
                }
                if (left[0] <= 0) {
                    pending.remove(p.getUniqueId());
                    cancel();
                    go(p, dest);
                    return;
                }
                p.sendActionBar(Util.c("<yellow>Teleporting in <white>" + left[0] + "s<yellow>... don't move!"));
                left[0]--;
            }
        }.runTaskTimer(pl, 0L, 20L);
        pending.put(p.getUniqueId(), task);
    }

    private void go(Player p, Location dest) {
        p.teleportAsync(dest).thenAccept(ok -> {
            if (ok) {
                p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 1f);
                p.sendActionBar(Util.c("<green>Teleported!"));
            }
        });
    }

    public void rtp(Player p) {
        var cfg = pl.getConfig();
        long cd = cfg.getLong("rtp.cooldown-seconds", 30) * 1000;
        Long last = rtpCooldown.get(p.getUniqueId());
        long now = System.currentTimeMillis();
        if (last != null && now - last < cd && !p.hasPermission("donutcore.bypass.cooldown")) {
            Util.msg(p, "<red>RTP cooldown: <white>" + Util.time(cd - (now - last)) + "<red> left.");
            return;
        }
        World w = Bukkit.getWorld(cfg.getString("rtp.world", "world"));
        if (w == null) {
            Util.msg(p, "<red>The RTP world is not loaded.");
            return;
        }
        Util.msg(p, "<gray>Searching for a safe location...");
        find(w, cfg.getInt("rtp.attempts", 15), loc -> {
            if (!p.isOnline()) return;
            if (loc == null) {
                Util.msg(p, "<red>Couldn't find a safe spot, try again.");
                return;
            }
            rtpCooldown.put(p.getUniqueId(), System.currentTimeMillis());
            teleport(p, loc);
        });
    }

    private void find(World w, int tries, Consumer<Location> done) {
        if (tries <= 0) {
            done.accept(null);
            return;
        }
        var cfg = pl.getConfig();
        double min = cfg.getDouble("rtp.min-radius", 500);
        double max = Math.max(min + 1, cfg.getDouble("rtp.max-radius", 5000));
        ThreadLocalRandom r = ThreadLocalRandom.current();
        double angle = r.nextDouble() * Math.PI * 2;
        double dist = min + r.nextDouble() * (max - min);
        Location spawn = w.getSpawnLocation();
        int x = (int) Math.floor(spawn.getX() + Math.cos(angle) * dist);
        int z = (int) Math.floor(spawn.getZ() + Math.sin(angle) * dist);

        w.getChunkAtAsync(x >> 4, z >> 4).thenAccept(chunk ->
                Bukkit.getScheduler().runTask(pl, () -> {
                    int y = w.getHighestBlockYAt(x, z);
                    Block ground = w.getBlockAt(x, y, z);
                    Location loc = new Location(w, x + 0.5, y + 1, z + 0.5);
                    if (isSafe(ground) && w.getWorldBorder().isInside(loc)) done.accept(loc);
                    else find(w, tries - 1, done);
                }));
    }

    private boolean isSafe(Block ground) {
        Material m = ground.getType();
        if (!m.isSolid() || ground.isLiquid() || UNSAFE.contains(m) || Tag.LEAVES.isTagged(m)) return false;
        Block feet = ground.getRelative(0, 1, 0);
        Block head = ground.getRelative(0, 2, 0);
        return feet.isPassable() && !feet.isLiquid() && head.isPassable() && !head.isLiquid();
    }
}

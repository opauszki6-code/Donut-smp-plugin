package hu.donut.core;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ScoreboardService {
    private static final int LINES = 9;

    private final DonutCore pl;
    private final Map<UUID, Scoreboard> boards = new HashMap<>();

    public ScoreboardService(DonutCore pl) {
        this.pl = pl;
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(pl, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) update(p);
        }, 20L, 20L);
    }

    public void remove(Player p) {
        Scoreboard sb = boards.remove(p.getUniqueId());
        if (sb != null && p.isOnline() && p.getScoreboard() == sb) {
            p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    public void update(Player p) {
        PlayerData d = pl.data().get(p.getUniqueId());
        if (!d.is(Setting.SCOREBOARD)) {
            remove(p);
            return;
        }
        Scoreboard sb = boards.computeIfAbsent(p.getUniqueId(), id -> create());
        if (p.getScoreboard() != sb) p.setScoreboard(sb);

        List<String> lines = List.of(
                "",
                "<green>$ <white>Money <gray>" + Util.fmt(d.money),
                "<aqua>◆ <white>Shards <gray>" + Util.fmt(d.shards),
                "<red>⚔ <white>Kills <gray>" + d.kills,
                "<red>☠ <white>Deaths <gray>" + d.deaths,
                "<yellow>⌚ <white>Playtime <gray>" + Util.time(d.totalPlaytime()),
                "<light_purple>● <white>Online <gray>" + Bukkit.getOnlinePlayers().size(),
                "",
                "<gold>" + pl.getConfig().getString("server-ip", "donutsmp.example"));
        for (int i = 0; i < LINES; i++) {
            Team t = sb.getTeam("l" + i);
            if (t != null) t.prefix(Util.c(lines.get(i)));
        }
    }

    private Scoreboard create() {
        Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective o = sb.registerNewObjective("dc", Criteria.DUMMY,
                Util.c(pl.getConfig().getString("scoreboard.title", "<gold><bold>DONUT SMP")));
        o.setDisplaySlot(DisplaySlot.SIDEBAR);
        o.numberFormat(NumberFormat.blank());
        for (int i = 0; i < LINES; i++) {
            String entry = "§" + Integer.toHexString(i) + "§r";
            Team t = sb.registerNewTeam("l" + i);
            t.addEntry(entry);
            o.getScore(entry).setScore(LINES - i);
        }
        return sb;
    }
}

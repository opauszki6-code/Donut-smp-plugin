package hu.donut.core;

import org.bukkit.Material;

public enum Setting {
    SCOREBOARD("Scoreboard", Material.OAK_SIGN, "Show the sidebar scoreboard"),
    TPA("Teleport Requests", Material.ENDER_PEARL, "Allow players to send you /tpa"),
    PAYMENTS("Payments", Material.GOLD_INGOT, "Accept /pay from other players"),
    MESSAGES("Private Messages", Material.PAPER, "Allow /msg from other players"),
    AUCTION_ALERTS("Auction Alerts", Material.BELL, "Notify me when my listings sell"),
    DEATH_MESSAGES("Death Messages", Material.SKELETON_SKULL, "Show death messages in chat"),
    MENU_SOUNDS("Menu Sounds", Material.NOTE_BLOCK, "Play click sounds in menus");

    public final String title;
    public final Material icon;
    public final String desc;

    Setting(String title, Material icon, String desc) {
        this.title = title;
        this.icon = icon;
        this.desc = desc;
    }
}

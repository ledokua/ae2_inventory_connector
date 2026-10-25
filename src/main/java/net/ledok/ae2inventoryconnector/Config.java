package net.ledok.ae2inventoryconnector;

import java.util.List;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue SCAN_RANGE;
    public static final ModConfigSpec.IntValue MAX_SCANNED_BLOCKS;
    public static final ModConfigSpec.BooleanValue ONLY_TRIMS_CONNECT;
    public static final ModConfigSpec.DoubleValue IDLE_POWER;
    public static final ModConfigSpec.IntValue SAFETY_REFRESH_TICKS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> CONNECTOR_BLACKLIST;

    /** Parsed {@link #CONNECTOR_BLACKLIST}; rebuilt when the config file changes or on /reload (tags). */
    private static Blacklist blacklist = Blacklist.EMPTY;

    static {
        var b = new ModConfigSpec.Builder();
        b.push("me_inventory_connector");
        SCAN_RANGE = b.comment("Max distance (in blocks) from the connector at which inventories are discovered.",
                "Same meaning as Tom's Storage 'inventoryConnectorRange'.")
                .defineInRange("scanRange", 16, 1, 64);
        MAX_SCANNED_BLOCKS = b.comment("Hard cap on blocks visited by one topology scan.")
                .defineInRange("maxScannedBlocks", 8192, 64, 1_000_000);
        ONLY_TRIMS_CONNECT = b.comment("If true, the scan only continues through trims, not through inventories.")
                .define("onlyTrimsConnect", false);
        IDLE_POWER = b.comment("AE energy drained per tick by each connector.")
                .defineInRange("idlePowerUsage", 1.0, 0.0, 1000.0);
        SAFETY_REFRESH_TICKS = b.comment(
                "Contents are refreshed when a connected block entity calls setChanged().",
                "A few modded inventories change items without doing that; this interval marks every",
                "inventory as possibly changed so it is re-read the next time AE asks. 0 disables it.")
                .defineInRange("safetyRefreshTicks", 200, 0, 72000);
        CONNECTOR_BLACKLIST = b.comment(
                "Blocks the connector must never use as inventories. Works like the",
                "ae2_inventory_connector:connector_skip block tag, without needing a datapack.",
                "  \"minecraft:furnace\"      a block",
                "  \"#c:barrels\"             every block in a block tag",
                "  \"@sophisticatedstorage\"  every block of a mod",
                "Saving this file applies changes right away; /reload also rescans.")
                .defineListAllowEmpty("connectorBlacklist", List.of(), () -> "", o -> o instanceof String);
        b.pop();
        SPEC = b.build();
    }

    private Config() {
    }

    public static Blacklist blacklist() {
        return blacklist;
    }

    /** Re-parses the blacklist (fresh tag lookups too). */
    public static void reloadBlacklist() {
        blacklist = SPEC.isLoaded() ? Blacklist.parse(CONNECTOR_BLACKLIST.get()) : Blacklist.EMPTY;
    }
}

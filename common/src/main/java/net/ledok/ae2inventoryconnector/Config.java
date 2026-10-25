package net.ledok.ae2inventoryconnector;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

/**
 * {@code config/ae2_inventory_connector.properties}, read when the server starts and again on {@code /reload}. A plain
 * properties file keeps the mod free of a config library on either loader.
 */
public final class Config {
    public static int scanRange = 16;
    public static int maxScannedBlocks = 8192;
    public static boolean onlyTrimsConnect = false;
    public static double idlePowerUsage = 1.0;
    public static int safetyRefreshTicks = 200;
    public static Blacklist connectorBlacklist = Blacklist.EMPTY;
    private static String connectorBlacklistText = "";

    private static final String FILE = AE2InventoryConnector.MOD_ID + ".properties";

    private Config() {
    }

    static void load(Path configDir) {
        var file = configDir.resolve(FILE);
        var p = new Properties();
        if (Files.exists(file)) {
            try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                p.load(r);
            } catch (IOException e) {
                AE2InventoryConnector.LOGGER.error("Could not read {}, using defaults", file, e);
            }
        }
        scanRange = intValue(p, "scanRange", 16, 1, 64);
        maxScannedBlocks = intValue(p, "maxScannedBlocks", 8192, 64, 1_000_000);
        onlyTrimsConnect = Boolean.parseBoolean(p.getProperty("onlyTrimsConnect", "false").trim());
        idlePowerUsage = doubleValue(p, "idlePowerUsage", 1.0, 0, 1000);
        safetyRefreshTicks = intValue(p, "safetyRefreshTicks", 200, 0, 72000);
        connectorBlacklistText = p.getProperty("connectorBlacklist", "").trim();
        connectorBlacklist = Blacklist.parse(List.of(connectorBlacklistText.split("[,\\s]+")));
        write(file);
    }

    private static void write(Path file) {
        var text = """
                # AE2 Inventory Connector server config. Run /reload to apply changes
                # (idlePowerUsage only affects connectors loaded after that).

                # Max distance (in blocks) from the connector at which inventories are discovered (1-64).
                # Same meaning as Tom's Storage 'inventoryConnectorRange'.
                scanRange=%d

                # Hard cap on blocks visited by one topology scan (64-1000000).
                maxScannedBlocks=%d

                # If true, the scan only continues through trims, not through inventories.
                onlyTrimsConnect=%s

                # AE energy drained per tick by each connector (0-1000).
                idlePowerUsage=%s

                # Contents are refreshed when a connected block entity calls setChanged(). A few modded
                # inventories change items without doing that; this interval (ticks) marks every inventory
                # as possibly changed so it is re-read the next time AE asks. 0 disables it.
                safetyRefreshTicks=%d

                # Blocks the connector must never use as inventories, separated by commas. Works like the
                # ae2_inventory_connector:connector_skip block tag, without needing a datapack.
                #   minecraft:furnace       a block
                #   #forge:barrels          every block in a block tag
                #   @sophisticatedstorage   every block of a mod
                # Example: connectorBlacklist=minecraft:furnace, #forge:barrels, @sophisticatedstorage
                connectorBlacklist=%s
                """.formatted(scanRange, maxScannedBlocks, onlyTrimsConnect, idlePowerUsage, safetyRefreshTicks,
                connectorBlacklistText);
        try {
            Files.createDirectories(file.getParent());
            try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                w.write(text);
            }
        } catch (IOException e) {
            AE2InventoryConnector.LOGGER.error("Could not write {}", file, e);
        }
    }

    private static int intValue(Properties p, String key, int def, int min, int max) {
        try {
            return Math.max(min, Math.min(max, Integer.parseInt(p.getProperty(key, "" + def).trim())));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static double doubleValue(Properties p, String key, double def, double min, double max) {
        try {
            return Math.max(min, Math.min(max, Double.parseDouble(p.getProperty(key, "" + def).trim())));
        } catch (NumberFormatException e) {
            return def;
        }
    }
}

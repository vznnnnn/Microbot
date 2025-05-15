package net.runelite.client.plugins.microbot.vzn.prohunter.strategy;

import lombok.Getter;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.Global;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItem;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.models.RS2Item;
import net.runelite.client.plugins.microbot.vzn.prohunter.ProHunterPlugin;
import net.runelite.client.plugins.microbot.vzn.prohunter.ProHunterStrategy;

public class NetTrapStrategy implements ProHunterStrategy {

    private static final int YOUNG_TREE_ID   = 9341;
    private static final int TRAP_SET_ID = 9257;
    private static final int TRAP_CAUGHT_ID = 9004; // when a lizard is caught
    private static final int ROPE_ID         = 954;
    private static final int SMALL_NET_ID    = 303;

    private static final WorldPoint SOUTH_TREE = new WorldPoint(3682, 3406, 0);
    private static final WorldPoint NORTH_TREE = new WorldPoint(3681, 3409, 0);

    @Getter private ProHunterPlugin plugin;

    public NetTrapStrategy(ProHunterPlugin plugin) {
        this.plugin = plugin;
    }

    /* ------------------------------------------------------------
     * Helpers
     * ---------------------------------------------------------- */

    /** One tile north of a tree is where the rope+net land. */
    private static WorldPoint lootTile(WorldPoint treeTile) {
        return new WorldPoint(treeTile.getX(), treeTile.getY() + 1, treeTile.getPlane());
    }

    /** Pick up rope / small net lying on the loot-tile. */
    private static void lootRopeAndNet(WorldPoint treeTile) {
        WorldPoint lootTile = new WorldPoint(treeTile.getX(), treeTile.getY() + 1, treeTile.getPlane());

        RS2Item[] groundItems = Rs2GroundItem.getAll(3); // 3-tile radius is safe

        for (RS2Item rs2Item : groundItems) {
            int itemId = rs2Item.getItem().getId();
            Tile tile = rs2Item.getTile();
            WorldPoint itemLocation = tile.getWorldLocation();

            if ((itemId == ROPE_ID || itemId == SMALL_NET_ID) && itemLocation.equals(lootTile)) {
                Rs2GroundItem.lootItemsBasedOnLocation(itemLocation, itemId);

                Global.sleepUntil(() ->
                        Rs2Inventory.hasItem(itemId), 2000);
            }
        }
    }

    /** Drop any caught swamp lizards if inventory is full. */
    private static void handleFullInventory() {
        if (Rs2Inventory.isFull()) {
            while (Rs2Inventory.hasItem("Swamp lizard")) {
                Rs2Inventory.interact("Swamp lizard", "Release");
                Global.sleep(50); // brief delay to avoid rapid clicks
            }
        }
    }

    /* ------------------------------------------------------------
     * Main trap-handling logic (simplified)
     * ---------------------------------------------------------- */

    private static void handleTrap(WorldPoint treeTile) {
        TileObject obj = Rs2GameObject.findObjectByLocation(treeTile);
        int objId = (obj != null) ? obj.getId() : -1;

        // ✅ Handle trap with successful catch (9004) immediately
        if (objId == TRAP_CAUGHT_ID) {
            Rs2GameObject.interact(obj, "Reset Net trap");
            Global.sleepUntil(() ->
                    !Rs2Inventory.isFull() && !Rs2Inventory.hasItem("Swamp lizard"), 3000);
            return;
        }

        // Handle regular active trap (9257), check if it has caught a lizard
        if (objId == TRAP_SET_ID) {
            ObjectComposition comp = Rs2GameObject.getObjectComposition(obj.getId());

            if (comp != null && Rs2GameObject.hasAction(comp, "Reset Net trap")) {
                Rs2GameObject.interact(obj, "Reset Net trap");
                Global.sleepUntil(() ->
                        !Rs2Inventory.isFull() && !Rs2Inventory.hasItem("Swamp lizard"), 3000);
                return;
            }

            // Trap is set but idle, do nothing
            return;
        }

        // Trap is missing or tree is idle → try setting a trap
        if ((obj == null || objId == YOUNG_TREE_ID) &&
                Rs2Inventory.hasItem("Rope") && Rs2Inventory.hasItem("Small fishing net")) {

            Rs2GameObject.interact(treeTile, "Set-trap");

            Global.sleepUntil(() -> {
                TileObject trapObj = Rs2GameObject.findObjectByLocation(treeTile);
                int trapId = (trapObj != null) ? trapObj.getId() : -1;
                return trapId == TRAP_SET_ID || trapId == TRAP_CAUGHT_ID;
            }, 4000);
        }
    }

    @Override
    public String getName() {
        return "Net Trap";
    }

    @Override
    public void run(ProHunterPlugin plugin) {
        // First, always handle full inventory
        handleFullInventory();

        // Then, always check both loot tiles for dropped gear
        lootRopeAndNet(SOUTH_TREE);
        lootRopeAndNet(NORTH_TREE);

        // Then, check if we're over our max script time
        plugin.getScript().checkScriptDurationAndLogout();

        // Finally, handle traps
        handleTrap(SOUTH_TREE);
        handleTrap(NORTH_TREE);

        // Tiny delay – tune as you like
        Global.sleep(600);
    }

}

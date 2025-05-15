package net.runelite.client.plugins.microbot.vzn.prohunter.strategy;

import lombok.Getter;
import net.runelite.api.ObjectComposition;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.Global;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItem;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.models.RS2Item;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.vzn.prohunter.ProHunterPlugin;
import net.runelite.client.plugins.microbot.vzn.prohunter.ProHunterStrategy;

import java.util.List;
import java.util.stream.Collectors;

public class BoxTrapStrategy implements ProHunterStrategy {

    private static final int BOX_TRAP_ITEM_ID = 10008;
    private static final int ACTIVE_TRAP_ID = 9380;
    private static final int RESET_TRAP_ID = 9385;
    private static final int SHAKING_TRAP_ID = 50727;

    private static final WorldPoint START_TILE = new WorldPoint(1666, 3001, 0);
    private static final int MAX_TRAPS = 3;

    @Getter private ProHunterPlugin plugin;

    public BoxTrapStrategy(ProHunterPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean isOurTrap(TileObject obj) {
        if (obj == null) return false;
        int id = obj.getId();
        return id == ACTIVE_TRAP_ID || id == RESET_TRAP_ID || id == SHAKING_TRAP_ID;
    }

    private List<WorldPoint> getOwnedTrapLocations() {
        return Rs2GameObject.getAll()
                .stream()
                .filter(this::isOurTrap)
                .map(TileObject::getWorldLocation)
                .distinct()
                .collect(Collectors.toList());
    }

    private void placeTrapNearStart() {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                WorldPoint tile = START_TILE.dx(dx).dy(dy);

                if (!trapExists(tile) && Rs2Inventory.hasItem(BOX_TRAP_ITEM_ID)) {
                    Rs2Walker.walkTo(tile, 0);

                    boolean atTile = Global.sleepUntil(() ->
                            Rs2Player.getWorldLocation().distanceTo(tile) <= 1, 3000);

                    Global.sleep(1000);

                    if (atTile) {
                        Rs2Inventory.interact(BOX_TRAP_ITEM_ID, "Lay");
                        Global.sleepUntil(() -> trapExists(tile), 4000);
                        return;
                    }
                }
            }
        }
    }

    private boolean trapExists(WorldPoint tile) {
        TileObject obj = Rs2GameObject.findObjectByLocation(tile);
        return isOurTrap(obj);
    }

    private void resetTrap(TileObject obj) {
        if (obj == null) return;
        ObjectComposition comp = Rs2GameObject.getObjectComposition(obj.getId());
        if (comp != null && Rs2GameObject.hasAction(comp, "Reset")) {
            Rs2GameObject.interact(obj, "Reset");
            Global.sleep(1000);
            Global.sleepUntil(() -> Rs2GameObject.getGameObject(obj.getWorldLocation()).getId() == ACTIVE_TRAP_ID, 3000);
            Global.sleep(500);
        }
    }

    private void lootTraps() {
        RS2Item[] items = Rs2GroundItem.getAll(3);
        for (RS2Item item : items) {
            if (item.getItem().getId() == BOX_TRAP_ITEM_ID && Rs2Player.distanceTo(START_TILE) <= 5) {
                Rs2GroundItem.lootItemsBasedOnLocation(item.getTile().getWorldLocation(), BOX_TRAP_ITEM_ID);
                Global.sleepUntil(() -> Rs2Player.getWorldLocation().equals(item.getTile().getWorldLocation()) && Rs2Inventory.hasItem(BOX_TRAP_ITEM_ID), 2000);
                Global.sleep(1000);
            }
        }
    }

    @Override
    public String getName() {
        return "Box Trap";
    }

    @Override
    public void run(ProHunterPlugin plugin) {
        if (Rs2Player.isAnimating()) {
            Microbot.status = "Animating";
            Global.sleepUntil(() -> !Rs2Player.isAnimating(), 2000);
            Global.sleep(500);
        }

        lootTraps();

        if (Rs2Player.getWorldLocation().distanceTo(START_TILE) > 5) {
            Rs2Walker.walkTo(START_TILE, 0);
            Global.sleepUntil(() -> Rs2Player.getWorldLocation().distanceTo(START_TILE) <= 2, 5000);
            Global.sleep(500);
        }

        List<WorldPoint> currentTraps = getOwnedTrapLocations();

        for (WorldPoint trapTile : currentTraps) {
            TileObject obj = Rs2GameObject.findObjectByLocation(trapTile);
            if (obj == null) continue;

            if (obj.getId() == RESET_TRAP_ID || obj.getId() == SHAKING_TRAP_ID) {
                resetTrap(obj);
            }
        }

        // Place trap if we can
        if (currentTraps.size() < MAX_TRAPS && Rs2Inventory.hasItem(BOX_TRAP_ITEM_ID)) {
            placeTrapNearStart();
        }
    }

}

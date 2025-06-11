package net.runelite.client.plugins.microbot.mining;

import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.mining.enums.Rocks;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.depositbox.Rs2DepositBox;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.misc.Rs2UiHelper;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.security.Login;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

enum State {
    MINING,
    RESETTING,
}

public class AutoMiningScript extends Script {

    public static final String version = "1.4.4";
    private static final int GEM_MINE_UNDERGROUND = 11410;
    private static final int BASALT_MINE = 11425;
    private final int TRACKED_ORES_LIMIT = 10;
    private final LinkedList<WorldPoint> recentlyMinedOres = new LinkedList<>();
    private final Map<WorldPoint, Long> oreRespawnTimers = new HashMap<>();
    private final long ORE_RESPAWN_TIME_MS = 30000; // 30 seconds respawn (adjust per ore)
    State state = State.MINING;

    public boolean run(AutoMiningConfig config) {
        initialPlayerLocation = null;
        Rs2Antiban.resetAntibanSettings();
        Rs2Antiban.antibanSetupTemplates.applyMiningSetup();
        Rs2AntibanSettings.actionCooldownChance = 0.1;
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!super.run()) return;
                if (!Microbot.isLoggedIn()) return;
                if (Rs2AntibanSettings.actionCooldownActive) return;
                if (initialPlayerLocation == null) {
                    initialPlayerLocation = Rs2Player.getWorldLocation();
                }

                if (!config.ORE().hasRequiredLevel()) {
                    Microbot.log("You do not have the required mining level to mine this ore.");
                    return;
                }

                if (Rs2Equipment.isWearing("Dragon pickaxe"))
                    Rs2Combat.setSpecState(true, 1000);

                if (Rs2Player.isMoving() || Rs2Player.isAnimating() || Microbot.pauseAllScripts) return;

                //code to change worlds if there are too many players in the distance to stray tiles
                int maxPlayers = config.maxPlayersInArea();
                if (maxPlayers > 0) {
                    WorldPoint localLocation = Rs2Player.getWorldLocation();

                    long nearbyPlayers = Microbot.getClient().getPlayers().stream()
                            .filter(p -> p != null && p != Microbot.getClient().getLocalPlayer())
                            .filter(p -> {
                                if (config.distanceToStray() == 0) {
                                    // Only count players standing on the same exact tile
                                    return p.getWorldLocation().equals(localLocation);
                                }
                                // Count players within distanceToStray
                                return p.getWorldLocation().distanceTo(localLocation) <= config.distanceToStray();
                            })
                            //filter if players are using mining animation
                            .filter(p -> p.getAnimation() != -1)
                            .count();

                    if (nearbyPlayers >= maxPlayers) {
                        Microbot.status = "Too many players nearby. Hopping...";
                        Rs2Random.waitEx(3200, 800); // Delay to avoid UI locking

                        int world = Login.getRandomWorld(Rs2Player.isMember());
                        boolean hopped = Microbot.hopToWorld(world);
                        if (hopped) {
                            Microbot.status = "Hopped to world: " + world;
                            return; // Exit current cycle after hop
                        }
                    }
                }


                switch (state) {
                    case MINING:
                        if (Rs2Inventory.isFull()) {
                            state = State.RESETTING;
                            return;
                        }

                        GameObject rock = Rs2GameObject.findReachableObject(config.ORE().getName(), true, config.distanceToStray(), initialPlayerLocation);

                        if (rock != null) {
                            System.out.println("Interacting with rock");
                            if (Rs2GameObject.interact(rock)) {
                                System.out.println("Interacted with rock");
                                Microbot.status = "Mining " + config.ORE().getName();

                                WorldPoint minedTile = rock.getWorldLocation();
                                trackMinedOre(minedTile); // Track mined ore

                                Rs2Player.waitForXpDrop(Skill.MINING, true);
                                Rs2Antiban.actionCooldown();
                                Rs2Antiban.takeMicroBreakByChance();
                            }
                        } else {
                            System.out.println("Waiting for respawn...");
                            Microbot.status = "Waiting for respawn...";
                            hoverNextRespawningOre();
                        }

                        break;
                    case RESETTING:
                        List<String> itemNames = Arrays.stream(config.itemsToBank().split(",")).map(String::toLowerCase).collect(Collectors.toList());

                        if (config.useBank()) {
                            if (config.ORE() == Rocks.GEM && Rs2Player.getWorldLocation().getRegionID() == GEM_MINE_UNDERGROUND) {
                                if (Rs2DepositBox.openDepositBox()) {
                                    if (Rs2Inventory.contains("Open gem bag")) {
                                        Rs2Inventory.interact("Open gem bag", "Empty");
                                        Rs2DepositBox.depositAllExcept("Open gem bag");
                                    } else {
                                        Rs2DepositBox.depositAll();
                                    }
                                    Rs2DepositBox.closeDepositBox();
                                }
                            }
                            else if (Rocks.BASALT == config.ORE() && BASALT_MINE == Rs2Player.getWorldLocation().getRegionID()) {
                                if (Rs2Walker.walkTo(2872,3935,0)){
                                    Rs2Inventory.useItemOnNpc(ItemID.BASALT, NpcID.SNOWFLAKE);
                                    Rs2Walker.walkTo(2841,10339,0);
                                }
                            } else {
                                if (!Rs2Bank.bankItemsAndWalkBackToOriginalPosition(itemNames, initialPlayerLocation, 0, config.distanceToStray()))
                                    return;
                            }

                        } else {
                            Rs2Inventory.dropAllExcept(false, config.interactOrder(), Arrays.stream(config.itemsToKeep().split(",")).map(String::trim).toArray(String[]::new));
                        }

                        state = State.MINING;
                        break;
                }
            } catch (Exception ex) {
                Microbot.log(ex.getMessage());
            }
        }, 0, 50, TimeUnit.MILLISECONDS);
        return true;
    }

    @Override
    public void shutdown(){
        super.shutdown();
        Rs2Antiban.resetAntibanSettings();
    }

    private void trackMinedOre(WorldPoint tile) {
        if (recentlyMinedOres.size() >= TRACKED_ORES_LIMIT) {
            recentlyMinedOres.removeFirst();
        }
        recentlyMinedOres.add(tile);
        oreRespawnTimers.put(tile, System.currentTimeMillis());
    }

    private Optional<WorldPoint> getNextRespawningOre() {
        long now = System.currentTimeMillis();
        return oreRespawnTimers.entrySet().stream()
                .filter(entry -> now - entry.getValue() < ORE_RESPAWN_TIME_MS)
                .sorted(Comparator.comparingLong(entry -> entry.getValue() + ORE_RESPAWN_TIME_MS))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    private void hoverNextRespawningOre() {
        getNextRespawningOre().ifPresent(tile -> {
            Tile[][][] tiles = Microbot.getClient().getScene().getTiles();
            Tile targetTile = null;

            for (Tile[] row : tiles[Microbot.getClient().getPlane()]) {
                for (Tile sceneTile : row) {
                    if (sceneTile != null && sceneTile.getWorldLocation().equals(tile)) {
                        targetTile = sceneTile;
                        break;
                    }
                }
                if (targetTile != null) break;
            }

            if (targetTile != null) {
                Point point = Rs2UiHelper.getClickingPoint(Rs2UiHelper.getTileClickbox(targetTile), true);
                System.out.println("Hovering over respawn tile at: " + tile);
                Microbot.naturalMouse.moveTo(point.getX(), point.getY());
            } else {
                System.out.println("Respawn tile not found in scene.");
            }
        });
    }

}

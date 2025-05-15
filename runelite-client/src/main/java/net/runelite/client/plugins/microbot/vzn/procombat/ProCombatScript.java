package net.runelite.client.plugins.microbot.vzn.procombat;

import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.grounditems.GroundItem;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.Global;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItem;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.misc.Rs2Potion;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static net.runelite.client.plugins.microbot.util.antiban.enums.ActivityIntensity.EXTREME;

public class ProCombatScript extends Script {

    public static final double VERSION = 1.0;

    private static final WorldPoint PLAYER_ATTACK_LOCATION = new WorldPoint(2715, 9127, 0);
    private static final WorldPoint GROUP_TARGET_LOCATION = new WorldPoint(2715, 9128, 0);
    private static final WorldPoint GROUP_WALK_TRICK_LOCATION = new WorldPoint(2714, 9128, 0);
    private static final String BONE_NAME = "Bones";
    private static final int MAX_BONES_PER_TILE = 3;
    private static final int SEARCH_RADIUS = 4;

    static {
        Microbot.enableAutoRunOn = false;
        Rs2Antiban.resetAntibanSettings();
        Rs2AntibanSettings.usePlayStyle = true;
        Rs2AntibanSettings.simulateFatigue = false;
        Rs2AntibanSettings.simulateAttentionSpan = true;
        Rs2AntibanSettings.behavioralVariability = true;
        Rs2AntibanSettings.nonLinearIntervals = true;
        Rs2AntibanSettings.dynamicActivity = true;
        Rs2AntibanSettings.profileSwitching = true;
        Rs2AntibanSettings.naturalMouse = true;
        Rs2AntibanSettings.simulateMistakes = true;
        Rs2AntibanSettings.moveMouseOffScreen = false;
        Rs2AntibanSettings.moveMouseRandomly = true;
        Rs2AntibanSettings.moveMouseRandomlyChance = 0.04;
        Rs2Antiban.setActivityIntensity(EXTREME);
    }

    private Instant lastGroupedAt = Instant.now();

    public boolean run(ProCombatConfig config) {
        Microbot.enableAutoRunOn = false;

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() ->
        {
            try {
                if (!Microbot.isLoggedIn() || !super.run()) {
                    return;
                }

                handleTick(config);
            } catch (Exception ex) {
                Microbot.log("Error in main loop: " + ex.getMessage());
                System.out.println("Exception message: " + ex.getMessage());
                ex.printStackTrace();
            }
        }, 0, 50, TimeUnit.MILLISECONDS);
        return true;
    }

    private void handleTick(ProCombatConfig config) {
        if (shouldRetreat(config)) {
            retreatToSafety(config);
            return;
        }

        evaluateAndConsumePotions(config);
        spreadBones(config);
        groupMobs(config);

        // Walk to standing location
        if (!Rs2Player.getWorldLocation().equals(PLAYER_ATTACK_LOCATION)) {
            Microbot.log("Walking to player attack location");
            Rs2Walker.walkFastCanvas(PLAYER_ATTACK_LOCATION);
        }
    }

    private void retreatToSafety(ProCombatConfig config) {
        Microbot.pauseAllScripts = true;
        Rs2Inventory.interact(config.teleportItem(), config.teleportItemAction());
        Microbot.pauseAllScripts = false;
    }

    private boolean shouldRetreat(ProCombatConfig config) {
        int currentHealth = Microbot.getClient().getBoostedSkillLevel(Skill.HITPOINTS);
        int currentPrayer = Microbot.getClient().getBoostedSkillLevel(Skill.PRAYER);
        boolean noFood = Rs2Inventory.getInventoryFood().isEmpty();
        boolean noPrayerPotions = Rs2Inventory.items()
                .stream()
                .noneMatch(item -> item != null && item.getName() != null && !Rs2Potion.getPrayerPotionsVariants().contains(item.getName()));

        return (noFood && currentHealth <= config.healthThreshold()) || (noPrayerPotions && currentPrayer < 10);
    }

    private void evaluateAndConsumePotions(ProCombatConfig config) {
        Microbot.log("evaluateAndConsumePotions");
        Rs2Player.eatAt(config.minEatPercent());
        Rs2Player.drinkPrayerPotionAt(config.minPrayerPercent());

        if (!isRangingPotionActive(config.boostedStatsThreshold())) {
            consumePotion(Rs2Potion.getRangePotionsVariants());
        }

        if (!isMagicPotionActive(config.boostedStatsThreshold())) {
            consumePotion(getMagicPotionsVariants());
        }

        int poisonStatus = ProCombatPlugin.instance.getClient().getVarbitValue(102);
        if (poisonStatus > 0) {
            consumePotion(getAntiPoisonVariants());
        }
    }

    private boolean isRangingPotionActive(int threshold) {
        return Rs2Player.hasRangingPotionActive(threshold) || Rs2Player.hasDivineBastionActive() || Rs2Player.hasDivineRangedActive();
    }

    private boolean isMagicPotionActive(int threshold) {
        return Microbot.getClient().getBoostedSkillLevel(Skill.MAGIC) - threshold > Microbot.getClient().getRealSkillLevel(Skill.MAGIC);
    }

    private List<String> getMagicPotionsVariants() {
        return List.of(
                "Magic potion"
        );
    }

    private List<String> getAntiPoisonVariants() {
        return List.of(
                "Anti-venom",
                "Extended anti-venom",
                "Antipoison",
                "Superantipoison"
        );
    }

    private void consumePotion(List<String> keyword) {
        var potion = Rs2Inventory.get(keyword);
        if (potion != null) {
            Rs2Inventory.interact(potion, "Drink");
        }
    }

    private boolean pickupAmountOfItemAtLocation(WorldPoint location, int itemId, int amount) {
        final Predicate<GroundItem> filter = groundItem ->
                groundItem.getLocation().equals(location) && groundItem.getItemId() == itemId;

        List<GroundItem> groundItems = Rs2GroundItem.getGroundItems().values().stream()
                .filter(filter)
                .collect(Collectors.toList());

        int looted = 0;
        for (GroundItem groundItem : groundItems) {
            Rs2GroundItem.coreLoot(groundItem);
            looted++;

            if (looted >= amount || Rs2Inventory.isFull()) {
                break;
            }
        }

        return Rs2GroundItem.validateLoot(filter);
    }

    private void spreadBones(ProCombatConfig config) {
        if (!config.spreadBones()) {
            return;
        }

        Microbot.log("spreadBones");

        // Group bones by tile
        Map<WorldPoint, List<Integer>> boneMap = Arrays.stream(Rs2GroundItem.getAll(3))
                .filter(item -> item.getItem().getName().equalsIgnoreCase(BONE_NAME))
                .filter(item -> PLAYER_ATTACK_LOCATION.distanceTo(item.getTile().getWorldLocation()) <= SEARCH_RADIUS)
                .collect(Collectors.groupingBy(
                        item -> item.getTile().getWorldLocation(),
                        Collectors.mapping(item -> item.getTileItem().getId(), Collectors.toList())
                ));

        // Pickup bones
        for (Map.Entry<WorldPoint, List<Integer>> entry : boneMap.entrySet()) {
            WorldPoint tile = entry.getKey();
            List<Integer> bones = entry.getValue();

            if (bones.size() > MAX_BONES_PER_TILE) {
                int bonesToPickup = bones.size() - MAX_BONES_PER_TILE;
                Microbot.log("There are " + bonesToPickup + " bones to pickup");

                for (int i = 0; i < bonesToPickup; i++) {
                    if (Rs2Inventory.isFull()) {
                        break;
                    }

                    pickupAmountOfItemAtLocation(tile, bones.get(i), 1);
                }
            }
        }

        // Drop bones on new tiles
        List<WorldPoint> nearbyTiles = new ArrayList<>();

        // Generate all tiles within our bounds
        WorldPoint dropTileBoxMin = new WorldPoint(2713, 9128, 0);
        WorldPoint dropTileBoxMax = new WorldPoint(2717, 9129, 0);

        int minX = Math.min(dropTileBoxMin.getX(), dropTileBoxMax.getX());
        int minY = Math.min(dropTileBoxMin.getY(), dropTileBoxMax.getY());
        int maxX = Math.max(dropTileBoxMin.getX(), dropTileBoxMax.getX());
        int maxY = Math.max(dropTileBoxMin.getY(), dropTileBoxMax.getY());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                nearbyTiles.add(new WorldPoint(x, y, PLAYER_ATTACK_LOCATION.getPlane()));
            }
        }

        // Shuffle the tiles
        nearbyTiles.sort(Comparator.comparingInt(PLAYER_ATTACK_LOCATION::distanceTo));
        Collections.reverse(nearbyTiles);

        // Recompute bone count per tile
        Map<WorldPoint, Long> boneCountByTile = Arrays.stream(Rs2GroundItem.getAll(3))
                .filter(item -> item.getItem().getName().equalsIgnoreCase("Bones"))
                .collect(Collectors.groupingBy(
                        item -> item.getTile().getWorldLocation(),
                        Collectors.counting()
                ));

        // Attempt to drop bones on tiles with < 3 (including 0) bones
        for (WorldPoint tile : nearbyTiles) {
            long boneCount = boneCountByTile.getOrDefault(tile, 0L);

            while (boneCount < 3 && Rs2Inventory.hasItem("Bones")) {
                if (!Rs2Player.getWorldLocation().equals(tile)) {
                    Microbot.log("Walking to tile to drop bones");
                    Rs2Walker.walkFastCanvas(tile);
                    Global.sleep(400);
                }

                Rs2Inventory.drop("Bones");  // Assumes dropping on current tile
                boneCount++;
                Global.sleep(400);
            }

            if (!Rs2Inventory.hasItem("Bones")) {
                break;
            }
        }

        // Final step: Return to fighting location
        if (!Rs2Player.getWorldLocation().equals(PLAYER_ATTACK_LOCATION)) {
            Rs2Walker.walkFastCanvas(PLAYER_ATTACK_LOCATION);
        }
    }

    private void groupMobs(ProCombatConfig config) {
        if (!config.groupMobs()) {
            return;
        }

        Microbot.log("groupMobs");

        // Cooldown
        Duration runtime = Duration.between(lastGroupedAt, Instant.now());
        if (runtime.toSeconds() < 7) {
            return;
        }

        List<Rs2NpcModel> targets = Rs2Npc.getNpcs("Skeleton").filter((npc) -> npc.getWorldLocation().distanceTo(Rs2Player.getWorldLocation()) <= 5).collect(Collectors.toList());
        int onGroupTargetTile = Math.toIntExact(targets.stream().filter((npc) -> npc.getWorldLocation().equals(GROUP_TARGET_LOCATION)).count());
        int offGroupTargetTile = Math.toIntExact(targets.size()) - onGroupTargetTile;

        // Do the walk trick
        if (offGroupTargetTile >= onGroupTargetTile) {
            for (int i = 0; i < 3; i++) {
                Rs2Walker.walkFastCanvas(GROUP_WALK_TRICK_LOCATION);
                sleep(450, 550);
                Rs2Walker.walkFastCanvas(PLAYER_ATTACK_LOCATION);
                sleep(450, 550);

                if (!Rs2Player.getWorldLocation().equals(PLAYER_ATTACK_LOCATION)) {
                    Rs2Walker.walkFastCanvas(PLAYER_ATTACK_LOCATION);
                }
            }
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();

        if (mainScheduledFuture != null && !mainScheduledFuture.isCancelled()) {
            mainScheduledFuture.cancel(true);
        }

        Microbot.log("Shutting down script");
    }

}

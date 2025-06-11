package net.runelite.client.plugins.microbot.vzn.prommtunnel;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.plugins.grounditems.GroundItem;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItem;
import net.runelite.client.plugins.microbot.util.inventory.InteractOrder;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.misc.Rs2Potion;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.vzn.prommtunnel.stage.*;
import net.runelite.client.plugins.microbot.vzn.util.TimeUtil;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static net.runelite.client.plugins.microbot.util.antiban.enums.ActivityIntensity.EXTREME;

public class ProMMTunnelScript extends Script {

    public static final double VERSION = 1.0;

    public static final WorldPoint PLAYER_ATTACK_TILE = new WorldPoint(2715, 9127, 0);
    private static final WorldPoint GROUP_TARGET_TILE = new WorldPoint(2715, 9128, 0);
    private static final WorldPoint GROUP_WALK_TRICK_TILE = new WorldPoint(2714, 9128, 0);
    private static final WorldPoint RESET_AGGRESSION_TILE = new WorldPoint(2744, 9137, 0);
    private static final WorldPoint TUNNEL_SAFE_TILE = new WorldPoint(2733, 9138, 0);
    private static final String MONSTER_NAME = "Skeleton";
    private static final String BONE_NAME = "Bones";
    private static final int MAX_BONES_PER_TILE = 3;
    private static final int SEARCH_RADIUS = 4;

    private ProMMTunnelPlugin plugin;
    private ProMMTunnelConfig config;

    private CombatState state = null;
    private ScriptState attackState;
    private List<PrepareStageImpl> stages;
    private int stage = 0;

    @Getter private Instant startTime = null;
    @Getter @Setter private int startXp = -1;

    private Instant lastAggressionReset = null;
    private Instant lastGroupedAt = null;
    private Instant lastSpreadAt = null;
    private Instant lastPoisonCheck = null;
    public boolean playerDied = false;
    public boolean bankError = false;

    @Override
    public void shutdown() {
        super.shutdown();

        if (mainScheduledFuture != null && !mainScheduledFuture.isCancelled()) {
            mainScheduledFuture.cancel(true);
        }

        Microbot.log("Shutting down script");
    }

    public boolean run(ProMMTunnelConfig config) {
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
        Rs2AntibanSettings.moveMouseOffScreen = true;
        Rs2AntibanSettings.moveMouseRandomly = true;
        Rs2AntibanSettings.moveMouseRandomlyChance = 0.04;
        Rs2Antiban.setActivityIntensity(EXTREME);

        this.plugin = ProMMTunnelPlugin.instance;
        this.config = config;

        this.setupStages();

        this.startTime = Instant.now();
        this.lastAggressionReset = null;
        this.lastGroupedAt = null;
        this.lastSpreadAt = null;
        this.lastPoisonCheck = null;
        this.playerDied = false;
        this.bankError = false;

        this.mainScheduledFuture = this.scheduledExecutorService.scheduleWithFixedDelay(() ->
        {
            try {
                if (!Microbot.isLoggedIn() || !super.run()) {
                    return;
                }

                handleTick();
            } catch (Exception ex) {
                Microbot.log("Error in main loop: " + ex.getMessage());
                ex.printStackTrace();
            }
        }, 0, 50, TimeUnit.MILLISECONDS);
        return true;
    }

    private void detectState() {
        CombatState previousState = state;

        if (isInMonkeyMadnessTunnel()) {
            if (Rs2Player.getWorldLocation().distanceTo(PLAYER_ATTACK_TILE) <= 10) {
                state = CombatState.ATTACK;
            } else {
                state = CombatState.PREPARE;
                stage = 8;
            }
        } else {
            state = CombatState.PREPARE;

            if (Rs2GameObject.findObjectById(TravelTunnel.TUNNEL_LADDER_ID) != null) {
                stage = 7;
            } else if (state != previousState) {
                stage = 0;
            }
        }
    }

    private void handleTick() {
        if (playerDied) {
            Microbot.log("Player died - logging out");
            Rs2Player.logout();
            sleep(1000);
            return;
        }

        if (bankError) {
            Microbot.log("Bank error - logging out");
            Rs2Player.logout();
            sleep(1000);
            return;
        }

        if (startXp == -1) {
            startXp = Microbot.getClient().getSkillExperience(config.attackStyle().toRuneLiteSkill());
        }

        detectState();

        switch (state) {
            case PREPARE:
                handlePrepareTick();
                break;
            case ATTACK:
                handleAttackTick();
                break;
        }
    }

    private void setupStages() {
        this.stages = Arrays.asList(
                new TravelBank(plugin),
                new PrepareInventory(plugin),
                new TravelSpiritTree(plugin),
                new TravelGrandTree(plugin),
                new TravelDaero(plugin),
                new TravelWaydar(plugin),
                new TravelLumdo(plugin),
                new TravelTunnel(plugin),
                new TraverseTunnel(plugin)
        );
    }

    private void handlePrepareTick() {
        PrepareStageImpl currentStage = stages.get(stage);

        // Run tick if stage not complete
        if (!currentStage.isComplete()) {
            Microbot.log("Ticking stage " + stage);
            currentStage.tick();
        }

        // Go to next stage if complete
        if (currentStage.isComplete()) {
            Microbot.log("Completed stage - " + stage);
            int nextStageIndex = stage + 1;
            if (nextStageIndex >= stages.size()) {
                Microbot.log("All stages complete - switching to ATTACK state");
                state = CombatState.ATTACK;
                stage = 0;
            } else {
                stage = nextStageIndex;
                Microbot.log("Switched to next stage - " + stage);
            }

            sleep(1000);
        }
    }

    private void handleAttackTick() {
        if (shouldRetreat()) {
            retreatToSafety();
            return;
        }

        ensureProtectionPrayersEnabled();
        evaluateAndConsumeFoodAndPotions();
        resetAggression();

        if (spreadBones()) {
            return;
        }

        groupMobs();

        // Walk to standing location
        if (!Rs2Player.getWorldLocation().equals(PLAYER_ATTACK_TILE)) {
            Microbot.log("Walking to player attack location");
            Rs2Walker.walkFastCanvas(PLAYER_ATTACK_TILE);
        }
    }

    private boolean isInMonkeyMadnessTunnel() {
        WorldPoint location = Rs2Player.getWorldLocation();
        return (location.getX() >= 2560 && location.getX() <= 3010 &&
                location.getY() >= 9000 && location.getY() <= 9300 &&
                location.getPlane() == 0);
    }

    public void retreatToSafety() {
        Microbot.pauseAllScripts = true;
        Rs2Inventory.interact(config.teleportItem(), config.teleportItemAction());
        Microbot.pauseAllScripts = false;
        sleep(1500);
    }

    public boolean shouldRetreat() {
        int currentHealth = Microbot.getClient().getBoostedSkillLevel(Skill.HITPOINTS);
        int currentPrayer = Microbot.getClient().getBoostedSkillLevel(Skill.PRAYER);

        if (currentPrayer <= 10) {
            return true;
        }

        boolean noFood = Rs2Inventory.getInventoryFood().isEmpty();
        boolean noPrayerPotions = Rs2Inventory.items()
                .stream()
                .noneMatch(item -> item != null && item.getName() != null && !Rs2Potion.getPrayerPotionsVariants().contains(item.getName()));

        return (noFood && currentHealth <= config.healthThreshold()) || (noPrayerPotions && currentPrayer < 10);
    }

    public void ensureProtectionPrayersEnabled() {
        // Ensure protect melee is enabled
        if (!Rs2Prayer.isPrayerActive(Rs2PrayerEnum.PROTECT_MELEE)) {
            Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MELEE, true);
        }
    }

    public void evaluateAndConsumeFoodAndPotions() {
        Rs2Player.eatAt(config.minEatPercent());
        Rs2Player.drinkPrayerPotionAt(config.minPrayerPoints());

        if (!isRangingPotionActive(config.boostedStatsThreshold())) {
            consumePotion(Rs2Potion.getRangePotionsVariants());
        }

        if (!isMagicPotionActive(config.boostedStatsThreshold())) {
            consumePotion(getMagicPotionsVariants());
        }

        if (lastPoisonCheck == null || Duration.between(lastPoisonCheck, Instant.now()).toSeconds() >= 5) {
            lastPoisonCheck = Instant.now();

            Microbot.getClientThread().invoke(() -> {
                int poisonStatus = ProMMTunnelPlugin.instance.getClient().getVarpValue(VarPlayerID.POISON);
                if (poisonStatus > 0) {
                    consumePotion(getAntiPoisonVariants());
                }
            });
        }

        if (Rs2Inventory.hasItem("Vial")) {
            Rs2Inventory.dropAll((item) -> item.getName().equals("Vial"), InteractOrder.PROFESSIONAL);
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

    private boolean spreadBones() {
        if (!config.spreadBones()) {
            return false;
        }

        if (lastSpreadAt != null && !Rs2Inventory.hasItem(BONE_NAME)) {
            if (Duration.between(lastSpreadAt, Instant.now()).toSeconds() < 15) {
                return false;
            }
        }

        // Group bones by tile
        Map<WorldPoint, List<Integer>> boneMap = Arrays.stream(Rs2GroundItem.getAll(3))
                .filter(item -> item.getItem().getName().equalsIgnoreCase(BONE_NAME))
                .filter(item -> PLAYER_ATTACK_TILE.distanceTo(item.getTile().getWorldLocation()) <= SEARCH_RADIUS)
                .collect(Collectors.groupingBy(
                        item -> item.getTile().getWorldLocation(),
                        Collectors.mapping(item -> item.getTileItem().getId(), Collectors.toList())
                ));

        if (!boneMap.entrySet().stream().anyMatch(entry -> entry.getValue().size() > 3)) {
            return false;
        }

        boolean bonesSpread = false;

        // Pickup bones
        for (Map.Entry<WorldPoint, List<Integer>> entry : boneMap.entrySet()) {
            WorldPoint tile = entry.getKey();
            List<Integer> bones = entry.getValue();

            if (bones.size() > MAX_BONES_PER_TILE) {
                bonesSpread = true;

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

        // Generate all tiles within our bounds
        List<WorldPoint> nearbyTiles = new ArrayList<>();
        WorldPoint dropTileBoxMin = new WorldPoint(2713, 9128, 0);
        WorldPoint dropTileBoxMax = new WorldPoint(2717, 9129, 0);

        int minX = Math.min(dropTileBoxMin.getX(), dropTileBoxMax.getX());
        int minY = Math.min(dropTileBoxMin.getY(), dropTileBoxMax.getY());
        int maxX = Math.max(dropTileBoxMin.getX(), dropTileBoxMax.getX());
        int maxY = Math.max(dropTileBoxMin.getY(), dropTileBoxMax.getY());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                WorldPoint tile = new WorldPoint(x, y, PLAYER_ATTACK_TILE.getPlane());

                if (!(tile.equals(PLAYER_ATTACK_TILE) || tile.equals(GROUP_TARGET_TILE))) {
                    nearbyTiles.add(tile);
                }
            }
        }

        // Shuffle the tiles so that further tiles are prioritized
        nearbyTiles.sort(Comparator.comparingInt(PLAYER_ATTACK_TILE::distanceTo));
        Collections.reverse(nearbyTiles);

        // Recompute bone count per tile
        Map<WorldPoint, Long> boneCountByTile = Arrays.stream(Rs2GroundItem.getAll(3))
                .filter(item -> item.getItem().getName().equalsIgnoreCase(BONE_NAME))
                .collect(Collectors.groupingBy(
                        item -> item.getTile().getWorldLocation(),
                        Collectors.counting()
                ));

        // Attempt to drop bones on tiles with < 3 (including 0) bones
        for (WorldPoint tile : nearbyTiles) {
            long boneCount = boneCountByTile.getOrDefault(tile, 0L);

            while (boneCount < 3 && Rs2Inventory.hasItem(BONE_NAME)) {
                if (!Rs2Player.getWorldLocation().equals(tile)) {
                    Microbot.log("Walking to tile to drop bones");
                    Rs2Walker.walkFastCanvas(tile);
                    sleepUntil(() -> Rs2Player.getWorldLocation().equals(tile), 1000);
                }

                Microbot.log("Bone count: " + boneCount);

                bonesSpread = true;
                Rs2Inventory.drop(BONE_NAME);  // Assumes dropping on current tile
                Rs2Inventory.waitForInventoryChanges(1500);
                boneCount++;
            }

            if (!Rs2Inventory.hasItem(BONE_NAME)) {
                break;
            }
        }

        // Final step: Return to fighting location
        if (!Rs2Player.getWorldLocation().equals(PLAYER_ATTACK_TILE)) {
            Rs2Walker.walkFastCanvas(PLAYER_ATTACK_TILE);
        }

        if (bonesSpread) {
            lastSpreadAt = Instant.now();
        }

        return bonesSpread;
    }

    private void groupMobs() {
        if (!config.groupMobs()) {
            return;
        }

        // Cooldown
        if (lastGroupedAt != null) {
            Duration runtime = Duration.between(lastGroupedAt, Instant.now());
            if (runtime.toSeconds() < 7) {
                return;
            }
        }

        List<Rs2NpcModel> targets = Rs2Npc.getNpcs(MONSTER_NAME).filter((npc) -> npc.getWorldLocation().distanceTo(Rs2Player.getWorldLocation()) <= 5).collect(Collectors.toList());
        int onGroupTargetTile = Math.toIntExact(targets.stream().filter((npc) -> npc.getWorldLocation().equals(GROUP_TARGET_TILE)).count());
        int offGroupTargetTile = Math.toIntExact(targets.size()) - onGroupTargetTile;

        // Do the walk trick
        if (offGroupTargetTile >= onGroupTargetTile) {
            for (int i = 0; i < 2; i++) {
                Rs2Walker.walkFastCanvas(GROUP_WALK_TRICK_TILE);
                sleep(450, 550);
                Rs2Walker.walkFastCanvas(PLAYER_ATTACK_TILE);
                sleep(450, 550);

                if (!Rs2Player.getWorldLocation().equals(PLAYER_ATTACK_TILE)) {
                    Rs2Walker.walkFastCanvas(PLAYER_ATTACK_TILE);
                }
            }
        }
    }

    private void resetAggression() {
        if (!config.resetAggressionEnabled()) {
            return;
        }

        if (lastAggressionReset == null) {
            lastAggressionReset = Instant.now();
        }

        Duration timeDifference = Duration.between(lastAggressionReset, Instant.now());

        if (timeDifference.toMinutes() >= config.resetAggressionInterval()) {
            // Walk to area that is far enough away to reset monster aggression
            Rs2Walker.walkTo(RESET_AGGRESSION_TILE);
            Rs2Walker.walkFastCanvas(RESET_AGGRESSION_TILE);
            sleepUntil(() -> Rs2Player.getWorldLocation().distanceTo(RESET_AGGRESSION_TILE) <= 5, 5000);

            // Let's check food/potions just to be safe
            evaluateAndConsumeFoodAndPotions();

            // Walk back to player attack location
            Rs2Walker.walkTo(PLAYER_ATTACK_TILE);
            Rs2Walker.walkFastCanvas(PLAYER_ATTACK_TILE);
            sleepUntil(() -> Rs2Player.getWorldLocation().distanceTo(PLAYER_ATTACK_TILE) <= 5, 5000);

            // Reset last aggression
            lastAggressionReset = Instant.now();
        }
    }

    public String getTimeRunning() {
        return startTime != null ? TimeUtil.formatIntoAbbreviatedString((int) Duration.between(startTime, Instant.now()).toSeconds()) : "";
    }

    public int getXpGained() {
        return Microbot.getClient().getSkillExperience(config.attackStyle().toRuneLiteSkill()) - startXp;
    }

}

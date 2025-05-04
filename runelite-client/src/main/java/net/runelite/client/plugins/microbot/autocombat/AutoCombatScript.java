package net.runelite.client.plugins.microbot.autocombat;

import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.misc.Rs2Potion;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.runelite.client.plugins.microbot.util.antiban.enums.ActivityIntensity.EXTREME;

public class AutoCombatScript extends Script {

    public static final double VERSION = 1.0;

    private static final WorldPoint SAFE_LOCATION = new WorldPoint(2465, 3494, 0);
    private static final WorldPoint FIGHTING_LOCATION = new WorldPoint(2100, 5643, 0);
    private static final String TARGET_NAME = "Skeleton";

    public static int gameTickCount = 0;
    public static boolean playerMoved;

    private String lastChatMessage = "";
    private Instant outOfCombatTime = Instant.now();
    private Rs2NpcModel currentTarget = null;
    private int failedAttacks = 0;

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

    public boolean run(AutoCombatConfig config) {
        Microbot.enableAutoRunOn = false;

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn() || !super.run()) {
                    return;
                }

                handleFighting(config);
            } catch (Exception ex) {
                logOnceToChat("Error in main loop: " + ex.getMessage());
                System.out.println("Exception message: " + ex.getMessage());
                ex.printStackTrace();
            }
        }, 0, 50, TimeUnit.MILLISECONDS);
        return true;
    }

    private void handleFighting(AutoCombatConfig config) {
        if (shouldRetreat(config)) {
            retreatToSafety(config);
            return;
        }

//        if (currentTarget == null || currentTarget.isDead()) {
//            logOnceToChat("Target is null or dead");
//        }
//
//        handleTargetSelection();
//        attackTarget(config);
//
//        if (currentTarget == null) {
//            return;
//        }

        evaluateAndConsumePotions(config);
    }

    private void handleTargetSelection() {
        // Ensure currently selected target is also who we are attacking
        if (currentTarget != null) {
            var tempTarget = getTarget(true);
            if ((tempTarget != null && tempTarget.getIndex() != currentTarget.getIndex()) || currentTarget.isDead()) {
                logOnceToChat("Invalid target was selected, switching to correct enemy");
                currentTarget = tempTarget;
            }
        }

        if (!Rs2Player.isInCombat()) {
            if (outOfCombatTime == null) {
                outOfCombatTime = Instant.now();
            } else if (Instant.now().isAfter(outOfCombatTime.plusSeconds(6))) {
                logOnceToChat("Out of combat for 6 seconds, forcing new target");
                currentTarget = getTarget(true);

                if (currentTarget != null) {
                    Rs2Npc.attack(currentTarget);
                } else {
                    logOnceToChat("Unable to force new target, walking to gorillas and trying again");
                    Rs2Walker.walkTo(FIGHTING_LOCATION);
                    currentTarget = getTarget(true);

                    // Last attempt, just attack a gorilla
                    if (currentTarget == null) {
                        Rs2Npc.attack(TARGET_NAME);
                    }
                }
                outOfCombatTime = null; // Reset after forcing new target
            }
        } else {
            outOfCombatTime = null;
        }
    }

    private void attackTarget(AutoCombatConfig config) {
        if (currentTarget != null && !currentTarget.isDead()) {
            Rs2Player.eatAt(config.minEatPercent());
            Rs2Player.drinkPrayerPotionAt(config.minPrayerPercent());

            if (currentTarget != null) {
                if (!Rs2Player.isAnimating(1600)) {
                    if (currentTarget != null && !currentTarget.isDead()) {
                        var didWeAttack = Rs2Npc.attack(currentTarget);
                        if (didWeAttack) {
                            failedAttacks = 0;
                        } else {
                            failedAttacks++;
                            if (failedAttacks >= 7) {
                                currentTarget = getTarget(true);
                                failedAttacks = 0;
                            }
                        }
                    }
                }
            }
        } else {
            logOnceToChat("CurrentTarget is null or dead");
        }
    }

    private void escapeToSafety() {
        Rs2Inventory.interact("Varrock teleport", "Commune");
    }

    public Rs2NpcModel getTarget() {
        return getTarget(false);
    }

    public Rs2NpcModel getTarget(boolean force) {
        if (currentTarget != null && !currentTarget.isDead() && !force) {
            return currentTarget;
        }

        var interacting = Rs2Player.getInteracting();
        if (interacting != null) {
            if (Objects.equals(interacting.getName(), TARGET_NAME)) {
                return (Rs2NpcModel) interacting;
            }
        }

        var playerLocation = Microbot.getClient().getLocalPlayer().getWorldLocation();

        var alreadyInteractingNpcs = Rs2Npc.getNpcsForPlayer(TARGET_NAME);
        if (!alreadyInteractingNpcs.isEmpty()) {
            return alreadyInteractingNpcs.stream()
                    .min(Comparator.comparingInt(npc -> npc.getWorldLocation().distanceTo(playerLocation))).get();
        }

        var targetsStream = Rs2Npc.getNpcs(TARGET_NAME);
        if (targetsStream == null) {
            logOnceToChat("No target found");
            return null;
        }

        var player = Rs2Player.getLocalPlayer();
        String playerName = player.getName();
        List<Rs2NpcModel> targets = targetsStream.collect(Collectors.toList());

        for (Rs2NpcModel target : targets) {
            if (target != null) {
                var interactingTwo = target.getInteracting();
                String interactingName = interactingTwo != null ? interactingTwo.getName() : "None";
                if (interactingTwo != null && Objects.equals(interactingName, playerName)) {
                    return target;
                }
            }
        }

        logOnceToChat("Finding closest target");

        return targets.stream()
                .filter(npc -> npc != null && !npc.isDead() && !npc.isInteracting())
                .min(Comparator.comparingInt(npc -> npc.getWorldLocation().distanceTo(playerLocation))).stream().findFirst()
                .orElse(null);
    }

    private void retreatToSafety(AutoCombatConfig config) {
        Microbot.pauseAllScripts = true;
        escapeToSafety();
        sleepUntil(() -> Microbot.getClient().getLocalPlayer().getWorldLocation().equals(SAFE_LOCATION), 5000);
        disableAllPrayers();
        Microbot.pauseAllScripts = false;
    }

    private boolean shouldRetreat(AutoCombatConfig config) {
        int currentHealth = Microbot.getClient().getBoostedSkillLevel(Skill.HITPOINTS);
        int currentPrayer = Microbot.getClient().getBoostedSkillLevel(Skill.PRAYER);
        boolean noFood = Rs2Inventory.getInventoryFood().isEmpty();
        boolean noPrayerPotions = Rs2Inventory.items()
                .stream()
                .noneMatch(item -> item != null && item.getName() != null && !Rs2Potion.getPrayerPotionsVariants().contains(item.getName()));

        return (noFood && currentHealth <= config.healthThreshold()) || (noPrayerPotions && currentPrayer < 10);
    }

    public void disableAllPrayers() {
        Rs2Prayer.disableAllPrayers();
    }

    private void evaluateAndConsumePotions(AutoCombatConfig config) {
        Rs2Player.eatAt(config.minEatPercent());
        Rs2Player.drinkPrayerPotionAt(config.minPrayerPercent());

        if (!isRangingPotionActive(config.boostedStatsThreshold())) {
            consumePotion(Rs2Potion.getRangePotionsVariants());
        }
    }

    private boolean isRangingPotionActive(int threshold) {
        return Rs2Player.hasRangingPotionActive(threshold) || Rs2Player.hasDivineBastionActive() || Rs2Player.hasDivineRangedActive();
    }

    private void consumePotion(List<String> keyword) {
        var potion = Rs2Inventory.get(keyword);
        if (potion != null) {
            Rs2Inventory.interact(potion, "Drink");
        }
    }

    void logOnceToChat(String message) {
        if (!message.equals(lastChatMessage)) {
            Microbot.log(message);
            lastChatMessage = message;
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();

        currentTarget = null;
        disableAllPrayers();

        if (mainScheduledFuture != null && !mainScheduledFuture.isCancelled()) {
            mainScheduledFuture.cancel(true);
        }

        logOnceToChat("Shutting down script");
    }

}

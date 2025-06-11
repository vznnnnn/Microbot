package net.runelite.client.plugins.microbot.vzn.procombat;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.*;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.inventory.InteractOrder;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.misc.Rs2Potion;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;
import net.runelite.client.plugins.microbot.vzn.util.TimeUtil;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static net.runelite.client.plugins.microbot.util.antiban.enums.ActivityIntensity.EXTREME;

public class ProCombatScript extends Script {

    public static final double VERSION = 1.0;

    private ProCombatPlugin plugin;
    private ProCombatConfig config;

    @Getter private Instant startTime = null;
    @Getter @Setter private int kills = 0;
    public boolean playerDied = false;

    @Override
    public void shutdown() {
        super.shutdown();

        if (mainScheduledFuture != null && !mainScheduledFuture.isCancelled()) {
            mainScheduledFuture.cancel(true);
        }

        Microbot.log("Shutting down script");
    }

    public boolean run(ProCombatConfig config) {
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

        this.plugin = ProCombatPlugin.instance;
        this.config = config;

        this.startTime = Instant.now();
        this.playerDied = false;

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

    private void handleTick() {
        if (playerDied) {
            Microbot.log("Player died - logging out");
            Rs2Player.logout();
            sleep(1000);
            return;
        }
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

        if (!isCombatPotionActive(config.boostedStatsThreshold())) {
            consumePotion(Rs2Potion.getCombatPotionsVariants());
        }

        if (!isCombatPotionActive(config.boostedStatsThreshold())) {
            consumePotion(Rs2Potion.getStrengthPotionsVariants());
        }

        if (Rs2Inventory.hasItem("Vial")) {
            Rs2Inventory.dropAll((item) -> item.getName().equals("Vial"), InteractOrder.PROFESSIONAL);
        }
    }

    private boolean isCombatPotionActive(int threshold) {
        return Rs2Player.hasDivineCombatActive() || Rs2Player.hasStrengthActive(threshold);
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

    public String getTimeRunning() {
        return startTime != null ? TimeUtil.formatIntoAbbreviatedString((int) Duration.between(startTime, Instant.now()).toSeconds()) : "";
    }

}

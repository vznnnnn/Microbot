package net.runelite.client.plugins.microbot.vzn.profisher;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.inventory.InteractOrder;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static net.runelite.client.plugins.microbot.util.antiban.enums.ActivityIntensity.EXTREME;

public class ProFisherScript extends Script {

    public static final double VERSION = 1.0;

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
        Rs2AntibanSettings.simulateMistakes = false;
        Rs2AntibanSettings.moveMouseOffScreen = false;
        Rs2AntibanSettings.moveMouseRandomly = true;
        Rs2AntibanSettings.moveMouseRandomlyChance = 0.04;
        Rs2Antiban.setActivityIntensity(EXTREME);
    }

    private String lastChatMessage = "";
    private final List<String> FISH = Arrays.asList("Raw trout", "Raw salmon");

    public boolean run(ProFisherConfig config) {
        Microbot.enableAutoRunOn = false;

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn() || !super.run()) {
                    return;
                }

                handleTick(config);
            } catch (Exception ex) {
                logOnceToChat("Error in main loop: " + ex.getMessage());
                System.out.println("Exception message: " + ex.getMessage());
                ex.printStackTrace();
            }
        }, 0, 10, TimeUnit.MILLISECONDS);

        return true;
    }

    public void doItemActionTick(ProFisherConfig config) {
        if (config.useItem1().contains(",")) {
            List<String> itemNames = Arrays.asList(config.useItem1().split(", "));
            boolean hasAnyItem = itemNames.stream().anyMatch(itemName -> Rs2Inventory.hasItem(itemName, true));

            if (!hasAnyItem) {
                Rs2Player.logout();
                JOptionPane.showMessageDialog(null, "One of the use items is missing", "Missing Items", JOptionPane.ERROR_MESSAGE);
                shutdown();
                return;
            }
        } else {
            if (!Rs2Inventory.hasItem(config.useItem1(), true)) {
                Rs2Player.logout();
                JOptionPane.showMessageDialog(null, "One of the use items is missing", "Missing Items", JOptionPane.ERROR_MESSAGE);
                shutdown();
                return;
            }
        }

        if (!Rs2Inventory.hasItem(config.useItem2(), true)) {
            Rs2Player.logout();
            JOptionPane.showMessageDialog(null, "One of the use items is missing", "Missing Items", JOptionPane.ERROR_MESSAGE);
            shutdown();
            return;
        }

        // Use 1st item on 2nd item every ~2 ticks (1.2 seconds)
        if (config.useItem1().contains(",")) {
            List<String> itemNames = Arrays.asList(config.useItem1().split(", "));
            String firstAvailableItem = itemNames.stream().filter(itemName -> Rs2Inventory.hasItem(itemName, true)).findFirst().orElse(null);
            Rs2Inventory.interact(firstAvailableItem, "Use", true);
        } else {
            Rs2Inventory.interact(config.useItem1(), "Use", true);
        }

        sleep(config.itemActionDelay());
        Rs2Inventory.interact(config.useItem2(), "Use", true);
    }

    public void restartFishing() {
        Rs2Npc.interact("Rod Fishing spot", "Lure");
    }

    private void handleTick(ProFisherConfig config) {
        // Drop fish for powerfishing
        if (Rs2Inventory.isFull()) {
            Rs2Inventory.dropAll(item -> FISH.contains(item.getName()), InteractOrder.PROFESSIONAL);
            sleep(1000);
        }

        doItemActionTick(config);
        sleep(config.lureDelay());
        restartFishing();
        sleep(config.nextActionMinDelay(), config.nextActionMaxDelay());
    }

    void logOnceToChat(String message) {
        if (!message.equals(lastChatMessage)) {
            Microbot.log(message);
            lastChatMessage = message;
        }
    }

}

package net.runelite.client.plugins.microbot.vzn.proagility;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static net.runelite.client.plugins.microbot.util.antiban.enums.ActivityIntensity.EXTREME;

public class ProAgilityScript extends Script {

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

    public static final double VERSION = 1.0;

    private String lastChatMessage = "";
    public int tickets = 0;
    public boolean playerFell = false;

    private void logOnceToChat(String message) {
        if (!message.equals(lastChatMessage)) {
            Microbot.log(message);
            lastChatMessage = message;
        }
    }

    /** Logout if the script has been running for over an hour **/
    public void checkScriptDurationAndLogout() {
        Instant start = ProAgilityPlugin.instance.getScriptStartTime();
        Duration runtime = Duration.between(start, Instant.now());

        if (runtime.toHours() >= 3) {
            Rs2Player.logout();
            Microbot.log("Logged out due to excessive runtime");
        }
    }

    public boolean run(ProAgilityPlugin plugin) {
        Microbot.enableAutoRunOn = false;

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn() || !super.run()) {
                    return;
                }

                Microbot.log("Running strategy");
                plugin.getStrategy().run(plugin);
            } catch (Exception ex) {
                logOnceToChat("Error in main loop: " + ex.getMessage());
                System.out.println("Exception message: " + ex.getMessage());
                ex.printStackTrace();
            }
        }, 0, 10, TimeUnit.MILLISECONDS);

        return true;
    }

}

package net.runelite.client.plugins.microbot.autocombat;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.util.containers.FixedSizeQueue;
import net.runelite.client.plugins.microbot.util.misc.TimeUtils;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;
import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;

@PluginDescriptor(
        name = PluginDescriptor.vzn + "AutoCombat",
        description = "Automates healing, prayer upkeep, and exiting via teleport when out of supplies",
        tags = {"combat", "afk", "chins", "chinning", "microbot"},
        enabledByDefault = false
)
@Slf4j
public class AutoCombatPlugin extends Plugin {

    public static FixedSizeQueue<WorldPoint> lastLocation = new FixedSizeQueue<>(2);
    private ScheduledExecutorService scheduledExecutorService;
    @Inject private AutoCombatConfig config;
    @Inject private OverlayManager overlayManager;
    @Inject private AutoCombatOverlay autoCombatOverlay;
    @Inject private AutoCombatScript autoCombatScript;
    private Instant scriptStartTime;

    @Provides
    AutoCombatConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(AutoCombatConfig.class);
    }

    @Override
    protected void startUp() throws AWTException {
        scriptStartTime = Instant.now();

        if (overlayManager != null) {
            overlayManager.add(autoCombatOverlay);
        }

        autoCombatScript.run(config);
    }

    @Override
    protected void shutDown() {
        autoCombatScript.shutdown();
        overlayManager.remove(autoCombatOverlay);

        if (scheduledExecutorService != null && !scheduledExecutorService.isShutdown()) {
            scheduledExecutorService.shutdown();
        }
    }

    protected String getTimeRunning() {
        return scriptStartTime != null ? TimeUtils.getFormattedDurationBetween(scriptStartTime, Instant.now()) : "";
    }

    @Subscribe
    public void onGameTick(GameTick gameTick) {
        var currentLocation = Rs2Player.getWorldLocation();
        AutoCombatScript.playerMoved = !lastLocation.contains(currentLocation);
        lastLocation.add(currentLocation);
        AutoCombatScript.gameTickCount++;
    }

}

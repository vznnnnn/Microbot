package net.runelite.client.plugins.microbot.vzn.prowoodcutter;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.util.misc.TimeUtils;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;
import java.time.Instant;

@PluginDescriptor(
        name = PluginDescriptor.vzn + "ProWoodcutter",
        description = "Automates woodcutting",
        tags = {"woodcut", "tree", "axe", "chop", "woodcutting", "microbot"},
        enabledByDefault = false
)
@Slf4j
public class AutoWoodcuttingPlugin extends Plugin {

    @Inject private OverlayManager overlayManager;
    @Inject private AutoWoodcuttingConfig config;
    @Inject private AutoWoodcuttingOverlay overlay;
    @Inject private AutoWoodcuttingScript script;
    private Instant scriptStartTime;

    @Provides
    AutoWoodcuttingConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(AutoWoodcuttingConfig.class);
    }

    @Override
    protected void startUp() throws AWTException {
        scriptStartTime = Instant.now();

        if (overlayManager != null) {
            overlayManager.add(overlay);
        }

        script.run(config);
    }

    @Override
    protected void shutDown() {
        script.shutdown();
        overlayManager.remove(overlay);
    }

    protected String getTimeRunning() {
        return scriptStartTime != null ? TimeUtils.getFormattedDurationBetween(scriptStartTime, Instant.now()) : "";
    }

}

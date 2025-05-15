package net.runelite.client.plugins.microbot.vzn.profletcher;

import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.vzn.profletcher.strategy.DartTipsStrategy;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;
import java.time.Instant;

@PluginDescriptor(
        name = PluginDescriptor.vzn + "ProFletcher",
        description = "Fletcher",
        tags = {"fletch", "microbot"},
        enabledByDefault = false
)
@Slf4j
public class ProFletcherPlugin extends Plugin {

    public static ProFletcherPlugin instance;

    @Inject @Getter private Client client;
    @Inject @Getter private OverlayManager overlayManager;
    @Inject @Getter private ProFletcherConfig config;
    @Inject @Getter private ProFletcherScript script;
    @Inject @Getter private ProFletcherOverlay overlay;
    @Getter private Instant scriptStartTime;
    @Getter private ProFletcherStrategy strategy;

    @Provides
    ProFletcherConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(ProFletcherConfig.class);
    }

    @Override
    protected void startUp() throws AWTException {
        instance = this;
        scriptStartTime = Instant.now();

        if (overlayManager != null) {
            overlayManager.add(overlay);
        }

        ProFletcherTechnique technique = config.technique(); // from your @ConfigItem
        selectStrategy(technique);

        Microbot.log("Starting script...");
        script.run(this);
    }

    @Override
    protected void shutDown() {
        script.shutdown();
        overlayManager.remove(overlay);
    }

    public void selectStrategy(ProFletcherTechnique technique) {
        switch (technique) {
            case DART_TIPS:
                strategy = new DartTipsStrategy(this);
                break;
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (event.getGroup().equals("ProHunter") && event.getKey().equals("technique")) {
            ProFletcherTechnique technique = config.technique();
            selectStrategy(technique);
            Microbot.log("Switched to: " + technique);
        }
    }

}

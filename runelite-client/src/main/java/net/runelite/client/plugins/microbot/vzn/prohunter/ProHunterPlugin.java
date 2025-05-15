package net.runelite.client.plugins.microbot.vzn.prohunter;

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
import net.runelite.client.plugins.microbot.vzn.prohunter.strategy.BoxTrapStrategy;
import net.runelite.client.plugins.microbot.vzn.prohunter.strategy.FalconryStrategy;
import net.runelite.client.plugins.microbot.vzn.prohunter.strategy.NetTrapStrategy;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;
import java.time.Instant;

@PluginDescriptor(
        name = PluginDescriptor.vzn + "ProHunter",
        description = "Hunter",
        tags = {"hunt", "lizard", "catch", "microbot"},
        enabledByDefault = false
)
@Slf4j
public class ProHunterPlugin extends Plugin {

    public static ProHunterPlugin instance;

    @Inject @Getter private Client client;
    @Inject @Getter private OverlayManager overlayManager;
    @Inject @Getter private ProHunterConfig config;
    @Inject @Getter private ProHunterScript script;
    @Inject @Getter private ProHunterOverlay overlay;
    @Getter private Instant scriptStartTime;
    @Getter private ProHunterStrategy strategy;

    @Provides
    ProHunterConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(ProHunterConfig.class);
    }

    @Override
    protected void startUp() throws AWTException {
        instance = this;
        scriptStartTime = Instant.now();

        if (overlayManager != null) {
            overlayManager.add(overlay);
        }

        ProHunterTechnique technique = config.technique(); // from your @ConfigItem
        selectStrategy(technique);

        Microbot.log("Starting script...");
        script.run(this);
    }

    @Override
    protected void shutDown() {
        script.shutdown();
        overlayManager.remove(overlay);
    }

    public void selectStrategy(ProHunterTechnique technique) {
        switch (technique) {
            case BOX_TRAP:
                strategy = new BoxTrapStrategy(this);
                break;
            case NET_TRAP:
                strategy = new NetTrapStrategy(this);
                break;
            case FALCONRY:
                strategy = new FalconryStrategy(this);
                break;
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (event.getGroup().equals("ProHunter") && event.getKey().equals("technique")) {
            ProHunterTechnique technique = config.technique();
            selectStrategy(technique);
            Microbot.log("Switched to: " + technique);
        }
    }

}

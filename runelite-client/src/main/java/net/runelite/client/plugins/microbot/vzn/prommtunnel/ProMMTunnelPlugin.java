package net.runelite.client.plugins.microbot.vzn.prommtunnel;

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
import net.runelite.client.plugins.microbot.util.events.DeathEvent;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;

@PluginDescriptor(
        name = PluginDescriptor.vzn + "ProMMTunnel",
        description = "Automates healing, prayer upkeep, and exiting via teleport when out of supplies",
        tags = {"combat", "afk", "chins", "chinning", "microbot"},
        enabledByDefault = false
)
@Slf4j
public class ProMMTunnelPlugin extends Plugin {

    public static ProMMTunnelPlugin instance;

    @Inject @Getter private Client client;
    @Inject @Getter private OverlayManager overlayManager;
    @Inject @Getter private ProMMTunnelConfig config;
    @Inject @Getter private ProMMTunnelScript script;
    @Inject private ProMMTunnelOverlay overlay;

    @Provides
    ProMMTunnelConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(ProMMTunnelConfig.class);
    }

    @Override
    protected void startUp() throws AWTException {
        instance = this;

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

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (event.getKey().equals("attackStyle")) {
            if (!event.getOldValue().equals(event.getNewValue())) {
                script.setStartXp(Microbot.getClient().getSkillExperience(config.attackStyle().toRuneLiteSkill()));
            }
        }
    }

    @Subscribe
    public void onDeathEvent(DeathEvent event) {
        script.playerDied = true;
    }

}

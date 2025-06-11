package net.runelite.client.plugins.microbot.vzn.proconstruction;

import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.events.NpcDespawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;
import java.util.Objects;

@PluginDescriptor(
        name = PluginDescriptor.vzn + "ProConstruction",
        description = "Automates construction",
        tags = {"house", "construction"},
        enabledByDefault = false
)
@Slf4j
public class ProConstructionPlugin extends Plugin {

    public static ProConstructionPlugin instance;

    @Inject @Getter private Client client;
    @Inject @Getter private OverlayManager overlayManager;
    @Inject @Getter private ProConstructionConfig config;
    @Inject @Getter private ProConstructionScript script;
    @Inject private ProConstructionOverlay overlay;

    @Provides
    ProConstructionConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(ProConstructionConfig.class);
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
    public void onNpcDespawned(NpcDespawned event) {
        NPC npc = event.getNpc();

        if (Objects.requireNonNull(npc.getName()).equalsIgnoreCase("Demon butler")) {
            script.setButlerDespawned(true);
        }
    }

}

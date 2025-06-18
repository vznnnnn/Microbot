package net.runelite.client.plugins.microbot.vzn.problastfurnace;

import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;

@PluginDescriptor(
        name = PluginDescriptor.vzn + "ProBlastFurnace",
        description = "Automates Blast Furnace minigame",
        tags = {"smelt", "smithing", "smith", "blast", "furnace"},
        enabledByDefault = false
)
@Slf4j
public class ProBlastFurnacePlugin extends Plugin {

    public static ProBlastFurnacePlugin instance;

    @Inject @Getter private Client client;
    @Inject @Getter private OverlayManager overlayManager;
    @Inject @Getter private ProBlastFurnaceConfig config;
    @Inject @Getter private ProBlastFurnaceScript script;
    @Inject private ProBlastFurnaceOverlay overlay;

    @Provides
    ProBlastFurnaceConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(ProBlastFurnaceConfig.class);
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

}

package net.runelite.client.plugins.microbot.vzn.profisher;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.AnimationChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;
import java.time.Instant;

@PluginDescriptor(
        name = PluginDescriptor.vzn + "ProFisher",
        description = "3-Tick Fishing",
        tags = {"fish", "tick", "microbot"},
        enabledByDefault = false
)
@Slf4j
public class ProFisherPlugin extends Plugin {

    @Inject private Client client;
    @Inject private OverlayManager overlayManager;
    @Inject private ProFisherConfig config;
    @Inject private ProFisherScript script;
    @Inject private ProFisherOverlay overlay;
    private Instant scriptStartTime;

    @Provides
    ProFisherConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(ProFisherConfig.class);
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

    @Subscribe
    public void onAnimationChanged(AnimationChanged event) {
//        if (client.getGameState() != GameState.LOGGED_IN) {
//            return;
//        }
//
//        Player localPlayer = client.getLocalPlayer();
//        if (localPlayer != event.getActor()) {
//            return;
//        }
//
//        int animation = localPlayer.getAnimation();
//
//        if (animation == 623) {
//            script.doItemActionTick(config);
//        } else if (animation == 5249) {
//            script.restartFishing();
//        }
    }

}

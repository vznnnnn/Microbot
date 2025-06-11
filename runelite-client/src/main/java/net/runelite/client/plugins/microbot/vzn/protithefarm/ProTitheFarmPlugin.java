package net.runelite.client.plugins.microbot.vzn.protithefarm;

import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@PluginDescriptor(
        name = PluginDescriptor.vzn + "ProTitheFarm",
        description = "Automates Tithe Farm minigame",
        tags = {"tithe", "farm"},
        enabledByDefault = false
)
@Slf4j
public class ProTitheFarmPlugin extends Plugin {

    public static ProTitheFarmPlugin instance;

    @Inject @Getter private Client client;
    @Inject @Getter private OverlayManager overlayManager;
    @Inject @Getter private ProTitheFarmConfig config;
    @Inject @Getter private ProTitheFarmScript script;
    @Inject private ProTitheFarmOverlay overlay;

    @Provides
    ProTitheFarmConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(ProTitheFarmConfig.class);
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
    public void onChatMessage(ChatMessage chatMessage) {
        String message = chatMessage.getMessage();
        if (message.contains("%")) {
            Pattern pattern = Pattern.compile("(\\d+(\\.\\d+)?)%");
            Matcher matcher = pattern.matcher(message);

            if (matcher.find()) {
                String percentage = matcher.group(1);
                script.setGricollerCanCharges((int) (Float.parseFloat(percentage)));
            }
        } else if (message.equalsIgnoreCase("Gricoller's can is already full.")) {
            script.setGricollerCanCharges(100);
        }
    }

}

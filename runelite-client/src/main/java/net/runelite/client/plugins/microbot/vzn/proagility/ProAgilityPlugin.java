package net.runelite.client.plugins.microbot.vzn.proagility;

import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.StatChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.vzn.proagility.strategy.WildernessAgilityCourseStrategy;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;
import java.time.Instant;

@PluginDescriptor(
        name = PluginDescriptor.vzn + "ProAgility",
        description = "Agility",
        tags = {"agility", "microbot"},
        enabledByDefault = false
)
@Slf4j
public class ProAgilityPlugin extends Plugin {

    public static ProAgilityPlugin instance;

    @Inject @Getter private Client client;
    @Inject @Getter private OverlayManager overlayManager;
    @Inject @Getter private ProAgilityConfig config;
    @Inject @Getter private ProAgilityScript script;
    @Inject @Getter private ProAgilityOverlay overlay;
    @Getter private Instant scriptStartTime;
    @Getter private ProAgilityStrategy strategy;
    @Getter private int startingXp = 0;

    @Provides
    ProAgilityConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(ProAgilityConfig.class);
    }

    @Override
    protected void startUp() throws AWTException {
        instance = this;
        scriptStartTime = Instant.now();

        if (overlayManager != null) {
            overlayManager.add(overlay);
        }

        ProAgilityTechnique technique = config.technique(); // from your @ConfigItem
        selectStrategy(technique);

        startingXp = client.getSkillExperience(Skill.AGILITY);

        Microbot.log("Starting script...");
        script.run(this);
    }

    @Override
    protected void shutDown() {
        script.shutdown();
        overlayManager.remove(overlay);
    }

    public void selectStrategy(ProAgilityTechnique technique) {
        switch (technique) {
            case WILDERNESS_AGILITY_COURSE:
                strategy = new WildernessAgilityCourseStrategy(this);
                break;
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (event.getGroup().equals("ProHunter") && event.getKey().equals("technique")) {
            ProAgilityTechnique technique = config.technique();
            selectStrategy(technique);
            Microbot.log("Switched to: " + technique);
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage event) {
        if (event.getType() == ChatMessageType.GAMEMESSAGE) {
            String message = event.getMessage();

            if (message.contains("from the Agility dispenser")) {
                this.script.tickets++;
            }
            else if (message.contains("You slip and fall to the pit below")
                    || message.contains("You lose your footing and fall into the lava")) {
                Microbot.log("Detected fall into pit/lava");
                this.script.playerFell = true;
            }
            else if (message.contains("You skillfully swing across")
                    || message.contains("You safely cross to the other side")
                    || message.contains("You skillfully edge across the gap")) {
                Microbot.log("Reset playerFell");
                this.script.playerFell = false;
            }
        }
    }

    @Subscribe
    public void onStatChanged(StatChanged event) {
        if (startingXp == 0) {
            startingXp = client.getSkillExperience(Skill.AGILITY);
        }
    }

    public int getExperienceGained() {
        return client.getSkillExperience(Skill.AGILITY) - startingXp;
    }

}

package net.runelite.client.plugins.microbot.vzn.prohunter;

import net.runelite.client.config.*;

@ConfigGroup("ProHunter")
@ConfigInformation("")
public interface ProHunterConfig extends Config {

    @ConfigSection(
            name = "General",
            description = "",
            position = 0
    )
    String general = "general";

    @ConfigItem(
            keyName = "technique",
            name = "Technique",
            description = "Which hunter technique to use",
            position = 0
    )
    default ProHunterTechnique technique() {
        return ProHunterTechnique.BOX_TRAP;
    }

    @ConfigSection(
            name = "Falconry",
            description = "",
            position = 1
    )
    String falconry = "falconry";

    @ConfigItem(
            keyName = "falconryActionDelay",
            name = "Action Delay",
            description = "The delay between actions",
            section = falconry,
            position = 0
    )
    default int falconryActionDelay() {
        return 1750;
    }

    @ConfigItem(
            keyName = "falconryPollInterval",
            name = "Poll Interval",
            description = "The polling interval",
            section = falconry,
            position = 1
    )
    default int falconryPollInterval() {
        return 500;
    }

}

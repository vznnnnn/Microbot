package net.runelite.client.plugins.microbot.vzn.proagility;

import net.runelite.client.config.*;

@ConfigGroup("ProAgility")
@ConfigInformation("")
public interface ProAgilityConfig extends Config {

    @ConfigSection(
            name = "General",
            description = "",
            position = 0
    )
    String general = "general";

    @ConfigItem(
            keyName = "technique",
            name = "Technique",
            description = "Which agility technique to use",
            position = 0
    )
    default ProAgilityTechnique technique() {
        return ProAgilityTechnique.WILDERNESS_AGILITY_COURSE;
    }

}

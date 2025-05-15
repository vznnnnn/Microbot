package net.runelite.client.plugins.microbot.vzn.profletcher;

import net.runelite.client.config.*;

@ConfigGroup("ProFletcher")
@ConfigInformation("")
public interface ProFletcherConfig extends Config {

    @ConfigSection(
            name = "General",
            description = "",
            position = 0
    )
    String general = "general";

    @ConfigItem(
            keyName = "technique",
            name = "Technique",
            description = "Which fletcher technique to use",
            position = 0
    )
    default ProFletcherTechnique technique() {
        return ProFletcherTechnique.DART_TIPS;
    }

}

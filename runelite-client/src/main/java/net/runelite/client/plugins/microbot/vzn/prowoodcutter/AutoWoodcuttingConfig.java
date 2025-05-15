package net.runelite.client.plugins.microbot.vzn.prowoodcutter;

import net.runelite.client.config.*;
import net.runelite.client.plugins.microbot.woodcutting.enums.WoodcuttingResetOptions;

@ConfigGroup("AutoWoodcutting")
@ConfigInformation("")
public interface AutoWoodcuttingConfig extends Config {

    @ConfigSection(
            name = "General",
            description = "",
            position = 1
    )
    String generalSection = "generalSection";

    @ConfigItem(
            keyName = "distanceToStray",
            name = "Distance to Stray",
            description = "Set how far you can travel from your initial position in tiles",
            position = 2,
            section = generalSection
    )
    default int distanceToStray()
    {
        return 20;
    }

    @ConfigSection(
            name = "Reset",
            description = "",
            position = 2
    )
    String resetSection = "resetSection";

    @ConfigItem(
            keyName = "itemAction",
            name = "",
            description = "Task to perform with logs",
            section = resetSection,
            position = 0
    )
    default WoodcuttingResetOptions itemAction() {
        return WoodcuttingResetOptions.DROP;
    }

}

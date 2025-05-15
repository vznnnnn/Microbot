package net.runelite.client.plugins.microbot.vzn.profisher;

import net.runelite.client.config.*;

@ConfigGroup("ProFisher")
@ConfigInformation("")
public interface ProFisherConfig extends Config {

    @ConfigSection(
            name = "General",
            description = "",
            position = 0
    )
    String general = "general";

    @ConfigItem(
            keyName = "useItem1",
            name = "Use Item #1",
            description = "The name of the first item to use. Multiple items can be specified, separated by comma",
            section = general,
            position = 0
    )
    default String useItem1() {
        return "Guam leaf";
    }

    @ConfigItem(
            keyName = "useItem2",
            name = "Use Item #2",
            description = "The name of the second item to use.",
            section = general,
            position = 1
    )
    default String useItem2() {
        return "Swamp tar";
    }

    @ConfigItem(
            keyName = "nextActionMinDelay",
            name = "Next Action Min Delay",
            description = "The minimum delay between starting the next action (after lure)",
            section = general,
            position = 2
    )
    default int nextActionMinDelay() {
        return 650;
    }

    @ConfigItem(
            keyName = "nextActionMaxDelay",
            name = "Next Action Max Delay",
            description = "The maximum delay between starting the next action (after lure)",
            section = general,
            position = 3
    )
    default int nextActionMaxDelay() {
        return 1000;
    }

    @ConfigItem(
            keyName = "itemActionDelay",
            name = "Item Action Delay",
            description = "The delay between 1st item use action and 2nd item use action",
            section = general,
            position = 4
    )
    default int itemActionDelay() {
        return 50;
    }

    @ConfigItem(
            keyName = "lureDelay",
            name = "Lure Delay",
            description = "The delay between use item action and lure action",
            section = general,
            position = 5
    )
    default int lureDelay() {
        return 150;
    }

}

package net.runelite.client.plugins.microbot.vzn.protithefarm;

import net.runelite.client.config.*;

@ConfigGroup("ProTitheFarm")
@ConfigInformation("")
public interface ProTitheFarmConfig extends Config {

    @ConfigSection(
            name = "debug",
            description = "Debug section",
            position = 1
    )
    String debugSection = "debug";

    @ConfigItem(
            name = "Enable Overlay",
            keyName = "enableOverlay",
            description = "If the overlay should be drawn",
            position = 1
    )
    default boolean enableOverlay() {
        return true;
    }

    @ConfigItem(
            name = "Start X",
            keyName = "startPointX",
            description = "",
            position = 2
    )
    default int getStartPointX() {
        return 20;
    }

    @ConfigItem(
            name = "Start Y",
            keyName = "startPointY",
            description = "",
            position = 3
    )
    default int getStartPointY() {
        return 20;
    }

}

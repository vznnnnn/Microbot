package net.runelite.client.plugins.microbot.autocombat;

import net.runelite.client.config.*;

@ConfigGroup("AutoCombat")
@ConfigInformation("")
public interface AutoCombatConfig extends Config {

    @ConfigSection(
            name = "Food and Potions",
            description = "Settings for banking and required supplies",
            position = 3
    )
    String foodAndPotionsSection = "foodAndPotions";

    @ConfigItem(
            keyName = "minEatPercent",
            name = "Minimum Health Percent",
            description = "Percentage of health below which the bot will eat food",
            section = foodAndPotionsSection,
            position = 0
    )
    default int minEatPercent() {
        return 65;
    }

    @ConfigItem(
            keyName = "minPrayerPercent",
            name = "Minimum Prayer Percent",
            description = "Percentage of prayer points below which the bot will drink a prayer potion",
            section = foodAndPotionsSection,
            position = 1
    )
    default int minPrayerPercent() {
        return 30;
    }

    @ConfigItem(
            keyName = "healthThreshold",
            name = "Health Threshold to Exit",
            description = "Minimum health percentage to stay and fight",
            section = foodAndPotionsSection,
            position = 2
    )
    default int healthThreshold() {
        return 70;
    }

    @ConfigItem(
            keyName = "boostedStatsThreshold",
            name = "% Boosted Stats Threshold",
            description = "The threshold for using a potion when the boosted stats are below the maximum.",
            section = foodAndPotionsSection,
            position = 5
    )
    @Range(
            min = 1,
            max = 100
    )
    default int boostedStatsThreshold() {
        return 5;
    }

}

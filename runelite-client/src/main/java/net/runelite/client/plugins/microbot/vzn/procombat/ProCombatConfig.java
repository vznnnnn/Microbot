package net.runelite.client.plugins.microbot.vzn.procombat;

import net.runelite.client.config.*;

@ConfigGroup("ProCombat")
@ConfigInformation("")
public interface ProCombatConfig extends Config {

    @ConfigSection(
            name = "Features",
            description = "",
            position = 1
    )
    String features = "features";

    @ConfigItem(
            keyName = "spreadBones",
            name = "Spread Bones",
            description = "Should stacks of more than 3 bones be picked up and moved to other tiles",
            section = features,
            position = 0
    )
    default boolean spreadBones() {
        return true;
    }

    @ConfigItem(
            keyName = "groupMobs",
            name = "Group Mobs",
            description = "Should the walk trick be used to group mobs on a single tile",
            section = features,
            position = 1
    )
    default boolean groupMobs() {
        return true;
    }

    @ConfigItem(
            keyName = "teleportItem",
            name = "Teleport Item",
            description = "The name of the teleport item",
            section = features,
            position = 2
    )
    default String teleportItem() {
        return "Teleport to house";
    }

    @ConfigItem(
            keyName = "teleportItemAction",
            name = "Teleport Item Action",
            description = "The action to invoke on the teleport item",
            section = features,
            position = 3
    )
    default String teleportItemAction() {
        return "Break";
    }

    @ConfigSection(
            name = "Food and Potions",
            description = "Settings for supplies",
            position = 2
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

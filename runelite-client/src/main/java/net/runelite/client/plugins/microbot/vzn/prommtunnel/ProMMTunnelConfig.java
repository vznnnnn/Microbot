package net.runelite.client.plugins.microbot.vzn.prommtunnel;

import net.runelite.client.config.*;

@ConfigGroup("ProMMTunnel")
@ConfigInformation("")
public interface ProMMTunnelConfig extends Config {

    @ConfigSection(
            name = "Features",
            description = "",
            position = 0
    )
    String features = "features";

    @ConfigItem(
            keyName = "attackStyle",
            name = "Attack Style",
            description = "Which attack style to use",
            section = features,
            position = -1
    )
    default AttackStyle attackStyle() {
        return AttackStyle.MAGIC;
    }

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
        return "Varrock teleport";
    }

    @ConfigItem(
            keyName = "teleportItemAction",
            name = "Teleport Item Action",
            description = "The action to invoke on the teleport item",
            section = features,
            position = 3
    )
    default String teleportItemAction() {
        return "Commune";
    }

    @ConfigItem(
            keyName = "resetAggressionEnabled",
            name = "Use Reset Aggression",
            description = "If reset aggression should be enabled",
            section = features,
            position = 4
    )
    default boolean resetAggressionEnabled() {
        return true;
    }

    @ConfigItem(
            keyName = "resetAggressionInterval",
            name = "Reset Aggression Interval",
            description = "The number of minutes before running to an area to reset the monster aggression",
            section = features,
            position = 5
    )
    default int resetAggressionInterval() {
        return 10;
    }

    @ConfigSection(
            name = "Food and Potions",
            description = "Settings for supplies",
            position = 1
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
            keyName = "minPrayerPoints",
            name = "Minimum Prayer Points",
            description = "Minimum prayer points for the bot to drink a prayer potion",
            section = foodAndPotionsSection,
            position = 1
    )
    default int minPrayerPoints() {
        return 20;
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

    @ConfigSection(
            name = "Bank",
            description = "Settings for banking",
            position = 2
    )
    String bankSection = "bank";

    @ConfigItem(
            keyName = "bankPin",
            name = "Bank PIN",
            description = "Your Bank PIN",
            section = bankSection,
            position = 0
    )
    default String bankPin() {
        return "1234";
    }

}

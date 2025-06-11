package net.runelite.client.plugins.microbot.vzn.protithefarm.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.microbot.Microbot;

@Getter
@RequiredArgsConstructor
public enum TitheFarmMaterial {

    GOLOVANOVA_SEED("Golovanova seed", 34, '1', ItemID.HOSIDIUS_TITHE_FRUIT_A, ItemID.HOSIDIUS_TITHE_SEED_A),
    BOLOGANO_SEED("Bologano seed", 54, '2', ItemID.HOSIDIUS_TITHE_FRUIT_B, ItemID.HOSIDIUS_TITHE_SEED_B),
    LOGAVANO_SEED("Logavano seed", 74, '3', ItemID.HOSIDIUS_TITHE_FRUIT_C, ItemID.HOSIDIUS_TITHE_SEED_C);

    final String name;
    final int levelRequired;
    final char option;
    final int fruitId;
    final int seedId;

    public static TitheFarmMaterial getSeedForLevel() {
        if (Microbot.getClient().getRealSkillLevel(Skill.FARMING) >= LOGAVANO_SEED.levelRequired)
            return LOGAVANO_SEED;
        if (Microbot.getClient().getRealSkillLevel(Skill.FARMING) >= BOLOGANO_SEED.levelRequired)
            return BOLOGANO_SEED;
        if (Microbot.getClient().getRealSkillLevel(Skill.FARMING) >= GOLOVANOVA_SEED.levelRequired)
            return GOLOVANOVA_SEED;

        return LOGAVANO_SEED;
    }

    @Override
    public String toString() {
        return name;
    }

}

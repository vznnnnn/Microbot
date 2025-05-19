package net.runelite.client.plugins.microbot.vzn.procombat;

import net.runelite.api.Skill;

public enum ProCombatAttackStyle {
    RANGED,
    MAGIC;

    public Skill toRuneLiteSkill() {
        return Skill.valueOf(name());
    }
}

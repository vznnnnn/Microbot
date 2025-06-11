package net.runelite.client.plugins.microbot.vzn.prommtunnel;

import net.runelite.api.Skill;

public enum AttackStyle {
    RANGED,
    MAGIC;

    public Skill toRuneLiteSkill() {
        return Skill.valueOf(name());
    }
}

package net.runelite.client.plugins.microbot.vzn.proagility.util;

import java.util.Set;

public class WeaponClassifier {

    // Magic weapons
    private static final Set<Integer> MAGIC_WEAPONS = Set.of(
            21006, // Kodai wand
            12899, // Toxic staff of the dead
            11907, // Trident of the seas
            22288, // Trident of the swamp
            22294  // Sanguinesti staff
    );

    // Ranged weapons
    private static final Set<Integer> RANGED_WEAPONS = Set.of(
            12926, // Toxic blowpipe
            20997, // Twisted bow
            19481, // Armadyl crossbow
            11235, // Dark bow
            861,   // Magic shortbow
            4734  // Karil's crossbow
    );

    // Melee weapons
    private static final Set<Integer> MELEE_WEAPONS = Set.of(
            11802, // AGS
            11694, // Armadyl godsword
            11700, // Zamorak godsword
            11806, // Bandos godsword
            11808, // Saradomin godsword
            13263, // Abyssal tentacle
            12006, // Dragon claws
            1215,  // Dragon dagger
            5698,  // Dragon dagger (p++)
            13576, // Dragon warhammer
            12391, // Granite maul
            4587,  // Dragon scimitar
            13265, // Abyssal bludgeon
            20784  // Dragon sword
    );

    public static boolean isMagicWeapon(int id) {
        return MAGIC_WEAPONS.contains(id);
    }

    public static boolean isRangedWeapon(int id) {
        return RANGED_WEAPONS.contains(id);
    }

    public static boolean isMeleeWeapon(int id) {
        return MELEE_WEAPONS.contains(id);
    }
}


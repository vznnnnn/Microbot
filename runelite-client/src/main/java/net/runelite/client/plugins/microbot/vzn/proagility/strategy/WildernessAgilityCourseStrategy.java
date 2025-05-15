package net.runelite.client.plugins.microbot.vzn.proagility.strategy;

import lombok.Getter;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.kit.KitType;
import net.runelite.client.plugins.microbot.util.Global;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.vzn.proagility.ProAgilityPlugin;
import net.runelite.client.plugins.microbot.vzn.proagility.ProAgilityStrategy;
import net.runelite.client.plugins.microbot.vzn.proagility.util.WeaponClassifier;

public class WildernessAgilityCourseStrategy implements ProAgilityStrategy {

    private static final WorldPoint PIPE_FINISH_LOCATION = new WorldPoint(3004, 3950, 0);
    private static final WorldPoint ROPE_FINISH_LOCATION = new WorldPoint(3005, 3958, 0);
    private static final WorldPoint NEARBY_LADDER_POSITION = new WorldPoint(3005, 10362, 0);
    private static final WorldPoint STEPPING_STONES_FINISH_LOCATION = new WorldPoint(2996, 3960, 0);
    private static final WorldPoint LOG_FINISH_LOCATION = new WorldPoint(2994, 3945, 0);

    @Getter private final ProAgilityPlugin plugin;

    private int stage = 0;

    public WildernessAgilityCourseStrategy(ProAgilityPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean atLocation(WorldPoint point) {
        return Rs2Player.getWorldLocation().equals(point);
    }

    private boolean atLocationOrFell(WorldPoint point) {
        return atLocation(point) || this.plugin.getScript().playerFell;
    }

    private boolean areSpikesNearby() {
        return Rs2GameObject.exists(53225);
    }

    private boolean isAnimating() {
        return Rs2Player.isAnimating();
    }

    private void squeezeThroughPipe() {
        Rs2GameObject.interact(23137, "Squeeze-through");
        Global.sleepUntil(() -> atLocation(PIPE_FINISH_LOCATION), 5000);
        Global.sleep(3500);

        if (atLocation(PIPE_FINISH_LOCATION)) {
            this.stage++;
        }
    }

    private void swingOnRope() {
        if (areSpikesNearby() || this.plugin.getScript().playerFell) {
            // Walk near ladder
            Rs2Walker.walkTo(NEARBY_LADDER_POSITION);
            Global.sleep(1000);

            // Climb ladder
            Rs2GameObject.interact(17385, "Climb-up");
            Global.sleepUntil(() -> !Rs2GameObject.exists(17385), 10000);
            Global.sleep(500);

            // Move to the starting side of the rope swing
            Rs2Walker.walkTo(new WorldPoint(3005, 3953, 0));
            Global.sleep(1000);

            // Reset playerFell
            this.plugin.getScript().playerFell = false;
        }
        else {
            // Swing on rope
            Rs2GameObject.interact(23132, "Swing-on");
            Global.sleepUntil(() -> (atLocation(ROPE_FINISH_LOCATION) || areSpikesNearby()) && !isAnimating(), 10000);
            Global.sleep(500);

            if (atLocation(ROPE_FINISH_LOCATION)) {
                this.stage++;
            }
        }
    }

    private void crossSteppingStones() {
        if (!atLocation(STEPPING_STONES_FINISH_LOCATION)) {
            Rs2GameObject.interact(23556, "Cross");
            Global.sleepUntil(() -> (atLocation(STEPPING_STONES_FINISH_LOCATION) || areSpikesNearby()) && !isAnimating(), 10000);

            if (atLocation(STEPPING_STONES_FINISH_LOCATION)) {
                this.stage++;
            }
        }
    }

    private void walkAcrossLog() {
        if (areSpikesNearby() || this.plugin.getScript().playerFell) {
            // Walk near ladder
            Rs2Walker.walkTo(NEARBY_LADDER_POSITION);
            Global.sleep(1000);

            // Climb ladder
            Rs2GameObject.interact(17385, "Climb-up");
            Global.sleepUntil(() -> !Rs2GameObject.exists(17385), 10000);
            Global.sleep(500);

            // Reset playerFell
            this.plugin.getScript().playerFell = false;
        }
        else {
            Rs2GameObject.interact(23542, "Walk-across");
            Global.sleepUntil(() -> (atLocation(LOG_FINISH_LOCATION) || areSpikesNearby()) && !isAnimating(), 12000);
            Global.sleep(500);

            if (atLocation(LOG_FINISH_LOCATION)) {
                this.stage++;
            }
        }
    }

    private void climbUpRocks() {
        Rs2GameObject.interact(23640, "Climb");
        Global.sleepUntil(() -> Rs2Player.getWorldLocation().getY() <= 3933, 10000);
        Global.sleep(1000);

        if (Rs2Player.getWorldLocation().getY() <= 3933) {
            this.stage++;
        }
    }

    private int getAmountOfTicketsInInventory() {
        for (Rs2ItemModel item : Rs2Inventory.all()) {
            if (item.getName().equalsIgnoreCase("Wilderness agility ticket")) {
                return item.getQuantity();
            }
        }
        return 0;
    }

    private void tagDispenser() {
        int ticketsBeforeTag = getAmountOfTicketsInInventory();
        Rs2GameObject.interact(53224, "Tag");
        Global.sleepUntil(() -> getAmountOfTicketsInInventory() >= ticketsBeforeTag + 1, 5000);

        int ticketsAfterTag = getAmountOfTicketsInInventory();
        if (ticketsAfterTag >= ticketsBeforeTag + 1) {
            if (ticketsAfterTag >= 3) {
                Rs2GameObject.interact(53224, "Redeem");
                Global.sleepUntil(() -> getAmountOfTicketsInInventory() == 0, 5000);
            }

            this.stage = 0;
        }
    }

    @Override
    public String getName() {
        return "Wilderness Agility Course";
    }

    @Override
    public void run(ProAgilityPlugin plugin) {
        Rs2Player.eatAt(80);

        for (Player player : Rs2Player.getPlayersInCombatLevelRange()) {
            if (player.getInteracting() != null && player.getInteracting().equals(Rs2Player.getLocalPlayer())) {
                int weaponId = player.getPlayerComposition().getEquipmentId(KitType.WEAPON);

                if (WeaponClassifier.isMagicWeapon(weaponId)) {
                    Rs2Prayer.disableAllPrayers();
                    Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MAGIC, true);
                } else if (WeaponClassifier.isRangedWeapon(weaponId)) {
                    Rs2Prayer.disableAllPrayers();
                    Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_RANGE, true);
                } else if (WeaponClassifier.isMeleeWeapon(weaponId)) {
                    Rs2Prayer.disableAllPrayers();
                    Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MELEE, true);
                }
            }
        }

        switch (this.stage) {
            case 0:
                this.squeezeThroughPipe();
                break;
            case 1:
                this.swingOnRope();
                break;
            case 2:
                this.crossSteppingStones();
                break;
            case 3:
                this.walkAcrossLog();
                break;
            case 4:
                this.climbUpRocks();
                break;
            case 5:
                this.tagDispenser();
                break;
        }
    }

}

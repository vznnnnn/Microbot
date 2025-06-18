package net.runelite.client.plugins.microbot.vzn.problastfurnace;

import lombok.Getter;
import lombok.Setter;

import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import net.runelite.client.plugins.microbot.vzn.util.TimeUtil;

import java.awt.event.KeyEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static net.runelite.client.plugins.microbot.util.antiban.enums.ActivityIntensity.EXTREME;

public class ProBlastFurnaceScript extends Script {

    public static final double VERSION = 1.0;

    private ProBlastFurnacePlugin plugin;
    private ProBlastFurnaceConfig config;

    @Getter private Instant startTime = null;
    @Getter @Setter private int startXp = -1;
    public boolean error = false;
    private Instant foremanPaidAt = null;

    @Override
    public void shutdown() {
        super.shutdown();

        if (mainScheduledFuture != null && !mainScheduledFuture.isCancelled()) {
            mainScheduledFuture.cancel(true);
        }

        Microbot.log("Shutting down script");
    }

    public boolean run(ProBlastFurnaceConfig config) {
        Microbot.enableAutoRunOn = false;
        Rs2Antiban.resetAntibanSettings();
        Rs2AntibanSettings.usePlayStyle = true;
        Rs2AntibanSettings.simulateFatigue = false;
        Rs2AntibanSettings.simulateAttentionSpan = true;
        Rs2AntibanSettings.behavioralVariability = true;
        Rs2AntibanSettings.nonLinearIntervals = true;
        Rs2AntibanSettings.dynamicActivity = true;
        Rs2AntibanSettings.profileSwitching = true;
        Rs2AntibanSettings.naturalMouse = true;
        Rs2AntibanSettings.simulateMistakes = true;
        Rs2AntibanSettings.moveMouseOffScreen = true;
        Rs2AntibanSettings.moveMouseRandomly = true;
        Rs2AntibanSettings.moveMouseRandomlyChance = 0.04;
        Rs2Antiban.setActivityIntensity(EXTREME);

        this.plugin = ProBlastFurnacePlugin.instance;
        this.config = config;
        this.startTime = Instant.now();
        this.error = false;

        this.mainScheduledFuture = this.scheduledExecutorService.scheduleWithFixedDelay(() ->
        {
            try {
                if (!Microbot.isLoggedIn() || !super.run()) {
                    return;
                }

                handleTick();
            } catch (Exception ex) {
                Microbot.log("Error in main loop: " + ex.getMessage());
                ex.printStackTrace();
            }
        }, 0, 50, TimeUnit.MILLISECONDS);
        return true;
    }

    private void handleTick() {
        if (error) {
            Microbot.log("Error - logging out");
            Rs2Player.logout();
            sleep(1000);
            return;
        }

        if (startXp == -1) {
            startXp = Microbot.getClient().getSkillExperience(Skill.SMITHING);
        }

        if (foremanPaidAt == null || Duration.between(foremanPaidAt, Instant.now()).toMinutes() > 10) {
            payForeman();
        } else if (dispenserContainsBars()) {
            takeBarsFromDispenser();
        } else if (Rs2Inventory.hasItem(ItemID.GOLD_ORE)) {
            if (Rs2Inventory.hasItem(ItemID.GAUNTLETS_OF_GOLDSMITHING)) {
                Rs2Inventory.equip(ItemID.GAUNTLETS_OF_GOLDSMITHING);
                Rs2Inventory.waitForInventoryChanges(1000);
            }

            Rs2GameObject.interact(9100, "Put-ore-on");
            sleepUntil(this::finishedPutOreOnConveyorBelt);

            if (hasNeedForemanPermissionWidget()) {
                payForeman();
            } else {
                Rs2Walker.walkTo(new WorldPoint(1940, 4962, 0));
                sleepUntil(this::dispenserContainsBars, 7500);
            }
        } else {
            Rs2Bank.openBank();
            sleepUntil(Rs2Bank::isOpen);

            if (Rs2Inventory.hasItem(ItemID.GOLD_BAR)) {
                Rs2Bank.depositAll(ItemID.GOLD_BAR);
            }

            if (Rs2Inventory.hasItem(ItemID.COINS)) {
                Rs2Bank.depositAll(ItemID.COINS);
            }

            Rs2Inventory.waitForInventoryChanges(1000);

            if (Rs2Player.getRunEnergy() < 40 && !Rs2Player.hasStaminaActive()) {
                Rs2Bank.withdrawOne("Stamina potion", 1);
                Rs2Inventory.waitForInventoryChanges(1000);

                Rs2Bank.closeBank();
                sleepUntil(() -> !Rs2Bank.isOpen());

                Rs2Inventory.interact("Stamina potion", "Drink");

                Rs2Bank.openBank();
                sleepUntil(Rs2Bank::isOpen);

                if (Rs2Inventory.hasItem("Stamina potion")) {
                    Rs2Bank.depositAll("Stamina potion");
                    Rs2Inventory.waitForInventoryChanges(1000);
                }

                if (Rs2Inventory.hasItem("Vial")) {
                    Rs2Bank.depositAll("Vial");
                    Rs2Inventory.waitForInventoryChanges(1000);
                }
            }

            Rs2Bank.withdrawX(ItemID.GOLD_ORE, 27);
            Rs2Inventory.waitForInventoryChanges(1000);
            Rs2Bank.closeBank();
            sleepUntil(() -> !Rs2Bank.isOpen());
        }
    }

    public String getTimeRunning() {
        return startTime != null ? TimeUtil.formatIntoAbbreviatedString((int) Duration.between(startTime, Instant.now()).toSeconds()) : "";
    }

    public int getXpGained() {
        return Microbot.getClient().getSkillExperience(Skill.SMITHING) - startXp;
    }
    
    private void payForeman() {
        if (Rs2Player.getRealSkillLevel(Skill.SMITHING) >= 60) {
            foremanPaidAt = Instant.now();
            return;
        }

        if (!Rs2Inventory.hasItem(ItemID.COINS)) {
            Rs2Bank.openBank();
            sleepUntil(Rs2Bank::isOpen);

            if (Rs2Inventory.isFull()) {
                if (Rs2Inventory.hasItem(ItemID.GOLD_ORE)) {
                    Rs2Bank.depositAll(ItemID.GOLD_ORE);
                }

                if (Rs2Inventory.hasItem(ItemID.GOLD_BAR)) {
                    Rs2Bank.depositAll(ItemID.GOLD_BAR);
                }
            }

            Rs2Bank.withdrawX(ItemID.COINS, 2500);
            Rs2Inventory.waitForInventoryChanges(1500);
            Rs2Bank.closeBank();
            sleepUntil(() -> !Rs2Bank.isOpen());
        }

        Rs2Npc.interact(2923, "Pay");
        sleepUntil(() -> Rs2Widget.hasWidget("Pay 2,500 coins to use the Blast Furnace?"));
        Rs2Keyboard.keyPress('1');
        sleepUntil(() -> Rs2Widget.hasWidget("Okay, you can use the furnace for ten minutes"));
        Rs2Inventory.waitForInventoryChanges(1000);
        foremanPaidAt = Instant.now();
    }

    private boolean hasNeedForemanPermissionWidget() {
        return Rs2Widget.hasWidget("You must ask the foreman's permission");
    }

    private boolean finishedPutOreOnConveyorBelt() {
        return !Rs2Inventory.hasItem(ItemID.GOLD_ORE) || hasNeedForemanPermissionWidget();
    }

    public boolean dispenserContainsBars() {
        return Microbot.getVarbitValue(VarbitID.BLAST_FURNACE_GOLD_BARS) > 0;
    }

    private void takeBarsFromDispenser() {
        if (Rs2Inventory.hasItem(ItemID.ICE_GLOVES)) {
            Rs2Inventory.equip(ItemID.ICE_GLOVES);
        } else if (Rs2Inventory.hasItem(ItemID.SMITHING_UNIFORM_GLOVES_ICE)) {
            Rs2Inventory.equip(ItemID.SMITHING_UNIFORM_GLOVES_ICE);
        }

        Rs2GameObject.interact(9092, "Take");

        sleepUntil(() ->
                Rs2Widget.hasWidget("What would you like to take?") ||
                        Rs2Widget.hasWidget("How many would you like") ||
                        Rs2Widget.hasWidget("The bars are still molten!"), 5000);

        boolean noIceGlovesEquipped = Rs2Widget.hasWidget("The bars are still molten!");

        if (noIceGlovesEquipped) {
            if (!Rs2Inventory.equip(ItemID.ICE_GLOVES) && !Rs2Inventory.interact(ItemID.SMITHING_UNIFORM_GLOVES_ICE, "Wear")) {
                Microbot.showMessage("Ice gloves or smith gloves required to loot the hot bars.");
                Rs2Player.logout();
                this.shutdown();
                return;
            }

            Rs2GameObject.interact(9092, "Take");
        }

        sleepUntil(() -> Rs2Widget.hasWidget("What would you like to take?") || Rs2Widget.hasWidget("How many would you like"), 3000);

        // If somehow multiple type of bars are created we need to clean up the dispenser.
        boolean multipleBarTypes = Rs2Widget.hasWidget("What would you like to take?");
        boolean canLootBar = Rs2Widget.hasWidget("How many would you like");

        if (super.run()) {
            if (canLootBar) {
                Rs2Keyboard.keyPress(KeyEvent.VK_SPACE);
            } else if (multipleBarTypes) {
                Rs2Keyboard.keyPress(KeyEvent.VK_SPACE);
            }

            Rs2Inventory.waitForInventoryChanges(5000);

            // fix inventory just in case we get stuck
            if (Rs2Widget.hasWidget("You don't have any free inventory space")) {
                Rs2Bank.openBank();
                sleepUntil(Rs2Bank::isOpen);

                for (Rs2ItemModel item : Rs2Inventory.items()) {
                    if (!(item.getId() == ItemID.GAUNTLETS_OF_GOLDSMITHING || item.getId() == ItemID.ICE_GLOVES)) {
                        Rs2Bank.depositAll(item.getId());
                        Rs2Inventory.waitForInventoryChanges(1000);
                    }
                }

                Rs2Bank.closeBank();
                sleepUntil(() -> !Rs2Bank.isOpen());
            }

            Rs2Inventory.equip(ItemID.GAUNTLETS_OF_GOLDSMITHING);
        }
    }

}

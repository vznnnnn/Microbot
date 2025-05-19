package net.runelite.client.plugins.microbot.vzn.procombat.stage;

import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.vzn.procombat.PrepareStageImpl;
import net.runelite.client.plugins.microbot.vzn.procombat.ProCombatPlugin;

import javax.swing.*;

import static net.runelite.client.plugins.microbot.util.Global.sleep;

public class PrepareInventory extends PrepareStageImpl {

    public PrepareInventory(ProCombatPlugin plugin) {
        super(plugin);
    }

    @Override
    public void tick() {
        Microbot.log("Preparing inventory");

        // Open bank
        Rs2Bank.openBank();
        sleep(500);
        Rs2Bank.handleBankPin(config.bankPin());
        sleep(1000);

        // Empty inventory
        Rs2Bank.depositAll();
        Rs2Inventory.waitForInventoryChanges(1500);
        sleep(500);

        // Take out rune pouch
        if (Rs2Bank.hasRunePouch()) {
            Rs2Bank.withdrawRunePouch();
            Rs2Inventory.waitForInventoryChanges(1500);
        } else {
            JOptionPane.showMessageDialog(null, "Bank error - Missing rune pouch");
            plugin.getScript().bankError = true;
            return;
        }

        // Take out food & prayer potion to heal
        boolean needsFood = Rs2Player.getHealthPercentage() < config.healthThreshold();
        boolean needsPrayer = Rs2Player.getBoostedSkillLevel(Skill.PRAYER) < config.minPrayerPoints();

        if (needsFood) {
            Rs2Bank.withdrawX("Shark", 5);
            Rs2Inventory.waitForInventoryChanges(1500);
        }

        if (needsPrayer) {
            Rs2Bank.withdrawX("Prayer potion(4)", 1);
            Rs2Inventory.waitForInventoryChanges(1500);
        }

        // Close bank
        sleep(500);
        Rs2Bank.closeBank();

        // Empty rune pouch
        Rs2ItemModel runePouchItem = getRunePouchItem();
        assert runePouchItem != null;
        Rs2Inventory.interact(runePouchItem, "Empty");

        // Heal to full
        if (needsFood) {
            while (Rs2Player.getHealthPercentage() < config.healthThreshold()) {
                Rs2Player.eatAt(config.healthThreshold());
                sleep(1000);
            }
        }

        if (needsPrayer) {
            while (Rs2Player.getBoostedSkillLevel(Skill.PRAYER) < config.minPrayerPoints()) {
                Rs2Player.drinkPrayerPotionAt(config.minPrayerPoints());
                sleep(1000);
            }
        }

        // Open bank
        Rs2Bank.openBank();
        sleep(500);

        // Deposit everything except rune pouch
        Rs2Bank.depositAllExcept(runePouchItem.getName());
        sleep(500);

        // Check if we have the required items
        if (!hasBankItemQuantity("Death rune", 4000)
                || !hasBankItemQuantity("Chaos rune", 8000)
                || !hasBankItemQuantity("Water rune", 8000)
                || !hasBankItemQuantity("Stamina potion(4)", 1)
                || !hasBankItemQuantity("Anti-venom(4)", 1)
                || !hasBankItemQuantity("Prayer potion(4)", 18)
                || !hasBankItemQuantity("Shark", 6)
                || !hasBankItemQuantity("Varrock teleport", 1)) {
            JOptionPane.showMessageDialog(null, "Bank error - Missing required supplies");
            plugin.getScript().bankError = true;
            return;
        }

        Rs2Bank.withdrawX("Death rune", 4000);
        Rs2Bank.withdrawX("Chaos rune", 8000);
        Rs2Bank.withdrawX("Water rune", 8000);
        Rs2Inventory.waitForInventoryChanges(1500);

        // Close bank
        Rs2Bank.closeBank();
        sleep(500);

        // Add runes to rune pouch
        addRunesToRunePouch(runePouchItem, "Death rune", "Chaos rune", "Water rune");
        Rs2Inventory.moveItemToSlot(Rs2Inventory.get("Rune pouch"), 27);

        // Open bank
        Rs2Bank.openBank();

        // Withdraw items
        Rs2Bank.withdrawItem("Stamina potion(4)");
        Rs2Bank.withdrawItem("Anti-venom(4)");
        Rs2Bank.withdrawX("Prayer potion(4)", 18);
        Rs2Bank.withdrawX("Shark", 6);
        Rs2Bank.withdrawX("Varrock teleport", 20);
        Rs2Inventory.waitForInventoryChanges(1500);

        // Close bank
        Rs2Bank.closeBank();
    }

    @Override
    public boolean isComplete() {
        return Rs2Inventory.hasItemAmount("Stamina potion(4)", 1)
                && Rs2Inventory.hasItemAmount("Anti-venom(4)", 1)
                && Rs2Inventory.hasItemAmount("Prayer potion(4)", 18)
                && Rs2Inventory.hasItemAmount("Shark", 6)
                && Rs2Inventory.hasItemAmount("Varrock teleport", 1)
                && Rs2Inventory.hasItemAmount("Rune pouch", 1)
                && Rs2Player.getHealthPercentage() >= config.healthThreshold()
                && Rs2Player.getBoostedSkillLevel(Skill.PRAYER) >= config.minPrayerPoints();
    }

    private Rs2ItemModel getRunePouchItem() {
        for (Rs2ItemModel item : Rs2Inventory.all()) {
            if (item.getName().startsWith("Rune pouch")) {
                return item;
            }
        }
        return null;
    }

    private void addRunesToRunePouch(Rs2ItemModel runePouchItem, String... itemNames) {
        if (Rs2Inventory.isItemSelected()) {
            Rs2Inventory.deselect();
        }

        for (String itemName : itemNames) {
            Rs2Inventory.interact(itemName, "Use");
            Rs2Inventory.interact(runePouchItem);
        }
    }

    private boolean hasBankItemQuantity(String itemName, int quantity) {
        Rs2ItemModel item = Rs2Bank.getBankItem(itemName);
        return item != null && item.getQuantity() >= quantity;
    }

}

package net.runelite.client.plugins.microbot.vzn.prommtunnel.stage;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.vzn.prommtunnel.PrepareStageImpl;
import net.runelite.client.plugins.microbot.vzn.prommtunnel.ProMMTunnelPlugin;

import static net.runelite.client.plugins.microbot.util.Global.sleep;
import static net.runelite.client.plugins.microbot.vzn.prommtunnel.ProMMTunnelScript.PLAYER_ATTACK_TILE;

public class TraverseTunnel extends PrepareStageImpl {

    public TraverseTunnel(ProMMTunnelPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "Traverse Tunnel";
    }

    @Override
    public void tick() {
        Microbot.log("Traversing tunnel");

        // Start walking in a separate thread
        Thread walkThread = new Thread(() -> Rs2Walker.walkTo(PLAYER_ATTACK_TILE));
        walkThread.start();

        // While walking, monitor health, prayer, and stamina
        while (walkThread.isAlive()) {
            if (plugin.getScript().shouldRetreat()) {
                plugin.getScript().retreatToSafety();
                return;
            }

            plugin.getScript().ensureProtectionPrayersEnabled();
            plugin.getScript().evaluateAndConsumeFoodAndPotions();

            if (Rs2Player.getRunEnergy() < 75) {
                if (!Rs2Player.hasStaminaActive()) {
                    Rs2Inventory.interact("Stamina potion", "Drink");
                }
            }

            sleep(1000);
        }

        // Optionally wait for walking to fully finish
        try {
            walkThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isComplete() {
        return Rs2Player.getWorldLocation().distanceTo(PLAYER_ATTACK_TILE) <= 10;
    }

}

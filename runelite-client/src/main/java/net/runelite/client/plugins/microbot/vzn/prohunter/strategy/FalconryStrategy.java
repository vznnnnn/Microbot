package net.runelite.client.plugins.microbot.vzn.prohunter.strategy;

import lombok.Getter;
import net.runelite.api.NPC;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.Global;
import net.runelite.client.plugins.microbot.util.inventory.InteractOrder;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.vzn.prohunter.ProHunterPlugin;
import net.runelite.client.plugins.microbot.vzn.prohunter.ProHunterStrategy;

import java.util.stream.StreamSupport;

public class FalconryStrategy implements ProHunterStrategy {

    private static final int SPOTTED_KEBBIT_ID = 5531;
    private static final int DARK_KEBBIT_ID = 5532;
    private static final int DASHING_KEBBIT_ID = 5533;
    private static final int GYR_FALCON_ID = 1342;
    private static final int DARK_GYR_FALCON_ID = 1344;
    private static final int DASHING_GYR_FALCON_ID = 1343;
    private static final int BONES_ID = 526;
    private static final int KEBBIT_FUR_ID = 10125;
    private static final int DARK_KEBBIT_FUR_ID = 10115;
    private static final int DASHING_KEBBIT_FUR_ID = 10105;
    private static final int FALCON_PROJECTILE_ID = 922;

    private static final WorldPoint START_TILE = new WorldPoint(2377, 3587, 0);
    private static final int MAX_DISTANCE_FROM_START = 20;

    @Getter private ProHunterPlugin plugin;

    public FalconryStrategy(ProHunterPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean isInFalconryArea() {
        return Rs2Player.getWorldLocation().distanceTo(START_TILE) <= MAX_DISTANCE_FROM_START;
    }

    private void returnToStartIfNeeded() {
        if (!isInFalconryArea()) {
            Rs2Walker.walkTo(START_TILE);
            Global.sleepUntil(this::isInFalconryArea, 5000);
        }
    }

    private void handleFullInventory() {
        if (Rs2Inventory.getEmptySlots() <= 1) {
            Rs2Inventory.dropAll((item) -> item.getId() == BONES_ID || item.getId() == KEBBIT_FUR_ID || item.getId() == DARK_KEBBIT_FUR_ID || item.getId() == DASHING_KEBBIT_FUR_ID, InteractOrder.PROFESSIONAL);
            Global.sleep(300);
        }
    }

    public boolean isProjectileActive(int projectileId) {
        if (Microbot.getClient() == null) {
            Microbot.log("Client is null");
            return false;
        }

        var worldView = Microbot.getClient().getWorldView(Rs2Player.getLocalLocation().getWorldView());
        if (worldView == null) {
            Microbot.log("worldView is null");
            return false;
        }

        if (Microbot.getClient().getProjectiles() == null) {
            Microbot.log("getProjectiles is null");
            return false;
        }

        return StreamSupport.stream(Microbot.getClient().getProjectiles().spliterator(), false)
                .anyMatch(p -> p.getId() == projectileId);
    }

    private NPC getKebbitNpc() {
        // Hunter level 69 or higher, try to catch a dashing kebbit
        // Hunter level 57 or higher, try to catch a dark kebbit
        // Hunter level lower than 57, try to catch a spotted kebbit
        int hunterLevel = Rs2Player.getBoostedSkillLevel(Skill.HUNTER);

        if (hunterLevel >= 69) {
            return Rs2Npc.getNpc(DASHING_KEBBIT_ID);
        } else if (hunterLevel >= 57) {
            return Rs2Npc.getNpc(DARK_KEBBIT_ID);
        } else {
            return Rs2Npc.getNpc(SPOTTED_KEBBIT_ID);
        }
    }

    private NPC getFalconNpc() {
        int[] falconIds = { GYR_FALCON_ID, DASHING_GYR_FALCON_ID, DARK_GYR_FALCON_ID };
        for (int id : falconIds) {
            NPC falcon = Rs2Npc.getNpc(id);
            if (falcon != null) return falcon;
        }
        return null;
    }

    @Override
    public String getName() {
        return "Falconry";
    }

    @Override
    public void run(ProHunterPlugin plugin) {
        // Check if we're over our max script time
        plugin.getScript().checkScriptDurationAndLogout();

        // Check if we're animating
        Global.sleepUntil(() -> !Rs2Player.isAnimating(), 1000);

        // Ensure we're in the start area
        returnToStartIfNeeded();

        // Handle full inventory
        handleFullInventory();

        // Check if falcon is flying
        if (isProjectileActive(FALCON_PROJECTILE_ID)) {
            Global.sleep(500);
            return;
        }

        // Try to retrieve falcon
        NPC falcon = getFalconNpc();
        if (falcon != null) {
            Rs2Npc.interact(falcon.getId(), "Retrieve");
            Global.sleepUntil(() -> getFalconNpc() == null, plugin.getConfig().falconryActionDelay());
        } else {
            NPC kebbit = getKebbitNpc();
            if (kebbit != null && kebbit.getInteracting() == null) {
                Rs2Npc.interact(kebbit.getId(), "Catch");
                Global.sleepUntil(() -> getFalconNpc() != null, plugin.getConfig().falconryActionDelay());
            }
        }

        Global.sleep(plugin.getConfig().falconryPollInterval()); // polling delay
    }

}

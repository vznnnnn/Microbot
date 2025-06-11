package net.runelite.client.plugins.microbot.vzn.prommtunnel.stage;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.vzn.prommtunnel.PrepareStageImpl;
import net.runelite.client.plugins.microbot.vzn.prommtunnel.ProMMTunnelPlugin;

import static net.runelite.client.plugins.microbot.util.Global.sleep;
import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;

public class TravelGrandTree extends PrepareStageImpl {

    private static final WorldPoint GRAND_TREE_ENTRANCE_TILE = new WorldPoint(2466, 3491, 0);
    private static final WorldPoint GRAND_TREE_INSIDE_TILE = new WorldPoint(2466, 3493, 0);
    private static final int GRAND_TREE_DOOR_ID = 1968;
    private static final int GRAND_TREE_LADDER_ID = 4458;

    public TravelGrandTree(ProMMTunnelPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "Travel Grand Tree";
    }

    @Override
    public void tick() {
        Microbot.log("Walking to Grand Tree");
        Rs2Walker.walkTo(GRAND_TREE_ENTRANCE_TILE);
        Rs2GameObject.interact(GRAND_TREE_DOOR_ID);
        sleepUntil(() -> Rs2Player.getWorldLocation().equals(GRAND_TREE_INSIDE_TILE));
        sleep(1000);
        Rs2GameObject.interact(GRAND_TREE_LADDER_ID, "Climb-up");
        sleepUntil(() -> Rs2Player.getWorldLocation().getPlane() == 1, 2500);
    }

    @Override
    public boolean isComplete() {
        return Rs2Player.getWorldLocation().getPlane() == 1;
    }

}

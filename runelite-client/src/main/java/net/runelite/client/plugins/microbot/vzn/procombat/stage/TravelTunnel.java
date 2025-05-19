package net.runelite.client.plugins.microbot.vzn.procombat.stage;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.vzn.procombat.PrepareStageImpl;
import net.runelite.client.plugins.microbot.vzn.procombat.ProCombatPlugin;

import static net.runelite.client.plugins.microbot.util.Global.sleep;

public class TravelTunnel extends PrepareStageImpl {

    private static final WorldPoint TUNNEL_LADDER_TILE = new WorldPoint(2764, 2703, 0);
    public static final int TUNNEL_LADDER_ID = 4780;
    private static final WorldPoint TUNNEL_TILE = new WorldPoint(2764, 9103, 0);

    public TravelTunnel(ProCombatPlugin plugin) {
        super(plugin);
    }

    @Override
    public void tick() {
        Microbot.log("Walking to tunnel");
        Rs2Walker.walkTo(TUNNEL_LADDER_TILE);
        Rs2GameObject.interact(TUNNEL_LADDER_ID, "Climb-down");
        sleep(1000);
    }

    @Override
    public boolean isComplete() {
        return Rs2Player.getWorldLocation().distanceTo(TUNNEL_TILE) <= 10;
    }

}

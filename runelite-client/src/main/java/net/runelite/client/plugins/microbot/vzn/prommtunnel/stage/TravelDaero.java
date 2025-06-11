package net.runelite.client.plugins.microbot.vzn.prommtunnel.stage;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.vzn.prommtunnel.PrepareStageImpl;
import net.runelite.client.plugins.microbot.vzn.prommtunnel.ProMMTunnelPlugin;

import static net.runelite.client.plugins.microbot.util.Global.sleep;
import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;

public class TravelDaero extends PrepareStageImpl {

    private static final WorldPoint DAERO_NPC_TILE = new WorldPoint(2481, 3486, 1);

    public TravelDaero(ProMMTunnelPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "Travel Daero";
    }

    @Override
    public void tick() {
        Microbot.log("Travel with Daero");
        Rs2Walker.walkTo(DAERO_NPC_TILE);
        Rs2Npc.interact("Daero", "Travel");
        sleepUntil(() -> Rs2Player.getWorldLocation().getPlane() == 0);
        sleep(3000);
    }

    @Override
    public boolean isComplete() {
        return Rs2Player.getWorldLocation().getPlane() == 0 && Rs2Npc.getNpc("Waydar") != null;
    }

}

package net.runelite.client.plugins.microbot.vzn.prommtunnel.stage;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.vzn.prommtunnel.PrepareStageImpl;
import net.runelite.client.plugins.microbot.vzn.prommtunnel.ProMMTunnelPlugin;

import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;

public class TravelLumdo extends PrepareStageImpl {

    private static final WorldPoint LUMDO_TILE = new WorldPoint(2802, 2707, 0);

    public TravelLumdo(ProMMTunnelPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "Travel Lumdo";
    }

    @Override
    public void tick() {
        Rs2Npc.interact("Lumdo", "Travel");
        sleepUntil(() -> Rs2Npc.getNpc("Lumdo") == null, 2500);
    }

    @Override
    public boolean isComplete() {
        return Rs2Player.getWorldLocation().distanceTo(LUMDO_TILE) <= 10;
    }

}

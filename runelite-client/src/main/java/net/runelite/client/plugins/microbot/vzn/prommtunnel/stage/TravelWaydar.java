package net.runelite.client.plugins.microbot.vzn.prommtunnel.stage;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.vzn.prommtunnel.PrepareStageImpl;
import net.runelite.client.plugins.microbot.vzn.prommtunnel.ProMMTunnelPlugin;

import static net.runelite.client.plugins.microbot.util.Global.sleep;
import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;

public class TravelWaydar extends PrepareStageImpl {

    public TravelWaydar(ProMMTunnelPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "Travel Waydar";
    }

    @Override
    public void tick() {
        Microbot.log("Travel with Waydar");
        Rs2Npc.interact("Waydar", "Travel");
        sleepUntil(() -> Rs2Npc.getNpc("Lumdo") != null);
        sleep(3000);
    }

    @Override
    public boolean isComplete() {
        return Rs2Npc.getNpc("Lumdo") != null;
    }

}

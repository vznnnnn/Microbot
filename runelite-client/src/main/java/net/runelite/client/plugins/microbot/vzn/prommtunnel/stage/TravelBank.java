package net.runelite.client.plugins.microbot.vzn.prommtunnel.stage;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.vzn.prommtunnel.PrepareStageImpl;
import net.runelite.client.plugins.microbot.vzn.prommtunnel.ProMMTunnelPlugin;

import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;

public class TravelBank extends PrepareStageImpl {

    private static final WorldPoint GE_BANK_TILE = new WorldPoint(3164, 3487, 0);

    public TravelBank(ProMMTunnelPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "Travel Bank";
    }

    @Override
    public void tick() {
        Microbot.log("Walking to GE bank");
        Rs2Prayer.disableAllPrayers();
        Rs2Walker.walkTo(GE_BANK_TILE);
        sleepUntil(() -> Rs2Player.getWorldLocation().distanceTo(GE_BANK_TILE) <= 20, 5000);
    }

    @Override
    public boolean isComplete() {
        return Rs2Player.getWorldLocation().distanceTo(GE_BANK_TILE) <= 20;
    }

}

package net.runelite.client.plugins.microbot.vzn.prommtunnel.stage;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import net.runelite.client.plugins.microbot.vzn.prommtunnel.PrepareStageImpl;
import net.runelite.client.plugins.microbot.vzn.prommtunnel.ProMMTunnelPlugin;

import static net.runelite.client.plugins.microbot.util.Global.sleep;
import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;

public class TravelSpiritTree extends PrepareStageImpl {

    private static final WorldPoint SPIRIT_TREE_TILE = new WorldPoint(3185, 3508, 0);
    private static final WorldPoint GNOME_STRONGHOLD_TILE = new WorldPoint(2461, 3444, 0);

    public TravelSpiritTree(ProMMTunnelPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "Travel Spirit Tree";
    }

    @Override
    public void tick() {
        Microbot.log("Walking to Spirit Tree");
        Rs2Walker.walkTo(SPIRIT_TREE_TILE);
        sleepUntil(() -> Rs2Player.getWorldLocation().distanceTo(SPIRIT_TREE_TILE) <= 10, 5000);
        Rs2GameObject.interact(1295, "Travel");
        sleepUntil(() -> Rs2Widget.findWidget("Spirit Tree Locations") != null, 5000);
        sleep(1000);
        Rs2Keyboard.keyPress('2');
        sleepUntil(() -> Rs2Player.getWorldLocation().distanceTo(GNOME_STRONGHOLD_TILE) < 10, 5000);
    }

    @Override
    public boolean isComplete() {
        return Rs2Player.getWorldLocation().distanceTo(GNOME_STRONGHOLD_TILE) < 10;
    }

}

package net.runelite.client.plugins.microbot.vzn.molanisk;

import com.google.inject.Inject;
import net.runelite.api.*;
import net.runelite.api.events.WallObjectSpawned;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
        name = PluginDescriptor.vzn + "Molanisks",
        description = "Tags Molanisks in object form",
        tags = {"slayer", "molanisk", "tag"},
        enabledByDefault = false
)
public class MolaniskPlugin extends Plugin {

    @Inject
    private Client client;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private MolaniskObjectOverlay molaniskObjectOverlay;

    @Override
    protected void startUp() throws Exception {
        overlayManager.add(molaniskObjectOverlay);
    }

    @Override
    protected void shutDown() throws Exception {
        overlayManager.remove(molaniskObjectOverlay);
    }

    @Subscribe
    public void onWallObjectSpawned(WallObjectSpawned event) {
        WallObject wallObject = event.getWallObject();
        if (wallObject == null) {
            return;
        }

        client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", String.format("Found WallObject ID: %d at %s", wallObject.getId(), wallObject.getWorldLocation()), null);
    }

}

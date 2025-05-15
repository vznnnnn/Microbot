package net.runelite.client.plugins.microbot.vzn.molanisk;

import net.runelite.api.Client;
import net.runelite.api.Tile;
import net.runelite.api.WallObject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;
import java.awt.Dimension;

@Singleton
public class MolaniskObjectOverlay extends Overlay {

    private static final int MOLANISK_WALL_ID = 22546;

    private final Client client;

    @Inject
    public MolaniskObjectOverlay(Client client) {
        this.client = client;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        Tile[][][] tiles = client.getScene().getTiles();
        int plane = client.getPlane();

        for (Tile[] row : tiles[plane]) {
            for (Tile tile : row) {
                if (tile == null) continue;

                WallObject wallObject = tile.getWallObject();
                if (wallObject == null) continue;

                if (wallObject.getId() == MOLANISK_WALL_ID) {
                    Shape poly = wallObject.getConvexHull();
                    if (poly != null) {
                        OverlayUtil.renderPolygon(graphics, poly, Color.RED);
                    }
                }
            }
        }

        return null;
    }

}

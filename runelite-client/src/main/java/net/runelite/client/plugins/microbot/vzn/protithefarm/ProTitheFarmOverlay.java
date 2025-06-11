package net.runelite.client.plugins.microbot.vzn.protithefarm;

import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.vzn.protithefarm.enums.TitheFarmMaterial;
import net.runelite.client.plugins.microbot.vzn.protithefarm.models.TitheFarmPlant;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.components.ImageComponent;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Objects;

public class ProTitheFarmOverlay extends OverlayPanel {

    private final ProTitheFarmPlugin plugin;
    private final ProTitheFarmConfig config;

    @Inject
    ProTitheFarmOverlay(ProTitheFarmPlugin plugin, ProTitheFarmConfig config) {
        super(plugin);

        this.plugin = plugin;
        this.config = config;

        setPosition(OverlayPosition.TOP_LEFT);
        setNaughty();
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        try {
            panelComponent.setPreferredSize(new Dimension(250, 300));

            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("ProTitheFarm")
                    .color(Color.MAGENTA)
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("State")
                    .leftColor(Color.WHITE)
                    .right(plugin.getScript().getState().toString())
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Status")
                    .leftColor(Color.WHITE)
                    .right(Microbot.status)
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Time Elapsed")
                    .leftColor(Color.WHITE)
                    .right(plugin.getScript().getTimeRunning())
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("XP Gained")
                    .leftColor(Color.WHITE)
                    .right(plugin.getScript().getXpGained() + "")
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Fruits Farmed")
                    .right("")
                    .build());

            panelComponent.getChildren().add(new ImageComponent(getImage(Objects.requireNonNull(TitheFarmMaterial.getSeedForLevel()).getFruitId(), plugin.getScript().getFruitsFarmed())));

            if (config.enableOverlay()) {
                for (TitheFarmPlant plant : plugin.getScript().getPlants()) {
                    if (plant == null || plant.getGameObject() == null) {
                        continue;
                    }

                    LocalPoint localLocation = plant.getGameObject().getLocalLocation();
                    int tileHeight = Perspective.getTileHeight(Microbot.getClient(), localLocation, Rs2Player.getWorldLocation().getPlane());
                    Polygon polygon = Perspective.getCanvasTilePoly(Microbot.getClient(), localLocation, tileHeight);

                    if (polygon != null) {
                        OverlayUtil.renderPolygon(graphics, polygon, Color.CYAN);
                    }

                    Point textLocation = Perspective.getCanvasTextLocation(
                            Microbot.getClient(),
                            graphics,
                            localLocation,
                            "p: " + plant.isEmptyPatchOrSeedling(),
                            20 // vertical offset to position text more clearly
                    );

                    if (textLocation != null) {
                        OverlayUtil.renderTextLocation(graphics, textLocation, "index: " + plant.getIndex(), Color.GREEN);
                    }
                }
            }
        } catch (Exception ex) {
            Microbot.logStackTrace(this.getClass().getSimpleName(), ex);
        }

        return super.render(graphics);
    }

    private BufferedImage getImage(int itemID, int amount) {
        return Microbot.getItemManager().getImage(itemID, amount, true);
    }

}

package net.runelite.client.plugins.microbot.vzn.proconstruction;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;

public class ProConstructionOverlay extends OverlayPanel {

    private final ProConstructionPlugin plugin;
    private final ProConstructionConfig config;

    @Inject
    ProConstructionOverlay(ProConstructionPlugin plugin, ProConstructionConfig config) {
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
                    .text("ProConstruction")
                    .color(Color.MAGENTA)
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
        } catch (Exception ex) {
            Microbot.logStackTrace(this.getClass().getSimpleName(), ex);
        }

        return super.render(graphics);
    }
}

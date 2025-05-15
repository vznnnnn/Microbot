package net.runelite.client.plugins.microbot.vzn.profisher;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;

public class ProFisherOverlay extends OverlayPanel {

    private final ProFisherConfig config;

    @Inject
    ProFisherOverlay(ProFisherPlugin plugin, ProFisherConfig config) {
        super(plugin);

        this.config = config;
        setPosition(OverlayPosition.TOP_LEFT);
        setNaughty();
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        try {
            panelComponent.setPreferredSize(new Dimension(200, 150));
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("TickFishing")
                    .color(Color.PINK)
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Status")
                    .leftColor(Color.WHITE)
                    .right(Microbot.status)
                    .build());
        } catch (Exception ex) {
            Microbot.logStackTrace(this.getClass().getSimpleName(), ex);
        }
        return super.render(graphics);
    }

}

package net.runelite.client.plugins.microbot.vzn.profletcher;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;

public class ProFletcherOverlay extends OverlayPanel {

    private ProFletcherPlugin plugin;

    @Inject
    ProFletcherOverlay(ProFletcherPlugin plugin) {
        super(plugin);

        this.plugin = plugin;

        setPosition(OverlayPosition.TOP_LEFT);
        setNaughty();
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        try {
            panelComponent.setPreferredSize(new Dimension(200, 150));
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("ProFletcher")
                    .color(Color.PINK)
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Strategy")
                    .leftColor(Color.WHITE)
                    .right(this.plugin.getStrategy().getName())
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

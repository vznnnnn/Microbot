package net.runelite.client.plugins.microbot.vzn.prohunter;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;

public class ProHunterOverlay extends OverlayPanel {

    private ProHunterPlugin plugin;

    @Inject
    ProHunterOverlay(ProHunterPlugin plugin) {
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
                    .text("ProHunter")
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

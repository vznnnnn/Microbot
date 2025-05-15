package net.runelite.client.plugins.microbot.vzn.profletcher.strategy;

import lombok.Getter;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.Global;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import net.runelite.client.plugins.microbot.vzn.profletcher.ProFletcherPlugin;
import net.runelite.client.plugins.microbot.vzn.profletcher.ProFletcherStrategy;

public class DartTipsStrategy implements ProFletcherStrategy {

    private static final String FEATHER = "Feather";
    private static final String DART_TIP_SUFFIX = "dart tip"; // Handles all types: bronze, iron, etc.

    @Getter private ProFletcherPlugin plugin;

    public DartTipsStrategy(ProFletcherPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "Dart Tips";
    }

    @Override
    public void run(ProFletcherPlugin plugin) {
        Widget inventory = Rs2Widget.getWidget(WidgetInfo.INVENTORY.getId());

        if (inventory == null || inventory.getChildren() == null) {
            return;
        }

        Widget dartTipWidget = null;
        Widget featherWidget = null;

        for (Widget item : inventory.getChildren()) {
            if (item == null || item.getName() == null) continue;

            String name = item.getName();
            if (name.contains(DART_TIP_SUFFIX)) {
                dartTipWidget = item;
            } else if (name.contains(FEATHER)) {
                featherWidget = item;
            }

            if (dartTipWidget != null && featherWidget != null) break;
        }

        if (dartTipWidget == null || featherWidget == null) {
            if (dartTipWidget == null) {
                Microbot.log("Dart tip widget is null");
            }
            if (featherWidget == null) {
                Microbot.log("Feather widget is null");
            }
            return;
        }

        // Click dart tip
        Microbot.getMouse().click(dartTipWidget.getBounds());
        Global.sleep(5, 10);

        // Click feather
        Microbot.getMouse().click(featherWidget.getBounds());
        Global.sleep(5, 10);
    }

}

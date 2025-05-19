package net.runelite.client.plugins.microbot.vzn.procombat;

public abstract class PrepareStageImpl {

    public ProCombatPlugin plugin;
    public ProCombatConfig config;

    public PrepareStageImpl(ProCombatPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public abstract void tick();

    public abstract boolean isComplete();

}

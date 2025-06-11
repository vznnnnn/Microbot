package net.runelite.client.plugins.microbot.vzn.prommtunnel;

public abstract class PrepareStageImpl {

    public ProMMTunnelPlugin plugin;
    public ProMMTunnelConfig config;

    public PrepareStageImpl(ProMMTunnelPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public abstract String getName();

    public abstract void tick();

    public abstract boolean isComplete();

}

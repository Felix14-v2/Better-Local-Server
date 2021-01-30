package ml.rederpz.betterlocalserver.config;

import ml.rederpz.betterlocalserver.BetterLocalServer;
import ml.rederpz.betterlocalserver.mixin.adapter.PlayerManagerAdapter;
import net.minecraft.client.util.NetworkUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.BaseText;
import net.minecraft.text.TranslatableText;
import net.minecraft.world.GameMode;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Rederpz on Jan 29, 2021
 */
public final class LocalServerConfig {

    private final String motd;
    private boolean allowCheats, allowFlight;
    private boolean useLocalPort;
    private boolean offlineMode;
    private int viewDistance;
    private GameMode gameMode;
    private boolean remote;
    private boolean pvp;
    private int port;

    public LocalServerConfig(final MinecraftServer server) {
        this.motd = server.getServerMotd();
        this.remote = server.isRemote();
        this.allowCheats = server.getPlayerManager().areCheatsAllowed();
        this.allowFlight = server.isFlightEnabled();
        this.port = this.remote ? server.getServerPort() : 25565;
        this.useLocalPort = false;
        this.viewDistance = server.getPlayerManager().getViewDistance();
        final GameMode defaultGameMode = ((PlayerManagerAdapter) server.getPlayerManager()).getGameMode();
        this.gameMode = defaultGameMode == null ? GameMode.NOT_SET : defaultGameMode;
        this.pvp = server.isPvpEnabled();
        this.offlineMode = !server.isOnlineMode();
    }

    public Map<BaseText, Map.Entry<Object, Object>> getChanges(final LocalServerConfig config) {
        final Map<BaseText, Map.Entry<Object, Object>> changes = new HashMap<>();

        changes.put(new TranslatableText("localServer.options.allowCheats"), new AbstractMap.SimpleEntry<>(this.allowCheats, config.allowCheats));
        changes.put(new TranslatableText("localServer.options.allowFlight"), new AbstractMap.SimpleEntry<>(this.allowFlight, config.allowFlight));
        changes.put(new TranslatableText("localServer.options.port"), new AbstractMap.SimpleEntry<>(this.port, config.port));
        changes.put(new TranslatableText("localServer.options.port.local"), new AbstractMap.SimpleEntry<>(this.useLocalPort, config.useLocalPort));
        changes.put(new TranslatableText("localServer.options.offlineMode"), new AbstractMap.SimpleEntry<>(this.offlineMode, config.offlineMode));
        changes.put(new TranslatableText("localServer.options.viewDistance"), new AbstractMap.SimpleEntry<>(this.viewDistance, config.viewDistance));
        changes.put(new TranslatableText("localServer.options.defaultGameMode"), new AbstractMap.SimpleEntry<>(this.gameMode, config.gameMode));
        changes.put(new TranslatableText("localServer.options.pvp"), new AbstractMap.SimpleEntry<>(this.pvp, config.pvp));
        changes.put(new TranslatableText("localServer.options.remote"), new AbstractMap.SimpleEntry<>(this.remote, config.remote));

        return changes;
    }

    public boolean areCheatsEnabled() {
        return this.allowCheats;
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public void setGameMode(final GameMode gameMode) {
        this.gameMode = gameMode == null ? GameMode.NOT_SET : gameMode;
    }

    public String getMOTD() {
        return motd;
    }

    public int getPort(final boolean create) {
        if (this.isUsingLocalPort() && create) {
            return this.port = NetworkUtils.findLocalPort();
        } else if (this.port < 0 || this.port > BetterLocalServer.MAX_PORT) {
            this.port = 25565;
        }

        return port;
    }

    public void setPort(final int port) {
        if (!this.remote) {
            this.port = port;
        }
    }

    public int getViewDistance() {
        return viewDistance;
    }

    public boolean isFlightEnabled() {
        return this.allowFlight;
    }

    public boolean isOfflineMode() {
        return offlineMode;
    }

    public boolean isPVPEnabled() {
        return pvp;
    }

    public boolean isRemote() {
        return remote;
    }

    public void setRemote(final boolean remote) {
        this.remote = remote;
    }

    public boolean isUsingLocalPort() {
        return useLocalPort;
    }

    public void setUsingLocalPort(final boolean useLocalPort) {
        this.useLocalPort = useLocalPort;
    }
}

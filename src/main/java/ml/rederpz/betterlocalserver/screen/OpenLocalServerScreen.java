package ml.rederpz.betterlocalserver.screen;

import ml.rederpz.betterlocalserver.BetterLocalServer;
import ml.rederpz.betterlocalserver.config.LocalServerConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.world.GameMode;

import java.util.Map;
import java.util.Objects;

/**
 * @author Rederpz on Jan 22, 2021
 */
@Environment(EnvType.CLIENT)
public final class OpenLocalServerScreen extends Screen {

    public static final String AUTO_TEXT = "auto";
    private final Text portText = new TranslatableText("localServer.options.port");
    private final Screen parent;
    private CheckboxWidget cheatsCheckbox, flightCheckbox, localPortCheckbox, pvpCheckbox, offlineCheckbox;
    private TextFieldWidget motdTextField, portTextField;
    private GameMode gameMode = GameMode.NOT_SET;
    private ButtonWidget gameModeButton;
    private LocalServerConfig config;
    private boolean localPortState;
    private int viewDistance;

    public OpenLocalServerScreen(final Screen parent) {
        super(new TranslatableText("localServer.title"));

        this.parent = parent;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void init() {
        final boolean integratedServerRunning = this.client.isIntegratedServerRunning() && this.client.getServer() != null;

        if (!integratedServerRunning) {
            this.client.openScreen(this.parent);
            return;
        }

        this.config = new LocalServerConfig(this.client.getServer());
        this.gameMode = this.config.getGameMode();
        this.viewDistance = this.config.getViewDistance();

        final int x = this.width / 2 - 155;
        final int y = (this.height / 4) - 16;

        this.addButton(this.motdTextField = new TextFieldWidget(this.textRenderer, x, y + 8, 300, 20, new TranslatableText("localServer.options.motd")));
        this.motdTextField.setText(this.config.getMOTD());
        this.motdTextField.setEditable(false);

        this.addButton(this.gameModeButton = new ButtonWidget(x, y + 32, 150, 20, LiteralText.EMPTY, (buttonWidget) -> {
            final int current = this.gameMode.ordinal();
            this.gameMode = GameMode.values()[current + 1 >= GameMode.values().length ? 0 : current + 1];
            this.updateButtonNames();
        }));

        this.addButton(this.cheatsCheckbox = new CheckboxWidget(x + 160, y + 32, 100, 20, new TranslatableText("localServer.options.allowCheats"), this.config.areCheatsEnabled()));

        final int viewDistance = this.config.getViewDistance();
        final double viewDistancePercent = (viewDistance - 2) / 30D;
        this.addButton(new SliderWidget(x, y + 56, 150, 20, new TranslatableText("localServer.options.viewDistance", viewDistance), viewDistancePercent) {
            @Override
            protected void updateMessage() {
                final int viewDistance = (int) (2 + (30 * this.value));
                this.setMessage(new TranslatableText("localServer.options.viewDistance", viewDistance));
            }

            @Override
            protected void applyValue() {
                OpenLocalServerScreen.this.viewDistance = (int) (2 + (30 * this.value));
            }
        });

        this.addButton(this.flightCheckbox = new CheckboxWidget(x + 160, y + 56, 100, 20, new TranslatableText("localServer.options.allowFlight"), this.config.isFlightEnabled()));

        final int portOffset = this.textRenderer.getWidth(this.portText) + 6;
        this.addButton(this.portTextField = new TextFieldWidget(this.textRenderer, x + portOffset, y + 80, 150 - portOffset, 20, new TranslatableText("localServer.options.port")));
        this.portTextField.setText(String.valueOf(this.config.getPort(false)));
        this.portTextField.setTextPredicate(s -> {
            if (s.equals(AUTO_TEXT) && this.config.isUsingLocalPort()) {
                return true;
            } else {
                try {
                    final int port = Integer.parseInt(s);
                    return port > 0 && port < BetterLocalServer.MAX_PORT;
                } catch (final IllegalArgumentException e) {
                    return false;
                }
            }
        });
        this.portTextField.setEditable(!this.config.isRemote());

        this.addButton(this.offlineCheckbox = new CheckboxWidget(x + 160, y + 80, 100, 20, new TranslatableText("localServer.options.offlineMode"), this.config.isOfflineMode()));
        this.offlineCheckbox.active = !this.config.isRemote();

        this.addButton(this.localPortCheckbox = new CheckboxWidget(x, y + 104, 100, 20, new TranslatableText("localServer.options.port.local"), this.config.isUsingLocalPort()));
        this.localPortCheckbox.active = !this.config.isRemote();

        this.addButton(this.pvpCheckbox = new CheckboxWidget(x + 160, y + 104, 100, 20, new TranslatableText("localServer.options.pvp"), this.config.isPVPEnabled()));

        this.addButton(new ButtonWidget(x + (this.config.isRemote() ? 0 : 80), y + 142, 150, 20, new TranslatableText("localServer.options.openDirectory"), button -> Util.getOperatingSystem().open(this.client.getServer().getIconFile().getParentFile().toURI())));

        if (this.config.isRemote()) {
            this.addButton(new ButtonWidget(x + 160, y + 142, 150, 20, new TranslatableText("localServer.options.stop"), button -> this.client.getServer().stop(false)));
        }

        this.addButton(new ButtonWidget(x, y + 166, 150, 20, new TranslatableText(this.config.isRemote() ? "localServer.options.confirm" : "localServer.options.start"), (buttonWidget) -> {
            this.client.openScreen(null);
            final Text responseText;

            if (this.config.isRemote()) {
                responseText = this.getResponse(this.updateServerOptions(this.client.getServer()), false);
            } else {
                final LocalServerConfig config = this.openServer(this.client.getServer());
                responseText = this.getResponse(config, true);
            }

            this.client.inGameHud.getChatHud().addMessage(responseText);
            this.client.updateWindowTitle();
        }));

        this.addButton(new ButtonWidget(x + 160, y + 166, 150, 20, ScreenTexts.CANCEL, (buttonWidget) -> this.client.openScreen(this.parent)));

        this.updateButtonNames();
    }

    private void updateButtonNames() {
        final MutableText text = new TranslatableText("localServer.options.defaultGameMode").append(": ");
        this.gameModeButton.setMessage(text.append(this.getText(this.gameMode)));
    }

    @Override
    public void render(final MatrixStack matrices, final int mouseX, final int mouseY, final float delta) {
        this.renderBackground(matrices);
        DrawableHelper.drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 40, 0xFFFFFFFF);
        DrawableHelper.drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 40, 0xFFFFFFFF);

        if (!this.config.isRemote()) {
            final boolean randomPort = this.localPortCheckbox.isChecked();
            if (this.localPortState != randomPort) {
                if (randomPort) {
                    this.portTextField.setEditable(false);
                    this.portTextField.setText(AUTO_TEXT);
                } else {
                    this.portTextField.setEditable(true);
                    this.portTextField.setText(String.valueOf(this.config.getPort(false)));
                }
                this.localPortState = randomPort;
            }
        }

        final int x = this.width / 2 - 155;
        final int y = (this.height / 4) - 16;

        this.textRenderer.drawWithShadow(matrices, this.portText, x + 1, y + 80 + 6, 0xFFFFFFFF);

        super.render(matrices, mouseX, mouseY, delta);
    }

    private LocalServerConfig openServer(final IntegratedServer server) {
        final LocalServerConfig config = this.updateServerOptions(server);
        config.setUsingLocalPort(this.localPortCheckbox.isChecked());

        if (server.openToLan(config.getGameMode(), config.areCheatsEnabled(), config.getPort(true))) {
            config.setGameMode(config.getGameMode());
            config.setPort(server.getServerPort());
            config.setRemote(true);
            return config;
        } else {
            return null;
        }
    }

    private LocalServerConfig updateServerOptions(final IntegratedServer server) {
        final GameMode gameMode = this.gameMode.equals(GameMode.NOT_SET) ? server.getDefaultGameMode() : this.gameMode;
        server.setOnlineMode(!this.offlineCheckbox.isChecked());
        server.setFlightEnabled(this.flightCheckbox.isChecked());
        server.setPvpEnabled(this.pvpCheckbox.isChecked());
        server.setMotd(this.motdTextField.getText());
        server.getPlayerManager().setViewDistance(this.viewDistance);
        server.getPlayerManager().setGameMode(gameMode);
        server.getPlayerManager().setCheatsAllowed(this.cheatsCheckbox.isChecked());
        return new LocalServerConfig(server);
    }

    private Text getResponse(final LocalServerConfig config, final boolean start) {
        if (config != null) {
            final MutableText text = new TranslatableText(start ? "commands.localServer.start" : "commands.localServer.update");
            final Map<BaseText, Map.Entry<Object, Object>> changes = this.config.getChanges(config);
            MutableText hoverText = new LiteralText("");
            int loggedChanges = 0;

            for (final BaseText option : changes.keySet()) {
                final Map.Entry<Object, Object> value = changes.get(option);

                if (!Objects.equals(value.getKey(), value.getValue())) {
                    hoverText = hoverText.append(option).append(": ").append(this.getText(value.getKey())).append(" -> ").append(this.getText(value.getValue())).append("\n");
                    loggedChanges++;
                }
            }

            if (loggedChanges == 0) {
                hoverText = new TranslatableText("commands.localServer.update.none");
            }else{
                hoverText = hoverText.append(new TranslatableText("commands.localServer.update.info", loggedChanges));
            }

            final Style bracketStyle = Style.EMPTY.withColor(Formatting.GRAY);
            final Style updatesStyle = Style.EMPTY.withColor(Formatting.YELLOW).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText));
            return text.append(new LiteralText(" [").setStyle(bracketStyle))
                    .append(new TranslatableText("commands.localServer.update.view").setStyle(updatesStyle))
                    .append(new LiteralText("]").setStyle(bracketStyle));
        } else {
            return new TranslatableText("commands.localServer.start.error");
        }
    }

    private Text getText(final Object value) {
        if (value instanceof Boolean) {
            final Boolean booleanValue = (Boolean) value;
            return new LiteralText(String.valueOf(booleanValue)).setStyle(Style.EMPTY.withColor(booleanValue ? Formatting.GREEN : Formatting.RED));
        } else if (value instanceof Number) {
            final Number numberValue = (Number) value;
            return new LiteralText(String.valueOf(numberValue)).setStyle(Style.EMPTY.withColor(Formatting.AQUA));
        } else if (value instanceof GameMode) {
            final GameMode gameModeValue = (GameMode) value;

            if (gameModeValue.equals(GameMode.NOT_SET)) {
                return new TranslatableText("localServer.options.gameMode.default").setStyle(Style.EMPTY.withItalic(true));
            } else {
                return new TranslatableText("selectWorld.gameMode." + gameModeValue.getName());
            }
        } else {
            return new LiteralText(String.valueOf(value));
        }
    }
}

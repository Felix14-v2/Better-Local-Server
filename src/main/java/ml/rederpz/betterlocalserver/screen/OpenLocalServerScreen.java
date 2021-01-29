package ml.rederpz.betterlocalserver.screen;

import ml.rederpz.betterlocalserver.mixin.adapter.PlayerManagerAdapter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.NetworkUtils;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.text.*;
import net.minecraft.util.Util;
import net.minecraft.world.GameMode;

/**
 * @author Rederpz on Jan 22, 2021
 */
@Environment(EnvType.CLIENT)
public final class OpenLocalServerScreen extends Screen {

    public static final String AUTO_TEXT = "auto";
    public static final int MAX_PORT = 65535;
    private final Text portText = new TranslatableText("localServer.options.port");
    private final Screen parent;
    private boolean open;
    private CheckboxWidget cheatsCheckbox, flightCheckbox, localPortCheckbox, pvpCheckbox, offlineCheckbox;
    private TextFieldWidget motdTextField, portTextField;
    private GameMode gameMode = GameMode.NOT_SET;
    private ButtonWidget gameModeButton;
    private boolean localPortState;

    public OpenLocalServerScreen(final Screen parent) {
        super(new TranslatableText("localServer.title"));

        this.parent = parent;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void init() {
        final boolean integratedServerRunning = this.client.isIntegratedServerRunning() && this.client.getServer() != null;
        this.open = integratedServerRunning && this.client.getServer().isRemote();

        if (!integratedServerRunning) {
            this.client.openScreen(this.parent);
            return;
        } else {
            final GameMode gameMode = ((PlayerManagerAdapter) this.client.getServer().getPlayerManager()).getGameMode();

            if (gameMode != null) {
                this.gameMode = gameMode;
            }
        }

        final int x = this.width / 2 - 155;
        final int y = (this.height / 4) - 16;

        this.addButton(this.motdTextField = new TextFieldWidget(this.textRenderer, x, y + 8, 300, 20, new TranslatableText("localServer.options.motd")));
        this.motdTextField.setText(this.client.getServer().getServerMotd());
        this.motdTextField.setEditable(false);

        this.addButton(this.gameModeButton = new ButtonWidget(x, y + 32, 150, 20, LiteralText.EMPTY, (buttonWidget) -> {
            final int current = this.gameMode.ordinal();
            this.gameMode = GameMode.values()[current + 1 >= GameMode.values().length ? 0 : current + 1];
            this.updateButtonNames();
        }));

        this.addButton(this.cheatsCheckbox = new CheckboxWidget(x + 160, y + 32, 100, 20, new TranslatableText("localServer.options.allowCheats"), this.client.getServer().getPlayerManager().areCheatsAllowed()));

        final int viewDistance = this.client.getServer().getPlayerManager().getViewDistance();
        final double viewDistancePercent = (viewDistance - 2) / 30D;
        this.addButton(new SliderWidget(x, y + 56, 150, 20, new TranslatableText("localServer.options.viewDistance", viewDistance), viewDistancePercent) {
            @Override
            protected void updateMessage() {
                final int viewDistance = (int) (2 + (30 * this.value));
                this.setMessage(new TranslatableText("localServer.options.viewDistance", viewDistance));
            }

            @Override
            protected void applyValue() {
                final int viewDistance = (int) (2 + (30 * this.value));
                OpenLocalServerScreen.this.client.getServer().getPlayerManager().setViewDistance(viewDistance);
            }
        });

        this.addButton(this.flightCheckbox = new CheckboxWidget(x + 160, y + 56, 100, 20, new TranslatableText("localServer.options.allowFlight"), this.client.getServer().isFlightEnabled()));

        final int portOffset = this.textRenderer.getWidth(this.portText) + 6;
        this.addButton(this.portTextField = new TextFieldWidget(this.textRenderer, x + portOffset, y + 80, 150 - portOffset, 20, new TranslatableText("localServer.options.port")));
        this.portTextField.setText(String.valueOf(this.open ? this.client.getServer().getServerPort() : 25565));
        this.portTextField.setTextPredicate(s -> {
            if (s.equals(AUTO_TEXT) && (this.localPortCheckbox != null && this.localPortCheckbox.isChecked())) {
                return true;
            } else {
                try {
                    final int port = Integer.parseInt(s);
                    return this.isValidPort(port);
                } catch (final IllegalArgumentException e) {
                    return false;
                }
            }
        });
        this.portTextField.setEditable(!this.open);

        this.addButton(this.offlineCheckbox = new CheckboxWidget(x + 160, y + 80, 100, 20, new TranslatableText("localServer.options.offlineMode"), !this.client.getServer().isOnlineMode()));
        this.offlineCheckbox.active = !this.open;

        this.addButton(this.localPortCheckbox = new CheckboxWidget(x, y + 104, 100, 20, new TranslatableText("localServer.options.port.local"), false));
        this.localPortCheckbox.active = !this.open;

        this.addButton(this.pvpCheckbox = new CheckboxWidget(x + 160, y + 104, 100, 20, new TranslatableText("localServer.options.pvp"), this.client.getServer().isPvpEnabled()));

        this.addButton(new ButtonWidget(x + (this.open ? 0 : 80), y + 142, 150, 20, new TranslatableText("localServer.options.openDirectory"), button -> Util.getOperatingSystem().open(this.client.getServer().getIconFile().getParentFile().toURI())));

        if (this.open) {
            this.addButton(new ButtonWidget(x + 160, y + 142, 150, 20, new TranslatableText("localServer.options.stop"), button -> this.client.getServer().stop(false)));
        }

        this.addButton(new ButtonWidget(x, y + 166, 150, 20, new TranslatableText(this.open ? "localServer.options.confirm" : "localServer.options.start"), (buttonWidget) -> {
            this.client.openScreen(null);
            final Text responseText;

            if (this.open) {
                this.client.openScreen(null);
                this.updateServerOptions(this.client.getServer());
                responseText = new TranslatableText("commands.localServer.update");
            } else {
                final int port = this.getPort();
                if (this.openServer(this.client.getServer(), port)) {
                    responseText = new TranslatableText("commands.localServer.start");
                } else {
                    responseText = new TranslatableText("commands.localServer.start.error");
                }
            }

            this.client.inGameHud.getChatHud().addMessage(responseText);
            this.client.updateWindowTitle();
        }));

        this.addButton(new ButtonWidget(x + 160, y + 166, 150, 20, ScreenTexts.CANCEL, (buttonWidget) -> this.client.openScreen(this.parent)));

        this.updateButtonNames();
    }

    private void updateButtonNames() {
        final MutableText text = new TranslatableText("localServer.options.defaultGameMode").append(": ");

        if (this.gameMode.equals(GameMode.NOT_SET)) {
            this.gameModeButton.setMessage(text.append(new TranslatableText("localServer.options.gameMode.default").setStyle(Style.EMPTY.withItalic(true))));
        } else {
            this.gameModeButton.setMessage(text.append(new TranslatableText("selectWorld.gameMode." + this.gameMode.getName())));
        }
    }

    @Override
    public void render(final MatrixStack matrices, final int mouseX, final int mouseY, final float delta) {
        this.renderBackground(matrices);
        DrawableHelper.drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 40, 0xFFFFFFFF);
        DrawableHelper.drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 40, 0xFFFFFFFF);

        if (!this.open) {
            final boolean randomPort = this.localPortCheckbox.isChecked();
            if (this.localPortState != randomPort) {
                if (randomPort) {
                    this.portTextField.setEditable(false);
                    this.portTextField.setText(AUTO_TEXT);
                } else {
                    this.portTextField.setEditable(true);
                    this.portTextField.setText(String.valueOf(25565));
                }
                this.localPortState = randomPort;
            }
        }

        final int x = this.width / 2 - 155;
        final int y = (this.height / 4) - 16;

        this.textRenderer.drawWithShadow(matrices, this.portText, x + 1, y + 80 + 6, 0xFFFFFFFF);

        super.render(matrices, mouseX, mouseY, delta);
    }

    private boolean openServer(final IntegratedServer server, final int port) {
        return server.openToLan(this.updateServerOptions(server), this.cheatsCheckbox.isChecked(), port);
    }

    private GameMode updateServerOptions(final IntegratedServer server) {
        final GameMode gameMode = this.gameMode.equals(GameMode.NOT_SET) ? server.getDefaultGameMode() : this.gameMode;
        server.setOnlineMode(!this.offlineCheckbox.isChecked());
        server.setFlightEnabled(this.flightCheckbox.isChecked());
        server.setPvpEnabled(this.pvpCheckbox.isChecked());
        server.setMotd(this.motdTextField.getText());
        server.getPlayerManager().setGameMode(gameMode);
        server.getPlayerManager().setCheatsAllowed(this.cheatsCheckbox.isChecked());
        return gameMode;
    }

    private int getPort() {
        if (this.localPortCheckbox.isChecked()) {
            return NetworkUtils.findLocalPort();
        } else {
            final int parsed = Integer.parseInt(this.portTextField.getText());
            return !this.isValidPort(parsed) ? NetworkUtils.findLocalPort() : parsed;
        }
    }

    private boolean isValidPort(final int port) {
        return port >= 0 && port <= MAX_PORT;
    }
}

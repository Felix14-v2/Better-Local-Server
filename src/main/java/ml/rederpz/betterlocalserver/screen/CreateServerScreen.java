package ml.rederpz.betterlocalserver.screen;

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
public final class CreateServerScreen extends Screen {

    public static final String AUTO_TEXT = "auto";
    public static final int MAX_PORT = 65535;
    private final Text portText = new TranslatableText("internalServer.port");
    private final Screen parent;
    private CheckboxWidget cheatsCheckbox, flightCheckbox, localPortCheckbox, pvpCheckbox, offlineCheckbox;
    private TextFieldWidget motdTextField, portTextField;
    private GameMode gameMode = GameMode.NOT_SET;
    private ButtonWidget gameModeButton;
    private boolean localPortState;

    public CreateServerScreen(final Screen parent) {
        super(new TranslatableText("internalServer.title"));

        this.parent = parent;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void init() {
        final int x = this.width / 2 - 155;
        final int y = (this.height / 4) - 16;

        this.addButton(this.motdTextField = new TextFieldWidget(this.textRenderer, x, y + 8, 300, 20, new TranslatableText("internalServer.motd")));
        this.motdTextField.setText(this.client.getServer().getServerMotd());
        this.motdTextField.setEditable(false);

        this.addButton(this.gameModeButton = new ButtonWidget(x, y + 32, 150, 20, LiteralText.EMPTY, (buttonWidget) -> {
            final int current = this.gameMode.ordinal();
            this.gameMode = GameMode.values()[current + 1 >= GameMode.values().length ? 0 : current + 1];
            this.updateButtonNames();
        }));

        this.addButton(this.cheatsCheckbox = new CheckboxWidget(x + 160, y + 32, 100, 20, new TranslatableText("internalServer.allowCheats"), this.client.getServer().getPlayerManager().areCheatsAllowed()));
        this.addButton(this.flightCheckbox = new CheckboxWidget(x + 160, y + 56, 100, 20, new TranslatableText("internalServer.allowFlight"), this.client.getServer().isFlightEnabled()));
        this.addButton(this.offlineCheckbox = new CheckboxWidget(x + 160, y + 80, 100, 20, new TranslatableText("internalServer.offlineMode"), !this.client.getServer().isOnlineMode()));
        this.addButton(this.pvpCheckbox = new CheckboxWidget(x + 160, y + 104, 100, 20, new TranslatableText("internalServer.pvp"), this.client.getServer().isPvpEnabled()));

        final int viewDistance = this.client.getServer().getPlayerManager().getViewDistance();
        final double viewDistancePercent = (viewDistance - 2) / 30D;
        this.addButton(new SliderWidget(x, y + 56, 150, 20, new TranslatableText("internalServer.viewDistance", viewDistance), viewDistancePercent) {
            @Override
            protected void updateMessage() {
                final int viewDistance = (int) (2 + (30 * this.value));
                this.setMessage(new TranslatableText("internalServer.viewDistance", viewDistance));
            }

            @Override
            protected void applyValue() {
                final int viewDistance = (int) (2 + (30 * this.value));
                CreateServerScreen.this.client.getServer().getPlayerManager().setViewDistance(viewDistance);
            }
        });

        final int portOffset = this.textRenderer.getWidth(this.portText) + 6;
        this.addButton(this.portTextField = new TextFieldWidget(this.textRenderer, x + portOffset, y + 80, 150 - portOffset, 20, new TranslatableText("internalServer.port")));
        this.portTextField.setText(String.valueOf(25565));
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

        this.addButton(this.localPortCheckbox = new CheckboxWidget(x, y + 104, 100, 20, new TranslatableText("internalServer.port.local"), false));
        this.addButton(new ButtonWidget(x + 80, y + 142, 150, 20, new TranslatableText("internalServer.openDirectory"), button -> Util.getOperatingSystem().open(this.client.getServer().getIconFile().getParentFile().toURI())));
        this.addButton(new ButtonWidget(x + 160, y + 166, 150, 20, ScreenTexts.CANCEL, (buttonWidget) -> this.client.openScreen(this.parent)));
        this.addButton(new ButtonWidget(x, y + 166, 150, 20, new TranslatableText("internalServer.start"), (buttonWidget) -> {
            this.client.openScreen(null);

            final TranslatableText responseText;
            final int port = this.getPort();
            if (this.openServer(this.client.getServer(), port)) {
                responseText = new TranslatableText("commands.publish.started", port);
            } else {
                responseText = new TranslatableText("commands.publish.failed");
            }

            this.client.inGameHud.getChatHud().addMessage(responseText);
            this.client.updateWindowTitle();
        }));

        this.updateButtonNames();
    }

    private void updateButtonNames() {
        final MutableText text = new TranslatableText("internalServer.defaultGameMode").append(": ");

        if (this.gameMode.equals(GameMode.NOT_SET)) {
            this.gameModeButton.setMessage(text.append(new TranslatableText("internalServer.gameMode.default").setStyle(Style.EMPTY.withItalic(true))));
        } else {
            this.gameModeButton.setMessage(text.append(new TranslatableText("selectWorld.gameMode." + this.gameMode.getName())));
        }
    }

    @Override
    public void render(final MatrixStack matrices, final int mouseX, final int mouseY, final float delta) {
        this.renderBackground(matrices);
        DrawableHelper.drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 40, 0xFFFFFFFF);
        DrawableHelper.drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 40, 0xFFFFFFFF);

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

        final int x = this.width / 2 - 155;
        final int y = (this.height / 4) - 16;

        this.textRenderer.drawWithShadow(matrices, this.portText, x + 1, y + 80 + 6, 0xFFFFFFFF);

        super.render(matrices, mouseX, mouseY, delta);
    }

    private boolean openServer(final IntegratedServer server, final int port) {
        final GameMode gameMode = this.gameMode.equals(GameMode.NOT_SET) ? server.getDefaultGameMode() : this.gameMode;
        server.setOnlineMode(!this.offlineCheckbox.isChecked());
        server.setFlightEnabled(!this.flightCheckbox.isChecked());
        server.setPvpEnabled(this.pvpCheckbox.isChecked());
        server.setMotd(this.motdTextField.getText());
        return server.openToLan(gameMode, this.cheatsCheckbox.isChecked(), port);
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

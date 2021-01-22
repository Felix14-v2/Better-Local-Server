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
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Util;
import net.minecraft.world.GameMode;

/**
 * @author Rederpz on Jan 22, 2021
 */
@Environment(EnvType.CLIENT)
public final class CreateServerScreen extends Screen {

    private final Screen parent;
    private CheckboxWidget cheatsCheckbox, flightCheckbox, pvpCheckbox, offlineCheckbox;
    private GameMode gameMode = GameMode.NOT_SET;
    private TextFieldWidget motdTextField;
    private ButtonWidget gameModeButton;
    private int port = 25565;

    public CreateServerScreen(final Screen parent) {
        super(new TranslatableText("internalServer.title"));

        this.parent = parent;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void init() {
        final int height = (this.height / 4) - 16;
        this.addButton(this.motdTextField = new TextFieldWidget(this.textRenderer, this.width / 2 - 155, height + 8, 300, 20, new TranslatableText("internalServer.motd")));
        this.motdTextField.setText(this.client.getServer().getServerMotd());
        this.motdTextField.setEditable(false);

        this.addButton(this.gameModeButton = new ButtonWidget(this.width / 2 - 155, height + 32, 150, 20, LiteralText.EMPTY, (buttonWidget) -> {
            final int current = this.gameMode.ordinal();
            this.gameMode = GameMode.values()[current + 1 >= GameMode.values().length ? 0 : current + 1];
            this.updateButtonNames();
        }));

        this.addButton(this.cheatsCheckbox = new CheckboxWidget(this.width / 2 + 5, height + 32, 100, 20, new TranslatableText("internalServer.allowCheats"), this.client.getServer().getPlayerManager().areCheatsAllowed()));
        this.addButton(this.flightCheckbox = new CheckboxWidget(this.width / 2 + 5, height + 56, 100, 20, new TranslatableText("internalServer.allowFlight"), this.client.getServer().isFlightEnabled()));
        this.addButton(this.offlineCheckbox = new CheckboxWidget(this.width / 2 + 5, height + 80, 100, 20, new TranslatableText("internalServer.offlineMode"), !this.client.getServer().isOnlineMode()));
        this.addButton(this.pvpCheckbox = new CheckboxWidget(this.width / 2 + 5, height + 104, 100, 20, new TranslatableText("internalServer.pvp"), this.client.getServer().isPvpEnabled()));

        final int viewDistance = this.client.getServer().getPlayerManager().getViewDistance();
        final double viewDistancePercent = (viewDistance - 2) / 30D;
        this.addButton(new SliderWidget(this.width / 2 - 155, height + 56, 150, 20, new TranslatableText("internalServer.viewDistance", viewDistance), viewDistancePercent) {
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

        this.addButton(new ButtonWidget(this.width / 2 - 155, height + 80, 150, 20, new TranslatableText("internalServer.openDirectory"), button -> Util.getOperatingSystem().open(this.client.getServer().getIconFile().getParentFile().toURI())));
        this.addButton(new ButtonWidget(this.width / 2 + 5, height + 128, 150, 20, ScreenTexts.CANCEL, (buttonWidget) -> this.client.openScreen(this.parent)));
        this.addButton(new ButtonWidget(this.width / 2 - 155, height + 128, 150, 20, new TranslatableText("internalServer.start"), (buttonWidget) -> {
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
        super.render(matrices, mouseX, mouseY, delta);
    }

    private boolean openServer(final IntegratedServer server, final int port) {
        final GameMode gameMode = this.gameMode.equals(GameMode.NOT_SET) ? server.getDefaultGameMode() : this.gameMode;
        server.setOnlineMode(this.offlineCheckbox.isChecked());
        server.setFlightEnabled(!this.flightCheckbox.isChecked());
        server.setPvpEnabled(this.pvpCheckbox.isChecked());
        server.setMotd(this.motdTextField.getText());
        return server.openToLan(gameMode, this.cheatsCheckbox.isChecked(), port);
    }

    private int getPort() {
        return this.isValidPort() ? this.port : NetworkUtils.findLocalPort();
    }

    private boolean isValidPort() {
        return this.port >= 0 && this.port <= 65353;
    }
}

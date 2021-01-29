package ml.rederpz.betterlocalserver.mixin.impl.screen;

import ml.rederpz.betterlocalserver.screen.OpenLocalServerScreen;
import net.minecraft.client.gui.screen.OpenToLanScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Rederpz on Jan 22, 2021
 */
@Mixin(OpenToLanScreen.class)
public abstract class OpenToLanScreenMixin extends Screen {

    @Shadow
    @Final
    private Screen parent;

    protected OpenToLanScreenMixin(final Text title) {
        super(title);
    }

    @SuppressWarnings("ConstantConditions")
    @Inject(method = "init()V", at = @At("HEAD"))
    private void onScreenCreated(final CallbackInfo callbackInfo) {
        this.client.openScreen(new OpenLocalServerScreen(this.parent));
    }
}

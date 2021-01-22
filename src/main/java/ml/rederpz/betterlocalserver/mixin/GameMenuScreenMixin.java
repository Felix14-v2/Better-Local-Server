package ml.rederpz.betterlocalserver.mixin;

import net.minecraft.client.gui.screen.GameMenuScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * @author Rederpz on Jan 22, 2021
 */
@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin {

    @ModifyConstant(method = "initWidgets", constant = @Constant(stringValue = "menu.shareToLan"))
    private String createLANButton(final String value) {
        return "menu.openInternalServer";
    }
}

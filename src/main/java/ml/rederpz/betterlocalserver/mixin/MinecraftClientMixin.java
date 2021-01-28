package ml.rederpz.betterlocalserver.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * @author Rederpz on Jan 28, 2021
 */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @ModifyConstant(method = "getWindowTitle", constant = @Constant(stringValue = "title.multiplayer.lan"))
    private String createLANButton(final String value) {
        return "title.multiplayer.internal";
    }
}

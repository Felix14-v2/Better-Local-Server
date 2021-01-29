package ml.rederpz.betterlocalserver.mixin.impl.server;

import ml.rederpz.betterlocalserver.mixin.adapter.PlayerManagerAdapter;
import net.minecraft.server.PlayerManager;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * @author Rederpz on Jan 28, 2021
 */
@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin implements PlayerManagerAdapter {

    @Shadow
    private GameMode gameMode;

    @Override
    public GameMode getGameMode() {
        return gameMode;
    }
}

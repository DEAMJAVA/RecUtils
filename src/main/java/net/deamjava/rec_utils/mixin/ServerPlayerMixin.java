package net.deamjava.rec_utils.mixin;

import net.deamjava.rec_utils.TabListSync;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    @Inject(method = "setGameMode", at = @At("RETURN"))
    private void recutils$onSetGameMode(GameType mode, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            TabListSync.broadcastGameModeCorrection((ServerPlayer) (Object) this);
        }
    }
}
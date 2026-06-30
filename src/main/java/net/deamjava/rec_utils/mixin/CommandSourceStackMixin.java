package net.deamjava.rec_utils.mixin;

import net.deamjava.rec_utils.RecFeature;
import net.deamjava.rec_utils.RecModeManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CommandSourceStack.class)
public abstract class CommandSourceStackMixin {

    @Redirect(
            method = "broadcastToAdmins",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;sendSystemMessage(Lnet/minecraft/network/chat/Component;)V"
            )
    )
    private void recutils$filterAdminBroadcast(ServerPlayer player, Component message) {
        boolean suppressed = RecModeManager.isRecMode(player.getUUID())
                && RecModeManager.isFeatureEnabled(player.getUUID(), RecFeature.NO_ADMIN_COMMAND);
        if (!suppressed) {
            player.sendSystemMessage(message);
        }
    }
}
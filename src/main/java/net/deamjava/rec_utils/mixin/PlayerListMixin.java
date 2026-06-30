package net.deamjava.rec_utils.mixin;

import net.deamjava.rec_utils.TabListSync;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {

    @Inject(
            method = "broadcastAll(Lnet/minecraft/network/protocol/Packet;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void recutils$personalizeGameModeBroadcast(Packet<?> packet, CallbackInfo ci) {
        if (!(packet instanceof ClientboundPlayerInfoUpdatePacket infoPacket)) return;
        if (!infoPacket.actions().contains(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE)) return;

        PlayerList self = (PlayerList) (Object) this;
        for (ServerPlayer viewer : self.getPlayers()) {
            viewer.connection.send(TabListSync.personalize(infoPacket, viewer));
        }
        ci.cancel();
    }

    @Redirect(
            method = "placeNewPlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;send(Lnet/minecraft/network/protocol/Packet;)V",
                    ordinal = 0
            )
    )
    private void recutils$personalizeInitialRoster(
            net.minecraft.server.network.ServerGamePacketListenerImpl conn,
            Packet<?> packet,
            net.minecraft.network.Connection connection,
            ServerPlayer player,
            net.minecraft.server.network.CommonListenerCookie cookie
    ) {
        if (packet instanceof ClientboundPlayerInfoUpdatePacket infoPacket) {
            conn.send(TabListSync.personalize(infoPacket, player));
        } else {
            conn.send(packet);
        }
    }
}
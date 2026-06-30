package net.deamjava.rec_utils

import net.deamjava.rec_utils.mixin.ClientboundPlayerInfoUpdatePacketAccessor
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.GameType

object TabListSync {

    private val DISGUISED_GAME_MODE = GameType.SURVIVAL

    /**
     * Returns a packet safe to send to [viewer]: if [viewer] has rec mode + no_spectators
     * enabled, any entry belonging to a spectator (other than the viewer themself) has its
     * gameMode rewritten to look like [DISGUISED_GAME_MODE]. Otherwise returns [original] untouched.
     */
    @JvmStatic
    fun personalize(
        original: ClientboundPlayerInfoUpdatePacket,
        viewer: ServerPlayer
    ): ClientboundPlayerInfoUpdatePacket {
        val hideEnabled = RecModeManager.isRecMode(viewer.uuid) &&
                RecModeManager.isFeatureEnabled(viewer.uuid, RecFeature.NO_SPECTATORS)
        if (!hideEnabled) return original

        var changed = false
        val newEntries = original.entries().map { entry ->
            if (entry.gameMode() == GameType.SPECTATOR && entry.profileId() != viewer.uuid) {
                changed = true
                ClientboundPlayerInfoUpdatePacket.Entry(
                    entry.profileId(),
                    entry.profile(),
                    entry.listed(),
                    entry.latency(),
                    DISGUISED_GAME_MODE,
                    entry.displayName(),
                    entry.showHat(),
                    entry.listOrder(),
                    entry.chatSession()
                )
            } else entry
        }
        if (!changed) return original

        val clone = ClientboundPlayerInfoUpdatePacket(original.actions(), emptyList())
        (clone as ClientboundPlayerInfoUpdatePacketAccessor).`recutils$setEntries`(newEntries)
        return clone
    }

    /** Forces an immediate resend of [target]'s real or disguised gamemode to every online viewer. */
    @JvmStatic
    fun broadcastGameModeCorrection(target: ServerPlayer) {
        val server = target.level().server
        val real = ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE, target)
        for (viewer in server.playerList.players) {
            if (viewer === target) continue
            viewer.connection.send(personalize(real, viewer))
        }
    }

    /** Called when a viewer toggles rec mode / no_spectators, to immediately resync their own view. */
    @JvmStatic
    fun resyncSpectatorVisibility(viewer: ServerPlayer) {
        val server = viewer.level().server
        for (other in server.playerList.players) {
            if (other === viewer) continue
            if (other.gameMode() == GameType.SPECTATOR) {
                val real = ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE, other)
                viewer.connection.send(personalize(real, viewer))
            }
        }
    }
}
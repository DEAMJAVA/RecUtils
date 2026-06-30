package net.deamjava.rec_utils

import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer

object RecModeCommand {

    fun register() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(
                Commands.literal("recmode")
                    .requires { it.entity is ServerPlayer }
                    .executes { ctx -> toggleRecMode(ctx.source) }
                    .then(
                        Commands.argument("feature", StringArgumentType.word())
                            .suggests { _, builder ->
                                RecFeature.entries.forEach { builder.suggest(it.id) }
                                builder.buildFuture()
                            }
                            .executes { ctx -> showFeature(ctx.source, StringArgumentType.getString(ctx, "feature")) }
                            .then(
                                Commands.argument("value", BoolArgumentType.bool())
                                    .executes { ctx ->
                                        setFeature(
                                            ctx.source,
                                            StringArgumentType.getString(ctx, "feature"),
                                            BoolArgumentType.getBool(ctx, "value")
                                        )
                                    }
                            )
                    )
            )
        }
    }

    private fun toggleRecMode(source: CommandSourceStack): Int {
        val player = source.player ?: return 0
        val nowEnabled = RecModeManager.toggleRecMode(player.uuid)
        source.sendSuccess({ Component.literal("Rec mode is now ${if (nowEnabled) "ENABLED" else "DISABLED"}") }, false)
        TabListSync.resyncSpectatorVisibility(player)
        return 1
    }

    private fun showFeature(source: CommandSourceStack, featureId: String): Int {
        val player = source.player ?: return 0
        val feature = RecFeature.fromId(featureId)
        if (feature == null) {
            source.sendFailure(Component.literal("Unknown feature: $featureId"))
            return 0
        }
        val value = RecModeManager.isFeatureEnabled(player.uuid, feature)
        source.sendSuccess({ Component.literal("${feature.id} = $value") }, false)
        return 1
    }

    private fun setFeature(source: CommandSourceStack, featureId: String, value: Boolean): Int {
        val player = source.player ?: return 0
        val feature = RecFeature.fromId(featureId)
        if (feature == null) {
            source.sendFailure(Component.literal("Unknown feature: $featureId"))
            return 0
        }
        RecModeManager.setFeature(player.uuid, feature, value)
        source.sendSuccess({ Component.literal("${feature.id} set to $value") }, false)
        if (feature == RecFeature.NO_SPECTATORS) {
            TabListSync.resyncSpectatorVisibility(player)
        }
        return 1
    }
}
package net.deamjava.rec_utils

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory

object RecUtils : ModInitializer {
	const val MOD_ID: String = "rec-utils"

	private val LOGGER = LoggerFactory.getLogger(MOD_ID)

	override fun onInitialize() {
		LOGGER.info("Initializing Rec Utils")

		RecModeManager.init()
		RecModeCommand.register()

		ServerLifecycleEvents.SERVER_STOPPING.register {
			RecModeManager.save()
		}
	}

	fun id(path: String): Identifier
			= Identifier.fromNamespaceAndPath(MOD_ID, path)
}
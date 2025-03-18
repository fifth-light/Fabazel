package top.fifthlight.fabazel

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.minecraft.MinecraftVersion
import net.minecraft.item.Items
import net.minecraft.text.Text
import org.lwjgl.glfw.GLFW
import org.slf4j.LoggerFactory

object Fabazel : ClientModInitializer {
    private val logger = LoggerFactory.getLogger(Fabazel::class.java)

    override fun onInitializeClient() {
        logger.info("Hello, Bazel!")
        logger.info("Item dirt's name is: {}", Items.DIRT.name)
        logger.info("Detected version: {}", MinecraftVersion.create().name)
        PlayerBlockBreakEvents.AFTER.register { world, player, pos, blockState, blockEntity ->
            player.sendMessage(Text.literal("Wow, you have broken one block!"), false)
        }
        ClientLifecycleEvents.CLIENT_STARTED.register {
            logger.info("Client loaded! GLFW Platform: ${GLFW.glfwGetPlatform()}")
        }
    }
}

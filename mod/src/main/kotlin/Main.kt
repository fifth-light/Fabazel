package top.fifthlight.fabazel

import net.fabricmc.api.ClientModInitializer
// import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.minecraft.MinecraftVersion
import net.minecraft.item.Items
import net.minecraft.util.Util
import org.lwjgl.glfw.GLFW
import org.slf4j.LoggerFactory

object Fabazel : ClientModInitializer {
    private val logger = LoggerFactory.getLogger(Fabazel::class.java)

    override fun onInitializeClient() {
        logger.info("Hello, Bazel!")
        logger.info("Item dirt's name is: {}", Items.DIRT.name)
        logger.info("Detected version: {}", MinecraftVersion.create().name)
        if (Util.getFormattedCurrentTime() == "") {
            // Don't make a real call, just test whether this can be compiled
            GLFW.glfwGetTime()
        }
        // Can't compile before remapping dependencies
        //ClientLifecycleEvents.CLIENT_STARTED.register {
        //    logger.info("Client loaded!")
        //}
    }
}

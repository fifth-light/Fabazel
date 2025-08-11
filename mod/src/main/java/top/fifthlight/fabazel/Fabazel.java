package top.fifthlight.fabazel;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.item.Items;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Fabazel implements ClientModInitializer {
    private final Logger logger = LoggerFactory.getLogger(Fabazel.class);

    @Override
    public void onInitializeClient() {
        logger.info("Hello, Bazel!");
        // Test calling Minecraft classes
        logger.info("Item dirt's name is: {}", Items.DIRT.getName());
        // Test calling Fabric API
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            // Test calling Minecraft libraries
            logger.info("Client loaded! GLFW Platform: {}", GLFW.glfwGetPlatform());
        });
    }
}
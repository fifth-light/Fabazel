package top.fifthlight.fabazel

import net.fabricmc.api.ClientModInitializer
import net.minecraft.MinecraftVersion
import net.minecraft.item.Items

class Fabazel : ClientModInitializer {
    override fun onInitializeClient() {
        println("Hello, Bazel!")
        println("Item dirt's name is: ${Items.DIRT.name}")
        println("Detected version: ${MinecraftVersion.create().name}")
    }
}

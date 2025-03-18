package top.fifthlight.fabazel.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class TestMixin {
    @Shadow
    @Final
    private Window window;

    @Inject(at = @At("TAIL"), method = "onInitFinished(Lnet/minecraft/client/MinecraftClient$LoadingContext;)Ljava/lang/Runnable;")
    protected void afterInitFinished(CallbackInfoReturnable<Runnable> ci) {
        System.out.println("Message from a test mixin");
        System.out.println("Current window: " + window);
    }
}

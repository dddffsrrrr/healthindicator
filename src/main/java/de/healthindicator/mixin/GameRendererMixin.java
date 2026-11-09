package de.healthindicator.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import de.healthindicator.CameraCapture;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Liest FOV und Kamera-Wackeln aus, damit die Anzeige ruhig neben dem Namen sitzt. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Inject(method = "getFov(Lnet/minecraft/client/Camera;FZ)F", at = @At("RETURN"), require = 0)
	private void healthindicator$captureFov(Camera camera, float partialTick, boolean useFovSetting,
											CallbackInfoReturnable<Float> cir) {
		if (useFovSetting) {
			CameraCapture.fov = cir.getReturnValue();
			CameraCapture.fovTimeMillis = System.currentTimeMillis();
		}
	}

	@Inject(method = "bobHurt(Lcom/mojang/blaze3d/vertex/PoseStack;F)V", at = @At("RETURN"), require = 0)
	private void healthindicator$captureHurt(PoseStack poseStack, float partialTick, CallbackInfo ci) {
		CameraCapture.setBob(poseStack.last().pose());
	}

	@Inject(method = "bobView(Lcom/mojang/blaze3d/vertex/PoseStack;F)V", at = @At("RETURN"), require = 0)
	private void healthindicator$captureBob(PoseStack poseStack, float partialTick, CallbackInfo ci) {
		CameraCapture.setBob(poseStack.last().pose());
	}
}

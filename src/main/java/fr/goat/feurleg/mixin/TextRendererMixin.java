package fr.goat.feurleg.mixin;

import fr.goat.feurleg.TextCapture;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.text.OrderedText;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tout le texte dessiné par Minecraft passe par l'une de ces deux méthodes.
 * Pendant qu'un menu est affiché, on note chaque texte et sa position à l'écran.
 */
@Mixin(TextRenderer.class)
public abstract class TextRendererMixin {

    @Inject(
        method = "drawInternal(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;IIZ)I",
        at = @At("HEAD"))
    private void feurleg$captureString(String text, float x, float y, int color, boolean shadow, Matrix4f matrix,
                                       VertexConsumerProvider consumers, TextRenderer.TextLayerType layer,
                                       int background, int light, boolean mirror,
                                       CallbackInfoReturnable<Integer> cir) {
        if (TextCapture.isCapturing() && text != null) {
            TextCapture.record(text, x, y, matrix);
        }
    }

    @Inject(
        method = "drawInternal(Lnet/minecraft/text/OrderedText;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)I",
        at = @At("HEAD"))
    private void feurleg$captureOrdered(OrderedText text, float x, float y, int color, boolean shadow, Matrix4f matrix,
                                        VertexConsumerProvider consumers, TextRenderer.TextLayerType layer,
                                        int background, int light,
                                        CallbackInfoReturnable<Integer> cir) {
        if (TextCapture.isCapturing() && text != null) {
            StringBuilder sb = new StringBuilder();
            text.accept((index, style, codePoint) -> {
                sb.appendCodePoint(codePoint);
                return true;
            });
            TextCapture.record(sb.toString(), x, y, matrix);
        }
    }
}

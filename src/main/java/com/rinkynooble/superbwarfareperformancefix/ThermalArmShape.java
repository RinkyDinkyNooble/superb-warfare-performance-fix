package com.rinkynooble.superbwarfareperformancefix;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

/**
 * The thermal arm patch marks the player model for a first-person setup, which is only right while Superb Warfare's
 * thermal callback uses the mark up: a callback at the return of renderHand that, on the same flag the patch checks,
 * sets the model up again. This compares that callback, instruction by instruction, with Superb Warfare 0.8.9.2, so a
 * version that changes it is left alone instead of patched.
 *
 * <p>Left out of the comparison: Minecraft's own method and field names, including the callback's calls through its
 * mixin's shadows, which differ between the dev and production jars.
 */
public final class ThermalArmShape {
    public static final String PLAYER_RENDERER = "net.minecraft.client.renderer.entity.player.PlayerRenderer";
    /** Superb Warfare's mixin with the thermal callback. */
    public static final String THERMAL_MIXIN = "com.atsuishio.superbwarfare.mixins.PlayerRendererMixin";

    private static final String INJECT = "Lorg/spongepowered/asm/mixin/injection/Inject;";

    // From Superb Warfare 0.8.9.2 (gradlew verifyThermal --args=print). Each line starts with the JVM opcode.
    private static final List<String> EXPECTED = List.of(
            "178 com/atsuishio/superbwarfare/event/ClientEventHandler activeThermalImaging Z",
            "153 -> 58",
            "25 7",
            "182 org/spongepowered/asm/mixin/injection/callback/CallbackInfo cancel()V",
            "25 0",
            "182 com/atsuishio/superbwarfare/mixins/PlayerRendererMixin ()Lnet/minecraft/client/model/EntityModel;",
            "192 net/minecraft/client/model/PlayerModel",
            "58 8",
            "25 0",
            "25 4",
            "182 com/atsuishio/superbwarfare/mixins/PlayerRendererMixin (Lnet/minecraft/client/player/AbstractClientPlayer;)V",
            "25 8",
            "11",
            "181 net/minecraft/client/model/PlayerModel  F",
            "25 8",
            "3",
            "181 net/minecraft/client/model/PlayerModel  Z",
            "25 8",
            "11",
            "181 net/minecraft/client/model/PlayerModel  F",
            "25 8",
            "25 4",
            "11",
            "11",
            "11",
            "11",
            "11",
            "182 net/minecraft/client/model/PlayerModel (Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",
            "25 5",
            "11",
            "181 net/minecraft/client/model/geom/ModelPart  F",
            "25 5",
            "25 1",
            "25 2",
            "25 4",
            "182 net/minecraft/client/player/AbstractClientPlayer ()Lnet/minecraft/resources/ResourceLocation;",
            "184 net/minecraft/client/renderer/RenderType (Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;",
            "185 net/minecraft/client/renderer/MultiBufferSource (Lnet/minecraft/client/renderer/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;",
            "18 15728880",
            "16 15",
            "16 10",
            "184 net/minecraft/client/renderer/texture/OverlayTexture (II)I",
            "182 net/minecraft/client/model/geom/ModelPart (Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V",
            "25 6",
            "11",
            "181 net/minecraft/client/model/geom/ModelPart  F",
            "25 6",
            "25 1",
            "25 2",
            "25 4",
            "182 net/minecraft/client/player/AbstractClientPlayer ()Lnet/minecraft/resources/ResourceLocation;",
            "184 net/minecraft/client/renderer/RenderType (Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;",
            "185 net/minecraft/client/renderer/MultiBufferSource (Lnet/minecraft/client/renderer/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;",
            "18 15728880",
            "16 15",
            "16 10",
            "184 net/minecraft/client/renderer/texture/OverlayTexture (II)I",
            "182 net/minecraft/client/model/geom/ModelPart (Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V",
            "177");

    private ThermalArmShape() {
    }

    public static boolean matches(ClassNode thermalMixin) {
        MethodNode callback = callback(thermalMixin);
        return callback != null && SmokeCheckShape.describe(callback, thermalMixin.name).equals(EXPECTED);
    }

    /** The mixin's only method injected at the return of renderHand; null if there is none or more than one. */
    public static MethodNode callback(ClassNode thermalMixin) {
        List<MethodNode> callbacks = new ArrayList<>();
        for (MethodNode method : thermalMixin.methods) {
            if (injectsAtRenderHandReturn(method.visibleAnnotations) || injectsAtRenderHandReturn(method.invisibleAnnotations)) {
                callbacks.add(method);
            }
        }
        return callbacks.size() == 1 ? callbacks.get(0) : null;
    }

    private static boolean injectsAtRenderHandReturn(List<AnnotationNode> annotations) {
        if (annotations == null) {
            return false;
        }
        for (AnnotationNode annotation : annotations) {
            if (annotation.desc.equals(INJECT)
                    && value(annotation, "method") instanceof List<?> methods && methods.size() == 1
                    && methods.get(0) instanceof String method && method.startsWith("renderHand(")
                    && value(annotation, "at") instanceof List<?> ats && ats.size() == 1
                    && ats.get(0) instanceof AnnotationNode at && "RETURN".equals(value(at, "value"))) {
                return true;
            }
        }
        return false;
    }

    private static Object value(AnnotationNode annotation, String name) {
        if (annotation.values != null) {
            for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
                if (name.equals(annotation.values.get(i))) {
                    return annotation.values.get(i + 1);
                }
            }
        }
        return null;
    }
}

package com.rinkynooble.superbwarfareperformancefix.mixin;

import com.github.mcmodderanchor.simplebedrockmodel.v2.client.compat.sodium.SodiumBakedChunkWriter;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.baked.BakedGeometryChunk;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.rinkynooble.superbwarfareperformancefix.client.PolyMeshWrites;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps poly mesh triangles whole with a shader pack. SimpleBedrockModel's fast Embeddium write of poly mesh vertices
 * declines (returns false) unless the buffer takes them as a plain copy, and SimpleBedrockModel then writes them one
 * vertex at a time, its own fallback, which Oculus completes correctly for triangles (see {@link PolyMeshWrites}).
 * Without a shader pack the fast write runs as before.
 */
@Mixin(value = SodiumBakedChunkWriter.class, remap = false)
public abstract class SodiumBakedChunkWriterMixin {
    @Inject(method = "writeVertices(Lcom/github/mcmodderanchor/simplebedrockmodel/v2/common/model/baked/BakedGeometryChunk;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFFLorg/joml/Matrix4f;Lorg/joml/Matrix3f;)Z",
            at = @At("HEAD"), cancellable = true)
    private void superbwarfareperformancefix$keepTrianglesWhole(BakedGeometryChunk chunk, VertexConsumer consumer, int light, int overlay,
                                                               float red, float green, float blue, float alpha,
                                                               Matrix4f pose, Matrix3f normal, CallbackInfoReturnable<Boolean> cir) {
        if (!PolyMeshWrites.copiedAsIs(consumer)) {
            cir.setReturnValue(false);
        }
    }
}

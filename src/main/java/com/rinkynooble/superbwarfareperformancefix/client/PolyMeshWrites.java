package com.rinkynooble.superbwarfareperformancefix.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import me.jellysquid.mods.sodium.client.render.vertex.buffer.ExtendedBufferBuilder;
import me.jellysquid.mods.sodium.client.render.vertex.buffer.SodiumBufferBuilder;
import net.caffeinemc.mods.sodium.api.vertex.format.common.ModelVertex;

/**
 * Decides whether SimpleBedrockModel's fast write of poly mesh triangles is safe for a buffer.
 *
 * <p>With Embeddium installed, SimpleBedrockModel (bundled in Superb Warfare) writes a model's poly mesh triangles in
 * batches, in Minecraft's own entity vertex layout, and asks the buffer to take them all at once. When the buffer
 * uses that layout too, the batch is copied as it is. With a shader pack, Oculus gives entity buffers a longer layout,
 * and the batch is converted by Oculus's converter, which only handles groups of four vertices (quads) and skips the
 * last one to three vertices of a batch while the buffer still moves past them. Triangles come in threes, so most
 * poly meshes end with a triangle whose corners are left over from earlier frames: the spikes and stretched stripes
 * around Superb Warfare's speed boats (and the A-10, AC-130, BMP-2, FH-77 and Kirov) with a shader pack.
 *
 * <p>Only Embeddium's own buffer types are recognised; anything else gets the slow path, which is always right.
 * This class names Embeddium's types, so it is only loaded from SimpleBedrockModel's Embeddium writer, which itself
 * only loads with Embeddium installed.
 */
public final class PolyMeshWrites {
    private PolyMeshWrites() {
    }

    /** True when the buffer behind {@code consumer} takes SimpleBedrockModel's vertices as a plain copy. */
    public static boolean copiedAsIs(VertexConsumer consumer) {
        if (consumer instanceof SodiumBufferBuilder wrapper) {
            consumer = wrapper.getOriginalBufferBuilder();
        }
        return consumer instanceof ExtendedBufferBuilder buffer
                && buffer.sodium$getFormatDescription() == ModelVertex.FORMAT;
    }
}

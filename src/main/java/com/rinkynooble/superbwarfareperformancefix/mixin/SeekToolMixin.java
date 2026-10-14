package com.rinkynooble.superbwarfareperformancefix.mixin;

import com.atsuishio.superbwarfare.entity.projectile.SmokeDecoyEntity;
import com.atsuishio.superbwarfare.tools.SeekTool;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Superb Warfare's smoke check (SeekTool.NOT_IN_SMOKE) asks the level for every entity within 8 blocks of the
 * entity it checks, then keeps only smoke clouds. The overlay trace runs it on each entity along the player's view
 * every tick, so in a crowd it walks the whole crowd once per crowd member.
 *
 * <p>Asking the level for smoke clouds alone finds exactly the same ones, from each chunk section's own list of
 * them, without walking past anything else. Applied only while the smoke check matches the one this was written
 * for (see SmokeCheckShape).
 */
@Mixin(SeekTool.class)
public abstract class SeekToolMixin {
    // NOT_IN_SMOKE's own lambda (NOT_IN_SMOKE$lambda$9 in 0.8.9.2), whatever number Kotlin gives it.
    @ModifyArg(method = "/^NOT_IN_SMOKE\\$lambda\\$\\d+$/",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/entity/EntityTypeTest;forClass(Ljava/lang/Class;)Lnet/minecraft/world/level/entity/EntityTypeTest;",
                    remap = true),
            remap = false)
    private static Class<?> superbwarfareperformancefix$onlySmokeClouds(Class<?> type) {
        return type == Entity.class ? SmokeDecoyEntity.class : type;
    }
}

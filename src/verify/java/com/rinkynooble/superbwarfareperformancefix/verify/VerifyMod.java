package com.rinkynooble.superbwarfareperformancefix.verify;

import com.atsuishio.superbwarfare.entity.projectile.SmokeDecoyEntity;
import com.atsuishio.superbwarfare.tools.SeekTool;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Dev-only mod (gradlew verifyJar), never shipped. /sbwfixverify runs Superb Warfare's smoke check, patched, on every
 * entity in the level, and compares each answer with the original query: every entity within 8 blocks, kept only if
 * it is a smoke cloud.
 */
@Mod(VerifyMod.MODID)
public class VerifyMod {
    public static final String MODID = "superbwarfareperformancefixverify";
    private static final int ROUNDS = 5;

    public VerifyMod() {
        MinecraftForge.EVENT_BUS.addListener(VerifyMod::onRegisterCommands);
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("sbwfixverify")
                .requires(source -> source.hasPermission(2))
                .executes(VerifyMod::run));
    }

    private static int run(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        List<Entity> entities = new ArrayList<>();
        level.getAllEntities().forEach(entities::add);

        int inSmoke = 0;
        int different = 0;
        long patchedNanos = 0;
        long originalNanos = 0;
        // Later rounds give the JIT time to settle; the times reported are from the last one.
        for (int round = 0; round < ROUNDS; round++) {
            inSmoke = 0;
            different = 0;
            patchedNanos = 0;
            originalNanos = 0;
            for (Entity entity : entities) {
                long start = System.nanoTime();
                boolean patched = SeekTool.NOT_IN_SMOKE.test(entity);
                long middle = System.nanoTime();
                boolean original = original(entity);
                long end = System.nanoTime();
                patchedNanos += middle - start;
                originalNanos += end - middle;
                if (!original) {
                    inSmoke++;
                }
                if (patched != original) {
                    different++;
                }
            }
        }

        String result = String.format(
                "Smoke check on %d entities (%d in smoke), patch %s: %s. Last round: patched %.2f ms, original %.2f ms.",
                entities.size(), inSmoke, patchApplied() ? "applied" : "NOT APPLIED",
                different == 0 ? "all the same" : different + " DIFFERENT", patchedNanos / 1e6, originalNanos / 1e6);
        context.getSource().sendSuccess(() -> Component.literal(result), true);
        return different == 0 ? 1 : 0;
    }

    /** The mixin copies its handler into SeekTool, so it is there only when the patch applied. */
    private static boolean patchApplied() {
        return Arrays.stream(SeekTool.class.getDeclaredMethods())
                .anyMatch(method -> method.getName().contains("superbwarfareperformancefix$onlySmokeClouds"));
    }

    /** Superb Warfare 0.8.9.2's NOT_IN_SMOKE before the patch. */
    private static boolean original(Entity entity) {
        return entity.level().getEntities(EntityTypeTest.forClass(Entity.class), entity.getBoundingBox().inflate(8.0),
                other -> other instanceof SmokeDecoyEntity && !(entity instanceof SmokeDecoyEntity)).isEmpty();
    }
}

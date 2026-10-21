package com.rinkynooble.superbwarfareperformancefix;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The smoke patch is only the same result while Superb Warfare's smoke check keeps only smoke clouds from what the
 * level returns. This compares the check's three methods, instruction by instruction, with Superb Warfare 0.8.9.2,
 * so a version that changes it is left alone instead of patched.
 *
 * <p>Left out of the comparison: Minecraft's own method names, which differ between the dev and production jars, and
 * the numbers Kotlin gives lambdas, which shift when Superb Warfare adds or removes a lambda elsewhere in SeekTool.
 */
public final class SmokeCheckShape {
    public static final String SEEK_TOOL = "com.atsuishio.superbwarfare.tools.SeekTool";

    /** NOT_IN_SMOKE's own method, as named by Kotlin; SeekToolMixin targets the same pattern. */
    private static final Pattern CHECK_NAME = Pattern.compile("NOT_IN_SMOKE\\$lambda\\$\\d+");
    private static final String CHECK_DESC = "(Lnet/minecraft/world/entity/Entity;)Z";
    private static final Pattern LAMBDA_NUMBER = Pattern.compile("\\$lambda\\$\\d+");

    // From Superb Warfare 0.8.9.2 (gradlew verifySmoke --args=print), in the order parts() returns them: the check,
    // the filter it passes to the level, and Kotlin's adapter between the two. Each line starts with the JVM opcode.
    private static final List<List<String>> EXPECTED = List.of(
            List.of(
                    "25 0",
                    "199 -> 4",
                    "3",
                    "172",
                    "25 0",
                    "182 net/minecraft/world/entity/Entity ()Lnet/minecraft/world/phys/AABB;",
                    "18 8.0",
                    "182 net/minecraft/world/phys/AABB (D)Lnet/minecraft/world/phys/AABB;",
                    "58 1",
                    "25 0",
                    "182 net/minecraft/world/entity/Entity ()Lnet/minecraft/world/level/Level;",
                    "18 Lnet/minecraft/world/entity/Entity;",
                    "184 net/minecraft/world/level/entity/EntityTypeTest (Ljava/lang/Class;)Lnet/minecraft/world/level/entity/EntityTypeTest;",
                    "25 1",
                    "25 0",
                    "186 invoke(Lnet/minecraft/world/entity/Entity;)Lkotlin/jvm/functions/Function1; -> com/atsuishio/superbwarfare/tools/SeekTool.NOT_IN_SMOKE$lambda$N$lambda$N(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;)Z",
                    "186 test(Lkotlin/jvm/functions/Function1;)Ljava/util/function/Predicate; -> com/atsuishio/superbwarfare/tools/SeekTool.NOT_IN_SMOKE$lambda$N$lambda$N(Lkotlin/jvm/functions/Function1;Ljava/lang/Object;)Z",
                    "182 net/minecraft/world/level/Level (Lnet/minecraft/world/level/entity/EntityTypeTest;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;",
                    "185 java/util/List isEmpty()Z",
                    "172"),
            List.of(
                    "25 1",
                    "193 com/atsuishio/superbwarfare/entity/projectile/SmokeDecoyEntity",
                    "153 -> 8",
                    "25 0",
                    "193 com/atsuishio/superbwarfare/entity/projectile/SmokeDecoyEntity",
                    "154 -> 8",
                    "4",
                    "167 -> 9",
                    "3",
                    "172"),
            List.of(
                    "25 0",
                    "18 $tmp0",
                    "184 kotlin/jvm/internal/Intrinsics checkNotNullParameter(Ljava/lang/Object;Ljava/lang/String;)V",
                    "25 0",
                    "25 1",
                    "185 kotlin/jvm/functions/Function1 invoke(Ljava/lang/Object;)Ljava/lang/Object;",
                    "192 java/lang/Boolean",
                    "182 java/lang/Boolean booleanValue()Z",
                    "172"));

    private SmokeCheckShape() {
    }

    public static boolean matches(ClassNode seekTool) {
        List<MethodNode> parts = parts(seekTool);
        if (parts == null) {
            return false;
        }
        for (int i = 0; i < parts.size(); i++) {
            if (!describe(parts.get(i)).equals(EXPECTED.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * The check (the only method named like NOT_IN_SMOKE's lambda, taking an entity and returning a boolean),
     * followed by the two SeekTool methods its lambdas call; null if any is missing.
     */
    public static List<MethodNode> parts(ClassNode seekTool) {
        List<MethodNode> checks = new ArrayList<>();
        for (MethodNode method : seekTool.methods) {
            if (CHECK_NAME.matcher(method.name).matches() && method.desc.equals(CHECK_DESC)) {
                checks.add(method);
            }
        }
        if (checks.size() != 1) {
            return null;
        }
        List<MethodNode> parts = new ArrayList<>(checks);
        for (AbstractInsnNode insn : checks.get(0).instructions) {
            if (insn instanceof InvokeDynamicInsnNode indy) {
                MethodNode called = indy.bsmArgs.length > 1 && indy.bsmArgs[1] instanceof Handle handle
                        && handle.getOwner().equals(seekTool.name) ? find(seekTool, handle.getName(), handle.getDesc()) : null;
                if (called == null) {
                    return null;
                }
                parts.add(called);
            }
        }
        return parts.size() == 3 ? parts : null;
    }

    private static MethodNode find(ClassNode owner, String name, String desc) {
        for (MethodNode method : owner.methods) {
            if (method.name.equals(name) && method.desc.equals(desc)) {
                return method;
            }
        }
        return null;
    }

    /** One line per instruction, starting with its opcode; jumps name their target by instruction number. */
    public static List<String> describe(MethodNode method) {
        return describe(method, null);
    }

    /**
     * The same, also leaving out member names for calls on {@code shadowOwner}: a mixin calling Minecraft's methods
     * through its shadows, named like Minecraft's own.
     */
    public static List<String> describe(MethodNode method, String shadowOwner) {
        List<AbstractInsnNode> real = new ArrayList<>();
        for (AbstractInsnNode insn : method.instructions) {
            if (insn.getOpcode() >= 0) {
                real.add(insn);
            }
        }
        List<String> lines = new ArrayList<>();
        for (AbstractInsnNode insn : real) {
            String op = String.valueOf(insn.getOpcode());
            if (insn instanceof VarInsnNode v) {
                lines.add(op + " " + v.var);
            } else if (insn instanceof IntInsnNode n) {
                lines.add(op + " " + n.operand);
            } else if (insn instanceof JumpInsnNode j) {
                lines.add(op + " -> " + target(real, j.label));
            } else if (insn instanceof TypeInsnNode t) {
                lines.add(op + " " + t.desc);
            } else if (insn instanceof LdcInsnNode l) {
                lines.add(op + " " + (l.cst instanceof Type type ? type.getDescriptor() : l.cst));
            } else if (insn instanceof MethodInsnNode m) {
                lines.add(op + " " + m.owner + " " + nameUnlessMinecraft(m.owner, m.name, shadowOwner) + m.desc);
            } else if (insn instanceof FieldInsnNode f) {
                lines.add(op + " " + f.owner + " " + nameUnlessMinecraft(f.owner, f.name, shadowOwner) + " " + f.desc);
            } else if (insn instanceof InvokeDynamicInsnNode d) {
                Object impl = d.bsmArgs.length > 1 ? d.bsmArgs[1] : null;
                lines.add(op + " " + d.name + d.desc + " -> " + (impl instanceof Handle h
                        ? h.getOwner() + "." + LAMBDA_NUMBER.matcher(h.getName()).replaceAll(Matcher.quoteReplacement("$lambda$N")) + h.getDesc()
                        : impl));
            } else {
                lines.add(op);
            }
        }
        return lines;
    }

    private static String nameUnlessMinecraft(String owner, String name, String shadowOwner) {
        return owner.startsWith("net/minecraft/") || owner.equals(shadowOwner) ? "" : name;
    }

    private static int target(List<AbstractInsnNode> real, LabelNode label) {
        for (AbstractInsnNode insn = label; insn != null; insn = insn.getNext()) {
            int index = real.indexOf(insn);
            if (index >= 0) {
                return index;
            }
        }
        return real.size();
    }
}

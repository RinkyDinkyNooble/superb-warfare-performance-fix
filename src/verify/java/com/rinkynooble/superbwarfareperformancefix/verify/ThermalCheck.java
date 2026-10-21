package com.rinkynooble.superbwarfareperformancefix.verify;

import com.rinkynooble.superbwarfareperformancefix.SmokeCheckShape;
import com.rinkynooble.superbwarfareperformancefix.ThermalArmShape;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

/**
 * Checks the thermal arm patch two ways.
 *
 * <p>1. Superb Warfare's thermal callback still has the shape ThermalArmShape expects, both in the dev jar and in the
 * production jar in libs/, whose Minecraft names differ (run with {@code --args=print} to print the shape after
 * updating Superb Warfare). The shape check must also refuse the callback without its setupAnim, and a second
 * callback at the same place.
 *
 * <p>2. Order, once {@code gradlew runData} has exported PlayerRenderer with every mod's mixins applied (see README).
 * In renderHand, playerAnimator's mark must come before vanilla's setupAnim, and this patch's mark after the sleeve
 * and before Superb Warfare's callback, with no other setupAnim in between, so that callback's setupAnim uses it up.
 */
public final class ThermalCheck {
    private static final String MIXIN_MERGED = "Lorg/spongepowered/asm/mixin/transformer/meta/MixinMerged;";
    private static final String SETUP_ANIM_DESC = "(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V";
    private static final String RENDER_DESC = "(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V";

    public static void main(String[] args) throws Exception {
        boolean shapeOk = checkShape(args.length > 0 && args[0].equals("print"));
        boolean orderOk = checkOrder();
        if (!shapeOk || !orderOk) {
            System.exit(1);
        }
    }

    private static boolean checkShape(boolean print) throws Exception {
        ClassNode dev = readDev();
        if (print) {
            MethodNode callback = ThermalArmShape.callback(dev);
            System.out.println(callback == null ? "No callback found." : callback.name + ":");
            for (String line : callback == null ? List.<String>of() : SmokeCheckShape.describe(callback, dev.name)) {
                System.out.println("            \"" + line + "\",");
            }
        }
        boolean ok = report(ThermalArmShape.matches(dev),
                "Superb Warfare's thermal callback matches the one the patch was written for.",
                "Superb Warfare's thermal callback differs, so the patch would not apply.");
        ok &= report(ThermalArmShape.matches(readProduction()),
                "also matches in the production jar, with its other Minecraft names.",
                "doesn't match in the production jar.");

        ClassNode noSetup = readDev();
        MethodNode callback = ThermalArmShape.callback(noSetup);
        for (AbstractInsnNode insn : callback.instructions) {
            if (insn instanceof MethodInsnNode call && call.desc.equals(SETUP_ANIM_DESC)) {
                callback.instructions.remove(insn);
            }
        }
        ok &= report(!ThermalArmShape.matches(noSetup),
                "refused without the callback's setupAnim.",
                "still matches without the callback's setupAnim.");

        ClassNode twoCallbacks = readDev();
        MethodNode copy = new MethodNode();
        ThermalArmShape.callback(twoCallbacks).accept(copy);
        copy.name += "Copy";
        twoCallbacks.methods.add(copy);
        ok &= report(!ThermalArmShape.matches(twoCallbacks),
                "refused with two callbacks at the return of renderHand.",
                "still matches with two callbacks at the return of renderHand.");
        return ok;
    }

    private static boolean checkOrder() throws Exception {
        File exported = new File(System.getProperty("sbwfix.exportedPlayerRenderer"));
        if (!exported.isFile()) {
            System.out.println("Order: not checked, " + exported + " is missing (gradlew runData exports it).");
            return true;
        }
        ClassNode renderer = new ClassNode();
        new ClassReader(Files.readAllBytes(exported.toPath())).accept(renderer, 0);
        MethodNode renderHand = null;
        for (MethodNode method : renderer.methods) {
            if (method.name.equals("renderHand")) {
                renderHand = method;
            }
        }
        if (renderHand == null) {
            return report(false, "", "renderHand is missing from the exported PlayerRenderer.");
        }

        String playerAnimatorMark = merged(renderer, "dev.kosmx.playerAnim.mixin.PlayerRendererMixin", "notifyModelOfFirstPerson");
        String ourMark = merged(renderer, "com.rinkynooble.superbwarfareperformancefix.mixin.PlayerRendererMixin", "markThermalArmFirstPerson");
        String thermalCallback = merged(renderer, ThermalArmShape.THERMAL_MIXIN, "renderHand");
        System.out.println("Order: renderHand calls, in order:");
        List<String> calls = new ArrayList<>();
        for (AbstractInsnNode insn : renderHand.instructions) {
            if (insn instanceof MethodInsnNode call) {
                String name = call.name.equals(playerAnimatorMark) ? "playerAnimator's mark"
                        : call.name.equals(ourMark) ? "this patch's mark"
                        : call.name.equals(thermalCallback) ? "Superb Warfare's thermal callback"
                        : call.name.equals("setupAnim") && call.desc.equals(SETUP_ANIM_DESC) ? "setupAnim"
                        : call.name.equals("render") && call.desc.equals(RENDER_DESC) ? "ModelPart.render"
                        : null;
                if (name != null) {
                    calls.add(name);
                    System.out.println("    " + name + "  (" + call.owner + "." + call.name + ")");
                }
            }
        }
        List<String> expected = List.of("playerAnimator's mark", "setupAnim", "ModelPart.render", "ModelPart.render",
                "this patch's mark", "Superb Warfare's thermal callback");
        return report(calls.equals(expected),
                "this patch marks the model right before Superb Warfare's thermal callback, and only vanilla's setupAnim comes before it.",
                "not in the expected order " + expected + ".");
    }

    /** The name Mixin gave the method it merged into the class from the given mixin, or null. */
    private static String merged(ClassNode target, String mixin, String handler) {
        for (MethodNode method : target.methods) {
            if (method.name.endsWith(handler) && method.visibleAnnotations != null) {
                for (AnnotationNode annotation : method.visibleAnnotations) {
                    if (annotation.desc.equals(MIXIN_MERGED) && annotation.values != null && annotation.values.contains(mixin)) {
                        return method.name;
                    }
                }
            }
        }
        return null;
    }

    private static boolean report(boolean ok, String pass, String fail) {
        System.out.println(ok ? "Thermal: " + pass : "Thermal: FAILED, " + fail);
        return ok;
    }

    private static ClassNode readDev() throws Exception {
        String path = ThermalArmShape.THERMAL_MIXIN.replace('.', '/') + ".class";
        try (InputStream in = ThermalCheck.class.getClassLoader().getResourceAsStream(path)) {
            return read(in);
        }
    }

    private static ClassNode readProduction() throws Exception {
        String path = ThermalArmShape.THERMAL_MIXIN.replace('.', '/') + ".class";
        try (ZipFile jar = new ZipFile(System.getProperty("sbwfix.productionJar"));
             InputStream in = jar.getInputStream(jar.getEntry(path))) {
            return read(in);
        }
    }

    private static ClassNode read(InputStream in) throws Exception {
        ClassNode node = new ClassNode();
        new ClassReader(in).accept(node, 0);
        return node;
    }
}

package com.rinkynooble.superbwarfareperformancefix.verify;

import com.rinkynooble.superbwarfareperformancefix.SmokeCheckShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.level.entity.EntityLookup;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelEntityGetterAdapter;
import net.minecraft.world.level.entity.Visibility;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Checks the smoke patch two ways.
 *
 * <p>1. Superb Warfare's smoke check in libs/ still has the shape SmokeCheckShape expects (run with
 * {@code --args=print} to print the shape after updating Superb Warfare). The shape check must also still accept it
 * with its lambdas renumbered, and refuse it with a changed filter.
 *
 * <p>2. Same result: builds random worlds in Minecraft's own entity section storage, moving, removing and adding
 * entities and changing section visibility between checks, and asks each one the way NOT_IN_SMOKE does, once for all
 * entities (the original) and once for smoke clouds only (the patch). Both must return the same smoke clouds in the
 * same order. Level.getEntities wraps both the same way: its only extra step loops over Forge part entities, and no
 * part entity can be a smoke cloud, since SmokeDecoyEntity extends Entity directly.
 */
public final class SmokeCheck {
    private static final int WORLDS = 3000;
    private static final int STEPS = 120;

    /** Stand-ins for Entity, other entities, SmokeDecoyEntity and a subclass of it. */
    static class Base implements EntityAccess {
        private static int nextId;
        final int id = nextId++;
        final UUID uuid = new UUID(0, id);
        AABB box;

        Base(AABB box) {
            this.box = box;
        }

        @Override public int getId() { return id; }
        @Override public UUID getUUID() { return uuid; }
        @Override public BlockPos blockPosition() { return BlockPos.containing(box.getCenter().x, box.minY, box.getCenter().z); }
        @Override public AABB getBoundingBox() { return box; }
        @Override public void setLevelCallback(EntityInLevelCallback callback) { }
        @Override public Stream<? extends EntityAccess> getSelfAndPassengers() { return Stream.of(this); }
        @Override public Stream<? extends EntityAccess> getPassengersAndSelf() { return Stream.of(this); }
        @Override public void setRemoved(Entity.RemovalReason reason) { }
        @Override public boolean shouldBeSaved() { return true; }
        @Override public boolean isAlwaysTicking() { return false; }
    }

    static class Mob extends Base { Mob(AABB box) { super(box); } }
    static class Smoke extends Base { Smoke(AABB box) { super(box); } }
    static class SmokeChild extends Smoke { SmokeChild(AABB box) { super(box); } }

    public static void main(String[] args) throws Exception {
        boolean shapeOk = checkShape(args.length > 0 && args[0].equals("print"));
        boolean sameOk = checkSameResults();
        if (!shapeOk || !sameOk) {
            System.exit(1);
        }
    }

    private static boolean checkShape(boolean print) throws Exception {
        ClassNode seekTool = readSeekTool();
        if (print) {
            var parts = SmokeCheckShape.parts(seekTool);
            for (var method : parts == null ? List.<MethodNode>of() : parts) {
                System.out.println(method.name + ":");
                for (String line : SmokeCheckShape.describe(method)) {
                    System.out.println("        \"" + line + "\",");
                }
            }
        }
        boolean ok = SmokeCheckShape.matches(seekTool);
        System.out.println(ok
                ? "Shape: Superb Warfare's smoke check matches the one the patch was written for."
                : "Shape: FAILED, Superb Warfare's smoke check differs, so the patch would not apply.");

        // The same check with every lambda renumbered, as when Superb Warfare adds a lambda elsewhere in SeekTool.
        ClassNode renumbered = readSeekTool();
        for (MethodNode method : renumbered.methods) {
            method.name = renumber(method.name);
            for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof InvokeDynamicInsnNode indy && indy.bsmArgs.length > 1 && indy.bsmArgs[1] instanceof Handle h) {
                    indy.bsmArgs[1] = new Handle(h.getTag(), h.getOwner(), renumber(h.getName()), h.getDesc(), h.isInterface());
                }
            }
        }
        boolean renumberedOk = SmokeCheckShape.matches(renumbered);
        System.out.println(renumberedOk
                ? "Shape: still matches with its lambdas renumbered."
                : "Shape: FAILED, no longer matches once its lambdas are renumbered.");

        // A changed filter (keeping every entity, not only smoke clouds) must not match.
        ClassNode changed = readSeekTool();
        MethodNode filter = SmokeCheckShape.parts(changed).get(1);
        for (AbstractInsnNode insn : filter.instructions) {
            if (insn instanceof TypeInsnNode type && insn.getOpcode() == Opcodes.INSTANCEOF) {
                type.desc = "net/minecraft/world/entity/Entity";
                break;
            }
        }
        boolean changedRefused = !SmokeCheckShape.matches(changed);
        System.out.println(changedRefused
                ? "Shape: a changed filter is refused, so the patch would leave it alone."
                : "Shape: FAILED, a changed filter still matches.");
        return ok && renumberedOk && changedRefused;
    }

    private static ClassNode readSeekTool() throws Exception {
        ClassNode seekTool = new ClassNode();
        String path = SmokeCheckShape.SEEK_TOOL.replace('.', '/') + ".class";
        try (InputStream in = SmokeCheck.class.getClassLoader().getResourceAsStream(path)) {
            new ClassReader(in).accept(seekTool, 0);
        }
        return seekTool;
    }

    private static String renumber(String name) {
        Matcher matcher = Pattern.compile("\\$lambda\\$(\\d+)").matcher(name);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(out, Matcher.quoteReplacement("$lambda$" + (Integer.parseInt(matcher.group(1)) + 31)));
        }
        return matcher.appendTail(out).toString();
    }

    private static boolean checkSameResults() {
        Random random = new Random(20261013L);
        long checks = 0;
        long found = 0;
        long lookedAtBefore = 0;
        long lookedAtAfter = 0;

        for (int world = 0; world < WORLDS; world++) {
            Map<Long, Visibility> startVisibility = new HashMap<>();
            EntitySectionStorage<Base> storage = new EntitySectionStorage<>(Base.class,
                    key -> startVisibility.computeIfAbsent(key, k -> randomVisibility(random)));
            LevelEntityGetterAdapter<Base> level = new LevelEntityGetterAdapter<>(new EntityLookup<>(), storage);
            List<Base> entities = new ArrayList<>();
            // Every fifth world is a crowd packed into a few sections, like a horde around the player.
            boolean crowd = world % 5 == 0;
            int size = 1 + random.nextInt(crowd ? 600 : 120);

            for (int step = 0; step < STEPS; step++) {
                int action = random.nextInt(10);
                if (action < 4 || entities.isEmpty()) {
                    for (int i = random.nextInt(size) / 4 + 1; i > 0; i--) {
                        Base entity = randomEntity(random, crowd);
                        entities.add(entity);
                        storage.getOrCreateSection(sectionOf(entity)).add(entity);
                    }
                } else if (action < 6) {
                    // Move: out of its old section and into its new one, like an entity crossing a section border.
                    Base entity = entities.get(random.nextInt(entities.size()));
                    leave(storage, entity);
                    entity.box = randomBox(random, crowd);
                    storage.getOrCreateSection(sectionOf(entity)).add(entity);
                } else if (action < 7) {
                    Base entity = entities.remove(random.nextInt(entities.size()));
                    leave(storage, entity);
                } else if (action < 8) {
                    Base entity = entities.get(random.nextInt(entities.size()));
                    storage.getOrCreateSection(sectionOf(entity)).updateChunkStatus(randomVisibility(random));
                }

                for (int q = 0; q < 4; q++) {
                    // NOT_IN_SMOKE checks the entity's box grown by 8; sometimes check any box.
                    Base self = random.nextInt(4) == 0 || entities.isEmpty()
                            ? randomEntity(random, crowd)
                            : entities.get(random.nextInt(entities.size()));
                    AABB box = random.nextInt(5) == 0 ? randomBox(random, crowd).inflate(random.nextDouble() * 40) : self.getBoundingBox().inflate(8.0);
                    Predicate<Base> filter = it -> it instanceof Smoke && !(self instanceof Smoke);

                    int[] counted = new int[2];
                    List<Base> before = ask(level, EntityTypeTest.forClass(Base.class), box, filter, counted, 0);
                    List<Base> after = ask(level, EntityTypeTest.forClass(Smoke.class), box, filter, counted, 1);
                    checks++;
                    found += before.isEmpty() ? 0 : 1;
                    lookedAtBefore += counted[0];
                    lookedAtAfter += counted[1];
                    if (!before.equals(after)) {
                        System.out.println("Same results: FAILED in world " + world + ", step " + step
                                + ": all entities found " + ids(before) + ", smoke clouds only found " + ids(after));
                        return false;
                    }
                }
            }
        }

        System.out.printf("Same results: %,d checks in %,d worlds, all the same (%,d found a smoke cloud).%n",
                checks, WORLDS, found);
        System.out.printf("Entities looked at per check: %.1f before, %.1f after.%n",
                (double) lookedAtBefore / checks, (double) lookedAtAfter / checks);
        return true;
    }

    private static <U extends Base> List<Base> ask(LevelEntityGetterAdapter<Base> level, EntityTypeTest<Base, U> test,
                                                   AABB box, Predicate<Base> filter, int[] counted, int slot) {
        List<Base> result = new ArrayList<>();
        level.get(test, box, entity -> {
            counted[slot]++;
            if (filter.test(entity)) {
                result.add(entity);
            }
            return AbortableIterationConsumer.Continuation.CONTINUE;
        });
        return result;
    }

    private static void leave(EntitySectionStorage<Base> storage, Base entity) {
        long key = sectionOf(entity);
        EntitySection<Base> section = storage.getSection(key);
        if (section != null && section.remove(entity) && section.isEmpty()) {
            storage.remove(key);
        }
    }

    private static long sectionOf(Base entity) {
        return SectionPos.asLong(entity.blockPosition());
    }

    private static Base randomEntity(Random random, boolean crowd) {
        AABB box = randomBox(random, crowd);
        // Smoke clouds are rare in a crowd, as in play.
        return switch (random.nextInt(crowd ? 200 : 10)) {
            case 0, 1 -> new Smoke(box);
            case 2 -> new SmokeChild(box);
            case 3 -> new Base(box);
            default -> new Mob(box);
        };
    }

    // Spread over sections on both sides of zero, with some boxes far wider than a section.
    private static AABB randomBox(Random random, boolean crowd) {
        double spread = crowd ? 32 : 96;
        Vec3 feet = new Vec3(random.nextDouble() * spread - spread / 2, random.nextDouble() * spread / 4 - 4, random.nextDouble() * spread - spread / 2);
        double width = random.nextInt(12) == 0 ? random.nextDouble() * 24 : 0.2 + random.nextDouble() * 1.6;
        double height = random.nextInt(12) == 0 ? random.nextDouble() * 16 : 0.2 + random.nextDouble() * 2.4;
        return new AABB(feet.x - width / 2, feet.y, feet.z - width / 2, feet.x + width / 2, feet.y + height, feet.z + width / 2);
    }

    private static Visibility randomVisibility(Random random) {
        int roll = random.nextInt(8);
        return roll == 0 ? Visibility.HIDDEN : roll < 3 ? Visibility.TRACKED : Visibility.TICKING;
    }

    private static List<Integer> ids(List<Base> entities) {
        return entities.stream().map(Base::getId).toList();
    }
}

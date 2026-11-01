package dev.impossibledragon;

import org.bukkit.Material;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** The actual block models: the 3-headed obsidian/amethyst dragon, its head, and the dragonling minion. */
final class ModelLibrary {

    private ModelLibrary() {
    }

    private static final Material DARK = Material.BLACKSTONE;
    private static final Material DARK2 = Material.POLISHED_BLACKSTONE_BRICKS;
    private static final Material PLATE = Material.POLISHED_BLACKSTONE;
    private static final Material MEMBRANE = Material.DEEPSLATE_TILES;
    private static final Material SPIKE = Material.AMETHYST_BLOCK;
    private static final Material EYE = Material.MAGENTA_CONCRETE;

    private static final Vector3f X = new Vector3f(1, 0, 0);
    private static final Vector3f Y = new Vector3f(0, 1, 0);
    private static final Vector3f Z = new Vector3f(0, 0, 1);

    private static final class B {
        final List<ModelPart> parts = new ArrayList<>();
        final List<ModelPart.Group> stack = new ArrayList<>();

        void push(float px, float py, float pz, Vector3f axis, ModelPart.Angle a) {
            stack.add(new ModelPart.Group(new Vector3f(px, py, pz), axis, a));
        }

        void pop() {
            stack.remove(stack.size() - 1);
        }

        void box(Material m, float cx, float cy, float cz, float sx, float sy, float sz) {
            box(m, cx, cy, cz, sx, sy, sz, 0, 0, 0, false);
        }

        void box(Material m, float cx, float cy, float cz, float sx, float sy, float sz,
                 float pitch, float yaw, float roll, boolean glow) {
            Quaternionf q = new Quaternionf().rotateY((float) Math.toRadians(yaw))
                    .rotateX((float) Math.toRadians(pitch)).rotateZ((float) Math.toRadians(roll));
            List<ModelPart.Group> inner = new ArrayList<>(stack);
            java.util.Collections.reverse(inner);
            parts.add(new ModelPart(m, new Vector3f(cx, cy, cz), new Vector3f(sx, sy, sz), q, glow, inner));
        }
    }

    // ------------------------------------------------------------------ head

    private static void head(B b, float ox, float oy, float oz, double phase) {
        // whole head sways on its neck
        b.push(ox, oy, oz - 2.8f, Y, t -> Math.sin(t * 0.06 + phase) * 0.14);
        b.box(DARK, ox, oy, oz, 2.4f, 1.8f, 2.6f);                                  // skull
        b.box(PLATE, ox, oy + 0.95f, oz - 0.2f, 1.6f, 0.3f, 2.2f);                  // brow plate
        b.box(DARK2, ox, oy - 0.2f, oz + 2.0f, 1.6f, 1.0f, 2.0f);                   // snout
        b.box(SPIKE, ox - 0.45f, oy + 0.2f, oz + 3.02f, 0.3f, 0.3f, 0.12f, 0, 0, 0, true); // nostrils
        b.box(SPIKE, ox + 0.45f, oy + 0.2f, oz + 3.02f, 0.3f, 0.3f, 0.12f, 0, 0, 0, true);
        b.box(EYE, ox - 1.22f, oy + 0.4f, oz + 0.5f, 0.16f, 0.36f, 0.8f, 0, 0, 0, true);  // glowing eyes
        b.box(EYE, ox + 1.22f, oy + 0.4f, oz + 0.5f, 0.16f, 0.36f, 0.8f, 0, 0, 0, true);
        b.box(SPIKE, ox - 0.95f, oy + 1.05f, oz - 1.0f, 0.5f, 0.5f, 2.0f, 28, 0, 0, false); // horns
        b.box(SPIKE, ox + 0.95f, oy + 1.05f, oz - 1.0f, 0.5f, 0.5f, 2.0f, 28, 0, 0, false);
        b.box(SPIKE, ox, oy + 1.2f, oz - 0.2f, 0.6f, 0.6f, 1.6f, 10, 0, 0, false);          // crest
        b.box(SPIKE, ox, oy + 1.15f, oz - 1.3f, 0.5f, 0.5f, 1.2f, 22, 0, 0, false);
        // lower jaw opens and closes
        final double ph = phase;
        b.push(ox, oy - 0.7f, oz - 1.0f, X, t -> 0.16 + 0.14 * Math.sin(t * 0.1 + ph));
        b.box(DARK, ox, oy - 0.85f, oz + 1.1f, 1.8f, 0.5f, 3.2f);
        b.box(SPIKE, ox - 0.65f, oy - 0.5f, oz + 2.3f, 0.2f, 0.4f, 0.2f);
        b.box(SPIKE, ox + 0.65f, oy - 0.5f, oz + 2.3f, 0.2f, 0.4f, 0.2f);
        b.pop();
        b.pop();
    }

    // ------------------------------------------------------------------ dragon

    static List<ModelPart> dragon() {
        B b = new B();

        // torso
        b.box(DARK, 0, 0, 0, 5f, 3.2f, 7f);
        b.box(PLATE, 0, 1.75f, 0, 3.2f, 0.5f, 6f);
        b.box(DARK2, 0, -1.7f, 0, 3.6f, 0.5f, 6f);
        for (float z : new float[]{2.6f, 1.0f, -0.6f, -2.2f}) {
            b.box(SPIKE, 0, 2.4f, z, 0.6f, 1.2f, 0.6f, 15, 0, 0, false);
        }

        // three necks + heads (centre, left, right) like the reference
        b.box(DARK, 0, 0.3f, 4.6f, 2.2f, 2.2f, 3f);
        b.box(DARK2, 0, 0.0f, 6.3f, 1.9f, 1.9f, 2f);
        b.box(SPIKE, 0, 1.5f, 4.4f, 0.5f, 1.0f, 0.5f, 20, 0, 0, false);
        b.box(SPIKE, 0, 1.2f, 6.0f, 0.5f, 0.9f, 0.5f, 20, 0, 0, false);
        head(b, 0, -0.5f, 8.2f, 0);
        for (int s = -1; s <= 1; s += 2) {
            b.box(DARK, s * 2.4f, 0.2f, 4.1f, 1.8f, 1.8f, 3f, 0, s * 22, 0, false);
            b.box(DARK2, s * 3.6f, -0.3f, 5.8f, 1.6f, 1.6f, 2.4f, 0, s * 30, 0, false);
            b.box(SPIKE, s * 2.4f, 1.3f, 4.0f, 0.45f, 1.0f, 0.45f, 20, 0, 0, false);
            b.box(SPIKE, s * 3.6f, 0.8f, 5.6f, 0.45f, 0.9f, 0.45f, 20, 0, 0, false);
            head(b, s * 4.9f, -0.9f, 7.6f, s * 1.7);
        }

        // wings
        for (int s = -1; s <= 1; s += 2) {
            final float sx = s;
            b.push(sx * 2.4f, 1.2f, 0.5f, Z, t -> sx * (0.16 + 0.38 * Math.sin(t * 0.11)));
            b.box(DARK, sx * 5.2f, 1.3f, 0.6f, 6f, 0.8f, 0.9f);
            b.box(MEMBRANE, sx * 5.2f, 1.15f, -2.2f, 6f, 0.14f, 4.6f);
            for (int i = 0; i < 3; i++) {
                b.box(SPIKE, sx * (3.4f + i * 1.9f), 1.25f, -5.2f, 0.45f, 0.5f, 2.2f, 0, sx * -12, 0, false);
            }
            b.push(sx * 8.3f, 1.3f, 0.5f, Z, t -> sx * 0.35 * Math.sin(t * 0.11 - 0.9));
            b.box(DARK, sx * 11.0f, 1.2f, 0.2f, 5.4f, 0.6f, 0.8f);
            b.box(MEMBRANE, sx * 11.0f, 1.05f, -2.0f, 5.4f, 0.12f, 4.0f);
            for (int i = 0; i < 3; i++) {
                b.box(SPIKE, sx * (9.8f + i * 1.9f), 1.15f, -4.7f, 0.4f, 0.45f, 2.0f, 0, sx * -12, 0, false);
            }
            b.box(SPIKE, sx * 14.0f, 1.3f, 1.0f, 0.5f, 0.5f, 2.0f, 0, sx * 20, 0, false); // wing claw
            b.pop();
            b.pop();
        }

        // tail: rises up behind the body, purple spine along it
        for (int i = 0; i < 8; i++) {
            final int n = i + 1;
            float z = -4.2f - i * 1.55f;
            float y = 0.4f + i * 0.5f;
            float sz = 1.9f - 0.17f * i;
            b.push(0, 0.6f, -3.5f, Y, t -> Math.sin(t * 0.07) * 0.05 * n);
            b.box(i % 2 == 0 ? DARK : DARK2, 0, y, z, sz, sz, 2.0f, 18, 0, 0, false);
            b.box(SPIKE, 0, y + sz / 2 + 0.3f, z, 0.45f, 0.9f, 0.45f, 18, 0, 0, false);
            if (i == 7) {
                b.box(SPIKE, 0, y + 0.9f, z - 1.2f, 0.7f, 0.7f, 2.4f, 40, 0, 0, true);
            }
            b.pop();
        }
        return b.parts;
    }

    // ------------------------------------------------------------------ single head (wither skull replacement)

    static List<ModelPart> headOnly() {
        B b = new B();
        head(b, 0, 0, 0, 0);
        return b.parts;
    }

    // ------------------------------------------------------------------ minion: dragonling

    static List<ModelPart> dragonling() {
        B b = new B();
        b.box(DARK, 0, 0, 0, 1.1f, 0.9f, 1.8f);
        b.box(PLATE, 0, 0.55f, 0, 0.7f, 0.2f, 1.4f);
        b.box(SPIKE, 0, 0.8f, 0.3f, 0.2f, 0.4f, 0.2f, 15, 0, 0, false);
        b.box(SPIKE, 0, 0.8f, -0.3f, 0.2f, 0.4f, 0.2f, 15, 0, 0, false);
        // head
        b.push(0, 0.2f, 0.9f, Y, t -> Math.sin(t * 0.09) * 0.2);
        b.box(DARK, 0, 0.25f, 1.45f, 0.9f, 0.75f, 0.9f);
        b.box(DARK2, 0, 0.1f, 2.1f, 0.6f, 0.4f, 0.8f);
        b.box(EYE, -0.47f, 0.35f, 1.6f, 0.08f, 0.16f, 0.3f, 0, 0, 0, true);
        b.box(EYE, 0.47f, 0.35f, 1.6f, 0.08f, 0.16f, 0.3f, 0, 0, 0, true);
        b.box(SPIKE, -0.3f, 0.7f, 1.2f, 0.2f, 0.2f, 0.8f, 28, 0, 0, false);
        b.box(SPIKE, 0.3f, 0.7f, 1.2f, 0.2f, 0.2f, 0.8f, 28, 0, 0, false);
        b.pop();
        // wings
        for (int s = -1; s <= 1; s += 2) {
            final float sx = s;
            b.push(sx * 0.5f, 0.4f, 0, Z, t -> sx * (0.2 + 0.7 * Math.sin(t * 0.35)));
            b.box(DARK, sx * 1.4f, 0.45f, 0.1f, 1.8f, 0.2f, 0.3f);
            b.box(MEMBRANE, sx * 1.4f, 0.4f, -0.5f, 1.8f, 0.05f, 1.2f);
            b.box(SPIKE, sx * 2.1f, 0.45f, -1.1f, 0.15f, 0.15f, 0.7f, 0, sx * -10, 0, false);
            b.pop();
        }
        // tail
        for (int i = 0; i < 4; i++) {
            final int n = i + 1;
            float s = 0.7f - 0.1f * i;
            b.push(0, 0, -0.9f, Y, t -> Math.sin(t * 0.15) * 0.12 * n);
            b.box(i % 2 == 0 ? DARK : DARK2, 0, 0.05f + i * 0.1f, -1.3f - i * 0.6f, s, s, 0.8f);
            b.box(SPIKE, 0, 0.35f + i * 0.1f, -1.3f - i * 0.6f, 0.15f, 0.3f, 0.15f);
            b.pop();
        }
        return b.parts;
    }
}

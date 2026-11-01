package dev.impossibledragon;

import org.bukkit.Material;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** One box of a block-built model. Coordinates: +x = left of the head, +y = up, +z = towards the head. */
final class ModelPart {

    interface Angle {
        double at(double t);
    }

    /** A rotating joint (wing, jaw, neck, tail segment...). */
    static final class Group {
        final Vector3f pivot;
        final Vector3f axis;
        final Angle angle;

        Group(Vector3f pivot, Vector3f axis, Angle angle) {
            this.pivot = pivot;
            this.axis = axis;
            this.angle = angle;
        }
    }

    final Material block;
    final Vector3f center;
    final Vector3f size;
    final Quaternionf rot;
    final boolean glow;
    /** applied in order: innermost joint first */
    final List<Group> groups = new ArrayList<>();

    ModelPart(Material block, Vector3f center, Vector3f size, Quaternionf rot, boolean glow, List<Group> groups) {
        this.block = block;
        this.center = center;
        this.size = size;
        this.rot = rot;
        this.glow = glow;
        this.groups.addAll(groups);
    }
}

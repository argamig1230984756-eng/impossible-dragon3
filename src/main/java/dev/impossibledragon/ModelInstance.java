package dev.impossibledragon;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * One animated model glued to an entity.
 * Java players see smooth, scaled, rotated BlockDisplay entities.
 * Bedrock (Geyser) players can't see display entities, so they get a voxel version built from
 * armor stands wearing blocks. Each audience only ever sees its own version.
 */
final class ModelInstance {

    enum Mode { DRAGON, MOB, PROJECTILE }

    private final ModelManager mgr;
    private final Entity anchor;
    private final List<ModelPart> parts;
    private final Mode mode;
    private final float scale;
    private final float yOffset;
    private final int maxStands;

    private final List<BlockDisplay> displays = new ArrayList<>();
    private final List<ArmorStand> stands = new ArrayList<>();
    private final Map<UUID, Set<UUID>> shownJava = new HashMap<>();
    private final Map<UUID, Set<UUID>> shownBedrock = new HashMap<>();

    // voxel sampling for the Bedrock version
    private final List<Integer> voxelPart = new ArrayList<>();
    private final List<Vector3f> voxelLocal = new ArrayList<>();
    private final List<ItemStack> voxelItem = new ArrayList<>();

    private boolean removed = false;
    private double lastTheta = Double.NaN;
    private double lastY = Double.NaN;
    private float bank = 0, tilt = 0;
    private double headX = 0, headZ = 1;

    ModelInstance(ModelManager mgr, Entity anchor, List<ModelPart> parts, Mode mode, float scale,
                  float yOffset, int maxStands) {
        this.mgr = mgr;
        this.anchor = anchor;
        this.parts = parts;
        this.mode = mode;
        this.scale = scale;
        this.yOffset = yOffset;
        this.maxStands = maxStands;
        buildVoxels();
    }

    boolean anchorIs(Entity e) {
        return anchor.getUniqueId().equals(e.getUniqueId());
    }

    World world() {
        return anchor.isValid() ? anchor.getWorld() : null;
    }

    boolean isRemoved() {
        return removed;
    }

    // ------------------------------------------------------------------ voxels (Bedrock)

    private int[] cells(ModelPart p, float step) {
        return new int[]{
                Math.max(1, Math.round(p.size.x * scale / step)),
                Math.max(1, Math.round(p.size.y * scale / step)),
                Math.max(1, Math.round(p.size.z * scale / step))};
    }

    private int countFor(float step) {
        int total = 0;
        for (ModelPart p : parts) {
            int[] c = cells(p, step);
            int all = c[0] * c[1] * c[2];
            if (c[0] >= 3 && c[1] >= 3 && c[2] >= 3) all -= (c[0] - 2) * (c[1] - 2) * (c[2] - 2);
            total += all;
        }
        return total;
    }

    private void buildVoxels() {
        float step = mgr.bedrockStep() * Math.max(0.35f, Math.min(1f, scale));
        while (countFor(step) > maxStands && step < 6f) step += 0.1f;
        for (int pi = 0; pi < parts.size(); pi++) {
            ModelPart p = parts.get(pi);
            int[] c = cells(p, step);
            ItemStack item = new ItemStack(p.block);
            for (int ix = 0; ix < c[0]; ix++) {
                for (int iy = 0; iy < c[1]; iy++) {
                    for (int iz = 0; iz < c[2]; iz++) {
                        boolean inner = ix > 0 && ix < c[0] - 1 && iy > 0 && iy < c[1] - 1 && iz > 0 && iz < c[2] - 1;
                        if (inner) continue;
                        voxelPart.add(pi);
                        voxelItem.add(item);
                        voxelLocal.add(new Vector3f(
                                ((ix + 0.5f) / c[0] - 0.5f) * p.size.x,
                                ((iy + 0.5f) / c[1] - 0.5f) * p.size.y,
                                ((iz + 0.5f) / c[2] - 0.5f) * p.size.z));
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ pose

    private Quaternionf root(Location l, double t) {
        Vector h;
        switch (mode) {
            case DRAGON -> {
                Vector d = l.getDirection().setY(0);
                if (d.lengthSquared() < 1.0E-4) d = new Vector(headX, 0, headZ);
                d.normalize();
                h = mgr.flipFacing() ? d : d.multiply(-1);
            }
            case PROJECTILE -> {
                h = anchor.getVelocity();
                if (h.lengthSquared() < 1.0E-5) h = l.getDirection();
                h = h.clone().normalize();
            }
            default -> {
                h = l.getDirection().setY(0);
                if (h.lengthSquared() < 1.0E-4) h = new Vector(headX, 0, headZ);
                h.normalize();
            }
        }
        double hx = h.getX(), hz = h.getZ();
        if (hx * hx + hz * hz < 1.0E-6) {
            hx = headX;
            hz = headZ;
        } else {
            headX = hx;
            headZ = hz;
        }
        double theta = Math.atan2(hx, hz);
        Quaternionf q = new Quaternionf().rotateY((float) theta);
        if (mode == Mode.PROJECTILE) {
            q.rotateX((float) -Math.asin(Math.max(-1, Math.min(1, h.getY()))));
        } else if (mode == Mode.DRAGON) {
            if (!Double.isNaN(lastTheta)) {
                double d = theta - lastTheta;
                while (d > Math.PI) d -= 2 * Math.PI;
                while (d < -Math.PI) d += 2 * Math.PI;
                float target = (float) Math.max(-0.5, Math.min(0.5, -d * 6));
                bank += (target - bank) * 0.25f;
            }
            if (!Double.isNaN(lastY)) {
                float target = (float) Math.max(-0.35, Math.min(0.35, -(l.getY() - lastY) * 0.35));
                tilt += (target - tilt) * 0.25f;
            }
            lastTheta = theta;
            lastY = l.getY();
            q.rotateZ(bank).rotateX(tilt);
        }
        return q;
    }

    /** world position (and optionally rotation) of a point of a part */
    private void place(ModelPart part, Vector3f local, double t, Quaternionf root, Location base,
                       Vector3f outPos, Quaternionf outRot) {
        Vector3f p = new Vector3f(local);
        Quaternionf q = new Quaternionf(part.rot);
        p = part.rot.transform(p).add(part.center);
        for (ModelPart.Group g : part.groups) {
            Quaternionf gr = new Quaternionf().rotateAxis((float) g.angle.at(t), g.axis.x, g.axis.y, g.axis.z);
            p.sub(g.pivot);
            gr.transform(p);
            p.add(g.pivot);
            q = new Quaternionf(gr).mul(q);
        }
        root.transform(p);
        p.mul(scale);
        outPos.set((float) base.getX() + p.x, (float) base.getY() + p.y, (float) base.getZ() + p.z);
        if (outRot != null) outRot.set(new Quaternionf(root).mul(q));
    }

    // ------------------------------------------------------------------ lifecycle

    void update(long tick, boolean javaViewers, boolean bedrockViewers, boolean sync, List<Player> players) {
        if (removed) return;
        if (!anchor.isValid()) {
            remove();
            return;
        }
        World w = anchor.getWorld();
        if (javaViewers && displays.isEmpty()) spawnDisplays(w);
        if (!javaViewers && !displays.isEmpty()) clear(displays, shownJava);
        if (bedrockViewers && stands.isEmpty()) spawnStands(w);
        if (!bedrockViewers && !stands.isEmpty()) clear(stands, shownBedrock);
        if (displays.isEmpty() && stands.isEmpty()) return;

        Location l = anchor.getLocation();
        float oy = mode == Mode.MOB ? (float) (anchor.getHeight() / 2.0) : yOffset;
        Location base = l.clone().add(0, oy, 0);
        double t = tick;
        Quaternionf root = root(l, t);

        Vector3f pos = new Vector3f();
        Quaternionf rot = new Quaternionf();
        Vector3f zero = new Vector3f();

        if (!displays.isEmpty()) {
            for (int i = 0; i < parts.size() && i < displays.size(); i++) {
                ModelPart part = parts.get(i);
                BlockDisplay d = displays.get(i);
                if (!d.isValid()) continue;
                place(part, zero, t, root, base, pos, rot);
                Vector3f sc = new Vector3f(part.size).mul(scale);
                Vector3f tr = new Vector3f(sc).mul(0.5f);
                rot.transform(tr);
                tr.negate();
                d.setTransformation(new Transformation(tr, new Quaternionf(rot), sc, new Quaternionf()));
                d.setInterpolationDelay(0);
                d.setInterpolationDuration(ModelManager.PERIOD + 1);
                d.teleport(new Location(w, pos.x, pos.y, pos.z));
            }
        }
        if (!stands.isEmpty()) {
            float yo = mgr.bedrockYOffset();
            for (int i = 0; i < stands.size() && i < voxelPart.size(); i++) {
                ArmorStand s = stands.get(i);
                if (!s.isValid()) continue;
                place(parts.get(voxelPart.get(i)), voxelLocal.get(i), t, root, base, pos, null);
                s.teleport(new Location(w, pos.x, pos.y + yo * Math.max(0.35f, Math.min(1f, scale)), pos.z));
            }
        }
        if (sync) sync(players);
    }

    private void spawnDisplays(World w) {
        Location at = anchor.getLocation();
        for (ModelPart part : parts) {
            BlockDisplay d = w.spawn(at, BlockDisplay.class, e -> {
                e.setBlock(part.block.createBlockData());
                e.setPersistent(false);
                e.setVisibleByDefault(false);
                e.setInterpolationDuration(ModelManager.PERIOD + 1);
                e.setTeleportDuration(ModelManager.PERIOD + 1);
                e.setViewRange(1.6f);
                e.setShadowRadius(0f);
                e.setBrightness(part.glow ? new Display.Brightness(15, 15) : mgr.modelBrightness());
                e.setTransformation(new Transformation(new Vector3f(), new Quaternionf(),
                        new Vector3f(0.01f), new Quaternionf()));
            });
            displays.add(d);
        }
    }

    private void spawnStands(World w) {
        Location at = anchor.getLocation();
        for (int i = 0; i < voxelPart.size(); i++) {
            final ItemStack item = voxelItem.get(i);
            ArmorStand s = w.spawn(at, ArmorStand.class, e -> {
                e.setVisible(false);
                e.setMarker(true);
                e.setGravity(false);
                e.setInvulnerable(true);
                e.setSilent(true);
                e.setPersistent(false);
                e.setBasePlate(false);
                e.setVisibleByDefault(false);
                if (e.getEquipment() != null) e.getEquipment().setHelmet(item);
            });
            stands.add(s);
        }
    }

    private void clear(List<? extends Entity> list, Map<UUID, Set<UUID>> shown) {
        for (Entity e : list) if (e.isValid()) e.remove();
        list.clear();
        shown.clear();
    }

    void sync(List<Player> players) {
        Set<UUID> online = new HashSet<>();
        for (Player p : players) {
            online.add(p.getUniqueId());
            if (mgr.isBedrock(p)) {
                reveal(p, stands, shownBedrock);
            } else {
                reveal(p, displays, shownJava);
            }
        }
        shownJava.keySet().retainAll(online);
        shownBedrock.keySet().retainAll(online);
    }

    private void reveal(Player p, List<? extends Entity> list, Map<UUID, Set<UUID>> shown) {
        if (list.isEmpty()) return;
        Set<UUID> s = shown.computeIfAbsent(p.getUniqueId(), k -> new HashSet<>());
        for (Entity e : list) {
            if (s.add(e.getUniqueId())) p.showEntity(mgr.plugin(), e);
        }
    }

    void remove() {
        removed = true;
        clear(displays, shownJava);
        clear(stands, shownBedrock);
    }
}

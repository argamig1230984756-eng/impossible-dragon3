package dev.impossibledragon;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
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
 * The translucent purple energy sphere around an end crystal (Java: a dome of tinted-glass panels).
 * Bedrock players can't see display entities - they get a particle shell instead (see DragonFight).
 */
final class CrystalBubble {

    private final ModelManager mgr;
    private final List<BlockDisplay> panels = new ArrayList<>();
    private final Map<UUID, Set<UUID>> shown = new HashMap<>();

    CrystalBubble(ModelManager mgr, Location center, int count, float radius) {
        this.mgr = mgr;
        World w = center.getWorld();
        float side = (float) Math.sqrt(4 * Math.PI * radius * radius / count) * 1.12f;
        double golden = Math.PI * (3 - Math.sqrt(5));
        Vector3f zAxis = new Vector3f(0, 0, 1);
        for (int i = 0; i < count; i++) {
            float y = 1f - 2f * (i + 0.5f) / count;
            if (y < -0.62f) continue; // pillar is underneath
            float r = (float) Math.sqrt(1 - y * y);
            double phi = i * golden;
            Vector3f n = new Vector3f((float) Math.cos(phi) * r, y, (float) Math.sin(phi) * r);
            Quaternionf q = new Quaternionf().rotationTo(zAxis, n);
            Vector3f sc = new Vector3f(side, side, 0.05f);
            Vector3f tr = new Vector3f(sc).mul(0.5f);
            q.transform(tr);
            tr.negate();
            Location at = center.clone().add(n.x * radius, n.y * radius, n.z * radius);
            BlockDisplay d = w.spawn(at, BlockDisplay.class, e -> {
                e.setBlock(Material.PURPLE_STAINED_GLASS.createBlockData());
                e.setPersistent(false);
                e.setVisibleByDefault(false);
                e.setViewRange(0.9f);
                e.setShadowRadius(0f);
                e.setBrightness(new Display.Brightness(15, 15));
                e.setTransformation(new Transformation(tr, q, sc, new Quaternionf()));
            });
            panels.add(d);
        }
    }

    void sync(List<Player> players) {
        Set<UUID> online = new HashSet<>();
        for (Player p : players) {
            online.add(p.getUniqueId());
            if (mgr.isBedrock(p)) continue;
            Set<UUID> s = shown.computeIfAbsent(p.getUniqueId(), k -> new HashSet<>());
            for (Entity e : panels) {
                if (s.add(e.getUniqueId())) p.showEntity(mgr.plugin(), e);
            }
        }
        shown.keySet().retainAll(online);
    }

    void remove() {
        for (Entity e : panels) if (e.isValid()) e.remove();
        panels.clear();
        shown.clear();
    }
}

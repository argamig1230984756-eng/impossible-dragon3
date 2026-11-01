package dev.impossibledragon;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Display;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Owns every live model, animates them and decides which players (Java / Bedrock) see which version. */
public final class ModelManager {

    static final int PERIOD = 2; // ticks between model updates

    private final ImpossibleDragon plugin;
    private final List<ModelInstance> instances = new ArrayList<>();
    private final Map<UUID, Boolean> bedrockCache = new HashMap<>();
    private final BukkitTask task;
    private long tick = 0;

    public ModelManager(ImpossibleDragon plugin) {
        this.plugin = plugin;
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, this::run, 2L, PERIOD);
    }

    ImpossibleDragon plugin() {
        return plugin;
    }

    boolean enabled() {
        return plugin.getConfig().getBoolean("models.enabled", true);
    }

    boolean flipFacing() {
        return plugin.getConfig().getBoolean("dragon.flip-facing", false);
    }

    float bedrockStep() {
        return (float) plugin.getConfig().getDouble("models.bedrock.block-spacing", 0.65);
    }

    float bedrockYOffset() {
        return (float) plugin.getConfig().getDouble("models.bedrock.y-offset", -1.75);
    }

    Display.Brightness modelBrightness() {
        int b = plugin.getConfig().getInt("models.brightness", 9);
        return new Display.Brightness(Math.max(0, Math.min(15, b)), Math.max(0, Math.min(15, b)));
    }

    boolean isBedrock(Player p) {
        return bedrockCache.computeIfAbsent(p.getUniqueId(), id -> {
            try {
                Class<?> api = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
                Object inst = api.getMethod("getInstance").invoke(null);
                return (boolean) api.getMethod("isFloodgatePlayer", UUID.class).invoke(inst, id);
            } catch (Throwable ignored) {
                return id.getMostSignificantBits() == 0L;
            }
        });
    }

    // ------------------------------------------------------------------ factories

    /** The 3-headed dragon body. The vanilla dragon is hidden (it stays as the hitbox / AI). */
    public void attachDragon(Entity dragon) {
        if (!enabled() || !plugin.getConfig().getBoolean("models.dragon.enabled", true)) return;
        if (dragon instanceof LivingEntity le) le.setInvisible(true);
        float scale = (float) plugin.getConfig().getDouble("models.dragon.scale", 0.85);
        float yo = (float) plugin.getConfig().getDouble("models.dragon.y-offset", 2.0);
        int max = plugin.getConfig().getInt("models.bedrock.max-blocks-dragon", 320);
        instances.add(new ModelInstance(this, dragon, ModelLibrary.dragon(), ModelInstance.Mode.DRAGON, scale, yo, max));
    }

    public void detachDragon(Entity dragon) {
        if (dragon instanceof LivingEntity le && le.isValid()) le.setInvisible(false);
        removeFor(dragon);
    }

    /** Minion dragonling: the vanilla mob turns invisible but keeps its AI and hitbox. */
    public void attachMinion(Entity mob) {
        if (!enabled() || !plugin.getConfig().getBoolean("models.minions.enabled", true)) return;
        if (mob instanceof LivingEntity le) {
            le.setInvisible(true);
            if (le.getEquipment() != null) le.getEquipment().clear();
        }
        float scale = (float) plugin.getConfig().getDouble("models.minions.scale", 1.0);
        int max = plugin.getConfig().getInt("models.bedrock.max-blocks-minion", 45);
        instances.add(new ModelInstance(this, mob, ModelLibrary.dragonling(), ModelInstance.Mode.MOB, scale, 0f, max));
    }

    /** Wither skull -> flying dragon head. The skull itself is hidden from every client. */
    public void attachSkull(Entity skull) {
        if (!enabled() || !plugin.getConfig().getBoolean("models.skull-heads.enabled", true)) return;
        skull.setVisibleByDefault(false);
        float scale = (float) plugin.getConfig().getDouble("models.skull-heads.scale", 0.22);
        int max = plugin.getConfig().getInt("models.bedrock.max-blocks-skull", 14);
        instances.add(new ModelInstance(this, skull, ModelLibrary.headOnly(), ModelInstance.Mode.PROJECTILE, scale, 0f, max));
    }

    CrystalBubble bubble(org.bukkit.Location c) {
        int n = plugin.getConfig().getInt("effects.crystal-bubble.panels", 64);
        float r = (float) plugin.getConfig().getDouble("effects.crystal-bubble.radius", 2.4);
        return new CrystalBubble(this, c, n, r);
    }

    private void removeFor(Entity e) {
        for (ModelInstance m : instances) {
            if (m.anchorIs(e)) m.remove();
        }
    }

    // ------------------------------------------------------------------ loop

    private void run() {
        tick += PERIOD;
        boolean sync = tick % 10 == 0;
        Map<World, boolean[]> viewers = new HashMap<>();
        Map<World, List<Player>> players = new HashMap<>();
        for (ModelInstance m : new ArrayList<>(instances)) {
            if (m.isRemoved()) {
                instances.remove(m);
                continue;
            }
            World w = m.world();
            if (w == null) {
                m.remove();
                instances.remove(m);
                continue;
            }
            boolean[] v = viewers.computeIfAbsent(w, k -> {
                boolean j = false, b = false;
                for (Player p : k.getPlayers()) {
                    if (isBedrock(p)) b = true;
                    else j = true;
                }
                return new boolean[]{j, b};
            });
            List<Player> ps = players.computeIfAbsent(w, World::getPlayers);
            m.update(tick, v[0], v[1], sync, ps);
        }
    }

    public void shutdown() {
        task.cancel();
        for (ModelInstance m : instances) m.remove();
        instances.clear();
    }
}

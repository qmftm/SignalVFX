package com.signalvfx.plugin.vfx;

import com.signalvfx.model.visual.BetterModelVisual;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Renders a {@link BetterModelVisual} via the BetterModel plugin's API, using
 * reflection so this plugin compiles and runs without a hard dependency.
 *
 * <p>Mirrors the documented entry point:
 * <pre>
 *   EntityTracker t = BetterModel.model("id")
 *       .map(renderer -&gt; renderer.getOrCreate(entity))
 *       .orElse(null);
 *   t.animate("clip");
 * </pre>
 * If the installed BetterModel exposes a different shape, {@link #render} logs
 * once and returns {@code false} so the caller can fall back to a display entity.
 * Wire this against the real BetterModel API jar to finalize the integration.
 */
public final class BetterModelRenderer {

    private static final String[] BETTERMODEL_CLASSES = {
            "kr.toxicity.model.api.BetterModel",
            "kr.toxicity.model.BetterModel"
    };

    private final JavaPlugin plugin;
    private boolean warned;

    public BetterModelRenderer(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("BetterModel");
    }

    public boolean render(Player caster, Location anchor, BetterModelVisual v) {
        if (!isAvailable() || v.getModelId() == null || v.getModelId().isBlank()) {
            return false;
        }
        Entity carrier = null;
        boolean spawnedCarrier = false;
        try {
            if (v.getAttach() == com.signalvfx.model.visual.Attach.CASTER) {
                carrier = caster;
            } else {
                carrier = spawnCarrier(anchor);
                spawnedCarrier = true;
            }

            Object renderer = model(v.getModelId());
            if (renderer == null) {
                return failOnce(carrier, spawnedCarrier, "model '" + v.getModelId() + "' not found");
            }
            Object tracker = getOrCreate(renderer, carrier);
            if (tracker == null) {
                return failOnce(carrier, spawnedCarrier, "getOrCreate returned null");
            }
            if (v.getAnimation() != null && !v.getAnimation().isBlank()) {
                animate(tracker, v.getAnimation());
            }
            if (spawnedCarrier) {
                Entity toRemove = carrier;
                Bukkit.getScheduler().runTaskLater(plugin, toRemove::remove, Math.max(1, v.getDurationTicks()));
            }
            return true;
        } catch (Throwable t) {
            return failOnce(carrier, spawnedCarrier, t.toString());
        }
    }

    // ---- reflection helpers -------------------------------------------

    private Object model(String id) throws Exception {
        Class<?> bm = findClass();
        if (bm == null) {
            return null;
        }
        Method model = bm.getMethod("model", String.class);
        Object result = model.invoke(null, id);
        if (result instanceof Optional<?> opt) {
            return opt.orElse(null);
        }
        return result;
    }

    private Object getOrCreate(Object renderer, Entity carrier) {
        // Try getOrCreate(Entity) first; some versions take an adapted handle.
        for (Method m : renderer.getClass().getMethods()) {
            if (!m.getName().equals("getOrCreate") || m.getParameterCount() != 1) {
                continue;
            }
            try {
                Class<?> param = m.getParameterTypes()[0];
                if (param.isAssignableFrom(Entity.class)) {
                    return m.invoke(renderer, carrier);
                }
                Object adapted = adapt(carrier, param);
                if (adapted != null) {
                    return m.invoke(renderer, adapted);
                }
            } catch (Exception ignored) {
                // try the next overload
            }
        }
        return null;
    }

    /** Attempts BetterModel's BukkitAdapter.adapt(entity) to produce the expected handle type. */
    private Object adapt(Entity entity, Class<?> expected) {
        for (String cn : new String[]{
                "kr.toxicity.model.api.util.BukkitAdapter",
                "kr.toxicity.model.api.nms.BukkitAdapter"}) {
            try {
                Class<?> adapter = Class.forName(cn);
                Method adapt = adapter.getMethod("adapt", Entity.class);
                Object out = adapt.invoke(null, entity);
                if (out != null && expected.isAssignableFrom(out.getClass())) {
                    return out;
                }
            } catch (Exception ignored) {
                // try the next candidate
            }
        }
        return null;
    }

    private void animate(Object tracker, String animation) {
        try {
            Method m = tracker.getClass().getMethod("animate", String.class);
            m.invoke(tracker, animation);
            return;
        } catch (Exception ignored) {
            // fall through to a modifier overload
        }
        for (Method m : tracker.getClass().getMethods()) {
            if (m.getName().equals("animate") && m.getParameterCount() == 2
                    && m.getParameterTypes()[0] == String.class) {
                try {
                    m.invoke(tracker, animation, defaultModifier(m.getParameterTypes()[1]));
                    return;
                } catch (Exception ignored) {
                    // give up quietly; the model still spawned
                }
            }
        }
    }

    private Object defaultModifier(Class<?> modifierType) {
        try {
            return modifierType.getField("DEFAULT").get(null);
        } catch (Exception e) {
            return null;
        }
    }

    private Class<?> findClass() {
        for (String cn : BETTERMODEL_CLASSES) {
            try {
                return Class.forName(cn);
            } catch (ClassNotFoundException ignored) {
                // try the next candidate
            }
        }
        return null;
    }

    private ArmorStand spawnCarrier(Location at) {
        return at.getWorld().spawn(at, ArmorStand.class, a -> {
            a.setInvisible(true);
            a.setMarker(true);
            a.setGravity(false);
            a.setSmall(true);
        });
    }

    private boolean failOnce(Entity carrier, boolean spawnedCarrier, String reason) {
        if (spawnedCarrier && carrier != null && carrier.getType() == EntityType.ARMOR_STAND) {
            carrier.remove();
        }
        if (!warned) {
            warned = true;
            plugin.getLogger().log(Level.WARNING,
                    "BetterModel present but its API could not be driven reflectively (" + reason
                            + "). Falling back to display entities. Wire against the BetterModel API jar to finalize.");
        }
        return false;
    }
}

package com.signalvfx.plugin.vfx;

import com.signalvfx.model.visual.BetterModelVisual;
import com.signalvfx.model.visual.DisplayEntityVisual;
import com.signalvfx.model.visual.ResourcePackVisual;
import com.signalvfx.model.visual.Visual;
import com.signalvfx.plugin.cast.Target;
import com.signalvfx.plugin.util.Coords;
import com.signalvfx.plugin.util.Keys;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

/**
 * Chooses and drives a renderer for a skill's {@link Visual}. BetterModel is
 * used when available; otherwise (or on failure) it falls back to a
 * display-entity render so the skill still shows.
 */
public final class VfxService {

    private final JavaPlugin plugin;
    private final DisplayEntityRenderer displayRenderer;
    private final BetterModelRenderer betterModel;

    public VfxService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.displayRenderer = new DisplayEntityRenderer(plugin);
        this.betterModel = new BetterModelRenderer(plugin);
    }

    public boolean isBetterModelAvailable() {
        return betterModel.isAvailable();
    }

    public void play(Player caster, Target target, Visual visual) {
        if (visual == null) {
            return;
        }
        Location anchor = anchorFor(caster, target, visual);
        playSound(anchor, visual);

        if (visual instanceof BetterModelVisual bm) {
            boolean ok = betterModel.render(caster, anchor, bm);
            if (!ok) {
                // Fall back to a simple item display so something still appears.
                displayRenderer.renderFallback(anchor, bm.getDurationTicks());
            }
        } else if (visual instanceof DisplayEntityVisual de) {
            displayRenderer.render(anchor, de);
        } else if (visual instanceof ResourcePackVisual rp) {
            renderResourcePack(anchor, rp);
        }
    }

    private Location anchorFor(Player caster, Target target, Visual visual) {
        Location base = switch (visual.getAttach()) {
            case CASTER, PROJECTILE, WORLD -> caster.getEyeLocation();
            case TARGET -> target.origin().clone();
        };
        Vector forward = caster.getEyeLocation().getDirection();
        return Coords.localToWorld(base, forward, visual.getOffset());
    }

    private void renderResourcePack(Location anchor, ResourcePackVisual rp) {
        Material mat = Keys.material(rp.getMaterial());
        if (mat == null) {
            mat = Material.PAPER;
        }
        ItemStack stack = new ItemStack(mat);
        if (rp.getCustomModelData() > 0) {
            ItemMeta meta = stack.getItemMeta();
            meta.setCustomModelData(rp.getCustomModelData());
            stack.setItemMeta(meta);
        }
        ItemDisplay display = anchor.getWorld().spawn(anchor, ItemDisplay.class, d -> {
            d.setItemStack(stack);
            float s = (float) rp.getScale();
            d.setTransformation(new org.bukkit.util.Transformation(
                    new org.joml.Vector3f(0, 0, 0),
                    new org.joml.Quaternionf(),
                    new org.joml.Vector3f(s, s, s),
                    new org.joml.Quaternionf()));
        });
        int life = Math.max(1, rp.getDurationTicks());
        plugin.getServer().getScheduler().runTaskLater(plugin, display::remove, life);
    }

    private void playSound(Location at, Visual visual) {
        String key = visual.getSoundKey();
        if (key == null || key.isBlank()) {
            return;
        }
        try {
            Sound sound = org.bukkit.Registry.SOUNDS.get(
                    org.bukkit.NamespacedKey.fromString(key.contains(":") ? key : "minecraft:" + key));
            if (sound != null) {
                at.getWorld().playSound(at, sound, visual.getSoundVolume(), visual.getSoundPitch());
            }
        } catch (Exception ignored) {
            // Unknown sound key — silently skip.
        }
    }
}

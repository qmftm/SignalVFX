package com.signalvfx.plugin.vfx;

import com.signalvfx.model.Vec3;
import com.signalvfx.model.visual.DisplayEntityVisual;
import com.signalvfx.model.visual.Keyframe;
import com.signalvfx.model.visual.Transform;
import com.signalvfx.plugin.util.Keys;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Renders a {@link DisplayEntityVisual} with vanilla display entities: applies
 * the base transform, billboard, brightness and glow, interpolates any
 * keyframes, and removes the entity after its lifetime.
 */
public final class DisplayEntityRenderer {

    private final JavaPlugin plugin;

    public DisplayEntityRenderer(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void render(Location at, DisplayEntityVisual v) {
        Display display = spawn(at, v);
        if (display == null) {
            return;
        }
        applyCommon(display, v);
        display.setTransformation(toTransformation(v.getBaseTransform()));
        scheduleKeyframes(display, v);

        int life = Math.max(1, v.getDurationTicks());
        Bukkit.getScheduler().runTaskLater(plugin, display::remove, life);
    }

    /** A minimal placeholder display used when a BetterModel render is unavailable. */
    public void renderFallback(Location at, int durationTicks) {
        ItemDisplay display = at.getWorld().spawn(at, ItemDisplay.class, d -> {
            d.setItemStack(new ItemStack(Material.END_ROD));
            d.setBillboard(Display.Billboard.CENTER);
            d.setGlowing(true);
        });
        Bukkit.getScheduler().runTaskLater(plugin, display::remove, Math.max(1, durationTicks));
    }

    private Display spawn(Location at, DisplayEntityVisual v) {
        return switch (v.getDisplayKind()) {
            case ITEM -> at.getWorld().spawn(at, ItemDisplay.class, d -> d.setItemStack(itemStack(v)));
            case BLOCK -> {
                Material mat = Keys.material(v.getBlock());
                Material block = mat != null && mat.isBlock() ? mat : Material.MAGMA_BLOCK;
                yield at.getWorld().spawn(at, BlockDisplay.class, d -> d.setBlock(block.createBlockData()));
            }
            case TEXT -> at.getWorld().spawn(at, TextDisplay.class, d -> d.setText(v.getText()));
        };
    }

    private ItemStack itemStack(DisplayEntityVisual v) {
        Material mat = Keys.material(v.getItem());
        ItemStack stack = new ItemStack(mat != null ? mat : Material.BLAZE_ROD);
        if (v.getCustomModelData() > 0) {
            ItemMeta meta = stack.getItemMeta();
            meta.setCustomModelData(v.getCustomModelData());
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private void applyCommon(Display display, DisplayEntityVisual v) {
        try {
            display.setBillboard(Display.Billboard.valueOf(v.getBillboard().name()));
        } catch (IllegalArgumentException ignored) {
            display.setBillboard(Display.Billboard.CENTER);
        }
        if (v.getBlockLight() >= 0 || v.getSkyLight() >= 0) {
            display.setBrightness(new Display.Brightness(
                    clampLight(v.getBlockLight()), clampLight(v.getSkyLight())));
        }
        if (v.isGlowing()) {
            display.setGlowing(true);
            Color color = parseColor(v.getGlowColor());
            if (color != null) {
                display.setGlowColorOverride(color);
            }
        }
    }

    private void scheduleKeyframes(Display display, DisplayEntityVisual v) {
        for (Keyframe kf : v.getKeyframes()) {
            long at = Math.max(0, kf.getAtTick());
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!display.isValid()) {
                    return;
                }
                display.setInterpolationDelay(0);
                display.setInterpolationDuration(Math.max(0, kf.getInterpolationDurationTicks()));
                display.setTransformation(toTransformation(kf.getTransform()));
            }, at);
        }
    }

    private org.bukkit.util.Transformation toTransformation(Transform t) {
        return new org.bukkit.util.Transformation(
                vec(t.getTranslation()),
                quat(t.getLeftRotation()),
                vec(t.getScale()),
                quat(t.getRightRotation()));
    }

    private Vector3f vec(Vec3 v) {
        return new Vector3f((float) v.getX(), (float) v.getY(), (float) v.getZ());
    }

    private Quaternionf quat(Vec3 eulerDeg) {
        return new Quaternionf().rotationXYZ(
                (float) Math.toRadians(eulerDeg.getX()),
                (float) Math.toRadians(eulerDeg.getY()),
                (float) Math.toRadians(eulerDeg.getZ()));
    }

    private int clampLight(int v) {
        return Math.max(0, Math.min(15, v));
    }

    private Color parseColor(String hex) {
        if (hex == null || hex.isBlank()) {
            return null;
        }
        try {
            String h = hex.startsWith("#") ? hex.substring(1) : hex;
            return Color.fromRGB(Integer.parseInt(h, 16));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

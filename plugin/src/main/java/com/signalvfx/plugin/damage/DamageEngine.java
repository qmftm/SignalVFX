package com.signalvfx.plugin.damage;

import com.signalvfx.model.DamagePoint;
import com.signalvfx.model.PotionEffectSpec;
import com.signalvfx.model.Shape;
import com.signalvfx.model.TargetFilter;
import com.signalvfx.plugin.util.Coords;
import com.signalvfx.plugin.util.Keys;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Collection;

/**
 * Resolves a damage point into actual damage: schedules it by delay/repeat,
 * collects entities inside its {@link Shape}, filters them, and applies damage,
 * knockback and potion effects.
 */
public final class DamageEngine {

    private final JavaPlugin plugin;

    public DamageEngine(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** Schedules a damage point relative to {@code base}/{@code forward} (snapshotted at cast). */
    public void schedule(Player caster, Location base, Vector forward, DamagePoint dp) {
        int count = Math.max(1, dp.getRepeatCount());
        int interval = Math.max(0, dp.getRepeatIntervalTicks());
        for (int k = 0; k < count; k++) {
            long delay = Math.max(0, dp.getDelayTicks()) + (long) k * interval;
            Bukkit.getScheduler().runTaskLater(plugin, () -> resolve(caster, base, forward, dp), delay);
        }
    }

    private void resolve(Player caster, Location base, Vector forward, DamagePoint dp) {
        if (base.getWorld() == null) {
            return;
        }
        Location center = Coords.localToWorld(base, forward, dp.getOffset());
        for (LivingEntity target : collect(center, forward, dp.getShape())) {
            if (!passesFilter(caster, target, dp.getTargetFilter())) {
                continue;
            }
            applyTo(caster, center, target, dp);
        }
    }

    private Collection<LivingEntity> collect(Location center, Vector forward, Shape shape) {
        return switch (shape.getType()) {
            case SPHERE -> {
                double r = shape.getRadius();
                Collection<LivingEntity> near = center.getWorld().getNearbyLivingEntities(center, r);
                near.removeIf(e -> e.getLocation().distanceSquared(center) > r * r);
                yield near;
            }
            case BOX -> center.getWorld().getNearbyLivingEntities(center,
                    shape.getHalfExtents().getX(), shape.getHalfExtents().getY(), shape.getHalfExtents().getZ());
            case CONE -> {
                double len = shape.getLength();
                double cos = Math.cos(Math.toRadians(shape.getAngle()));
                Vector f = forward.clone().normalize();
                Collection<LivingEntity> near = center.getWorld().getNearbyLivingEntities(center, len);
                near.removeIf(e -> {
                    Vector to = e.getLocation().toVector().subtract(center.toVector());
                    if (to.lengthSquared() > len * len || to.lengthSquared() < 1.0e-6) {
                        return to.lengthSquared() > len * len;
                    }
                    return f.dot(to.normalize()) < cos;
                });
                yield near;
            }
        };
    }

    private boolean passesFilter(Player caster, LivingEntity target, TargetFilter filter) {
        boolean isCaster = target.equals(caster);
        return switch (filter) {
            case SELF -> isCaster;
            case ALL -> true;
            // No team/party model yet: ALLIES behaves like "not the caster".
            case ENEMIES, ALLIES, NOT_CASTER -> !isCaster;
        };
    }

    private void applyTo(Player caster, Location center, LivingEntity target, DamagePoint dp) {
        if (dp.isIgnoreInvulnerability()) {
            target.setNoDamageTicks(0);
        }
        if (dp.getDamage() > 0) {
            target.damage(dp.getDamage(), caster);
        }
        if (dp.getKnockback() > 0) {
            Vector push = target.getLocation().toVector().subtract(center.toVector());
            push.setY(0);
            if (push.lengthSquared() < 1.0e-6) {
                push = new Vector(0, 0, 1);
            }
            push.normalize().multiply(dp.getKnockback());
            push.setY(0.35 * dp.getKnockback());
            target.setVelocity(target.getVelocity().add(push));
        }
        for (PotionEffectSpec spec : dp.getPotionEffects()) {
            PotionEffectType type = Keys.potion(spec.getType());
            if (type != null && spec.getDurationTicks() > 0) {
                target.addPotionEffect(new PotionEffect(type, spec.getDurationTicks(), spec.getAmplifier()));
            }
        }
    }
}

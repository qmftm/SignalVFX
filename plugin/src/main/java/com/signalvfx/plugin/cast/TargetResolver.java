package com.signalvfx.plugin.cast;

import com.signalvfx.model.CastSettings;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/**
 * Resolves the {@link Target} for a cast from its {@link com.signalvfx.model.Targeting}.
 * PROJECTILE resolves like TARGET_ENTITY here (hitscan); travelling projectiles
 * are driven by the VFX/cast layer.
 */
public final class TargetResolver {

    public Target resolve(Player caster, CastSettings cast) {
        Location eye = caster.getEyeLocation();
        Vector dir = eye.getDirection();
        double range = Math.max(cast.getRange(), 1.0);

        return switch (cast.getTargeting()) {
            case SELF -> new Target(caster.getLocation(), dir, caster);
            case TARGET_ENTITY, PROJECTILE -> {
                RayTraceResult hit = caster.getWorld().rayTraceEntities(eye, dir, range, 0.4,
                        e -> e instanceof LivingEntity && !e.equals(caster));
                if (hit != null && hit.getHitEntity() instanceof LivingEntity le) {
                    yield new Target(le.getLocation(), dir, le);
                }
                yield new Target(eye.clone().add(dir.clone().multiply(range)), dir, null);
            }
            case TARGET_BLOCK -> {
                RayTraceResult hit = caster.getWorld().rayTraceBlocks(eye, dir, range,
                        FluidCollisionMode.NEVER, true);
                Location loc = hit != null && hit.getHitPosition() != null
                        ? hit.getHitPosition().toLocation(caster.getWorld())
                        : eye.clone().add(dir.clone().multiply(range));
                yield new Target(loc, dir, null);
            }
        };
    }
}

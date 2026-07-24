package com.signalvfx.plugin.cast;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

/**
 * The resolved aim of a cast: a world origin, the caster's facing direction, and
 * (optionally) a locked-on entity.
 */
public record Target(Location origin, Vector direction, LivingEntity entity) {
}

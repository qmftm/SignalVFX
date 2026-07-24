package com.signalvfx.model.visual;

/**
 * Where a visual is anchored while it plays.
 */
public enum Attach {
    /** Locked to the caster's location/hand. */
    CASTER,
    /** Locked to the resolved target. */
    TARGET,
    /** Travels along the cast direction (beams / projectiles). */
    PROJECTILE,
    /** Spawned at a fixed world offset and left in place. */
    WORLD
}

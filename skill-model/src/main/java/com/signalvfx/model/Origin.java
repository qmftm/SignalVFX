package com.signalvfx.model;

/**
 * The reference frame that an offset (for a damage point or a visual) is
 * measured against when the skill is cast.
 */
public enum Origin {
    /** Relative to the caster's location, oriented to the caster's facing. */
    CASTER,
    /** Relative to the resolved target (entity or block the skill locked onto). */
    TARGET,
    /** Relative to the caster, but travelling along the cast direction (projectiles / beams). */
    CAST_DIRECTION
}

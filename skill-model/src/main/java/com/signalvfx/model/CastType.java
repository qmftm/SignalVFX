package com.signalvfx.model;

/**
 * How the skill is triggered and how long it takes to go off.
 */
public enum CastType {
    /** Fires immediately when triggered. */
    INSTANT,
    /** Requires holding for {@code castTimeTicks} before firing (a wind-up). */
    CHARGE,
    /** Fires repeatedly while the caster keeps channeling. */
    CHANNEL
}

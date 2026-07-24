package com.signalvfx.model;

/**
 * Which living entities a damage point is allowed to affect.
 */
public enum TargetFilter {
    /** Everything except the caster. */
    ENEMIES,
    /** Entities the caster is allied with (team / party); caster excluded. */
    ALLIES,
    /** The caster themselves only. */
    SELF,
    /** Every living entity in range, including the caster. */
    ALL,
    /** Every living entity in range except the caster. */
    NOT_CASTER
}

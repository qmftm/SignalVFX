package com.signalvfx.model;

/**
 * How the skill resolves the {@link Origin#TARGET} reference frame.
 */
public enum Targeting {
    /** No external target; everything is relative to the caster. */
    SELF,
    /** Ray-traces to the first living entity within {@code range}. */
    TARGET_ENTITY,
    /** Ray-traces to the first solid block within {@code range}. */
    TARGET_BLOCK,
    /** Fires a projectile along the cast direction; target resolves on impact. */
    PROJECTILE
}

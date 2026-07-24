package com.signalvfx.model;

/**
 * The geometric volume used to select entities around a damage point.
 */
public enum ShapeType {
    /** A sphere of {@code radius} around the point. */
    SPHERE,
    /** An axis-aligned box defined by {@code halfExtents}. */
    BOX,
    /** A cone of {@code angle} degrees and {@code length}, opening along the cast direction. */
    CONE
}

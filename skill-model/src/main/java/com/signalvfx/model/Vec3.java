package com.signalvfx.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * An immutable 3D vector. Used for offsets, scales, extents and translations.
 *
 * <p>Coordinates follow Minecraft conventions: {@code x} east/west,
 * {@code y} up/down, {@code z} north/south. Values are relative to whatever
 * reference frame the containing object declares (see {@link Origin}).
 */
public final class Vec3 {

    public static final Vec3 ZERO = new Vec3(0, 0, 0);
    public static final Vec3 ONE = new Vec3(1, 1, 1);

    private final double x;
    private final double y;
    private final double z;

    @JsonCreator
    public Vec3(@JsonProperty("x") double x,
                @JsonProperty("y") double y,
                @JsonProperty("z") double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public Vec3 withX(double x) {
        return new Vec3(x, y, z);
    }

    public Vec3 withY(double y) {
        return new Vec3(x, y, z);
    }

    public Vec3 withZ(double z) {
        return new Vec3(x, y, z);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Vec3 other)) {
            return false;
        }
        return Double.compare(x, other.x) == 0
                && Double.compare(y, other.y) == 0
                && Double.compare(z, other.z) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }
}

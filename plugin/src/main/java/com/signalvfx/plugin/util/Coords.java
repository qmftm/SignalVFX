package com.signalvfx.plugin.util;

import com.signalvfx.model.Vec3;
import org.bukkit.Location;
import org.bukkit.util.Vector;

/**
 * Maps editor-space offsets (local frame: +Z forward, +X right, +Y up — matching
 * the editor's canvas and 3D preview) into world locations relative to a base
 * point and a facing direction.
 */
public final class Coords {

    private Coords() {
    }

    /**
     * @param base    the reference world location
     * @param forward the facing direction (need not be normalized)
     * @param offset  local offset in blocks (+Z forward, +X right, +Y up)
     */
    public static Location localToWorld(Location base, Vector forward, Vec3 offset) {
        Vector f = forward.clone();
        if (f.lengthSquared() < 1.0e-6) {
            f = new Vector(0, 0, 1);
        }
        f.normalize();
        Vector up = new Vector(0, 1, 0);
        Vector right = f.clone().crossProduct(up);
        if (right.lengthSquared() < 1.0e-6) {
            // Forward is (near) vertical; pick an arbitrary horizontal right axis.
            right = new Vector(1, 0, 0);
        }
        right.normalize();
        Vector realUp = right.clone().crossProduct(f).normalize();

        Vector world = right.multiply(offset.getX())
                .add(realUp.multiply(offset.getY()))
                .add(f.multiply(offset.getZ()));
        return base.clone().add(world);
    }

    public static Vector toVector(Vec3 v) {
        return new Vector(v.getX(), v.getY(), v.getZ());
    }
}

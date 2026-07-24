package com.signalvfx.model;

/**
 * The volume of a damage point. The relevant fields depend on {@link #getType()}:
 * <ul>
 *     <li>{@link ShapeType#SPHERE} — uses {@code radius}.</li>
 *     <li>{@link ShapeType#BOX} — uses {@code halfExtents}.</li>
 *     <li>{@link ShapeType#CONE} — uses {@code angle} (degrees) and {@code length}.</li>
 * </ul>
 * Unused fields are kept at sensible defaults so the shape can be switched in
 * the editor without losing previously entered values.
 */
public class Shape {

    private ShapeType type = ShapeType.SPHERE;
    private double radius = 3.0;
    private Vec3 halfExtents = new Vec3(2, 2, 2);
    private double angle = 45.0;
    private double length = 5.0;

    public Shape() {
    }

    public static Shape sphere(double radius) {
        Shape s = new Shape();
        s.type = ShapeType.SPHERE;
        s.radius = radius;
        return s;
    }

    public ShapeType getType() {
        return type;
    }

    public void setType(ShapeType type) {
        this.type = type;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public Vec3 getHalfExtents() {
        return halfExtents;
    }

    public void setHalfExtents(Vec3 halfExtents) {
        this.halfExtents = halfExtents;
    }

    public double getAngle() {
        return angle;
    }

    public void setAngle(double angle) {
        this.angle = angle;
    }

    public double getLength() {
        return length;
    }

    public void setLength(double length) {
        this.length = length;
    }

    /**
     * A rough top-down (X/Z plane) radius used purely for editor previews.
     */
    public double approximatePlanarRadius() {
        return switch (type) {
            case SPHERE -> radius;
            case BOX -> Math.max(halfExtents.getX(), halfExtents.getZ());
            case CONE -> length;
        };
    }
}

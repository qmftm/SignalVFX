package com.signalvfx.model.visual;

import com.signalvfx.model.Vec3;

/**
 * A display entity transform: translation, scale, and left/right rotations
 * expressed as Euler angles in degrees (converted to quaternions by the
 * plugin). Matches the vanilla display transform decomposition.
 */
public class Transform {

    private Vec3 translation = Vec3.ZERO;
    private Vec3 scale = Vec3.ONE;
    /** Left rotation (applied before scale), Euler XYZ degrees. */
    private Vec3 leftRotation = Vec3.ZERO;
    /** Right rotation (applied after scale), Euler XYZ degrees. */
    private Vec3 rightRotation = Vec3.ZERO;

    public Transform() {
    }

    public Vec3 getTranslation() {
        return translation;
    }

    public void setTranslation(Vec3 translation) {
        this.translation = translation;
    }

    public Vec3 getScale() {
        return scale;
    }

    public void setScale(Vec3 scale) {
        this.scale = scale;
    }

    public Vec3 getLeftRotation() {
        return leftRotation;
    }

    public void setLeftRotation(Vec3 leftRotation) {
        this.leftRotation = leftRotation;
    }

    public Vec3 getRightRotation() {
        return rightRotation;
    }

    public void setRightRotation(Vec3 rightRotation) {
        this.rightRotation = rightRotation;
    }
}

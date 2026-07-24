package com.signalvfx.model.visual;

/**
 * One animation keyframe for a {@link DisplayEntityVisual}. The plugin
 * interpolates the display's transform toward this keyframe's {@link Transform}
 * over {@link #interpolationDurationTicks}, starting at {@link #atTick}.
 */
public class Keyframe {

    /** Ticks from the start of the visual at which this keyframe begins. */
    private int atTick = 0;
    /** Vanilla interpolation duration used to reach this keyframe's transform. */
    private int interpolationDurationTicks = 10;
    private Transform transform = new Transform();

    public Keyframe() {
    }

    public int getAtTick() {
        return atTick;
    }

    public void setAtTick(int atTick) {
        this.atTick = atTick;
    }

    public int getInterpolationDurationTicks() {
        return interpolationDurationTicks;
    }

    public void setInterpolationDurationTicks(int interpolationDurationTicks) {
        this.interpolationDurationTicks = interpolationDurationTicks;
    }

    public Transform getTransform() {
        return transform;
    }

    public void setTransform(Transform transform) {
        this.transform = transform;
    }
}

package com.signalvfx.model.visual;

/**
 * A visual rendered by the <a href="https://modrinth.com/plugin/bettermodel">BetterModel</a>
 * plugin: a server-side BlockBench model with named animations. SignalVFX only
 * references the model and the animation to play; BetterModel owns the geometry,
 * animation playback and the auto-generated resource pack.
 *
 * <p>The plugin soft-depends on BetterModel. When it is absent, a skill using
 * this visual should fall back to a simpler kind (display-entity / resource
 * pack) so the skill still functions without the rich model.
 */
public class BetterModelVisual extends Visual {

    /** Registered BetterModel model id (the {@code .bbmodel} name). */
    private String modelId = "";

    /** Named animation to play when the model spawns (empty = none). */
    private String animation = "";

    /** Animation playback speed multiplier. */
    private double animationSpeed = 1.0;

    /** Whether the animation loops for the visual's lifetime. */
    private boolean loop = false;

    /** Uniform scale applied to the spawned model. */
    private double scale = 1.0;

    /** Use the model's own hit-box (BetterModel) instead of SignalVFX damage ranges. */
    private boolean useModelHitbox = false;

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getAnimation() {
        return animation;
    }

    public void setAnimation(String animation) {
        this.animation = animation;
    }

    public double getAnimationSpeed() {
        return animationSpeed;
    }

    public void setAnimationSpeed(double animationSpeed) {
        this.animationSpeed = animationSpeed;
    }

    public boolean isLoop() {
        return loop;
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        this.scale = scale;
    }

    public boolean isUseModelHitbox() {
        return useModelHitbox;
    }

    public void setUseModelHitbox(boolean useModelHitbox) {
        this.useModelHitbox = useModelHitbox;
    }

    @Override
    public String kindLabel() {
        return "BetterModel";
    }
}

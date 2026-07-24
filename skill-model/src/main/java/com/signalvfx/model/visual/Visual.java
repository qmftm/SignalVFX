package com.signalvfx.model.visual;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.signalvfx.model.Vec3;

/**
 * The visual "skin" of a skill. A skill picks exactly one of the concrete
 * kinds — a {@link ResourcePackVisual} (custom models/animations shipped in a
 * resource pack) or a {@link DisplayEntityVisual} (server-spawned display
 * entities, no client-side pack required).
 *
 * <p>The {@code type} discriminator in JSON selects the concrete subtype.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ResourcePackVisual.class, name = "RESOURCE_PACK"),
        @JsonSubTypes.Type(value = DisplayEntityVisual.class, name = "DISPLAY_ENTITY")
})
public abstract class Visual {

    private Attach attach = Attach.CASTER;
    private Vec3 offset = Vec3.ZERO;

    /** How long the visual lives, in ticks (20 ticks = 1s). */
    private int durationTicks = 20;

    /** Optional sound played when the visual starts (vanilla or namespaced key). */
    private String soundKey = "";
    private float soundVolume = 1.0f;
    private float soundPitch = 1.0f;

    public Attach getAttach() {
        return attach;
    }

    public void setAttach(Attach attach) {
        this.attach = attach;
    }

    public Vec3 getOffset() {
        return offset;
    }

    public void setOffset(Vec3 offset) {
        this.offset = offset;
    }

    public int getDurationTicks() {
        return durationTicks;
    }

    public void setDurationTicks(int durationTicks) {
        this.durationTicks = durationTicks;
    }

    public String getSoundKey() {
        return soundKey;
    }

    public void setSoundKey(String soundKey) {
        this.soundKey = soundKey;
    }

    public float getSoundVolume() {
        return soundVolume;
    }

    public void setSoundVolume(float soundVolume) {
        this.soundVolume = soundVolume;
    }

    public float getSoundPitch() {
        return soundPitch;
    }

    public void setSoundPitch(float soundPitch) {
        this.soundPitch = soundPitch;
    }

    /** A short human-readable label for the editor's visual-kind selector. */
    @JsonIgnore
    public abstract String kindLabel();
}

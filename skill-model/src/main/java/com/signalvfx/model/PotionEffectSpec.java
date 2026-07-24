package com.signalvfx.model;

/**
 * An optional potion effect applied to entities hit by a damage point.
 * {@code type} is a vanilla effect key such as {@code minecraft:slowness}.
 */
public class PotionEffectSpec {

    private String type = "minecraft:slowness";
    private int amplifier = 0;
    private int durationTicks = 100;

    public PotionEffectSpec() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getAmplifier() {
        return amplifier;
    }

    public void setAmplifier(int amplifier) {
        this.amplifier = amplifier;
    }

    public int getDurationTicks() {
        return durationTicks;
    }

    public void setDurationTicks(int durationTicks) {
        this.durationTicks = durationTicks;
    }
}

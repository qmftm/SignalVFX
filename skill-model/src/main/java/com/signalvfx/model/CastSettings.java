package com.signalvfx.model;

/**
 * Trigger, timing and cost settings for a skill.
 */
public class CastSettings {

    private CastType type = CastType.INSTANT;
    private Targeting targeting = Targeting.TARGET_ENTITY;

    /** Wind-up time for {@link CastType#CHARGE}, or tick interval for {@link CastType#CHANNEL}. */
    private int castTimeTicks = 0;
    /** Cooldown before the skill can be cast again (20 ticks = 1s). */
    private int cooldownTicks = 40;
    /** Maximum distance for target resolution / projectile travel, in blocks. */
    private double range = 20.0;

    private Cost cost = new Cost();

    public CastSettings() {
    }

    public CastType getType() {
        return type;
    }

    public void setType(CastType type) {
        this.type = type;
    }

    public Targeting getTargeting() {
        return targeting;
    }

    public void setTargeting(Targeting targeting) {
        this.targeting = targeting;
    }

    public int getCastTimeTicks() {
        return castTimeTicks;
    }

    public void setCastTimeTicks(int castTimeTicks) {
        this.castTimeTicks = castTimeTicks;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    public void setCooldownTicks(int cooldownTicks) {
        this.cooldownTicks = cooldownTicks;
    }

    public double getRange() {
        return range;
    }

    public void setRange(double range) {
        this.range = range;
    }

    public Cost getCost() {
        return cost;
    }

    public void setCost(Cost cost) {
        this.cost = cost;
    }
}

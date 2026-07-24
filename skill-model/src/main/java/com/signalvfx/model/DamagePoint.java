package com.signalvfx.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A single damage locus of a skill. Editor users "place a point and set a
 * range"; that is captured here as an {@link #offset} from an {@link Origin}
 * plus a {@link Shape} that defines the affected volume.
 *
 * <p>A skill may have any number of damage points, each with its own delay so
 * that multi-stage attacks (a slash, then an explosion) can be authored.
 */
public class DamagePoint {

    private String id = UUID.randomUUID().toString().substring(0, 8);
    private String label = "Damage Point";

    private Origin origin = Origin.CASTER;
    private Vec3 offset = new Vec3(0, 0, 2);
    private Shape shape = Shape.sphere(3.0);

    /** Damage dealt to each affected entity, in half-hearts (Minecraft health). */
    private double damage = 6.0;

    /** Ticks after the skill starts before this point resolves (20 ticks = 1s). */
    private int delayTicks = 0;

    /** How many times the point fires, spaced by {@link #repeatIntervalTicks}. */
    private int repeatCount = 1;
    private int repeatIntervalTicks = 0;

    private TargetFilter targetFilter = TargetFilter.NOT_CASTER;

    /** Outward knockback strength from the point (0 disables). */
    private double knockback = 0.0;

    /** Whether to ignore i-frames / invulnerability ticks when applying damage. */
    private boolean ignoreInvulnerability = false;

    private List<PotionEffectSpec> potionEffects = new ArrayList<>();

    public DamagePoint() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Origin getOrigin() {
        return origin;
    }

    public void setOrigin(Origin origin) {
        this.origin = origin;
    }

    public Vec3 getOffset() {
        return offset;
    }

    public void setOffset(Vec3 offset) {
        this.offset = offset;
    }

    public Shape getShape() {
        return shape;
    }

    public void setShape(Shape shape) {
        this.shape = shape;
    }

    public double getDamage() {
        return damage;
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }

    public int getDelayTicks() {
        return delayTicks;
    }

    public void setDelayTicks(int delayTicks) {
        this.delayTicks = delayTicks;
    }

    public int getRepeatCount() {
        return repeatCount;
    }

    public void setRepeatCount(int repeatCount) {
        this.repeatCount = repeatCount;
    }

    public int getRepeatIntervalTicks() {
        return repeatIntervalTicks;
    }

    public void setRepeatIntervalTicks(int repeatIntervalTicks) {
        this.repeatIntervalTicks = repeatIntervalTicks;
    }

    public TargetFilter getTargetFilter() {
        return targetFilter;
    }

    public void setTargetFilter(TargetFilter targetFilter) {
        this.targetFilter = targetFilter;
    }

    public double getKnockback() {
        return knockback;
    }

    public void setKnockback(double knockback) {
        this.knockback = knockback;
    }

    public boolean isIgnoreInvulnerability() {
        return ignoreInvulnerability;
    }

    public void setIgnoreInvulnerability(boolean ignoreInvulnerability) {
        this.ignoreInvulnerability = ignoreInvulnerability;
    }

    public List<PotionEffectSpec> getPotionEffects() {
        return potionEffects;
    }

    public void setPotionEffects(List<PotionEffectSpec> potionEffects) {
        this.potionEffects = potionEffects;
    }
}

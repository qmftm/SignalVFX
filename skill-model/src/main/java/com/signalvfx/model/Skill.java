package com.signalvfx.model;

import com.signalvfx.model.visual.ResourcePackVisual;
import com.signalvfx.model.visual.Visual;

import java.util.ArrayList;
import java.util.List;

/**
 * The root of a skill/spell definition. This is the document the editor reads
 * and writes, and the plugin loads at runtime. Keep it JSON-friendly: plain
 * getters/setters, no behavior.
 *
 * <p>Schema version {@link #SCHEMA_VERSION} lets the plugin/editor migrate
 * older files as the format evolves.
 */
public class Skill {

    public static final int SCHEMA_VERSION = 1;

    private int schemaVersion = SCHEMA_VERSION;

    /** Stable machine id, e.g. {@code fireball}. Used by commands and configs. */
    private String id = "new_skill";
    private String name = "New Skill";
    private String description = "";
    private String author = "";

    /** Icon item shown in menus/editor, e.g. {@code minecraft:fire_charge}. */
    private String icon = "minecraft:fire_charge";

    private CastSettings cast = new CastSettings();

    /**
     * The single chosen visual. Defaults to a resource-pack visual, the
     * intended primary path (the plugin hosts/serves the pack); a
     * display-entity visual is the pack-free alternative.
     */
    private Visual visual = new ResourcePackVisual();

    private List<DamagePoint> damagePoints = new ArrayList<>();

    public Skill() {
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public CastSettings getCast() {
        return cast;
    }

    public void setCast(CastSettings cast) {
        this.cast = cast;
    }

    public Visual getVisual() {
        return visual;
    }

    public void setVisual(Visual visual) {
        this.visual = visual;
    }

    public List<DamagePoint> getDamagePoints() {
        return damagePoints;
    }

    public void setDamagePoints(List<DamagePoint> damagePoints) {
        this.damagePoints = damagePoints;
    }
}

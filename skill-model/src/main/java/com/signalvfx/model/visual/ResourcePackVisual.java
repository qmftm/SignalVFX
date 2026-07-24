package com.signalvfx.model.visual;

/**
 * A visual driven by assets shipped in a server resource pack: a custom item
 * model (via {@code customModelData} or an {@code itemModel} key) and/or a
 * player animation trigger.
 *
 * <p>The plugin renders this by giving the caster/target an item with the
 * configured model, or by playing the referenced animation. Nothing here is
 * spawned server-side beyond that item, so the look is fully authored in the
 * pack.
 */
public class ResourcePackVisual extends Visual {

    /** Base item that carries the custom model, e.g. {@code minecraft:paper}. */
    private String material = "minecraft:paper";

    /** Legacy custom-model-data selector (pre-1.21.4 packs). */
    private int customModelData = 0;

    /** Modern item-model key (1.21.4+), e.g. {@code signalvfx:fireball}. Wins over CMD when set. */
    private String itemModel = "";

    /** Optional animation/state name the pack listens for (e.g. an animated model state). */
    private String animation = "";

    /** Uniform display scale applied to the held/dropped model. */
    private double scale = 1.0;

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public void setCustomModelData(int customModelData) {
        this.customModelData = customModelData;
    }

    public String getItemModel() {
        return itemModel;
    }

    public void setItemModel(String itemModel) {
        this.itemModel = itemModel;
    }

    public String getAnimation() {
        return animation;
    }

    public void setAnimation(String animation) {
        this.animation = animation;
    }

    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        this.scale = scale;
    }

    @Override
    public String kindLabel() {
        return "Resource Pack";
    }
}

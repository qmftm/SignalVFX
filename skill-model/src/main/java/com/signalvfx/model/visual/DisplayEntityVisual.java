package com.signalvfx.model.visual;

import java.util.ArrayList;
import java.util.List;

/**
 * A visual built from server-spawned display entities. Requires no client
 * resource pack: the look comes from an item/block/text plus a base
 * {@link Transform} and an optional list of {@link Keyframe}s that the plugin
 * interpolates over time.
 */
public class DisplayEntityVisual extends Visual {

    private DisplayKind displayKind = DisplayKind.ITEM;

    /** Item key when {@link DisplayKind#ITEM}, e.g. {@code minecraft:blaze_rod}. */
    private String item = "minecraft:blaze_rod";
    /** Block key when {@link DisplayKind#BLOCK}, e.g. {@code minecraft:magma_block}. */
    private String block = "minecraft:magma_block";
    /** Text when {@link DisplayKind#TEXT}. */
    private String text = "";

    /** Optional custom-model-data on the displayed item (0 = none). */
    private int customModelData = 0;

    private Billboard billboard = Billboard.CENTER;

    /** Block-light override 0-15, or -1 to use ambient lighting. */
    private int blockLight = -1;
    /** Sky-light override 0-15, or -1 to use ambient lighting. */
    private int skyLight = -1;

    private boolean glowing = false;
    /** Glow color as {@code #RRGGBB}; empty uses the team/default color. */
    private String glowColor = "";

    private Transform baseTransform = new Transform();
    private List<Keyframe> keyframes = new ArrayList<>();

    public DisplayKind getDisplayKind() {
        return displayKind;
    }

    public void setDisplayKind(DisplayKind displayKind) {
        this.displayKind = displayKind;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public String getBlock() {
        return block;
    }

    public void setBlock(String block) {
        this.block = block;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public void setCustomModelData(int customModelData) {
        this.customModelData = customModelData;
    }

    public Billboard getBillboard() {
        return billboard;
    }

    public void setBillboard(Billboard billboard) {
        this.billboard = billboard;
    }

    public int getBlockLight() {
        return blockLight;
    }

    public void setBlockLight(int blockLight) {
        this.blockLight = blockLight;
    }

    public int getSkyLight() {
        return skyLight;
    }

    public void setSkyLight(int skyLight) {
        this.skyLight = skyLight;
    }

    public boolean isGlowing() {
        return glowing;
    }

    public void setGlowing(boolean glowing) {
        this.glowing = glowing;
    }

    public String getGlowColor() {
        return glowColor;
    }

    public void setGlowColor(String glowColor) {
        this.glowColor = glowColor;
    }

    public Transform getBaseTransform() {
        return baseTransform;
    }

    public void setBaseTransform(Transform baseTransform) {
        this.baseTransform = baseTransform;
    }

    public List<Keyframe> getKeyframes() {
        return keyframes;
    }

    public void setKeyframes(List<Keyframe> keyframes) {
        this.keyframes = keyframes;
    }

    @Override
    public String kindLabel() {
        return "Display Entity";
    }
}

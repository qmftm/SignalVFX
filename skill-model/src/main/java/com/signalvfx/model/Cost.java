package com.signalvfx.model;

/**
 * The resource consumed to cast a skill.
 */
public class Cost {

    /** The kind of resource spent on cast. */
    public enum Type {
        NONE,
        MANA,
        HEALTH,
        HUNGER,
        EXPERIENCE,
        ITEM
    }

    private Type type = Type.MANA;
    private double amount = 20.0;

    /** Item key (e.g. {@code minecraft:blaze_powder}) when {@code type == ITEM}. */
    private String itemKey = "";

    public Cost() {
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getItemKey() {
        return itemKey;
    }

    public void setItemKey(String itemKey) {
        this.itemKey = itemKey;
    }
}

package com.signalvfx.model.visual;

/**
 * Which vanilla display entity a {@link DisplayEntityVisual} spawns.
 */
public enum DisplayKind {
    /** {@code item_display} — renders an item/model. */
    ITEM,
    /** {@code block_display} — renders a block state. */
    BLOCK,
    /** {@code text_display} — renders text. */
    TEXT
}

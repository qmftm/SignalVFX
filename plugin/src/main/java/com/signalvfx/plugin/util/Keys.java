package com.signalvfx.plugin.util;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.potion.PotionEffectType;

/** Parses namespaced string keys from skill JSON into Bukkit types. */
public final class Keys {

    private Keys() {
    }

    /** Resolves an item/block material key like {@code minecraft:blaze_rod}; null if unknown. */
    public static Material material(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        NamespacedKey nk = parse(key);
        Material m = nk == null ? null : Registry.MATERIAL.get(nk);
        return m != null ? m : Material.matchMaterial(key);
    }

    public static PotionEffectType potion(String key) {
        NamespacedKey nk = parse(key);
        return nk == null ? null : Registry.EFFECT.get(nk);
    }

    private static NamespacedKey parse(String key) {
        String k = key.toLowerCase();
        if (!k.contains(":")) {
            k = "minecraft:" + k;
        }
        return NamespacedKey.fromString(k);
    }
}

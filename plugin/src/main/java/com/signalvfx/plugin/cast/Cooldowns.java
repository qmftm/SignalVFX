package com.signalvfx.plugin.cast;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Per-player, per-skill cooldown tracking in wall-clock millis. */
public final class Cooldowns {

    private final Map<UUID, Map<String, Long>> until = new HashMap<>();

    public boolean isReady(UUID player, String skillId) {
        Map<String, Long> map = until.get(player);
        return map == null || System.currentTimeMillis() >= map.getOrDefault(skillId, 0L);
    }

    public long remainingMillis(UUID player, String skillId) {
        Map<String, Long> map = until.get(player);
        long end = map == null ? 0 : map.getOrDefault(skillId, 0L);
        return Math.max(0, end - System.currentTimeMillis());
    }

    public void set(UUID player, String skillId, int cooldownTicks) {
        if (cooldownTicks <= 0) {
            return;
        }
        until.computeIfAbsent(player, k -> new HashMap<>())
                .put(skillId, System.currentTimeMillis() + cooldownTicks * 50L);
    }
}

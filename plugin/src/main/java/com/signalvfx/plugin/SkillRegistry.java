package com.signalvfx.plugin;

import com.signalvfx.model.Skill;
import com.signalvfx.model.io.SkillIO;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Loads and holds the server's skills. Skill JSON lives in
 * {@code plugins/SignalVFX/skills/*.json} and is parsed with the same
 * {@link SkillIO} the editor writes with, so the two never drift.
 */
public final class SkillRegistry {

    private final JavaPlugin plugin;
    private final Path skillsDir;
    private final Map<String, Skill> skills = new LinkedHashMap<>();

    public SkillRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
        this.skillsDir = plugin.getDataFolder().toPath().resolve("skills");
    }

    /** Clears and reloads every skill from disk, seeding an example on first run. */
    public void loadAll() {
        skills.clear();
        try {
            Files.createDirectories(skillsDir);
            seedExampleIfEmpty();
            try (Stream<Path> files = Files.list(skillsDir)) {
                files.filter(p -> p.toString().endsWith(".json")).sorted().forEach(this::loadOne);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to read skills directory: " + e.getMessage());
        }
    }

    private void loadOne(Path path) {
        try {
            Skill skill = SkillIO.read(path);
            if (skill.getId() == null || skill.getId().isBlank()) {
                plugin.getLogger().warning("Skipping " + path.getFileName() + ": missing id");
                return;
            }
            skills.put(skill.getId().toLowerCase(), skill);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load " + path.getFileName() + ": " + e.getMessage());
        }
    }

    private void seedExampleIfEmpty() throws IOException {
        try (Stream<Path> files = Files.list(skillsDir)) {
            if (files.anyMatch(p -> p.toString().endsWith(".json"))) {
                return;
            }
        }
        try (InputStream in = plugin.getResource("skills/example_nova.json")) {
            if (in != null) {
                Files.copy(in, skillsDir.resolve("example_nova.json"));
            }
        }
    }

    public Skill get(String id) {
        return id == null ? null : skills.get(id.toLowerCase());
    }

    public Collection<Skill> all() {
        return skills.values();
    }

    public int count() {
        return skills.size();
    }
}

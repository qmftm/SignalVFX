package com.signalvfx.model.io;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.signalvfx.model.Skill;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Central JSON (de)serialization for {@link Skill} documents. Both the editor
 * and the plugin go through this so the on-disk format stays identical.
 */
public final class SkillIO {

    private static final ObjectMapper MAPPER = buildMapper();

    private SkillIO() {
    }

    private static ObjectMapper buildMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        // Tolerate fields added by newer editors so older plugins don't hard-fail.
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }

    /** Shared, pre-configured mapper. Safe to reuse; do not reconfigure. */
    public static ObjectMapper mapper() {
        return MAPPER;
    }

    public static Skill read(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            return MAPPER.readValue(in, Skill.class);
        }
    }

    public static Skill read(InputStream in) throws IOException {
        return MAPPER.readValue(in, Skill.class);
    }

    public static Skill fromString(String json) throws IOException {
        return MAPPER.readValue(json, Skill.class);
    }

    public static void write(Path path, Skill skill) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (OutputStream out = Files.newOutputStream(path)) {
            MAPPER.writeValue(out, skill);
        }
    }

    public static String toString(Skill skill) throws IOException {
        return MAPPER.writeValueAsString(skill);
    }
}

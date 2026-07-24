package com.signalvfx.editor.bbmodel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A minimal reader for BlockBench {@code .bbmodel} files. It extracts only what
 * the editor needs to reference a model in a {@code BETTER_MODEL} visual: the
 * model id and the list of animation names. The full geometry is left to the
 * BetterModel plugin at runtime — we do not render it here.
 */
public final class BbModel {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String modelId;
    private final List<String> animations;

    private BbModel(String modelId, List<String> animations) {
        this.modelId = modelId;
        this.animations = animations;
    }

    public String getModelId() {
        return modelId;
    }

    public List<String> getAnimations() {
        return animations;
    }

    /**
     * Reads a {@code .bbmodel} file. BetterModel registers models by file name,
     * so the model id defaults to the file's base name; the file's own {@code
     * name} field is only used as a fallback. Animation names come from the
     * {@code animations[].name} array (empty if the model has none).
     */
    public static BbModel read(Path path) throws IOException {
        JsonNode root = MAPPER.readTree(path.toFile());

        String fileBase = stripExtension(path.getFileName().toString());
        String modelId = fileBase;
        if (fileBase.isBlank() && root.hasNonNull("name")) {
            modelId = root.get("name").asText();
        }

        List<String> animations = new ArrayList<>();
        JsonNode anims = root.get("animations");
        if (anims != null && anims.isArray()) {
            for (JsonNode anim : anims) {
                JsonNode name = anim.get("name");
                if (name != null && !name.asText().isBlank()) {
                    animations.add(name.asText());
                }
            }
        }
        return new BbModel(modelId, animations);
    }

    private static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }
}

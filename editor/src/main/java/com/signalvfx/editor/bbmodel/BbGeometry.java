package com.signalvfx.editor.bbmodel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses the geometry of a BlockBench {@code .bbmodel} file into a bone/cube
 * tree the editor can render as a rough 3D preview. Only cube elements and the
 * {@code outliner} bone hierarchy are read; meshes, textures, UVs and Molang
 * are ignored. Coordinates are kept in BlockBench model units (16 units = 1
 * block); the renderer converts to blocks.
 *
 * <p>The intent is spatial reference ("where is the model, how big, which
 * way"), not a faithful reproduction — BetterModel renders the real thing at
 * runtime.
 */
public final class BbGeometry {

    /** A cube element: an axis-aligned box, optionally rotated about its pivot. */
    public static final class Cube {
        public final double[] from;      // [x,y,z] model units
        public final double[] to;        // [x,y,z] model units
        public final double[] origin;    // rotation pivot
        public final double[] rotation;  // degrees [x,y,z]
        public final double inflate;     // uniform expansion per side

        Cube(double[] from, double[] to, double[] origin, double[] rotation, double inflate) {
            this.from = from;
            this.to = to;
            this.origin = origin;
            this.rotation = rotation;
            this.inflate = inflate;
        }
    }

    /** A bone (BlockBench group): a pivot + rotation applied to child bones/cubes. */
    public static final class Bone {
        public final String name;
        public final double[] origin;    // pivot
        public final double[] rotation;  // degrees [x,y,z]
        public final List<Bone> children = new ArrayList<>();
        public final List<Cube> cubes = new ArrayList<>();

        Bone(String name, double[] origin, double[] rotation) {
            this.name = name;
            this.origin = origin;
            this.rotation = rotation;
        }
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Bone root;
    private final int cubeCount;

    private BbGeometry(Bone root, int cubeCount) {
        this.root = root;
        this.cubeCount = cubeCount;
    }

    public Bone getRoot() {
        return root;
    }

    public int getCubeCount() {
        return cubeCount;
    }

    public static BbGeometry read(Path path) throws IOException {
        JsonNode json = MAPPER.readTree(path.toFile());

        Map<String, Cube> byUuid = new HashMap<>();
        JsonNode elements = json.get("elements");
        if (elements != null && elements.isArray()) {
            for (JsonNode el : elements) {
                String type = el.hasNonNull("type") ? el.get("type").asText() : "cube";
                if (!type.equals("cube")) {
                    continue; // meshes not supported in the preview
                }
                JsonNode uuid = el.get("uuid");
                if (uuid == null) {
                    continue;
                }
                Cube cube = new Cube(
                        vec3(el.get("from"), 0),
                        vec3(el.get("to"), 0),
                        vec3(el.get("origin"), 0),
                        vec3(el.get("rotation"), 0),
                        el.hasNonNull("inflate") ? el.get("inflate").asDouble() : 0.0);
                byUuid.put(uuid.asText(), cube);
            }
        }

        Bone root = new Bone("root", new double[]{0, 0, 0}, new double[]{0, 0, 0});
        int[] count = {0};
        JsonNode outliner = json.get("outliner");
        if (outliner != null && outliner.isArray()) {
            for (JsonNode child : outliner) {
                addOutlinerChild(child, root, byUuid, count);
            }
        } else {
            // No outliner: attach every cube to the root.
            for (Cube c : byUuid.values()) {
                root.cubes.add(c);
                count[0]++;
            }
        }
        return new BbGeometry(root, count[0]);
    }

    private static void addOutlinerChild(JsonNode node, Bone parent, Map<String, Cube> byUuid, int[] count) {
        if (node.isTextual()) {
            Cube cube = byUuid.get(node.asText());
            if (cube != null) {
                parent.cubes.add(cube);
                count[0]++;
            }
            return;
        }
        // A group / bone.
        Bone bone = new Bone(
                node.hasNonNull("name") ? node.get("name").asText() : "group",
                vec3(node.get("origin"), 0),
                vec3(node.get("rotation"), 0));
        parent.children.add(bone);
        JsonNode children = node.get("children");
        if (children != null && children.isArray()) {
            for (JsonNode c : children) {
                addOutlinerChild(c, bone, byUuid, count);
            }
        }
    }

    private static double[] vec3(JsonNode node, double def) {
        double[] out = {def, def, def};
        if (node != null && node.isArray() && node.size() >= 3) {
            for (int i = 0; i < 3; i++) {
                out[i] = node.get(i).asDouble();
            }
        }
        return out;
    }
}

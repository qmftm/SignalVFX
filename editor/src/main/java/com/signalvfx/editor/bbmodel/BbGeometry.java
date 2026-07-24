package com.signalvfx.editor.bbmodel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses the geometry and animations of a BlockBench {@code .bbmodel} file into
 * a bone/cube tree the editor can render and animate as a rough 3D preview.
 * Only cube elements, the {@code outliner} bone hierarchy, and keyframe
 * animations are read; meshes, textures, UVs, IK and Molang expressions are
 * ignored (non-numeric keyframe values fall back to 0).
 *
 * <p>Coordinates are BlockBench model units (16 units = 1 block); the renderer
 * converts to blocks. The goal is spatial/temporal reference — where the model
 * and its moving parts are — not a faithful reproduction. BetterModel renders
 * the real thing at runtime.
 */
public final class BbGeometry {

    /** A cube element: an axis-aligned box, optionally rotated about its pivot. */
    public static final class Cube {
        public final double[] from;
        public final double[] to;
        public final double[] origin;
        public final double[] rotation;
        public final double inflate;

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
        public final String uuid;
        public final String name;
        public final double[] origin;
        public final double[] rotation;
        public final List<Bone> children = new ArrayList<>();
        public final List<Cube> cubes = new ArrayList<>();

        Bone(String uuid, String name, double[] origin, double[] rotation) {
            this.uuid = uuid;
            this.name = name;
            this.origin = origin;
            this.rotation = rotation;
        }
    }

    /** One animation keyframe: a time (seconds) and an x/y/z value for a channel. */
    public static final class Keyframe {
        public final double time;
        public final double[] value;
        public final boolean step;

        Keyframe(double time, double[] value, boolean step) {
            this.time = time;
            this.value = value;
            this.step = step;
        }
    }

    /** Position/rotation/scale keyframe tracks for a single bone within an animation. */
    public static final class BoneTrack {
        public final List<Keyframe> position = new ArrayList<>();
        public final List<Keyframe> rotation = new ArrayList<>();
        public final List<Keyframe> scale = new ArrayList<>();
    }

    /** A named animation: a length and per-bone (by uuid) keyframe tracks. */
    public static final class Animation {
        public final String name;
        public final double lengthSeconds;
        public final boolean loop;
        public final Map<String, BoneTrack> tracks;

        Animation(String name, double lengthSeconds, boolean loop, Map<String, BoneTrack> tracks) {
            this.name = name;
            this.lengthSeconds = lengthSeconds;
            this.loop = loop;
            this.tracks = tracks;
        }
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Bone root;
    private final int cubeCount;
    private final List<Animation> animations;

    private BbGeometry(Bone root, int cubeCount, List<Animation> animations) {
        this.root = root;
        this.cubeCount = cubeCount;
        this.animations = animations;
    }

    public Bone getRoot() {
        return root;
    }

    public int getCubeCount() {
        return cubeCount;
    }

    public List<Animation> getAnimations() {
        return animations;
    }

    public static BbGeometry read(Path path) throws IOException {
        JsonNode json = MAPPER.readTree(path.toFile());

        Map<String, Cube> byUuid = new HashMap<>();
        JsonNode elements = json.get("elements");
        if (elements != null && elements.isArray()) {
            for (JsonNode el : elements) {
                String type = el.hasNonNull("type") ? el.get("type").asText() : "cube";
                if (!type.equals("cube")) {
                    continue;
                }
                JsonNode uuid = el.get("uuid");
                if (uuid == null) {
                    continue;
                }
                byUuid.put(uuid.asText(), new Cube(
                        vec3(el.get("from"), 0),
                        vec3(el.get("to"), 0),
                        vec3(el.get("origin"), 0),
                        vec3(el.get("rotation"), 0),
                        el.hasNonNull("inflate") ? el.get("inflate").asDouble() : 0.0));
            }
        }

        Bone root = new Bone("root", "root", new double[]{0, 0, 0}, new double[]{0, 0, 0});
        int[] count = {0};
        JsonNode outliner = json.get("outliner");
        if (outliner != null && outliner.isArray()) {
            for (JsonNode child : outliner) {
                addOutlinerChild(child, root, byUuid, count);
            }
        } else {
            for (Cube c : byUuid.values()) {
                root.cubes.add(c);
                count[0]++;
            }
        }

        return new BbGeometry(root, count[0], readAnimations(json.get("animations")));
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
        Bone bone = new Bone(
                node.hasNonNull("uuid") ? node.get("uuid").asText() : "",
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

    private static List<Animation> readAnimations(JsonNode anims) {
        List<Animation> out = new ArrayList<>();
        if (anims == null || !anims.isArray()) {
            return out;
        }
        for (JsonNode anim : anims) {
            String name = anim.hasNonNull("name") ? anim.get("name").asText() : "animation";
            double length = anim.hasNonNull("length") ? anim.get("length").asDouble() : 0.0;
            boolean loop = anim.hasNonNull("loop") && anim.get("loop").asText().equalsIgnoreCase("loop");

            Map<String, BoneTrack> tracks = new LinkedHashMap<>();
            JsonNode animators = anim.get("animators");
            if (animators != null && animators.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> it = animators.fields();
                while (it.hasNext()) {
                    Map.Entry<String, JsonNode> e = it.next();
                    String boneUuid = e.getKey();
                    JsonNode kfs = e.getValue().get("keyframes");
                    if (kfs == null || !kfs.isArray()) {
                        continue;
                    }
                    BoneTrack track = new BoneTrack();
                    for (JsonNode kf : kfs) {
                        String channel = kf.hasNonNull("channel") ? kf.get("channel").asText() : "";
                        double time = kf.hasNonNull("time") ? kf.get("time").asDouble() : 0.0;
                        boolean step = kf.hasNonNull("interpolation")
                                && kf.get("interpolation").asText().equalsIgnoreCase("step");
                        double[] value = firstDataPoint(kf.get("data_points"),
                                channel.equals("scale") ? 1.0 : 0.0);
                        Keyframe keyframe = new Keyframe(time, value, step);
                        switch (channel) {
                            case "position" -> track.position.add(keyframe);
                            case "rotation" -> track.rotation.add(keyframe);
                            case "scale" -> track.scale.add(keyframe);
                            default -> { /* ignore unknown channels */ }
                        }
                    }
                    sortByTime(track.position);
                    sortByTime(track.rotation);
                    sortByTime(track.scale);
                    tracks.put(boneUuid, track);
                }
            }
            out.add(new Animation(name, length, loop, tracks));
        }
        return out;
    }

    private static void sortByTime(List<Keyframe> kfs) {
        kfs.sort((a, b) -> Double.compare(a.time, b.time));
    }

    private static double[] firstDataPoint(JsonNode dataPoints, double def) {
        double[] v = {def, def, def};
        if (dataPoints != null && dataPoints.isArray() && dataPoints.size() > 0) {
            JsonNode p = dataPoints.get(0);
            v[0] = numberOr(p.get("x"), def);
            v[1] = numberOr(p.get("y"), def);
            v[2] = numberOr(p.get("z"), def);
        }
        return v;
    }

    /** Reads a keyframe component, tolerating Molang/string values (falls back to {@code def}). */
    private static double numberOr(JsonNode node, double def) {
        if (node == null || node.isNull()) {
            return def;
        }
        if (node.isNumber()) {
            return node.asDouble();
        }
        try {
            return Double.parseDouble(node.asText().trim());
        } catch (NumberFormatException ex) {
            return def; // Molang expression — not evaluated in the preview
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

package com.alexcoronel.toadmod.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Builds a vanilla {@link LayerDefinition} from a Blockbench/Bedrock-format {@code .geo.json} file, so the
 * model that is edited in Blockbench is exactly the model that is rendered in game.
 *
 * Supported: bones (pivot, parent) and cubes (origin, size, uv, inflate, mirror). Bedrock Y-up coordinates are
 * converted to the vanilla entity-model space (Y down, feet at y = 24).
 */
final class ToadGeometry {
    private static final String RESOURCE = "/assets/toadmod/models/entity/toad.geo.json";

    private ToadGeometry() {
    }

    static LayerDefinition load() {
        JsonObject geometry;
        try (InputStream in = ToadGeometry.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Missing model resource " + RESOURCE);
            }
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            geometry = root.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + RESOURCE, e);
        }

        JsonObject description = geometry.getAsJsonObject("description");
        int texW = description.get("texture_width").getAsInt();
        int texH = description.get("texture_height").getAsInt();

        MeshDefinition mesh = new MeshDefinition();
        Map<String, PartDefinition> parts = new HashMap<>();
        Map<String, float[]> pivots = new HashMap<>(); // vanilla-space absolute pivot

        for (JsonElement boneElement : geometry.getAsJsonArray("bones")) {
            JsonObject bone = boneElement.getAsJsonObject();
            String name = bone.get("name").getAsString();
            String parentName = bone.has("parent") ? bone.get("parent").getAsString() : null;
            float[] pivot = floats(bone.getAsJsonArray("pivot"));
            float px = pivot[0];
            float py = pivot[1];
            float pz = pivot[2];

            PartDefinition parent = parentName == null ? mesh.getRoot() : parts.get(parentName);
            if (parent == null) {
                throw new IllegalStateException("Bone '" + name + "' is declared before its parent '" + parentName + "'");
            }
            float[] parentPivot = parentName == null ? new float[] {0, 0, 0} : pivots.get(parentName);
            float vy = 24.0F - py;

            CubeListBuilder cubes = CubeListBuilder.create();
            if (bone.has("cubes")) {
                for (JsonElement cubeElement : bone.getAsJsonArray("cubes")) {
                    JsonObject cube = cubeElement.getAsJsonObject();
                    float[] o = floats(cube.getAsJsonArray("origin"));
                    float[] s = floats(cube.getAsJsonArray("size"));
                    float[] uv = floats(cube.getAsJsonArray("uv"));
                    float inflate = cube.has("inflate") ? cube.get("inflate").getAsFloat() : 0.0F;
                    boolean mirror = cube.has("mirror") && cube.get("mirror").getAsBoolean();
                    cubes.texOffs((int) uv[0], (int) uv[1]).mirror(mirror).addBox(
                            o[0] - px, py - o[1] - s[1], o[2] - pz, s[0], s[1], s[2], new CubeDeformation(inflate));
                }
            }

            PartDefinition part = parent.addOrReplaceChild(name, cubes,
                    PartPose.offset(px - parentPivot[0], vy - parentPivot[1], pz - parentPivot[2]));
            parts.put(name, part);
            pivots.put(name, new float[] {px, vy, pz});
        }
        return LayerDefinition.create(mesh, texW, texH);
    }

    private static float[] floats(JsonArray array) {
        float[] out = new float[array.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = array.get(i).getAsFloat();
        }
        return out;
    }
}

package io.github.shaquibbai.deadlinedash.npc;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

/**
 * Resolves NPC model identifiers to their relative asset paths.
 * Strictly adheres to the Male* and Female* naming convention:
 *   - Male*   -> characters/npc/male/{model}.png
 *   - Female* -> characters/npc/female/{model}.png
 */
public class NPCModelResolver {
    public static final String MALE_BASE_PATH = "characters/npc/male/";
    public static final String FEMALE_BASE_PATH = "characters/npc/female/";

    /**
     * Resolves the given model ID to its asset file path.
     *
     * @param modelId the model ID from Tiled properties (e.g. "Male01_Forward")
     * @return the resolved asset path, or null if the ID is invalid or unrecognized
     */
    public static String resolvePath(String modelId) {
        if (modelId == null || modelId.trim().isEmpty()) {
            System.err.println("[NPC] WARNING: Model ID is null or empty.");
            return null;
        }

        String trimmed = modelId.trim();
        String filename = trimmed.endsWith(".png") ? trimmed : trimmed + ".png";

        if (trimmed.regionMatches(true, 0, "Male", 0, 4)) {
            return MALE_BASE_PATH + filename;
        } else if (trimmed.regionMatches(true, 0, "Female", 0, 6)) {
            return FEMALE_BASE_PATH + filename;
        } else {
            System.err.printf("[NPC] WARNING: Model ID '%s' does not match Male* or Female* naming convention.%n", modelId);
            return null;
        }
    }

    /**
     * Resolves the asset path and verifies its existence via LibGDX file handling if available.
     *
     * @param modelId the model ID from Tiled
     * @return the resolved asset path, or null if invalid or the asset file does not exist
     */
    public static String resolveAndVerify(String modelId) {
        String path = resolvePath(modelId);
        if (path == null) {
            return null;
        }

        if (Gdx.files != null) {
            FileHandle handle = Gdx.files.internal(path);
            if (!handle.exists()) {
                System.err.printf("[NPC] WARNING: Asset file '%s' for model '%s' does not exist.%n", path, modelId);
                return null;
            }
        }

        return path;
    }
}

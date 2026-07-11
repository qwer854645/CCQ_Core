package ccq.core.tacz;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

final class CcqDefaultGunPackContent {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    static final String OUTPUT_DIR_NAME = "ccq_default_gun";
    static final String STAGING_DIR_NAME = "ccq_default_gun.__ccq_build";
    static final String SOURCE_ZIP_GLOB = "tacz_default_gun*.zip";
    static final String MOD_JAR_PACK_PREFIX = "assets/tacz/custom/tacz_default_gun";
    static final String PACK_META_ENTRY = "gunpack.meta.json";
    static final List<String> KEEP_PREFIXES = List.of(
            "assets/tacz/tacz_sounds/ak47/",
            "assets/tacz/tacz_sounds/cz75/",
            "assets/tacz/tacz_sounds/mag_drop_sound/"
    );
    static final String SHARED_SOUND_MARKER = "assets/tacz/tacz_sounds/cz75/cz75_reload_magin.ogg";
    static final String AK47_SHARED_SOUND_MARKER = "assets/tacz/tacz_sounds/ak47/ak47_reload_magin.ogg";
    static final List<String> KEEP_FILES = List.of(
            PACK_META_ENTRY,
            "assets/tacz/display/ammo/12g_display.json",
            "assets/tacz/geo_models/ammo/12g.json",
            "assets/tacz/geo_models/shell/12g_shell.json",
            "assets/tacz/scripts/default_state_machine.lua",
            "assets/tacz/textures/ammo/slot/12g.png",
            "assets/tacz/textures/ammo/uv/12g.png",
            "assets/tacz/textures/ammo/uv/12g_n.png",
            "assets/tacz/textures/ammo/uv/12g_s.png",
            "assets/tacz/textures/flash/common_muzzle_flash.png",
            "assets/tacz/textures/shell/12g_shell.png",
            "assets/tacz/textures/shell/12g_shell_n.png",
            "assets/tacz/textures/shell/12g_shell_s.png",
            "data/tacz/index/ammo/12g.json",
            "data/tacz/recipe/ammo/12g.json",
            "data/tacz/recipe_filters/default.json"
    );

    private CcqDefaultGunPackContent() {
    }

    static byte[] createGunpackInfoJson() {
        JsonObject root = new JsonObject();
        root.addProperty("version", "1.0.0-ccq");
        root.addProperty("name", "pack.tacz.ccq_default_gun.name");
        root.addProperty("desc", "pack.tacz.ccq_default_gun.desc");
        root.addProperty("license", "CC BY-NC-ND 4.0");
        root.add("authors", GSON.toJsonTree(new String[]{"TACZ Dev Team", "Create Craft & Quiet"}));
        root.addProperty("date", LocalDate.now().toString());
        root.addProperty("url", "https://creativecommons.org/licenses/by-nc-nd/4.0/");
        return serializeJson(root);
    }

    static byte[] createEnglishLangJson() {
        JsonObject root = new JsonObject();
        root.addProperty("pack.tacz.ccq_default_gun.name", "CCQ Default Gun Pack");
        root.addProperty(
                "pack.tacz.ccq_default_gun.desc",
                "For Create Craft & Quiet: 12 gauge ammo, shared sounds, and other assets required by third-party gun packs. "
                        + "Includes TACZ default pack materials under CC BY-NC-ND 4.0."
        );
        root.addProperty("tacz.type.ammo.name", "Ammo");
        root.addProperty("tacz.ammo.12g.name", "§912 Gauge Bullet");
        return serializeJson(root);
    }

    static byte[] createChineseLangJson() {
        JsonObject root = new JsonObject();
        root.addProperty("pack.tacz.ccq_default_gun.name", "CCQ 精简枪包");
        root.addProperty(
                "pack.tacz.ccq_default_gun.desc",
                "Create Craft & Quiet 专用：仅保留 12 号霰弹、共享音效与第三方枪包依赖的共享资源。含 TACZ 默认包素材，遵循 CC BY-NC-ND 4.0。"
        );
        root.addProperty("tacz.type.ammo.name", "子弹");
        root.addProperty("tacz.ammo.12g.name", "§912 号口径霰弹");
        return serializeJson(root);
    }

    static byte[] createNoticeText() {
        String notice = """
                CCQ Default Gun Pack (ccq_default_gun)
                ======================================

                This folder was generated at runtime by ccq_core from an installed TACZ default gun pack.
                It retains only 12 gauge ammo, shared sounds, and other assets required by third-party gun packs.

                Original materials are from the TACZ Default Gun Pack (TACZ Dev Team, CC BY-NC-ND 4.0).
                Do not redistribute modified pack files without author permission.
                """;
        return notice.replace("\n", "\r\n").getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] serializeJson(JsonObject root) {
        String json = GSON.toJson(root).replace("\n", "\r\n");
        if (!json.endsWith("\r\n")) {
            json = json + "\r\n";
        }
        return json.getBytes(StandardCharsets.UTF_8);
    }
}

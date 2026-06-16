package ccq.core.tacz;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * CCQ runtime configuration for Applied Armorer gun terminal tabs.
 * Generated in code — not shipped as gun-pack resource files.
 */
final class AppliedArmorerWorkbenchData {
    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    private record TabDefinition(String id, String nameKey, String item, String nbtKey, String nbtValue) {
    }

    private static final List<TabDefinition> TABS = List.of(
            new TabDefinition("tacz:pistol", "tacz.type.pistol.name", "tacz:modern_kinetic_gun", "GunId", "applied_armorer:niklas_pistol_semi_pride"),
            new TabDefinition("tacz:shotgun", "tacz.type.shotgun.name", "tacz:modern_kinetic_gun", "GunId", "applied_armorer:moritz_shotgun_sg914"),
            new TabDefinition("tacz:rifle", "tacz.type.rifle.name", "tacz:modern_kinetic_gun", "GunId", "applied_armorer:moritz_rifle_ar77"),
            new TabDefinition("tacz:sniper", "tacz.type.sniper.name", "tacz:modern_kinetic_gun", "GunId", "applied_armorer:moritz_sniper_semi_k30"),
            new TabDefinition("tacz:smg", "tacz.type.smg.name", "tacz:modern_kinetic_gun", "GunId", "applied_armorer:niklas_smg_freedom"),
            new TabDefinition("tacz:mg", "tacz.type.mg.name", "tacz:modern_kinetic_gun", "GunId", "applied_armorer:moritz_mg_hmg22"),
            new TabDefinition("tacz:rpg", "tacz.type.rpg.name", "tacz:modern_kinetic_gun", "GunId", "applied_armorer:moritz_gernade_gl3"),
            new TabDefinition("tacz:ammo", "tacz.type.ammo.name", "tacz:ammo", "AmmoId", "applied_armorer:fluix_battery"),
            new TabDefinition("tacz:scope", "tacz.type.scope.name", "tacz:attachment", "AttachmentId", "applied_armorer:scope_ms_14"),
            new TabDefinition("tacz:extended_mag", "tacz.type.extended_mag.name", "tacz:attachment", "AttachmentId", "applied_armorer:extended_mag_aa_3"),
            new TabDefinition("tacz:grip", "tacz.type.grip.name", "tacz:attachment", "AttachmentId", "applied_armorer:grip_eazy"),
            new TabDefinition("tacz:muzzle", "tacz.type.muzzle.name", "tacz:attachment", "AttachmentId", "applied_armorer:muzzle_bs_mod4"),
            new TabDefinition("tacz:stock", "tacz.type.stock.name", "tacz:attachment", "AttachmentId", "applied_armorer:bracelet_aerial_wristband")
    );

    private AppliedArmorerWorkbenchData() {
    }

    static byte[] createPatchedContent() {
        return serialize(buildExpectedRoot());
    }

    static boolean matchesExpectedContent(byte[] content) {
        if (content == null) {
            return false;
        }

        try {
            JsonObject current = GSON.fromJson(decodeJsonText(content), JsonObject.class);
            return current != null && current.equals(buildExpectedRoot());
        } catch (JsonSyntaxException exception) {
            return false;
        }
    }

    private static JsonObject buildExpectedRoot() {
        JsonObject root = new JsonObject();
        root.addProperty("filter", "applied_armorer:default");

        JsonArray tabs = new JsonArray();
        for (TabDefinition tab : TABS) {
            JsonObject nbt = new JsonObject();
            nbt.addProperty(tab.nbtKey, tab.nbtValue);

            JsonObject icon = new JsonObject();
            icon.addProperty("item", tab.item);
            icon.add("nbt", nbt);

            JsonObject tabObject = new JsonObject();
            tabObject.addProperty("id", tab.id);
            tabObject.addProperty("name", tab.nameKey);
            tabObject.add("icon", icon);
            tabs.add(tabObject);
        }

        root.add("tabs", tabs);
        return root;
    }

    private static byte[] serialize(JsonObject root) {
        String json = GSON.toJson(root).replace("\n", "\r\n");
        if (!json.endsWith("\r\n")) {
            json = json + "\r\n";
        }

        return json.getBytes(StandardCharsets.UTF_8);
    }

    private static String decodeJsonText(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF) {
            return new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        }

        return new String(bytes, StandardCharsets.UTF_8);
    }
}

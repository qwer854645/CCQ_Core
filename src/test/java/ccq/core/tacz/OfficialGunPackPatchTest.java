package ccq.core.tacz;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import com.google.gson.*;
import static org.junit.jupiter.api.Assertions.*;
class OfficialGunPackPatchTest {
    private static final Path JAR=Path.of("C:/Users/Administrator/Documents/Codex/2026-10-30/x20/work/tacz-official/tacz-neoforge-1.21.1-1.2.0.jar");
    @Test void extractsOfficialOverlayAndMigratesLegacyOutputIdempotently(@TempDir Path temp)throws Exception {
        var out=temp.resolve("tacz/ccq_default_gun");Files.createDirectories(out);Files.writeString(out.resolve("old-marker"),"legacy");
        OfficialGunPackPatch.strip(JAR,out);
        assertFalse(Files.exists(out.resolve("old-marker")));
        for(var f:CcqDefaultGunPackContent.KEEP_FILES)assertTrue(Files.isRegularFile(out.resolve(f)),f);
        var meta=JsonParser.parseString(Files.readString(out.resolve("gunpack.meta.json"))).getAsJsonObject();assertFalse(meta.has("overlays"));
        assertFalse(Files.exists(out.resolve("data/tacz/index/guns")));
        var stamp=Files.getLastModifiedTime(out.resolve("ccq-layout.txt"));OfficialGunPackPatch.strip(JAR,out);assertEquals(stamp,Files.getLastModifiedTime(out.resolve("ccq-layout.txt")));
        try(var paths=Files.list(temp.resolve("ccq-gunpack-backups"))){assertEquals(1,paths.count());}
    }
    @Test void patchesCopyOfActualPackWithoutChangingBaseAndKeepsOtherOverlays(@TempDir Path temp)throws Exception {
        var input=Path.of("F:/MC/ser/.minecraft/versions/Create_Craft&Quiet/tacz/Applied Armorer-v1.1.4.1-for114+.zip");
        var pack=temp.resolve("tacz/Applied Armorer.zip");Files.createDirectories(pack.getParent());Files.copy(input,pack);
        var base=GunPackZipPatcher.readEntry(pack,AppliedArmorerPatch.WORKBENCH_DATA_ENTRY);
        var meta=JsonParser.parseString(new String(GunPackZipPatcher.readEntry(pack,"gunpack.meta.json"),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        OfficialGunPackPatch.patchWorkbench(pack,meta);
        assertArrayEquals(base,GunPackZipPatcher.readEntry(pack,AppliedArmorerPatch.WORKBENCH_DATA_ENTRY));
        var patched=JsonParser.parseString(new String(GunPackZipPatcher.readEntry(pack,"ccq_1_21_1/"+AppliedArmorerPatch.WORKBENCH_DATA_ENTRY),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        for(var tab:patched.getAsJsonArray("tabs")){var icon=tab.getAsJsonObject().getAsJsonObject("icon");assertTrue(icon.has("id"));assertTrue(icon.getAsJsonObject("components").has("minecraft:custom_data"));assertFalse(icon.has("nbt"));}
        byte[] result=Files.readAllBytes(pack);OfficialGunPackPatch.patchWorkbench(pack,meta);assertArrayEquals(result,Files.readAllBytes(pack));
    }
}

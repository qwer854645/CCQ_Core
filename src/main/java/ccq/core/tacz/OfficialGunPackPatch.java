package ccq.core.tacz;

import com.google.gson.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipFile;

/** Official 1.2 layout only; legacy port processing remains separate. */
final class OfficialGunPackPatch {
    private static final String FORMAT="official-1.2-flat-1.21.1-v1";
    static synchronized void apply(Path dir,Path jar)throws IOException {
        strip(jar,dir.resolve(CcqDefaultGunPackContent.OUTPUT_DIR_NAME));
        try(var paths=Files.list(dir)) {
            for(var pack:paths.toList()) {
                if(Files.isRegularFile(pack)&&pack.toString().endsWith(".zip")||Files.isDirectory(pack)) {
                    byte[] meta=read(pack,"gunpack.meta.json");
                    if(meta==null)continue;
                    var root=JsonParser.parseString(new String(meta,StandardCharsets.UTF_8).replace("\ufeff","")).getAsJsonObject();
                    if(root.has("namespace")&&java.util.Set.of("applied_armorer","create_armorer","ars_armorer").contains(root.get("namespace").getAsString())) {
                        try {
                            root=convertLegacyPack(pack,root);
                            if(root.get("namespace").getAsString().equals("applied_armorer"))patchWorkbench(pack,root);
                        }catch(IOException e){org.slf4j.LoggerFactory.getLogger("ccq_core").error("Gunpack conversion refused; original retained: {}",pack,e);}
                    }
                }
            }
        }
        // Only retire default exports once the replacement has been published.
        try(var paths=Files.list(dir)) {
            for(var path:paths.toList())if(path.getFileName().toString().startsWith("tacz_default_gun")) {
                byte[] meta=read(path,"gunpack.meta.json");
                if(meta!=null&&GunPackFileOperations.containsTaczNamespace(meta)) {
                    backup(path);GunPackFileOperations.deletePack(path);
                }
            }
        }
    }
    private static JsonObject convertLegacyPack(Path pack,JsonObject meta)throws IOException {
        if(meta.has("overlays"))for(var layer:meta.getAsJsonObject("overlays").getAsJsonArray("entries"))
            if(layer.getAsJsonObject().get("directory").getAsString().equals("compat_1_21_1"))return meta;
        try {
            var versions=new java.util.HashMap<String,String>();
            for(var mod:net.neoforged.fml.ModList.get().getMods())versions.put(mod.getModId(),mod.getVersion().toString());
            var type=Class.forName("com.tacz.guns.resource.convert.UniversalPackConverter");
            var registry=net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(net.minecraft.core.registries.BuiltInRegistries.REGISTRY);
            var converter=type.getConstructor(net.minecraft.core.HolderLookup.Provider.class,java.util.Map.class).newInstance(registry,versions);
            var history=pack.getParent().getParent().resolve("ccq-gunpack-conversions");Files.createDirectories(history);
            var output=Files.createTempDirectory(history,"conversion-");
            Path input=output.resolve(pack.getFileName().toString());
            var clean=meta.deepCopy();
            if(meta.has("overlays")) {
                for(var entry:meta.getAsJsonObject("overlays").getAsJsonArray("entries"))
                    if(!entry.getAsJsonObject().get("directory").getAsString().equals("ccq_1_21_1"))throw new IOException("Unsupported existing overlay in "+pack);
                clean.remove("overlays");
            }
            try(var zin=new ZipFile(pack.toFile());var zout=new java.util.zip.ZipOutputStream(Files.newOutputStream(input))) {
                for(var entry:zin.stream().toList()) {
                    if(entry.isDirectory()||entry.getName().startsWith("ccq_1_21_1/"))continue;
                    byte[] bytes;try(var stream=zin.getInputStream(entry)){bytes=stream.readAllBytes();}
                    if(entry.getName().equals("gunpack.meta.json"))bytes=new Gson().toJson(clean).getBytes(StandardCharsets.UTF_8);
                    else if(entry.getName().contains("/recipes/")&&entry.getName().endsWith(".json")) {
                        var json=JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject();
                        // Older TaCZ packs used a bare conditions key. Normalize on
                        // the converter input so the official Forge adapter sees it.
                        if(json.has("conditions")&&!json.has("forge:conditions")) {
                            json.add("forge:conditions",json.remove("conditions"));bytes=new Gson().toJson(json).getBytes(StandardCharsets.UTF_8);
                        }
                    }
                    zout.putNextEntry(new java.util.zip.ZipEntry(entry.getName()));zout.write(bytes);zout.closeEntry();
                }
            }
            var result=type.getMethod("convert",Path.class,Path.class,boolean.class).invoke(converter,input,output,true);
            var report=result.getClass().getMethod("report").invoke(result);
            if((Boolean)report.getClass().getMethod("hasErrors").invoke(report)||(Integer)report.getClass().getField("skippedRecipes").get(report)>0)throw new IOException("Official pack conversion failed; see "+output+"; diagnostics="+new Gson().toJson(report));
            var converted=(Path)result.getClass().getMethod("packPath").invoke(result);
            if(converted==null||!Files.isRegularFile(converted))throw new IOException("Official conversion did not produce a pack");
            if(!Files.isRegularFile(pack))throw new IOException("Automatic official conversion currently requires a zip pack");
            backup(pack);
            Path staged=Files.createTempFile(pack.getParent(),"ccq-convert-",".tmp");
            try {Files.copy(converted,staged,StandardCopyOption.REPLACE_EXISTING);
                try{Files.move(staged,pack,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
                catch(AtomicMoveNotSupportedException e){Files.move(staged,pack,StandardCopyOption.REPLACE_EXISTING);}
            }finally{Files.deleteIfExists(staged);}
            return JsonParser.parseString(new String(read(pack,"gunpack.meta.json"),StandardCharsets.UTF_8)).getAsJsonObject();
        }catch(ReflectiveOperationException e){throw new IOException("Cannot convert legacy Applied Armorer pack",e);}
    }
    static void strip(Path jar,Path output)throws IOException {
        String version;
        try(var zip=new ZipFile(jar.toFile())) {
            version=zip.stream().mapToLong(e->e.getCrc()).reduce(0L,(a,b)->a*31+b)+":"+FORMAT;
        }
        var marker=output.resolve("ccq-layout.txt");
        if(Files.isRegularFile(marker)&&Files.readString(marker).equals(version)
            &&CcqDefaultGunPackContent.KEEP_FILES.stream().allMatch(f->Files.isRegularFile(output.resolve(f))))return;
        var stage=output.resolveSibling("ccq_default_gun.__official_build");
        GunPackFileOperations.resetDirectory(stage);
        try(var zip=new ZipFile(jar.toFile())) {
            String prefix=CcqDefaultGunPackContent.MOD_JAR_PACK_PREFIX+"/";
            for(String file:CcqDefaultGunPackContent.KEEP_FILES) {
                if(file.equals("gunpack.meta.json"))continue;
                var entry=zip.getEntry(prefix+"compat_1_21_1/"+file);
                if(entry==null)entry=zip.getEntry(prefix+file);
                if(entry==null)throw new IOException("Official gunpack missing "+file);
                GunPackFileOperations.writeRelativeBytes(stage,file,zip.getInputStream(entry).readAllBytes());
            }
            GunPackFileOperations.copyKeepPrefixesFromPrefixedZip(jar,prefix,stage,CcqDefaultGunPackContent.KEEP_PREFIXES);
            Files.writeString(stage.resolve("gunpack.meta.json"),"{\"namespace\":\"tacz\",\"dependencies\":{\"minecraft\":\"[1.21.1]\"}}");
            GunPackFileOperations.writeRelativeBytes(stage,"assets/tacz/gunpack_info.json",CcqDefaultGunPackContent.createGunpackInfoJson());
            GunPackFileOperations.writeRelativeBytes(stage,"assets/tacz/lang/en_us.json",CcqDefaultGunPackContent.createEnglishLangJson());
            GunPackFileOperations.writeRelativeBytes(stage,"assets/tacz/lang/zh_cn.json",CcqDefaultGunPackContent.createChineseLangJson());
            GunPackFileOperations.writeRelativeBytes(stage,"NOTICE.txt",CcqDefaultGunPackContent.createNoticeText());
            Files.writeString(stage.resolve("ccq-layout.txt"),version);
            if(Files.exists(output))backup(output);
            GunPackFileOperations.publishDirectory(stage,output);
        }finally{if(Files.exists(stage))GunPackFileOperations.deletePack(stage);}
    }
    static void patchWorkbench(Path pack,JsonObject meta)throws IOException {
        var gson=new GsonBuilder().setPrettyPrinting().create();
        var data=JsonParser.parseString(new String(AppliedArmorerWorkbenchData.createPatchedContent(),StandardCharsets.UTF_8)).getAsJsonObject();
        for(var tab:data.getAsJsonArray("tabs")) {
            var icon=tab.getAsJsonObject().getAsJsonObject("icon");
            icon.add("id",icon.remove("item"));icon.addProperty("count",1);
            var components=new JsonObject();components.addProperty("minecraft:custom_data",icon.remove("nbt").toString());icon.add("components",components);
        }
        String entry="ccq_1_21_1/"+AppliedArmorerPatch.WORKBENCH_DATA_ENTRY;
        byte[] content=gson.toJson(data).getBytes(StandardCharsets.UTF_8);
        var overlays=meta.has("overlays")?meta.getAsJsonObject("overlays"):new JsonObject();
        var entries=overlays.has("entries")?overlays.getAsJsonArray("entries"):new JsonArray();
        var updated=new JsonArray();for(var v:entries)if(!v.getAsJsonObject().get("directory").getAsString().equals("ccq_1_21_1"))updated.add(v);
        var layer=new JsonObject();layer.addProperty("directory","ccq_1_21_1");layer.addProperty("minecraft","[1.21.1]");updated.add(layer);
        overlays.add("entries",updated);meta.add("overlays",overlays);
        byte[] metadata=gson.toJson(meta).getBytes(StandardCharsets.UTF_8);
        if(Arrays.equals(read(pack,entry),content)&&Arrays.equals(read(pack,"gunpack.meta.json"),metadata))return;
        backup(pack);write(pack,entry,content);write(pack,"gunpack.meta.json",metadata);
    }
    private static byte[] read(Path pack,String entry)throws IOException {
        if(Files.isDirectory(pack))return Files.isRegularFile(pack.resolve(entry))?Files.readAllBytes(pack.resolve(entry)):null;
        if(!Files.isRegularFile(pack)||!pack.toString().endsWith(".zip"))return null;
        return GunPackZipPatcher.readEntry(pack,entry);
    }
    private static void write(Path pack,String entry,byte[] data)throws IOException {
        if(Files.isDirectory(pack))GunPackFileOperations.writeRelativeBytes(pack,entry,data);
        else GunPackZipPatcher.replaceEntry(pack,entry,data);
    }
    private static void backup(Path pack)throws IOException {
        Path target=pack.getParent().getParent().resolve("ccq-gunpack-backups").resolve(pack.getFileName()+"-"+java.time.Instant.now().toEpochMilli());
        Files.createDirectories(target.getParent());
        if(Files.isDirectory(pack))try(var walk=Files.walk(pack)) {
            for(var source:walk.toList()) {var dest=target.resolve(pack.relativize(source));if(Files.isDirectory(source))Files.createDirectories(dest);else Files.copy(source,dest);}
        }else Files.copy(pack,target);
    }
}

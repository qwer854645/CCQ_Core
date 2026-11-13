package ccq.core.testground;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** Persist the first overworld spawn and backfill a real structure start in older saves. */
final class SpawnFacilityAnchor extends SavedData {
    static final ResourceLocation ID=ResourceLocation.parse("ccq_testground:abandoned_facility");
    private static volatile BlockPos active;
    private BlockPos anchor;
    private boolean generated;
    private SpawnFacilityAnchor(BlockPos anchor){this.anchor=anchor.immutable();setDirty();}
    static BlockPos position(){return active;}
    static boolean overlaps(BoundingBox box,int margin){
        var p=active;
        return p!=null && box.minX()<=p.getX()+121+margin && box.maxX()>=p.getX()-122-margin
                && box.minZ()<=p.getZ()+51+margin && box.maxZ()>=p.getZ()-52-margin;
    }
    static void started(ServerStartedEvent event){
        MinecraftServer server=event.getServer();
        var data=server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(()->new SpawnFacilityAnchor(server.overworld().getSharedSpawnPos()),SpawnFacilityAnchor::load),
                "ccq_abandoned_facility");
        active=data.anchor;
        if(!data.generated)server.execute(()->data.generate(server));
    }
    static void stopped(ServerStoppedEvent event){active=null;}
    private static SpawnFacilityAnchor load(CompoundTag tag,HolderLookup.Provider registries){
        var data=new SpawnFacilityAnchor(BlockPos.of(tag.getLong("anchor")));
        data.generated=tag.getBoolean("generated");
        return data;
    }
    private void generate(MinecraftServer server){
        var level=server.getLevel(TestGroundModule.DIMENSION);
        if(level==null)return;
        var structure=level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(ID);
        if(structure==null)return;
        var root=new ChunkPos(anchor);
        var chunk=level.getChunk(root.x,root.z);
        var existing=chunk.getStartForStructure(structure);
        if(existing!=null&&existing.isValid()){generated=true;setDirty();return;}
        var generator=level.getChunkSource().getGenerator();
        var start=structure.generate(level.registryAccess(),generator,generator.getBiomeSource(),
                level.getChunkSource().randomState(),level.getStructureManager(),level.getSeed(),root,0,level,
                biome->structure.biomes().contains(biome));
        if(!start.isValid())return;
        var bounds=start.getBoundingBox();
        // Load first, then attach the start: neighboring generation cannot place half
        // of this backfill concurrently. Existing starts are never rebuilt on restart.
        for(int x=bounds.minX()>>4;x<=bounds.maxX()>>4;x++)
            for(int z=bounds.minZ()>>4;z<=bounds.maxZ()>>4;z++)level.getChunk(x,z);
        chunk.setStartForStructure(structure,start);
        var random=RandomSource.create(level.getSeed()^91827366L);
        for(int x=bounds.minX()>>4;x<=bounds.maxX()>>4;x++)for(int z=bounds.minZ()>>4;z<=bounds.maxZ()>>4;z++){
            var part=level.getChunk(x,z);
            part.addReferenceForStructure(structure,root.toLong());
            var clip=new BoundingBox(x*16,level.getMinBuildHeight(),z*16,x*16+15,level.getMaxBuildHeight()-1,z*16+15);
            start.placeInChunk(level,level.structureManager(),generator,random,clip,new ChunkPos(x,z));
        }
        generated=true;setDirty();
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries){
        tag.putLong("anchor",anchor.asLong());tag.putBoolean("generated",generated);return tag;
    }
}

package ccq.core.testground;

import com.google.gson.Gson;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ccq_ruin_bake")
@PrefixGameTestTemplate(false)
public class EngineeringRuinStateBake {
    @GameTest(template="empty",batch="ruin_states",timeoutTicks=2000)
    public static void fourRotationsUseActualNeighborStates(GameTestHelper helper)throws Exception {
        var level=helper.getLevel().getServer().getLevel(TestGroundModule.DIMENSION);
        CompoundTag raw;
        try(var stream=level.getServer().getResourceManager().getResourceOrThrow(ResourceLocation.parse("ccq_testground:structure/engineering/engineering_base_ruined.nbt")).open()) {
            raw=NbtIo.readCompressed(stream,NbtAccounter.unlimitedHeap());
        }
        var blocks=raw.getList("blocks",10);var rawPalette=raw.getList("palette",10);
        var variants=new LinkedHashMap<String,Object>();var palette=new ArrayList<Object>();var indices=new HashMap<String,Integer>();
        for(var rotation:Rotation.values()) {
            var origin=new BlockPos(40000+rotation.ordinal()*1024,160,40000);
            var template=new StructureTemplate();template.load(level.holderLookup(Registries.BLOCK),raw);
            var settings=new StructurePlaceSettings().setRotation(rotation).setKnownShape(true).setIgnoreEntities(true).setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING);
            var bounds=template.getBoundingBox(settings,origin);var forced=new ArrayList<net.minecraft.world.level.ChunkPos>();
            try {
                for(int x=(bounds.minX()>>4)-1;x<=(bounds.maxX()>>4)+1;x++)for(int z=(bounds.minZ()>>4)-1;z<=(bounds.maxZ()>>4)+1;z++) {
                    var chunk=new net.minecraft.world.level.ChunkPos(x,z);level.getChunk(x,z);level.setChunkForced(x,z,true);forced.add(chunk);
                }
                template.placeInWorld(level,origin,origin,settings,level.random,2);
                for(int pass=0;pass<8;pass++) {
                    int changes=0;
                    for(var value:blocks) {
                        var block=(CompoundTag)value;
                        if(rawPalette.getCompound(block.getInt("state")).getString("Name").equals("minecraft:air"))continue;
                        var p=block.getList("pos",3);var pos=origin.offset(new BlockPos(p.getInt(0),p.getInt(1),p.getInt(2)).rotate(rotation));
                        var state=level.getBlockState(pos);var updated=Block.updateFromNeighbourShapes(state,level,pos);
                        if(!state.equals(updated)){level.setBlock(pos,updated,2|16);changes++;}
                    }
                    if(changes==0)break;
                    helper.assertTrue(pass<7,"Ruin neighbor updates converge");
                }
                var inverse=switch(rotation){case CLOCKWISE_90->Rotation.COUNTERCLOCKWISE_90;case COUNTERCLOCKWISE_90->Rotation.CLOCKWISE_90;default->rotation;};
                var entries=new ArrayList<Object>();
                for(var value:blocks) {
                    var block=(CompoundTag)value;var savedState=rawPalette.getCompound(block.getInt("state"));
                    if(savedState.getString("Name").equals("minecraft:air"))continue;
                    var p=block.getList("pos",3);var pos=origin.offset(new BlockPos(p.getInt(0),p.getInt(1),p.getInt(2)).rotate(rotation));
                    var state=level.getBlockState(pos);var normalized=Block.updateFromNeighbourShapes(state,level,pos);
                    helper.assertTrue(state.equals(normalized),"Stable ruin neighbor state in "+rotation+" at "+p);
                    if(!state.isAir()&&state.getProperties().stream().noneMatch(property->EngineeringStateBake.CONNECTIONS.contains(property.getName())))continue;
                    var tag=NbtUtils.writeBlockState(state.rotate(inverse));var properties=new TreeMap<String,String>();
                    if(tag.contains("Properties",10))for(String key:tag.getCompound("Properties").getAllKeys())properties.put(key,tag.getCompound("Properties").getString(key));
                    var record=new LinkedHashMap<String,Object>();record.put("Name",tag.getString("Name"));if(!properties.isEmpty())record.put("Properties",properties);
                    int index=indices.computeIfAbsent(new Gson().toJson(record),ignored->{palette.add(record);return palette.size()-1;});
                    entries.add(Map.of("pos",List.of(p.getInt(0),p.getInt(1),p.getInt(2)),"state",index));
                }
                variants.put(rotation.name(),entries);
            } finally {for(var chunk:forced)level.setChunkForced(chunk.x,chunk.z,false);}
        }
        Files.writeString(Path.of(System.getProperty("ccq.engineeringRuinBakeOutput")),new Gson().toJson(Map.of("palette",palette,"variants",variants)));
        helper.succeed();
    }
}

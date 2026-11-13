package ccq.core.testground;

import java.util.*;
import java.nio.file.*;
import com.google.gson.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ccq_intact_bake")
@PrefixGameTestTemplate(false)
public class EngineeringStateBake {
    static final BlockPos ORIGIN=new BlockPos(30000,196,30000);
    static final Set<String> CONNECTIONS=Set.of("north","east","south","west","up","down","position","connection","shape","type","support");
    @GameTest(template="empty",batch="authoring_connections",timeoutTicks=1500)
    public static void bakeLightingAndFourRotations(GameTestHelper helper)throws Exception {
        var level=helper.getLevel().getServer().getLevel(TestGroundModule.DIMENSION);
        CompoundTag raw;
        try(var stream=level.getServer().getResourceManager().getResourceOrThrow(ResourceLocation.parse("ccq_testground:structure/engineering/engineering_base.nbt")).open()) {
            raw=NbtIo.readCompressed(stream,NbtAccounter.unlimitedHeap());
        }
        var template=new StructureTemplate();template.load(level.holderLookup(Registries.BLOCK),raw);
        place(level,template,ORIGIN,Rotation.NONE);
        var priorLights=new ArrayList<Object>();
        var output=Path.of(System.getProperty("ccq.engineeringBakeOutput"));
        if(Files.exists(output))for(var element:JsonParser.parseString(Files.readString(output)).getAsJsonObject().getAsJsonArray("lighting")) {
            var entry=element.getAsJsonObject();var p=entry.getAsJsonArray("pos");
            priorLights.add(Map.of("pos",List.of(p.get(0).getAsInt(),p.get(1).getAsInt(),p.get(2).getAsInt()),"from",entry.get("from").getAsString()));
        }
        helper.runAfterDelay(10,()->lightPass(helper,level,raw,priorLights,0));
    }
    private static void place(ServerLevel level,StructureTemplate template,BlockPos origin,Rotation rotation) {
        var settings=new StructurePlaceSettings().setRotation(rotation).setKnownShape(true).setIgnoreEntities(true).setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING);
        var box=template.getBoundingBox(settings,origin);
        for(int x=(box.minX()>>4)-1;x<=(box.maxX()>>4)+1;x++)for(int z=(box.minZ()>>4)-1;z<=(box.maxZ()>>4)+1;z++) {
            level.getChunk(x,z);level.setChunkForced(x,z,true);
        }
        template.placeInWorld(level,origin,origin,settings,level.random,2);
    }
    private static void lightPass(GameTestHelper helper,ServerLevel level,CompoundTag raw,List<Object> lights,int iteration) {
        var lighting=EngineeringTemplateLighting.calculate(level,ORIGIN);
        var candidates=new ArrayList<BlockPos>();
        for(int x=4;x<=115;x++)for(int z=6;z<=293;z++)for(int y:new int[]{1,4,6}) {
            if(y==4&&!((x>=10&&x<=40&&z>=77&&z<=113)||(x>=16&&x<=35&&z>=141&&z<=165)))continue;
            if(y==6&&!(x>=30&&x<=42&&z>=15&&z<=31))continue;
            var pos=ORIGIN.offset(x,y,z);var floor=level.getBlockState(pos);var feet=pos.above();
            if(!floor.isFaceSturdy(level,pos,Direction.UP)||!level.getBlockState(feet).isAir()||!level.getBlockState(feet.above()).isAir())continue;
            if(lighting.getLayerListener(LightLayer.SKY).getLightValue(feet)>=15||lighting.getLayerListener(LightLayer.BLOCK).getLightValue(feet)>=8)continue;
            candidates.add(pos);
        }
        org.slf4j.LoggerFactory.getLogger("ccq_testground").info("Author lighting pass {}: {} dark floor cells",iteration,candidates.size());
        if(!candidates.isEmpty()) {
            helper.assertTrue(iteration<12,"Lighting authoring converges");
            var selected=new ArrayList<BlockPos>();
            for(var dark:candidates) {
                BlockPos pos=null;
                for(int radius=0;radius<=4&&pos==null;radius++)for(int dx=-radius;dx<=radius&&pos==null;dx++)for(int dz=-radius;dz<=radius&&pos==null;dz++) {
                    var candidate=dark.offset(dx,0,dz);var state=level.getBlockState(candidate);
                    var name=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
                    if(Set.of("minecraft:polished_andesite","minecraft:stone_bricks","minecraft:andesite","minecraft:spruce_planks","minecraft:gray_concrete","minecraft:light_gray_concrete").contains(name)
                            &&level.getBlockState(candidate.above()).isAir()&&level.getBlockState(candidate.above(2)).isAir())pos=candidate;
                }
                helper.assertTrue(pos!=null,"Dark work surface has a safe nearby paving light location: "+dark);
                var lightPos=pos;
                if(selected.stream().anyMatch(other->other.distManhattan(lightPos)<=6))continue;
                var old=level.getBlockState(pos);
                lights.add(Map.of("pos",List.of(pos.getX()-ORIGIN.getX(),pos.getY()-ORIGIN.getY(),pos.getZ()-ORIGIN.getZ()),"from",net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(old.getBlock()).toString()));
                level.setBlock(pos,Blocks.SEA_LANTERN.defaultBlockState(),2);selected.add(pos);
            }
            helper.runAfterDelay(1,()->lightPass(helper,level,raw,lights,iteration+1));return;
        }
        var palette=raw.getList("palette",10);int lightIndex=palette.size();palette.add(NbtUtils.writeBlockState(Blocks.SEA_LANTERN.defaultBlockState()));
        var positions=new HashSet<List<Integer>>();
        for(Object entry:lights)positions.add((List<Integer>)((Map<?,?>)entry).get("pos"));
        for(var value:raw.getList("blocks",10)) {
            var block=(CompoundTag)value;var p=block.getList("pos",3);
            if(positions.contains(List.of(p.getInt(0),p.getInt(1),p.getInt(2))))block.putInt("state",lightIndex);
        }
        var variants=new LinkedHashMap<String,Object>();
        var authoredPalette=new ArrayList<Object>();var authoredIndices=new HashMap<String,Integer>();
        for(var rotation:Rotation.values()) {
            var origin=ORIGIN.offset(rotation.ordinal()*512,0,512);
            var template=new StructureTemplate();template.load(level.holderLookup(Registries.BLOCK),raw);
            place(level,template,origin,rotation);
            for(int pass=0;pass<6;pass++) {
                int changed=0;
                for(var value:raw.getList("blocks",10)) {
                    var block=(CompoundTag)value;var p=block.getList("pos",3);
                    var pos=origin.offset(new BlockPos(p.getInt(0),p.getInt(1),p.getInt(2)).rotate(rotation));
                    var state=level.getBlockState(pos);var updated=Block.updateFromNeighbourShapes(state,level,pos);
                    if(!state.equals(updated)) {
                        helper.assertTrue(updated.is(state.getBlock()),"Neighbour normalization does not remove blocks: "+pos+" "+state+" -> "+updated);
                        level.setBlock(pos,updated,2|16);changed++;
                    }
                }
                if(changed==0)break;
            }
            var entries=new ArrayList<Object>();
            var inverse=switch(rotation){case CLOCKWISE_90->Rotation.COUNTERCLOCKWISE_90;case COUNTERCLOCKWISE_90->Rotation.CLOCKWISE_90;default->rotation;};
            for(var value:raw.getList("blocks",10)) {
                var block=(CompoundTag)value;var p=block.getList("pos",3);
                var pos=origin.offset(new BlockPos(p.getInt(0),p.getInt(1),p.getInt(2)).rotate(rotation));
                var state=level.getBlockState(pos);
                if(state.getProperties().stream().noneMatch(property->CONNECTIONS.contains(property.getName())))continue;
                var local=state.rotate(inverse);
                helper.assertTrue(local.rotate(rotation).equals(state),"Block rotations can be encoded reversibly");
                var serialized=NbtUtils.writeBlockState(local);var properties=new TreeMap<String,String>();
                if(serialized.contains("Properties",10))for(String key:serialized.getCompound("Properties").getAllKeys())properties.put(key,serialized.getCompound("Properties").getString(key));
                var saved=new LinkedHashMap<String,Object>();saved.put("Name",serialized.getString("Name"));if(!properties.isEmpty())saved.put("Properties",properties);
                var key=new Gson().toJson(saved);
                int index=authoredIndices.computeIfAbsent(key,ignored->{authoredPalette.add(saved);return authoredPalette.size()-1;});
                entries.add(Map.of("pos",List.of(p.getInt(0),p.getInt(1),p.getInt(2)),"state",index));
            }
            variants.put(rotation.name(),entries);
        }
        try {Files.writeString(Path.of(System.getProperty("ccq.engineeringBakeOutput")),new Gson().toJson(Map.of("palette",authoredPalette,"lighting",lights,"variants",variants)));}
        catch(Exception exception){throw new RuntimeException(exception);}
        org.slf4j.LoggerFactory.getLogger("ccq_testground").info("Authored {} recessed lights and four connection-state variants",lights.size());helper.succeed();
    }
}

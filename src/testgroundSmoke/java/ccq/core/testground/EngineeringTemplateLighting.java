package ccq.core.testground;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.*;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.*;

/** Run vanilla light propagation deterministically for a complete test fixture. */
final class EngineeringTemplateLighting {
    static final Set<String> CONNECTIONS=Set.of("north","east","south","west","up","down","position","connection","shape","type","support");

    static void assertConnections(GameTestHelper helper,ServerLevel level,BlockPos origin,Rotation rotation,ListTag blocks) {
        for(var value:blocks) {
            var p=((CompoundTag)value).getList("pos",3);
            var pos=origin.offset(new BlockPos(p.getInt(0),p.getInt(1),p.getInt(2)).rotate(rotation));
            var actual=level.getBlockState(pos);var connected=Block.updateFromNeighbourShapes(actual,level,pos);
            for(var property:actual.getProperties())if(CONNECTIONS.contains(property.getName()))
                helper.assertTrue(connected.hasProperty(property)&&actual.getValue(property).equals(connected.getValue(property)),"Derived connection state matches neighbors in "+rotation+" at "+p+": "+actual+" -> "+connected);
        }
    }

    static void assertFloorsAndLighting(GameTestHelper helper,ServerLevel level,BlockPos origin,Rotation rotation) {
        var engine=calculate(level,origin);int checked=0,min=15;
        for(int x=4;x<=115;x++)for(int z=6;z<=293;z++) {
            helper.assertTrue(!level.getBlockState(origin.offset(new BlockPos(x,1,z).rotate(rotation))).isAir(),"Factory paving has no missing cells");
            for(int y:new int[]{1,4,6}) {
                if(y==4&&!((x>=10&&x<=40&&z>=77&&z<=113)||(x>=16&&x<=35&&z>=141&&z<=165)))continue;
                if(y==6&&!(x>=30&&x<=42&&z>=15&&z<=31))continue;
                var floor=origin.offset(new BlockPos(x,y,z).rotate(rotation));var feet=floor.above();var state=level.getBlockState(floor);
                if(!state.isFaceSturdy(level,floor,Direction.UP)||!level.getBlockState(feet).isAir()||!level.getBlockState(feet.above()).isAir())continue;
                if(engine.getLayerListener(LightLayer.SKY).getLightValue(feet)>=15)continue;
                int light=engine.getLayerListener(LightLayer.BLOCK).getLightValue(feet);
                helper.assertTrue(light>=8,"Covered factory floor is well lit in "+rotation+" at "+new BlockPos(x,y+1,z)+": "+light);
                checked++;min=Math.min(min,light);
            }
        }
        org.slf4j.LoggerFactory.getLogger("ccq_testground").info("Factory light audit {}: {} covered floor cells, minimum block light {}",rotation,checked,min);
    }
    static LevelLightEngine calculate(ServerLevel level,BlockPos origin) {
        var chunks=new HashMap<Long,LevelChunk>();
        // Include the full footprint for any rotation and a light-propagation margin.
        for(int x=((origin.getX()-300)>>4)-1;x<=((origin.getX()+300)>>4)+1;x++)
            for(int z=((origin.getZ()-300)>>4)-1;z<=((origin.getZ()+300)>>4)+1;z++) {
                if(level.getChunkSource().hasChunk(x,z))chunks.put(ChunkPos.asLong(x,z),level.getChunk(x,z));
            }
        var engine=new LevelLightEngine(new LightChunkGetter(){
            public LightChunk getChunkForLighting(int x,int z){return chunks.get(ChunkPos.asLong(x,z));}
            public BlockGetter getLevel(){return level;}
        },true,true);
        for(var chunk:chunks.values()) {
            chunk.getSkyLightSources().fillFrom(chunk);
            for(int i=0;i<chunk.getSectionsCount();i++)
                engine.updateSectionStatus(SectionPos.of(chunk.getPos(),chunk.getSectionYFromSectionIndex(i)),chunk.getSections()[i].hasOnlyAir());
            engine.setLightEnabled(chunk.getPos(),true);
            engine.propagateLightSources(chunk.getPos());
        }
        while(engine.hasLightWork())engine.runLightUpdates();
        return engine;
    }
}

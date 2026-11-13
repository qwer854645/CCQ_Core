package ccq.core.testground;

import java.util.*;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ccq_omni_gearshift")
@PrefixGameTestTemplate(false)
public class OmniGearshiftGameTests {
    record Rig(BlockPos origin,Direction dir,int kind) {
        BlockPos p(int i){return origin.relative(dir,i);}
        int omni(){return kind==0?3:2;}
        int gear(){return kind==0?2:kind==2?4:3;}
        int end(){return kind==2||kind==3?5:4;}
        BlockPos signal(){return p(gear()).relative(dir.getAxis()==Direction.Axis.Y?Direction.EAST:Direction.UP);}
        float speed(ServerLevel l,int i){return ((KineticBlockEntity)l.getBlockEntity(p(i))).getSpeed();}
        void toggle(ServerLevel l,boolean on){l.setBlock(signal(),on?Blocks.REDSTONE_BLOCK.defaultBlockState():Blocks.AIR.defaultBlockState(),3);}
    }
    static Block block(String id){return BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));}
    static void rpm(ServerLevel level,BlockPos p,int value){try {
        var be=level.getBlockEntity(p);((ScrollValueBehaviour)be.getClass().getField("targetSpeed").get(be)).setValue(value);
    }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
    @GameTest(template="empty",timeoutTicks=240)
    public static void inputAndOutputGearshiftsAndClutchesAcrossEveryAxis(GameTestHelper h)throws Exception {
        var level=h.getLevel().getServer().getLevel(TestGroundModule.DIMENSION);
        var rigs=new ArrayList<Rig>();var chunks=new HashSet<net.minecraft.world.level.ChunkPos>();
        var omni=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("dndesires:omni_speed_controller"));
        int index=0;
        for(var dir:Direction.values())for(int kind=0;kind<5;kind++) {
            var r=new Rig(new BlockPos(110000+(index++)*20,200,110000),dir,kind);rigs.add(r);
            for(int i=0;i<=r.end();i++) {
                var p=r.p(i);var chunk=new net.minecraft.world.level.ChunkPos(p);level.setChunkForced(chunk.x,chunk.z,true);chunks.add(chunk);
                level.setBlock(p,block("create:shaft").defaultBlockState().setValue(BlockStateProperties.AXIS,dir.getAxis()),3);
            }
            level.setBlock(r.p(0),block("create:creative_motor").defaultBlockState().setValue(BlockStateProperties.FACING,dir),3);
            ((CreativeMotorBlockEntity)level.getBlockEntity(r.p(0))).generatedSpeed.setValue(64);
            level.setBlock(r.p(r.omni()),omni.defaultBlockState().setValue(BlockStateProperties.AXIS,dir.getAxis()),3);rpm(level,r.p(r.omni()),16);
            level.setBlock(r.p(r.gear()),block(kind==4?"create:clutch":"create:gearshift").defaultBlockState().setValue(BlockStateProperties.AXIS,dir.getAxis()).setValue(BlockStateProperties.POWERED,false),3);
            if(kind==3){level.setBlock(r.p(4),omni.defaultBlockState().setValue(BlockStateProperties.AXIS,dir.getAxis()),3);rpm(level,r.p(4),16);}
        }
        h.runAfterDelay(40,()->{for(var r:rigs){
            h.assertTrue(r.speed(level,r.end())==16,"Initial target output "+r+" actual="+r.speed(level,r.end()));
            org.slf4j.LoggerFactory.getLogger("ccq_omni").info("OMNIBASE {} kind={} input={} output={}",r.dir,r.kind,r.speed(level,r.omni()),r.speed(level,r.end()));
            r.toggle(level,true);
        }});
        h.runAfterDelay(70,()->{for(var r:rigs){
            float expected=r.kind==4?0:r.kind==0||r.kind==3?16:-16;
            h.assertTrue(r.speed(level,r.end())==expected,"Gearshift/Clutch toggled "+r+" expected="+expected+" actual="+r.speed(level,r.end()));
            org.slf4j.LoggerFactory.getLogger("ccq_omni").info("OMNIREVERSED {} kind={} input={} output={}",r.dir,r.kind,r.speed(level,r.omni()),r.speed(level,r.end()));
            r.toggle(level,false);
        }});
        h.runAfterDelay(100,()->{for(var r:rigs){h.assertTrue(r.speed(level,r.end())==16,"Return to forward");rpm(level,r.p(r.omni()),-32);}});
        h.runAfterDelay(130,()->{for(var r:rigs){
            float expected=r.kind==3?16:-32;
            h.assertTrue(r.speed(level,r.end())==expected,"Negative controller target "+r+" actual="+r.speed(level,r.end()));r.toggle(level,true);
        }});
        h.runAfterDelay(180,()->{try {
            for(var r:rigs){float expected=r.kind==4?0:r.kind==3?16:r.kind==0?-32:32;
                h.assertTrue(r.speed(level,r.end())==expected,"Reversal after negative target "+r+" actual="+r.speed(level,r.end()));
            }
            org.slf4j.LoggerFactory.getLogger("ccq_omni").info("Omni gearshift diagnosis: 30 fixtures passed upstream/downstream/second-controller/clutch checks on all six directions, including negative RPM and repeated switching");h.succeed();
        }finally{chunks.forEach(c->level.setChunkForced(c.x,c.z,false));}});
    }
}

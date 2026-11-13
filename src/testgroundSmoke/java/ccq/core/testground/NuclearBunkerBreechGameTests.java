package ccq.core.testground;

import java.util.*;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ccq_bunker_mechanics")
@PrefixGameTestTemplate(false)
public class NuclearBunkerBreechGameTests {
    record Rig(BlockPos origin,Rotation rotation,int x,int z) {
        int sign(){return z==19?-1:1;}
        int inward(){return x==19?1:-1;}
        BlockPos at(int dx,int y,int dz){return origin.offset(new BlockPos(x+inward()*dx,y,z+sign()*dz).rotate(rotation));}
        void lever(ServerLevel level,int channel,boolean on) {
            var p=at(channel+2,15,-4);var state=level.getBlockState(p);
            if(state.getValue(BlockStateProperties.POWERED)!=on)((LeverBlock)state.getBlock()).pull(state,level,p,null);
        }
        MechanicalPistonBlockEntity lift(ServerLevel level){return (MechanicalPistonBlockEntity)level.getBlockEntity(at(6,25,-3));}
        float open(ServerLevel level){var b=level.getBlockEntity(at(0,18,-3));return b.saveWithFullMetadata(level.registryAccess()).getFloat("Progress");}
    }
    @GameTest(template="empty",batch="bunker_breech",timeoutTicks=700)
    public static void waterPowerDocksOpensClosesAndRetractsBreechBridges(GameTestHelper helper)throws Exception {
        var level=helper.getLevel().getServer().getLevel(TestGroundModule.DIMENSION);
        var source=level.getServer().createCommandSourceStack().withLevel(level).withPermission(4).withSuppressedOutput();
        var rigs=new ArrayList<Rig>();var forced=new HashSet<net.minecraft.world.level.ChunkPos>();var origins=new ArrayList<BlockPos>();
        for(var rotation:Rotation.values()) {
            var origin=new BlockPos(88000+rotation.ordinal()*1024,172,88000);origins.add(origin);
            EngineeringBaseCommands.placeBunker(source,origin.above(8),rotation,Mirror.NONE,1,0);
            var end=origin.offset(new BlockPos(95,31,95).rotate(rotation));
            for(int x=Math.min(origin.getX(),end.getX())>>4;x<=Math.max(origin.getX(),end.getX())>>4;x++)for(int z=Math.min(origin.getZ(),end.getZ())>>4;z<=Math.max(origin.getZ(),end.getZ())>>4;z++) {
                level.setChunkForced(x,z,true);forced.add(new net.minecraft.world.level.ChunkPos(x,z));
            }
            for(int x:new int[]{19,76})for(int z:new int[]{19,76})rigs.add(new Rig(origin,rotation,x,z));
        }
        helper.runAfterDelay(80,()->{for(var r:rigs){
            helper.assertTrue(r.lift(level).offset<.01,"Breech bridge starts retracted: "+r+" offset="+r.lift(level).offset+" speed="+r.lift(level).getSpeed());
            helper.assertTrue(level.getBlockState(r.at(9,20,-7)).is(Blocks.LADDER),"Ladder moves to hall face of shared backing");
            helper.assertTrue(level.getBlockState(r.at(9,25,-6)).isAir(),"Roof ladder exit has headroom");
            helper.assertTrue(level.getBlockState(r.at(7,16,-3)).isAir()&&level.getBlockState(r.at(6,16,-6)).isAir(),"Former free-standing drive columns leave passage clear");
            r.lever(level,6,true);
        }});
        helper.runAfterDelay(150,()->{for(var r:rigs){
            var b=(KineticBlockEntity)level.getBlockEntity(r.at(0,18,-3));
            helper.assertTrue(r.lift(level).offset>2.99&&b.getSpeed()!=0,"Docked bridge powers actual breech: "+r+" offset="+r.lift(level).offset+" speed="+b.getSpeed()+" error="+r.lift(level).getLastAssemblyException());
            helper.assertTrue(r.open(level)<.01,"Default breech setting closes: "+r+" open="+r.open(level));
            r.lever(level,7,true);
        }});
        helper.runAfterDelay(240,()->{for(var r:rigs){helper.assertTrue(r.open(level)>.99,"Water-powered breech fully opens: "+r+" open="+r.open(level));r.lever(level,7,false);}});
        helper.runAfterDelay(330,()->{for(var r:rigs){helper.assertTrue(r.open(level)<.01,"Reversing closes breech: "+r+" open="+r.open(level));r.lever(level,6,false);}});
        helper.runAfterDelay(400,()->{for(var r:rigs){
            helper.assertTrue(r.lift(level).offset<.01,"Lift retracts: "+r+" offset="+r.lift(level).offset+" speed="+r.lift(level).getSpeed());
            helper.assertTrue(level.getBlockState(r.at(0,19,-3)).isAir()&&level.getBlockState(r.at(0,20,-3)).isAir(),"Docking shaft clears cannon rotation");
            r.lever(level,6,true);
        }});
        helper.runAfterDelay(500,()->{for(int i=0;i<origins.size();i++)try {
            EngineeringBaseCommands.placeBunker(source,origins.get(i).above(8),Rotation.values()[i],Mirror.NONE,1,0);
        }catch(Exception e){throw new RuntimeException(e);}});
        helper.runAfterDelay(540,()->{try {
            for(var r:rigs)helper.assertTrue(r.lift(level).offset<.01,"Replacing docked bridge restores parked state");
            org.slf4j.LoggerFactory.getLogger("ccq_testground").info("Breech water power validation: 16 bridges docked and retracted within 70 ticks at 32 RPM, opened, closed and regenerated across four rotations");
            helper.succeed();
        }finally{forced.forEach(c->level.setChunkForced(c.x,c.z,false));}});
    }
}


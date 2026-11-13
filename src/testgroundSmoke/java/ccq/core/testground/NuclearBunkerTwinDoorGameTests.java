package ccq.core.testground;

import java.util.*;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlockEntity;
import com.simibubi.create.content.kinetics.crank.HandCrankBlockEntity;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ccq_bunker_mechanics")
@PrefixGameTestTemplate(false)
public class NuclearBunkerTwinDoorGameTests {
    record Door(BlockPos origin,Rotation rotation,int z,int base,MechanicalPistonBlockEntity piston,HandCrankBlockEntity outside,HandCrankBlockEntity inside) {
        BlockPos at(int x,int y,int zz){return origin.offset(new BlockPos(x,y,zz).rotate(rotation));}
        int left(){return base==2?45:44;}
        int travel(){return base==2?6:4;}
    }
    @GameTest(template="empty",batch="bunker_twin_doors",timeoutTicks=1400)
    public static void twoConcealedGatesOperateIndependentlyInEveryRotation(GameTestHelper helper)throws Exception {
        var level=helper.getLevel().getServer().getLevel(TestGroundModule.DIMENSION);
        var source=level.getServer().createCommandSourceStack().withLevel(level).withPermission(4).withSuppressedOutput();
        var expectedGlue=new HashMap<BlockPos,Integer>();
        var forced=new HashSet<net.minecraft.world.level.ChunkPos>();var outer=new ArrayList<Door>();var inner=new ArrayList<Door>();
        for(var rotation:Rotation.values()) {
            var requested=new BlockPos(50000+rotation.ordinal()*1024,180,50000);var origin=requested.below(8);
            // Deliberately recreate the obsolete blast wall in the future passage.
            var stale=origin.offset(new BlockPos(48,10,77).rotate(rotation));level.getChunk(stale.getX()>>4,stale.getZ()>>4);
            level.setBlock(stale,Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(),2);
            EngineeringBaseCommands.placeBunker(source,requested,rotation,Mirror.NONE,1,0);
            helper.assertTrue(level.getBlockState(stale).isAir(),"Old blast-wall concrete is explicitly erased");
            var bounds=net.minecraft.world.level.levelgen.structure.BoundingBox.fromCorners(origin,origin.offset(new BlockPos(95,31,95).rotate(rotation)));
            for(int x=bounds.minX()>>4;x<=bounds.maxX()>>4;x++)for(int z=bounds.minZ()>>4;z<=bounds.maxZ()>>4;z++) {
                level.setChunkForced(x,z,true);forced.add(new net.minecraft.world.level.ChunkPos(x,z));
            }
            for(int z:new int[]{79,89}) {
                var piston=(MechanicalPistonBlockEntity)level.getBlockEntity(origin.offset(new BlockPos(48,18,z).rotate(rotation)));
                var a=(HandCrankBlockEntity)level.getBlockEntity(origin.offset(new BlockPos(52,10,z+2).rotate(rotation)));
                var b=(HandCrankBlockEntity)level.getBlockEntity(origin.offset(new BlockPos(52,10,z-2).rotate(rotation)));
                helper.assertTrue(piston!=null&&a!=null&&b!=null,"Independent gate hardware exists");
                var door=new Door(origin,rotation,z,9,piston,a,b);(z==79?inner:outer).add(door);
                helper.runAfterDelay(4,()->{
                    var allGateGlue=AABB.ofSize(Vec3.atCenterOf(door.at(48,16,85)),32,32,22);
                    expectedGlue.putIfAbsent(door.origin,level.getEntitiesOfClass(SuperGlueEntity.class,allGateGlue).size());
                    var region=AABB.ofSize(Vec3.atCenterOf(door.at(48,15,z)),28,24,4);
                    var glue=level.getEntitiesOfClass(SuperGlueEntity.class,region);
                    helper.assertTrue(glue.size()>=6,"Six precise glue regions per paired gate: "+rotation+" z="+z+" count="+glue.size());
                    helper.assertTrue(glue.stream().noneMatch(g->g.contains(door.at(48,16,z))),"Fixed ceiling is outside all glue regions");
                });
            }
        }
        drive(helper,outer,5,205,true,true);
        helper.runAfterDelay(210,()->{assertDoor(helper,outer,true);assertDoor(helper,inner,false);});
        drive(helper,inner,220,420,true,true);
        helper.runAfterDelay(425,()->{assertDoor(helper,outer,true);assertDoor(helper,inner,true);});
        drive(helper,outer,435,635,false,false);
        helper.runAfterDelay(640,()->{assertDoor(helper,outer,false);assertDoor(helper,inner,true);});
        drive(helper,inner,650,850,false,false);
        helper.runAfterDelay(855,()->{assertDoor(helper,outer,false);assertDoor(helper,inner,false);});
        // Replace the complete bunker over an OPEN outer gate. The old leaf,
        // glue and first-version concrete must not survive the regeneration.
        drive(helper,outer,870,1070,true,true);
        helper.runAfterDelay(1075,()->{
            assertDoor(helper,outer,true);assertDoor(helper,inner,false);
            for(var d:outer) {
                level.setBlock(d.at(48,10,77),Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(),2);
                try {EngineeringBaseCommands.placeBunker(source,d.origin.above(8),d.rotation,Mirror.NONE,1,0);}
                catch(Exception e){throw new RuntimeException(e);}
                helper.assertTrue(level.getBlockState(d.at(48,10,77)).isAir(),"Rebuild clears obsolete concrete in the original position");
            }
        });
        helper.runAfterDelay(1090,()->{
            try {
                for(var d:outer) {
                    var region=AABB.ofSize(Vec3.atCenterOf(d.at(48,16,85)),32,32,22);
                    helper.assertTrue(level.getEntitiesOfClass(SuperGlueEntity.class,region).size()==expectedGlue.get(d.origin),"Rebuilding restores fresh glue count, without old duplicates: expected="+expectedGlue.get(d.origin)+" actual="+level.getEntitiesOfClass(SuperGlueEntity.class,region).size());
                    helper.assertTrue(level.getEntitiesOfClass(com.simibubi.create.content.contraptions.AbstractContraptionEntity.class,region).isEmpty(),"No old moving gate entity remains");
                    for(int z:new int[]{79,89}) {
                        var p=(MechanicalPistonBlockEntity)level.getBlockEntity(d.at(48,18,z));
                        helper.assertTrue(p!=null&&p.offset<.01f,"Regenerated gates both start closed: "+d.rotation+" z="+z+" offset="+(p==null?"missing":p.offset));
                        for(int x=44;x<=51;x++)for(int y=9;y<=14;y++)helper.assertTrue(!level.getBlockState(d.at(x,y,z)).isAir(),"New closed gate is complete");
                    }
                }
                helper.succeed();
            } finally {forced.forEach(c->level.setChunkForced(c.x,c.z,false));}
        });
    }
    static void drive(GameTestHelper helper,List<Door> doors,int start,int end,boolean opening,boolean outside) {
        boolean[] backwards=new boolean[doors.size()];
        for(int t=start;t<start+40;t+=5)helper.runAfterDelay(t,()->doors.forEach(d->(outside?d.outside:d.inside).turn(false)));
        helper.runAfterDelay(start+40,()->{
            for(int i=0;i<doors.size();i++)backwards[i]=opening?doors.get(i).piston.offset<.01f:doors.get(i).piston.offset>doors.get(i).travel()-.01f;
        });
        for(int t=start+45;t<=end-10;t+=5)helper.runAfterDelay(t,()->{
            for(int i=0;i<doors.size();i++){var d=doors.get(i);(outside?d.outside:d.inside).turn(backwards[i]);}
        });
    }
    static void assertDoor(GameTestHelper helper,List<Door> doors,boolean open) {
        var level=helper.getLevel().getServer().getLevel(TestGroundModule.DIMENSION);
        for(var d:doors) {
            helper.assertTrue(open?d.piston.offset>d.travel()-.01f:d.piston.offset<.01f,"Gate "+d.z+" "+d.rotation+" target open="+open+" offset="+d.piston.offset+" error="+d.piston.getLastAssemblyException());
            for(int x=d.left();x<=(d.base==2?50:51);x++)for(int y=d.base;y<=(d.base==2?d.base+2:14);y++)helper.assertTrue(level.getBlockState(d.at(x,y,d.z)).isAir()==open,"Leaf clearance/return at "+d.at(x,y,d.z));
            if(d.base==2)continue;
            helper.assertTrue(passableAcross(level,d)==open,"Closed gate has no bypass gap; open gate has a traversable passage");
            var right=(MechanicalPistonBlockEntity)level.getBlockEntity(d.at(47,19,d.z));
            helper.assertTrue(open?right.offset>3.99f:right.offset<.01f,"Both leaves move together: "+d.rotation+" right="+right.offset);
            for(int x=34;x<=61;x++)for(int y=9;y<=22;y++) {
                var a=level.getBlockState(d.at(x,y,95));var b=level.getBlockState(d.at(95-x,y,95));
                helper.assertTrue(a.equals(b),"Outer facade is symmetric: x="+x+" y="+y);
            }
            for(int zz=76;zz<=95;zz++)for(int x:new int[]{34,35,60,61})for(int y=9;y<=22;y++)
                helper.assertTrue(!level.getBlockState(d.at(x,y,zz)).isAir(),"Continuous side shell joins gate to hull");
            for(int zz:new int[]{78,80,88,90,94,95})for(int x=44;x<=51;x++)
                helper.assertTrue(!level.getBlockState(d.at(x,15,zz)).isAir(),"Continuous lintel without top slit");

        }
    }
    static boolean passableAcross(net.minecraft.server.level.ServerLevel level,Door d) {
        var queue=new java.util.ArrayDeque<BlockPos>();var visited=new java.util.HashSet<BlockPos>();
        var start=new BlockPos(47,9,d.z-1);queue.add(start);visited.add(start);
        while(!queue.isEmpty()) {
            var p=queue.remove();if(p.equals(new BlockPos(47,9,d.z+1)))return true;
            for(var direction:Direction.values()) {
                var q=p.relative(direction);
                if(q.getX()<34||q.getX()>61||q.getY()<9||q.getY()>20||q.getZ()<d.z-1||q.getZ()>d.z+1||!visited.add(q))continue;
                var w=d.at(q.getX(),q.getY(),q.getZ());var player=new AABB(w.getX()+.2,w.getY(),w.getZ()+.2,w.getX()+.8,w.getY()+1.8,w.getZ()+.8);
                if(!level.getBlockCollisions(null,player).iterator().hasNext())queue.add(q);
            }
        }
        return false;
    }

}

package ccq.core.testground;

import java.util.*;
import com.simibubi.create.content.kinetics.base.*;
import com.simibubi.create.content.kinetics.crank.HandCrankBlockEntity;
import com.simibubi.create.content.contraptions.pulley.PulleyBlockEntity;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ccq_testground")
@PrefixGameTestTemplate(false)
public class EngineeringYardGameTests {
    @GameTest(template="empty",batch="engineering_yards",timeoutTicks=600)
    public static void completedYardsOperateAndRemainAccessible(GameTestHelper helper) {
        var level=helper.getLevel().getServer().getLevel(TestGroundModule.DIMENSION);
        var origin=new BlockPos(24000,196,24000);
        var template=level.getServer().getStructureManager().getOrCreate(ResourceLocation.parse("ccq_testground:engineering/engineering_base"));
        var settings=new StructurePlaceSettings().setIgnoreEntities(true).setKnownShape(true);
        var bounds=template.getBoundingBox(settings,origin);var forced=new HashSet<ChunkPos>();
        for(int x=(bounds.minX()>>4)-1;x<=(bounds.maxX()>>4)+1;x++)for(int z=(bounds.minZ()>>4)-1;z<=(bounds.maxZ()>>4)+1;z++) {
            level.getChunk(x,z);level.setChunkForced(x,z,true);forced.add(new ChunkPos(x,z));
        }
        helper.assertTrue(template.placeInWorld(level,origin,origin,settings,level.random,2),"Expanded base placed");
        var cranks=new ArrayList<HandCrankBlockEntity>();var reversed=new HashSet<HandCrankBlockEntity>();
        boolean[] running={false};
        helper.onEachTick(()->{if(running[0])cranks.forEach(crank->crank.turn(reversed.contains(crank)));});
        helper.runAfterDelay(40,()->{
            for(var local:List.of(new BlockPos(17,14,152),new BlockPos(98,4,50),new BlockPos(109,3,51),new BlockPos(20,4,236))) {
                var pos=origin.offset(local);var state=level.getBlockState(pos);
                var inward=state.getValue(BlockStateProperties.FACING).getOpposite();
                var target=pos.relative(inward);var neighbor=level.getBlockState(target);
                helper.assertTrue(neighbor.getBlock()instanceof IRotate rotate&&rotate.hasShaftTowards(level,target,neighbor,inward.getOpposite()),"Yard crank connects to an actual shaft face: "+local);
                cranks.add((HandCrankBlockEntity)level.getBlockEntity(pos));
            }
            for(int z=6;z<=294;z++) {
                var pos=origin.offset(51,2,z);
                helper.assertTrue(level.noCollision(null,new AABB(pos.getX()+.2,pos.getY()+.001,pos.getZ()+.2,pos.getX()+.8,pos.getY()+1.8,pos.getZ()+.8)),"Main road and rear exit stay clear at "+z);
            }
            for(int x:new int[]{3,116})for(int z=5;z<=294;z++)for(int y=1;y<=4;y++)
                helper.assertTrue(!level.getBlockState(origin.offset(x,y,z)).isAir(),"Continuous side boundary: "+x+","+z+","+y);
            for(int z:new int[]{5,294})for(int x=4;x<=115;x++) {
                if((z==5&&43<=x&&x<=59)||(z==294&&46<=x&&x<=56))continue;
                for(int y=1;y<=4;y++)helper.assertTrue(!level.getBlockState(origin.offset(x,y,z)).isAir(),"Front/rear wall continuous outside gates");
            }
            running[0]=true;
            helper.runAfterDelay(4,()->{
                var left=(PulleyBlockEntity)level.getBlockEntity(origin.offset(22,14,152));
                if(left.getSpeed()>0)reversed.add(cranks.getFirst());
                helper.runAfterDelay(100,()->{
                    var a=(PulleyBlockEntity)level.getBlockEntity(origin.offset(22,14,152));
                    var b=(PulleyBlockEntity)level.getBlockEntity(origin.offset(30,14,152));
                    helper.assertTrue(a.getSpeed()!=0&&a.getSpeed()==b.getSpeed(),"Assembly yard lift is synchronized");
                    var parent=a.getMirrorParent()==null?a:b;
                    helper.assertTrue(parent.getAttachedContraption()!=null&&parent.getInterpolatedOffset(1)<7.9f,"Assembly yard lift actually raises its beam");
                    helper.assertTrue(parent.getAttachedContraption().getContraption().getBlocks().size()==9,"Entire nine-block lifting beam moves together");
                    var press=(KineticBlockEntity)level.getBlockEntity(origin.offset(98,4,51));
                    var mixer=(KineticBlockEntity)level.getBlockEntity(origin.offset(107,4,51));
                    var pump=(KineticBlockEntity)level.getBlockEntity(origin.offset(19,3,236));
                    helper.assertTrue(press.getSpeed()!=0,"Service annex press receives real rotation");
                    helper.assertTrue(mixer.getSpeed()!=0,"Fabrication bay mixer receives real rotation");
                    helper.assertTrue(pump.getSpeed()!=0,"Utility pump receives rotation through its cogwheel");
                    helper.assertTrue(level.getFluidState(origin.offset(22,2,236)).is(net.minecraft.tags.FluidTags.WATER),"Utility pump actually supplies its low separate basin");
                    helper.assertTrue(level.getBlockState(origin.offset(26,3,236)).isAir(),"Output basin rim is only one block above the yard");
                    running[0]=false;a.disassemble();b.disassemble();
                    forced.forEach(pos->level.setChunkForced(pos.x,pos.z,false));helper.succeed();
                });
            });
        });
    }
}

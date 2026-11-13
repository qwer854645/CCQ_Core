package ccq.core.testground;

import java.util.*;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.waterwheel.WaterWheelBlockEntity;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ccq_bunker_mechanics")
@PrefixGameTestTemplate(false)
public class NuclearBunkerPowerGameTests {
    static final int[] PRESS={35,38,41},FAN={46,49,52},MILL={57,60,63},SAW={35,39,43};
    record Rig(BlockPos origin,Rotation rotation) {
        BlockPos at(int x,int y,int z){return origin.offset(new BlockPos(x,y,z).rotate(rotation));}
    }
    @GameTest(template="empty",batch="bunker_water_power",timeoutTicks=900)
    public static void allWallMountedMachineRowsReceivePowerAndProduceActualOutputs(GameTestHelper helper)throws Exception {
        var level=helper.getLevel().getServer().getLevel(TestGroundModule.DIMENSION);
        var source=level.getServer().createCommandSourceStack().withLevel(level).withPermission(4).withSuppressedOutput();
        var rigs=new ArrayList<Rig>();var forced=new HashSet<net.minecraft.world.level.ChunkPos>();
        for(var rotation:Rotation.values()) {
            var requested=new BlockPos(80000+rotation.ordinal()*1024,180,80000);var origin=requested.below(8);
            EngineeringBaseCommands.placeBunker(source,requested,rotation,Mirror.NONE,1,0);rigs.add(new Rig(origin,rotation));
            var b=origin.offset(new BlockPos(95,31,95).rotate(rotation));
            for(int x=Math.min(origin.getX(),b.getX())>>4;x<=Math.max(origin.getX(),b.getX())>>4;x++)for(int z=Math.min(origin.getZ(),b.getZ())>>4;z<=Math.max(origin.getZ(),b.getZ())>>4;z++) {
                level.setChunkForced(x,z,true);forced.add(new net.minecraft.world.level.ChunkPos(x,z));
            }
        }
        helper.runAfterDelay(100,()->{for(var r:rigs)omni(level,r,16);});
        helper.runAfterDelay(130,()->{for(var r:rigs) {
            var press=(KineticBlockEntity)level.getBlockEntity(r.at(20,17,35));
            helper.assertTrue(Math.abs(press.getSpeed())==16,"Omni master controller changes actual workshop RPM in "+r.rotation+" speed="+press.getSpeed()+" theoretical="+press.getTheoreticalSpeed()+" overstress="+press.isOverStressed());
            omni(level,r,8);
        }});
        helper.runAfterDelay(160,()->{
            for(var r:rigs) {
                for(int x=69;x<=80;x++) {
                    var w=(WaterWheelBlockEntity)level.getBlockEntity(r.at(x,10,19));
                    helper.assertTrue(Math.abs(w.getGeneratedSpeed())>=4,"Waterwheel produces torque: "+r.rotation+" x="+x);
                }
                for(int z:PRESS) {
                    powered(helper,level,r.at(20,17,z));
                    ((DepotBlockEntity)level.getBlockEntity(r.at(20,15,z))).setHeldItem(new ItemStack(Items.IRON_INGOT));
                }
                for(int z:FAN) {
                    powered(helper,level,r.at(21,16,z));
                    var input=z==46?Items.BEEF:z==49?net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("create:crushed_raw_iron")):Items.RAW_IRON;
                    ((DepotBlockEntity)level.getBlockEntity(r.at(23,15,z))).setHeldItem(new ItemStack(input));
                }
                for(int z:MILL) {
                    powered(helper,level,r.at(20,16,z));
                    var m=(com.simibubi.create.content.kinetics.millstone.MillstoneBlockEntity)level.getBlockEntity(r.at(20,16,z));
                    m.inputInv.insertItem(0,new ItemStack(Items.WHEAT),false);
                }
                for(int x:SAW) {
                    powered(helper,level,r.at(x,15,20));
                    var saw=(com.simibubi.create.content.kinetics.saw.SawBlockEntity)level.getBlockEntity(r.at(x,15,20));
                    helper.assertTrue(saw.saveWithFullMetadata(level.registryAccess()).getCompound("Filter").getString("id").equals("minecraft:stripped_oak_log"),"Saw retains correct recipe filter");
                    var p=net.minecraft.world.phys.Vec3.atCenterOf(saw.getBlockPos()).add(0,.7,0);
                    saw.insertItem(new net.minecraft.world.entity.item.ItemEntity(level,p.x,p.y,p.z,new ItemStack(Items.OAK_LOG)));
                }
            }
        });
        helper.runAfterDelay(800,()->{
            try {
                for(var r:rigs) {
                    for(int z:PRESS) {
                        var depot=(DepotBlockEntity)level.getBlockEntity(r.at(20,15,z));
                        helper.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(depot.getHeldItem().getItem()).toString().equals("create:iron_sheet"),"Every press makes iron sheet: "+r.rotation+" z="+z);
                    }
                    for(int z:FAN) {
                        var depot=(DepotBlockEntity)level.getBlockEntity(r.at(23,15,z));
                        var fan=(com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity)level.getBlockEntity(r.at(21,16,z));
                        var expected=z==46?Items.COOKED_BEEF:z==49?Items.IRON_NUGGET:Items.IRON_INGOT;
                        helper.assertTrue(depot.getHeldItem().is(expected),"Each fan completes its assigned process: "+r.rotation+" z="+z+" expected="+expected+" result="+depot.getHeldItem()+" speed="+fan.getSpeed()+" flow="+fan.getAirFlowDirection());
                        helper.assertTrue(!fan.getAirCurrent().bounds.contains(net.minecraft.world.phys.Vec3.atCenterOf(r.at(25,16,z))),"Trapdoor shield keeps airflow out of the operator aisle");
                    }
                    for(int z:MILL) {
                        var mill=(com.simibubi.create.content.kinetics.millstone.MillstoneBlockEntity)level.getBlockEntity(r.at(20,16,z));
                        helper.assertTrue(!mill.outputInv.getStackInSlot(0).isEmpty(),"Every millstone processes wheat: "+r.rotation+" z="+z);
                    }
                    // Rotated corners need a normalized AABB.
                    var a=r.at(28,13,12);var b=r.at(50,22,30);
                    var area=new net.minecraft.world.phys.AABB(Math.min(a.getX(),b.getX()),Math.min(a.getY(),b.getY()),Math.min(a.getZ(),b.getZ()),Math.max(a.getX(),b.getX())+1,Math.max(a.getY(),b.getY())+1,Math.max(a.getZ(),b.getZ())+1).inflate(8);
                    int stripped=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,area).stream().filter(e->e.getItem().is(Items.STRIPPED_OAK_LOG)).mapToInt(e->e.getItem().getCount()).sum();
                    helper.assertTrue(stripped>=3,"All three saws eject their cut logs: "+r.rotation+" count="+stripped);
                }
                org.slf4j.LoggerFactory.getLogger("ccq_testground").info("Wall workshop validation: all 48 machines across four rotations completed real recipes without overstress");
                helper.succeed();
            }finally {forced.forEach(c->level.setChunkForced(c.x,c.z,false));}
        });
    }
    private static void omni(net.minecraft.server.level.ServerLevel level,Rig r,int rpm) {
        try {
            var be=level.getBlockEntity(r.at(64,11,19));
            ((com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour)be.getClass().getField("targetSpeed").get(be)).setValue(rpm);
        }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
    }
    private static void powered(GameTestHelper helper,net.minecraft.server.level.ServerLevel level,BlockPos pos) {
        var be=(KineticBlockEntity)level.getBlockEntity(pos);
        helper.assertTrue(be!=null&&Math.abs(be.getSpeed())>=8&&!be.isOverStressed(),"Wall machine receives usable power at "+pos+" speed="+(be==null?"missing":be.getSpeed()));
    }
}

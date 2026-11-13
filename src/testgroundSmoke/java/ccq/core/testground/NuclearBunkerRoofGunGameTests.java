package ccq.core.testground;

import java.util.*;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.kinetics.speedController.SpeedControllerBlockEntity;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ccq_bunker_mechanics")
@PrefixGameTestTemplate(false)
public class NuclearBunkerRoofGunGameTests {
    static class Gun {
        final BlockPos origin;final Rotation rotation;final int x,z,sign;final Object mount;
        final SpeedControllerBlockEntity controller;
        AbstractContraptionEntity entity;float lastYaw,totalYaw,signedYaw,reverseBase,pitchBase,polarity;int quadrants;
        Gun(ServerLevel level,BlockPos origin,Rotation rotation,int x,int z) {
            this.origin=origin;this.rotation=rotation;this.x=x;this.z=z;sign=z==19?-1:1;
            mount=level.getBlockEntity(at(0,16,0));
            controller=(SpeedControllerBlockEntity)level.getBlockEntity(at(3,15,0));
        }
        BlockPos at(int dx,int y,int dz){return origin.offset(new BlockPos(x+sign*dx,y,z+sign*dz).rotate(rotation));}
        void reverse(ServerLevel level,boolean pitch,boolean powered) {
            pull(level,control(pitch?4:2),powered);
        }
        BlockPos control(int i){int towardHall=(x==19?1:-1)*sign;return at(towardHall*(i+2),15,-4);}
        void pull(ServerLevel level,BlockPos lever,boolean powered) {
            var state=level.getBlockState(lever);
            if(state.getValue(BlockStateProperties.POWERED)!=powered)
                ((LeverBlock)state.getBlock()).pull(state,level,lever,null);
        }
        float angle(String name){return ((Number)call(mount,name,new Class<?>[]{float.class},1f)).floatValue();}
        void clutch(ServerLevel level,boolean pitch,boolean powered) {
            pull(level,control(pitch?3:1),powered);
        }
    }
    @GameTest(template="empty",batch="bunker_roof_guns",timeoutTicks=1200)
    public static void roofGunsActuallyTurnFullCircleAndPitchBothWays(GameTestHelper helper)throws Exception {
        var level=helper.getLevel().getServer().getLevel(TestGroundModule.DIMENSION);
        var source=level.getServer().createCommandSourceStack().withLevel(level).withPermission(4).withSuppressedOutput();
        var forced=new HashSet<net.minecraft.world.level.ChunkPos>();var guns=new ArrayList<Gun>();var origins=new ArrayList<BlockPos>();
        for(var rotation:Rotation.values()) {
            var requested=new BlockPos(72000+rotation.ordinal()*1024,180,72000);var origin=requested.below(8);origins.add(origin);
            EngineeringBaseCommands.placeBunker(source,requested,rotation,Mirror.NONE,1,0);
            var bounds=net.minecraft.world.level.levelgen.structure.BoundingBox.fromCorners(origin,origin.offset(new BlockPos(95,31,95).rotate(rotation)));
            for(int x=bounds.minX()>>4;x<=bounds.maxX()>>4;x++)for(int z=bounds.minZ()>>4;z<=bounds.maxZ()>>4;z++) {
                level.setChunkForced(x,z,true);forced.add(new net.minecraft.world.level.ChunkPos(x,z));
            }
            for(int x:new int[]{19,76})for(int z:new int[]{19,76})guns.add(new Gun(level,origin,rotation,x,z));
        }
        // Broad firing galleries must pass outgoing bore lines, including downward
        // fire over the breastwork and upward fire below the concrete lintel.
        helper.runAfterDelay(30,()->{
            for(var g:guns)for(int angle:new int[]{-20,-10,0,10,20,30,40,45,50,60,70,80,90,100,110})for(int pitch:new int[]{-10,0,10})for(int margin:new int[]{-2,-1,0,1,2}) {
                double a=Math.toRadians(angle),p=Math.toRadians(pitch);
                double dx=(g.x==19?-1:1)*Math.cos(a),dz=(g.z==19?-1:1)*Math.sin(a);
                var east=g.rotation.rotate(Direction.EAST);var south=g.rotation.rotate(Direction.SOUTH);
                var v=new Vec3(dx*east.getStepX()+dz*south.getStepX(),Math.tan(p),dx*east.getStepZ()+dz*south.getStepZ());
                var pivot=Vec3.atLowerCornerOf(g.origin.offset(new BlockPos(g.x,18,g.z).rotate(g.rotation))).add(.5,.5,.5);
                pivot=pivot.add(margin==-2?new Vec3(0,-.2,0):margin==2?new Vec3(0,.2,0):margin==-1?new Vec3(-v.z*.2,0,v.x*.2):margin==1?new Vec3(v.z*.2,0,-v.x*.2):Vec3.ZERO);
                var hit=level.clip(new net.minecraft.world.level.ClipContext(pivot.add(v.scale(6)),pivot.add(v.scale(18)),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,net.minecraft.world.phys.shapes.CollisionContext.empty()));
                helper.assertTrue(hit.getType()==HitResult.Type.MISS,"Half-open firing gallery is clear: "+g.rotation+" gun="+g.x+","+g.z+" bearing="+angle+" pitch="+pitch+" hit="+hit.getBlockPos());
            }
        });
        boolean[] running={false};int[] phase={0};float[] target={10};int[] ticks={0};
        helper.onEachTick(()->{
            ticks[0]++;
            if(!running[0])return;
            for(var g:guns) {
                if(phase[0]==1) {
                    float yaw=g.angle("getYawOffset");g.totalYaw+=Math.abs(Mth.wrapDegrees(yaw-g.lastYaw));g.signedYaw+=Mth.wrapDegrees(yaw-g.lastYaw);g.lastYaw=yaw;
                    g.quadrants|=1<<(int)(((yaw%360+360)%360)/90);
                    if(ticks[0]%20==0)clearance(helper,level,g);
                } else if(phase[0]==2) {
                    float error=target[0]-g.angle("getPitchOffset");
                    int speed=Math.abs(error)<.7f?0:8;
                    g.reverse(level,true,Math.signum(error)*g.polarity<0);g.controller.targetSpeed.setValue(speed);
                }
            }
        });
        helper.runAfterDelay(40,()->{
            for(var g:guns) {
                g.pull(level,g.control(0),true);
                g.clutch(level,true,true);g.clutch(level,false,false);g.controller.targetSpeed.setValue(16);
            }
            running[0]=true;
        });
        helper.runAfterDelay(50,()->{for(var g:guns){
                g.entity=(AbstractContraptionEntity)call(g.mount,"getContraption",new Class<?>[]{});
                helper.assertTrue(g.entity!=null&&g.entity.getContraption().getBlocks().size()==8,"Eight-part roof gun assembles in "+g.rotation);
        }});
        helper.runAfterDelay(55,()->{for(var g:guns)g.lastYaw=g.angle("getYawOffset");phase[0]=1;});
        helper.runAfterDelay(680,()->{
            phase[0]=0;
            for(var g:guns) {
                var yaw=(com.simibubi.create.content.kinetics.base.KineticBlockEntity)call(g.mount,"getYawInterface",new Class<?>[]{});
                helper.assertTrue(g.totalYaw>355&&g.quadrants==15,"Actual full-circle yaw: "+g.rotation+" gun="+g.x+","+g.z+" sweep="+g.totalYaw+" speed="+yaw.getSpeed()+" theoretical="+yaw.getTheoreticalSpeed()+" running="+call(g.mount,"isRunning",new Class<?>[]{}));
                g.reverseBase=g.angle("getYawOffset");g.reverse(level,false,true);
            }
        });
        helper.runAfterDelay(745,()->{
            for(var g:guns) {
                float delta=Mth.wrapDegrees(g.angle("getYawOffset")-g.reverseBase);
                helper.assertTrue(delta*Math.signum(g.signedYaw)<-10,"Independent yaw reverse switch changes actual direction: "+g.rotation+" delta="+delta+" previous="+g.signedYaw+" gear="+level.getBlockState(g.at(4,14,3))+" lever="+level.getBlockState(g.at(5,15,3))+" support="+level.getBlockState(g.at(5,14,3))+" signal="+level.hasNeighborSignal(g.at(4,14,3)));
                g.controller.targetSpeed.setValue(0);g.clutch(level,false,true);g.clutch(level,true,false);
            }
        });
        helper.runAfterDelay(755,()->{for(var g:guns){g.pitchBase=g.angle("getPitchOffset");g.controller.targetSpeed.setValue(8);}});
        helper.runAfterDelay(760,()->{
            for(var g:guns){float change=g.angle("getPitchOffset")-g.pitchBase;helper.assertTrue(Math.abs(change)>.3,"Pitch interface actually moves");g.polarity=Math.signum(change);}
            phase[0]=2;
        });
        helper.runAfterDelay(800,()->{
            for(var g:guns){helper.assertTrue(Math.abs(g.angle("getPitchOffset")-10)<1.6,"Positive pitch reaches 10 degrees: "+g.angle("getPitchOffset"));clearance(helper,level,g);}
            target[0]=-10;
        });
        helper.runAfterDelay(890,()->{
            for(var g:guns){helper.assertTrue(Math.abs(g.angle("getPitchOffset")+10)<1.6,"Negative pitch reaches -10 degrees: "+g.angle("getPitchOffset"));clearance(helper,level,g);}
            running[0]=false;phase[0]=0;
            for(var g:guns)g.controller.targetSpeed.setValue(0);
        });
        helper.runAfterDelay(900,()->{
            for(int i=0;i<origins.size();i++) {
                try {EngineeringBaseCommands.placeBunker(source,origins.get(i).above(8),Rotation.values()[i],Mirror.NONE,1,0);}
                catch(Exception e){throw new RuntimeException(e);}
            }
        });
        helper.runAfterDelay(920,()->{
            // Exercise one fire channel at a time; no ammunition is inserted.
            var g=guns.getFirst();var p=g.control(5);var state=level.getBlockState(p);
            ((ButtonBlock)state.getBlock()).press(state,level,p,null);
        });
        helper.runAfterDelay(927,()->{
            for(int i=0;i<guns.size();i++) {
                var g=guns.get(i);var state=level.getBlockState(g.at(0,16,0));
                helper.assertTrue(mountFlag(state,"fire_powered")== (i==0),"Fire button reaches only its own gun");
                helper.assertTrue(!mountFlag(state,"assembly_powered"),"Fire button does not assemble or affect other control channels");
            }
        });
        helper.runAfterDelay(952,()->{
            for(var g:guns)helper.assertTrue(!mountFlag(level.getBlockState(g.at(0,16,0)),"fire_powered"),"Fire button pulse releases automatically");
        });
        helper.runAfterDelay(965,()->{
            try {
                for(var origin:origins) {
                    var region=AABB.ofSize(Vec3.atCenterOf(origin.offset(48,16,48)),200,40,200);
                    helper.assertTrue(level.getEntitiesOfClass(AbstractContraptionEntity.class,region).isEmpty(),"Regeneration removes the old aimed cannon entities");
                }
                org.slf4j.LoggerFactory.getLogger("ccq_testground").info("Roof cannon validation: {} water-powered guns completed full yaw, wireless assembly and independent yaw/pitch reversal, isolated fire-button pulse and aimed regeneration",guns.size());helper.succeed();
            } finally {forced.forEach(c->level.setChunkForced(c.x,c.z,false));}
        });
    }
    private static boolean mountFlag(net.minecraft.world.level.block.state.BlockState state,String name) {
        return (Boolean)state.getValue((net.minecraft.world.level.block.state.properties.BooleanProperty)state.getBlock().getStateDefinition().getProperty(name));
    }
    private static void clearance(GameTestHelper helper,ServerLevel level,Gun g) {
        helper.assertTrue(!g.entity.isStalled(),"Roof gun is not stalled by walls or ceiling");
        for(var pos:g.entity.getContraption().getBlocks().keySet()) {
            double minX=Double.POSITIVE_INFINITY,minY=minX,minZ=minX,maxX=Double.NEGATIVE_INFINITY,maxY=maxX,maxZ=maxX;
            for(int x=0;x<=1;x++)for(int y=0;y<=1;y++)for(int z=0;z<=1;z++) {
                var v=g.entity.toGlobalVector(new Vec3(pos.getX()+x,pos.getY()+y,pos.getZ()+z),1);
                minX=Math.min(minX,v.x);minY=Math.min(minY,v.y);minZ=Math.min(minZ,v.z);maxX=Math.max(maxX,v.x);maxY=Math.max(maxY,v.y);maxZ=Math.max(maxZ,v.z);
            }
            var box=new AABB(minX,minY,minZ,maxX,maxY,maxZ).deflate(.03);
            helper.assertTrue(!level.getBlockCollisions(null,box).iterator().hasNext(),"Rotated gun clears actual world geometry: "+g.rotation+" gun="+g.x+","+g.z+" yaw="+g.angle("getYawOffset")+" pitch="+g.angle("getPitchOffset")+" local="+pos);
        }
    }
    private static Object call(Object target,String name,Class<?>[] types,Object... args) {
        try{return target.getClass().getMethod(name,types).invoke(target,args);}
        catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
    }
}

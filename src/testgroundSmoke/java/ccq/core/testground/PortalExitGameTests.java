package ccq.core.testground;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ccq_portal_exit")
@PrefixGameTestTemplate(false)
public class PortalExitGameTests {
    @GameTest(template="empty",batch="fallback",timeoutTicks=1800)
    public static void floodedAndSolidColumnsAlwaysProduceDryStableExit(GameTestHelper h) {
        ServerLevel level=h.getLevel();
        var frames=new java.util.ArrayList<PortalFrame>();
        for(int i=0;i<2;i++) {
            var center=new BlockPos(180000+i*256,level.getMaxBuildHeight()-2,180000);
            int base=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,center.getX(),center.getZ());
            // Fill all candidate columns up to build height, so natural placement cannot fit.
            for(int x=-20;x<=42;x++)for(int z=-20;z<=42;z++)for(int y=base;y<level.getMaxBuildHeight();y++)
                level.setBlock(new BlockPos(center.getX()+x,y,center.getZ()+z),(i==0?Blocks.WATER:Blocks.STONE).defaultBlockState(),18);
            var axis=i==0?Direction.Axis.X:Direction.Axis.Z;
            var source=new PortalFrame(center,axis,i==0?2:21,i==0?3:21);
            var frame=PortalExitBuilder.create(level,center,source);
            h.assertTrue(frame!=null,"Fallback exists for water/solid columns");
            for(int cx=(frame.origin().getX()-16)>>4;cx<=(frame.origin().getX()+frame.width()+16)>>4;cx++)for(int cz=(frame.origin().getZ()-16)>>4;cz<=(frame.origin().getZ()+frame.width()+16)>>4;cz++)level.setChunkForced(cx,cz,true);
            verify(h,level,frame);h.assertTrue(frame.axis()==axis&&frame.width()==source.width()&&frame.height()==source.height(),"Preserves source/train aperture and orientation");frames.add(frame);
        }
        h.runAfterDelay(30,()->{try{for(var frame:frames)verify(h,level,frame);h.succeed();}finally{
            for(var frame:frames)for(int cx=(frame.origin().getX()-16)>>4;cx<=(frame.origin().getX()+frame.width()+16)>>4;cx++)for(int cz=(frame.origin().getZ()-16)>>4;cz<=(frame.origin().getZ()+frame.width()+16)>>4;cz++)level.setChunkForced(cx,cz,false);
        }});
    }
    @GameTest(template="empty",batch="void_border",timeoutTicks=1200)
    public static void voidAndBorderUseInBoundsPlatform(GameTestHelper h) {
        ServerLevel level=h.getLevel();var border=level.getWorldBorder();
        double cx=border.getCenterX(),cz=border.getCenterZ(),size=border.getSize();
        try {
            border.setCenter(-190000,-190000);border.setSize(64);
            // GameTest's superflat world has air outside its shallow bottom slab.
            for(int x=-190032;x<=-189968;x++)for(int z=-190032;z<=-189968;z++)for(int y=level.getMinBuildHeight();y<=-55;y++)
                level.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),18);
            for(var axis:new Direction.Axis[]{Direction.Axis.X,Direction.Axis.Z}) {
                var source=new PortalFrame(new BlockPos(-200000,500,-200000),axis,4,5);
                var frame=PortalExitBuilder.create(level,source.origin(),source);verify(h,level,frame);
                var normal=axis==Direction.Axis.X?Direction.SOUTH:Direction.EAST;
                for(int x=-1;x<=frame.width();x++)for(int z=-1;z<=1;z++)h.assertTrue(border.isWithinBounds(frame.at(x,-1).relative(normal,z)),"Complete platform remains inside border");
            }
        }finally{border.setCenter(cx,cz);border.setSize(size);}
        h.succeed();
    }
    @GameTest(template="empty",batch="natural",timeoutTicks=300)
    public static void replaceablePlantsAndBuiltFloorsAreValidSites(GameTestHelper h) {
        var level=h.getLevel();var preferred=new BlockPos(182000,200,182000);
        for(int x=-2;x<=7;x++)for(int z=-2;z<=2;z++) {
            level.setBlock(preferred.offset(x,-1,z),Blocks.BRICKS.defaultBlockState(),18);
            level.setBlock(preferred.offset(x,0,z),Blocks.SHORT_GRASS.defaultBlockState(),18);
        }
        var source=new PortalFrame(preferred,Direction.Axis.X,4,5);
        var exit=PortalExitBuilder.create(level,preferred,source);verify(h,level,exit);
        h.assertTrue(exit.origin().equals(preferred),"Use nearby built floor and clear plants instead of failing or moving away");h.succeed();
    }
    @GameTest(template="empty",batch="existing",timeoutTicks=300)
    public static void createTracksStillPairAndReturn(GameTestHelper h){TestGroundGameTests.nativeCreatePortalTracks(h);}
    @GameTest(template="empty",batch="items",timeoutTicks=500)
    public static void itemsAndExperienceStillTeleport(GameTestHelper h){TestGroundGameTests.stationaryDropsAndExperienceTeleport(h);}
    @GameTest(template="empty",batch="persistence",timeoutTicks=100)
    public static void framesAndLinksStillPersist(GameTestHelper h){TestGroundGameTests.frameAndPairPersistence(h);}
    static void verify(GameTestHelper h,ServerLevel level,PortalFrame f) {
        h.assertTrue(f!=null,"Exit must exist");h.assertTrue(f.isComplete(level),"Portal surface stays lit");
        h.assertTrue(f.equals(PortalFrame.find(level,f.origin(),f.axis())),"Complete structural frame");
        var normal=f.axis()==Direction.Axis.X?Direction.SOUTH:Direction.EAST;
        for(int x=0;x<f.width();x++)for(int z=-1;z<=1;z++) {
            h.assertTrue(PortalFrame.isFrame(level.getBlockState(f.at(x,-1).relative(normal,z))),"Solid landing floor");
            for(int y=0;y<f.height();y++) {
                var state=level.getBlockState(f.at(x,y).relative(normal,z));
                h.assertTrue(z==0?state.is(TestGroundModule.PORTAL.get()):state.isAir(),"Dry clear arrival and both approaches");
            }
        }
    }
}

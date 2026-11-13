package ccq.core.testground;

import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.Heightmap;

/** Vanilla-style vertical site search followed by a guaranteed cleared platform. */
final class PortalExitBuilder {
    private PortalExitBuilder() {}
    static PortalFrame create(ServerLevel level,BlockPos preferred,PortalFrame source) {
        var material=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("create:industrial_iron_block"));
        if(material==Blocks.AIR)return null;
        int minY=level.getMinBuildHeight()+1;
        int maxY=Math.min(level.getMaxBuildHeight(),level.getMinBuildHeight()+level.getLogicalHeight())-source.height()-3;
        if(maxY<minY)return null;
        var border=level.getWorldBorder();
        boolean alongX=source.axis()==Direction.Axis.X;
        int minX=Mth.ceil(border.getMinX())+2,minZ=Mth.ceil(border.getMinZ())+2;
        int maxX=Mth.floor(border.getMaxX())-(alongX?source.width()+2:3);
        int maxZ=Mth.floor(border.getMaxZ())-(alongX?3:source.width()+2);
        if(maxX<minX||maxZ<minZ)return null;
        var anchor=new BlockPos(Mth.clamp(preferred.getX(),minX,maxX),Mth.clamp(preferred.getY(),minY,maxY),Mth.clamp(preferred.getZ(),minZ,maxZ));
        PortalFrame best=null;double distance=Double.MAX_VALUE;
        // Like PortalForcer, inspect the bottom of each replaceable vertical run.
        for(var cursor:BlockPos.spiralAround(anchor,16,Direction.EAST,Direction.SOUTH)) {
            int x=cursor.getX(),z=cursor.getZ();
            if(x<minX||x>maxX||z<minZ||z>maxZ)continue;
            level.getChunk(x>>4,z>>4);
            int top=Math.min(maxY,level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z));
            for(int y=top;y>=minY;y--) {
                var p=new BlockPos(x,y,z);
                if(!replaceable(level,p))continue;
                while(y>minY&&replaceable(level,p.below())) {y--;p=p.below();}
                var candidate=new PortalFrame(p,source.axis(),source.width(),source.height());
                double d=p.distSqr(anchor);
                if(d<distance&&canHost(level,candidate)){best=candidate;distance=d;}
            }
        }
        if(best!=null)return build(level,best,material);
        // Vanilla also clears a platform when terrain, water or roofs leave no site.
        int surface=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,anchor.getX(),anchor.getZ());
        int lower=Math.min(maxY,Math.max(Math.max(minY,70),surface));
        var fallback=new PortalFrame(new BlockPos(anchor.getX(),Mth.clamp(preferred.getY(),lower,maxY),anchor.getZ()),source.axis(),source.width(),source.height());
        sealFluids(level,fallback,material);
        return build(level,fallback,material);
    }
    private static boolean replaceable(ServerLevel level,BlockPos pos) {
        var state=level.getBlockState(pos);
        return state.canBeReplaced()&&state.getFluidState().isEmpty();
    }
    private static boolean canHost(ServerLevel level,PortalFrame frame) {
        var normal=frame.axis()==Direction.Axis.X?Direction.SOUTH:Direction.EAST;
        for(int x=-1;x<=frame.width();x++)for(int z=-1;z<=1;z++) {
            var floor=frame.at(x,-1).relative(normal,z);
            if(!level.getBlockState(floor).isSolid()||!level.getBlockState(floor).getFluidState().isEmpty())return false;
            for(int y=1;y<=frame.height()+2;y++)if(!replaceable(level,floor.above(y)))return false;
        }
        return true;
    }
    private static void sealFluids(ServerLevel level,PortalFrame frame,Block material) {
        var normal=frame.axis()==Direction.Axis.X?Direction.SOUTH:Direction.EAST;
        // Keep a forced underwater exit dry after neighboring fluid ticks.
        for(int x=-2;x<=frame.width()+1;x++)for(int z=-2;z<=2;z++)for(int y=0;y<=frame.height()+2;y++) {
            if(x!=-2&&x!=frame.width()+1&&z!=-2&&z!=2&&y!=frame.height()+2)continue;
            var pos=frame.at(x,y).relative(normal,z);
            if(!level.getBlockState(pos).getFluidState().isEmpty())level.setBlock(pos,material.defaultBlockState(),Block.UPDATE_ALL);
        }
    }
    private static PortalFrame build(ServerLevel level,PortalFrame frame,Block material) {
        var normal=frame.axis()==Direction.Axis.X?Direction.SOUTH:Direction.EAST;
        for(int x=-1;x<=frame.width();x++)for(int z=-1;z<=1;z++) {
            var floor=frame.at(x,-1).relative(normal,z);
            level.setBlock(floor,material.defaultBlockState(),Block.UPDATE_ALL);
            for(int y=1;y<=frame.height()+2;y++)level.setBlock(floor.above(y),Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
        }
        for(int x=-1;x<=frame.width();x++)for(int y=0;y<=frame.height();y++)
            if(x==-1||x==frame.width()||y==frame.height())level.setBlock(frame.at(x,y),material.defaultBlockState(),Block.UPDATE_ALL);
        frame.fill(level);
        return frame;
    }
}

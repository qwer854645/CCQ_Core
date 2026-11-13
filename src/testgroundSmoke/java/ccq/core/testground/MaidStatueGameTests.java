package ccq.core.testground;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ccq_statues")
@PrefixGameTestTemplate(false)
public class MaidStatueGameTests {
    static final String[] IDS={"azure_maid_seated","azure_maid_standing"};
    @GameTest(template="empty",batch="statue_commands",timeoutTicks=1200)
    public static void templatesPlaceInFourRotationsWithHiddenRewards(GameTestHelper h)throws Exception {
        var level=h.getLevel().getServer().getLevel(TestGroundModule.DIMENSION);
        var source=level.getServer().createCommandSourceStack().withLevel(level).withPermission(4).withSuppressedOutput();
        for(int i=0;i<IDS.length;i++)for(var rot:Rotation.values()) {
            var origin=new BlockPos(110000+i*1024+rot.ordinal()*128,180,110000);
            for(int cx=(origin.getX()-32)>>4;cx<=(origin.getX()+32)>>4;cx++)for(int cz=(origin.getZ()-32)>>4;cz<=(origin.getZ()+32)>>4;cz++)level.getChunk(cx,cz);
            String angle=switch(rot){case NONE->"none";case CLOCKWISE_90->"clockwise_90";case CLOCKWISE_180->"180";case COUNTERCLOCKWISE_90->"counterclockwise_90";};
            h.assertTrue(level.getServer().getCommands().getDispatcher().execute("place template ccq_testground:statues/"+IDS[i]+" "+origin.getX()+" 180 110000 "+angle,source)==1,"Template command succeeds");
            check(h,level,IDS[i],origin,rot);
        }
        h.succeed();
    }
    @GameTest(template="empty",batch="statue_natural",timeoutTicks=2400)
    public static void bothStatuesGenerateNaturallyAcrossChunks(GameTestHelper h)throws Exception {
        var level=h.getLevel().getServer().getLevel(TestGroundModule.DIMENSION);
        var flag=net.minecraft.world.level.levelgen.WorldOptions.class.getDeclaredField("generateStructures");flag.setAccessible(true);
        flag.setBoolean(level.getServer().getWorldData().worldGenOptions(),true);
        var set=level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).get(ResourceLocation.parse("ccq_testground:azure_maid_statues"));
        var placement=(RandomSpreadStructurePlacement)set.placement();var found=new HashSet<String>();
        for(int x=1100;x<1112&&found.size()<2;x++)for(int z=1100;z<1112&&found.size()<2;z++) {
            var chunk=placement.getPotentialStructureChunk(level.getSeed(),x*placement.spacing(),z*placement.spacing());
            var generated=level.getChunk(chunk.x,chunk.z,ChunkStatus.STRUCTURE_STARTS);
            for(var id:IDS) {
                if(found.contains(id))continue;
                var structure=level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(ResourceLocation.parse("ccq_testground:"+id));
                var start=generated.getStartForStructure(structure);if(start==null||!start.isValid())continue;
                var bounds=start.getBoundingBox();
                for(int a=bounds.maxX()>>4;a>=bounds.minX()>>4;a--)for(int b=bounds.maxZ()>>4;b>=bounds.minZ()>>4;b--)level.getChunk(a,b);
                var piece=(TemplateStructurePiece)start.getPieces().getFirst();check(h,level,id,piece.templatePosition(),piece.getRotation());found.add(id);
            }
        }
        h.assertTrue(found.size()==2,"Both variants generated naturally: "+found);h.succeed();
    }
    static void check(GameTestHelper h,net.minecraft.server.level.ServerLevel level,String id,BlockPos origin,Rotation rotation)throws Exception {
        CompoundTag raw;
        try(var stream=level.getServer().getResourceManager().getResourceOrThrow(ResourceLocation.parse("ccq_testground:structure/statues/"+id+".nbt")).open()){raw=NbtIo.readCompressed(stream,NbtAccounter.unlimitedHeap());}
        var palette=raw.getList("palette",10);int diamonds=0,netherite=0;
        for(var tag:raw.getList("blocks",10)) {
            var b=(CompoundTag)tag;var p=b.getList("pos",3);var local=new BlockPos(p.getInt(0),p.getInt(1),p.getInt(2));var pos=origin.offset(local.rotate(rotation));
            var expected=ResourceLocation.parse(palette.getCompound(b.getInt("state")).getString("Name"));var actual=level.getBlockState(pos);
            h.assertTrue(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(actual.getBlock()).equals(expected),"Authored block placed: "+id+" "+local+" expected="+expected+" actual="+actual);
            if(actual.is(Blocks.DIAMOND_BLOCK)||actual.is(Blocks.NETHERITE_BLOCK)) {
                if(actual.is(Blocks.DIAMOND_BLOCK))diamonds++;else netherite++;
                for(var d:Direction.values())h.assertTrue(level.getBlockState(pos.relative(d)).isSolidRender(level,pos.relative(d)),"Treasure remains hidden on all six sides: "+id+" "+local);
            }
        }
        h.assertTrue(diamonds==4&&netherite==2,"Each statue has exactly four diamond and two netherite blocks");
    }
}



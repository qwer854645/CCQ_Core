package ccq.core.testground;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ccq_nuclear_bunker")
@PrefixGameTestTemplate(false)
public class NuclearBunkerGameTests {
    @GameTest(template="empty",batch="bunker_commands",timeoutTicks=1800)
    public static void commandsPlaceWholeBunkerAndRotatedTemplate(GameTestHelper helper)throws Exception {
        var level=helper.getLevel().getServer().getLevel(TestGroundModule.DIMENSION);
        var forced=new HashSet<Long>();for(long c:level.getForcedChunks())forced.add(c);
        var source=level.getServer().createCommandSourceStack().withLevel(level).withPermission(4).withSuppressedOutput();
        for(boolean rotated:new boolean[]{false,true}) {
            var requested=new BlockPos(rotated?31000:30000,180,30000);
            level.getChunk(requested.getX()>>4,requested.getZ()>>4);
            String cmd=(rotated?"place template ccq_testground:engineering/nuclear_bunker":"place structure ccq_testground:nuclear_bunker")
                    +" "+requested.getX()+" 180 30000"+(rotated?" clockwise_90":"");
            helper.assertTrue(level.getServer().getCommands().getDispatcher().execute(cmd,source)==1,"Bunker /place succeeds off grid");
            check(helper,level,requested.below(8),rotated?Rotation.CLOCKWISE_90:Rotation.NONE,false);
        }
        var after=new HashSet<Long>();for(long c:level.getForcedChunks())after.add(c);
        helper.assertTrue(forced.equals(after),"No persistent forced chunks leaked");helper.succeed();
    }

    @GameTest(template="empty",batch="bunker_natural",timeoutTicks=2400)
    public static void naturalBunkerGeneratesWholeFromFarCorner(GameTestHelper helper)throws Exception {
        var level=helper.getLevel().getServer().getLevel(TestGroundModule.DIMENSION);
        var flag=net.minecraft.world.level.levelgen.WorldOptions.class.getDeclaredField("generateStructures");flag.setAccessible(true);
        flag.setBoolean(level.getServer().getWorldData().worldGenOptions(),true);
        var structure=level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(ResourceLocation.parse("ccq_testground:nuclear_bunker"));
        var set=level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).get(ResourceLocation.parse("ccq_testground:nuclear_bunkers"));
        var placement=(RandomSpreadStructurePlacement)set.placement();StructureStart found=null;
        for(int x=700;x<716&&found==null;x++)for(int z=700;z<716&&found==null;z++) {
            var p=placement.getPotentialStructureChunk(level.getSeed(),x*placement.spacing(),z*placement.spacing());
            var start=level.getChunk(p.x,p.z,ChunkStatus.STRUCTURE_STARTS).getStartForStructure(structure);
            if(start!=null&&start.isValid())found=start;
        }
        helper.assertTrue(found!=null,"A naturally eligible bunker exists");
        helper.assertTrue(found.getPieces().size()==1,"96x96 bunker fits in one native structure piece");
        var piece=(TemplateStructurePiece)found.getPieces().getFirst();var bounds=found.getBoundingBox();
        for(int x=bounds.maxX()>>4;x>=bounds.minX()>>4;x--)for(int z=bounds.maxZ()>>4;z>=bounds.minZ()>>4;z--)level.getChunk(x,z);
        check(helper,level,piece.templatePosition(),piece.getRotation(),true);helper.succeed();
    }

    private static void check(GameTestHelper helper,net.minecraft.server.level.ServerLevel level,BlockPos origin,Rotation rotation,boolean natural)throws Exception {
        CompoundTag raw;
        try(var stream=level.getServer().getResourceManager().getResourceOrThrow(ResourceLocation.parse("ccq_testground:structure/engineering/nuclear_bunker.nbt")).open()) {
            raw=NbtIo.readCompressed(stream,NbtAccounter.unlimitedHeap());
        }
        var palette=raw.getList("palette",10);int charges=0,stock=0,air=0,guns=0,loot=0,lowerBeds=0,upperBeds=0;var stockTypes=new HashSet<String>();
        for(var entry:raw.getList("blocks",10)) {
            var b=(CompoundTag)entry;var p=b.getList("pos",3);var local=new BlockPos(p.getInt(0),p.getInt(1),p.getInt(2));
            var pos=origin.offset(local.rotate(rotation));var tag=palette.getCompound(b.getInt("state"));String id=tag.getString("Name");
            helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse(id)),"Installed block registry contains "+id);
            var expected=NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(),tag).rotate(rotation);var actual=level.getBlockState(pos);
            expected=net.minecraft.world.level.block.Block.updateFromNeighbourShapes(expected,level,pos);
            boolean ore=natural&&local.getY()<8&&id.equals("minecraft:andesite")&&actual.is(net.neoforged.neoforge.common.Tags.Blocks.ORES);
            boolean water=expected.is(net.minecraft.world.level.block.Blocks.WATER)&&actual.is(net.minecraft.world.level.block.Blocks.WATER);
            helper.assertTrue(actual.equals(expected)||ore||water,"Complete bunker state at "+local+" expected="+expected+" actual="+actual);
            helper.assertTrue(actual.equals(net.minecraft.world.level.block.Block.updateFromNeighbourShapes(actual,level,pos)),"Bunker furniture connections are stable at "+local);
            EngineeringCrankChecks.check(helper,level,pos,actual);
            if(id.contains(":nuclear_charge_t"))charges++;
            if(id.equals("createbigcannons:cannon_mount"))guns++;
            if(id.endsWith("_bed")&&tag.getCompound("Properties").getString("part").equals("head")) {
                if(local.getY()==9)lowerBeds++;else if(local.getY()>=15)upperBeds++;
            }
            if(b.getCompound("nbt").contains("LootTable",8)) {
                var saved=level.getBlockEntity(pos).saveWithFullMetadata(level.registryAccess());
                helper.assertTrue(saved.getString("LootTable").equals("ccq_testground:chests/engineering/bunker_observation"),"Unopened observation supply retains its random loot table");loot++;
            }
            if(id.equals("minecraft:air")&&local.getY()<8)air++;
            if(b.getCompound("nbt").contains("Items",9)) {
                helper.assertTrue(level.getBlockEntity(pos) instanceof net.minecraft.world.Container,"Barrel inventory exists");
                var container=(net.minecraft.world.Container)level.getBlockEntity(pos);
                for(var itemEntry:b.getCompound("nbt").getList("Items",10)) {
                    var item=(CompoundTag)itemEntry;var actualItem=container.getItem(item.getByte("Slot"));
                    helper.assertTrue(!actualItem.isEmpty()&&BuiltInRegistries.ITEM.getKey(actualItem.getItem()).toString().equals(item.getString("id"))&&actualItem.getCount()==item.getInt("count"),"Authored stock survives at "+local+" expected="+item+" actual="+actualItem);
                    var expectedItem=net.minecraft.world.item.ItemStack.parse(level.registryAccess(),item).orElseThrow();
                    helper.assertTrue(net.minecraft.world.item.ItemStack.isSameItemSameComponents(actualItem,expectedItem),"Potion and equipment components survive at "+local);stockTypes.add(item.getString("id"));
                    helper.assertTrue(actualItem.getCount()<=actualItem.getMaxStackSize(),"Stock obeys item stack limits: "+actualItem);
                }
                stock++;
            }
        }
        helper.assertTrue(charges==18&&guns==4&&stock>=60&&stockTypes.size()>120&&loot==10&&air>4000,"Bunker inventory/geometry totals: charges="+charges+" guns="+guns+" stock="+stock+" types="+stockTypes.size()+" loot="+loot+" air="+air);
        helper.assertTrue(lowerBeds==14&&upperBeds==0,"Ten barracks beds and four medical beds are downstairs; upper deck contains work functions");
        for(var door:List.of(new BlockPos(31,9,22),new BlockPos(64,9,24),new BlockPos(31,9,71),new BlockPos(64,9,71))) {
            helper.assertTrue(level.getBlockState(origin.offset(door.rotate(rotation))).getBlock() instanceof net.minecraft.world.level.block.DoorBlock,"Corner room has a manual door at its entrance");
            helper.assertTrue(!level.getBlockState(origin.offset(door.above(3).rotate(rotation))).isAir(),"Corner partition joins the ceiling above its entrance");
        }
        for(int x=33;x<=62;x++)
            helper.assertTrue(level.getBlockState(origin.offset(new BlockPos(x,18,32).rotate(rotation))).isAir(),"Open upper operations deck at "+x);
        for(int x:new int[]{26,68}) {
            helper.assertTrue(level.getBlockState(origin.offset(new BlockPos(x,14,53).rotate(rotation))).is(net.minecraft.world.level.block.Blocks.STONE_BRICK_STAIRS),"Shared stair reaches upper deck");
            helper.assertTrue(level.getBlockState(origin.offset(new BlockPos(x,14,57).rotate(rotation))).isAir(),"Shared stair headroom cuts through intermediate deck");
        }
        for(int x:new int[]{19,76})for(int z:new int[]{19,76})
            helper.assertTrue(!level.getBlockState(origin.offset(new BlockPos(x-6,10,z).rotate(rotation))).is(net.minecraft.world.level.block.Blocks.LADDER),"Individual cannon ladder removed");
        for(int x:new int[]{30,38,39,47,48,56,57,65})for(int z=19;z<=31;z++)
            helper.assertTrue(level.getBlockState(origin.offset(new BlockPos(x,5,z).rotate(rotation))).isAir(),"Open basement loading deck at "+x+","+z);
        org.slf4j.LoggerFactory.getLogger("ccq_testground").info("Bunker verified: natural={}, rotation={}, charges={}, stock={}, underground air={}",natural,rotation,charges,stock,air);
    }
}

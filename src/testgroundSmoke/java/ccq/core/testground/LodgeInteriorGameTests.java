package ccq.core.testground;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ccq_testground")
@PrefixGameTestTemplate(false)
public class LodgeInteriorGameTests {
    static final BlockPos ORIGIN=new BlockPos(20000,160,20000);
    @GameTest(template="empty",batch="lodge_interior",timeoutTicks=500)
    public static void furnishedLodgeIsUsable(GameTestHelper helper) throws Exception {
        var level=helper.getLevel().getServer().getLevel(TestGroundModule.DIMENSION);
        var id=ResourceLocation.parse("ccq_testground:engineering/duty_lodge");
        var template=level.getServer().getStructureManager().getOrCreate(id);
        var settings=new StructurePlaceSettings().setIgnoreEntities(true).setKnownShape(true);
        var box=template.getBoundingBox(settings,ORIGIN.offset(29,0,14));
        for(int cx=(box.minX()>>4)-1;cx<=(box.maxX()>>4)+1;cx++) for(int cz=(box.minZ()>>4)-1;cz<=(box.maxZ()>>4)+1;cz++) {
            level.getChunk(cx,cz);level.setChunkForced(cx,cz,true);
        }
        CompoundTag raw;
        try(var stream=level.getServer().getResourceManager().getResourceOrThrow(ResourceLocation.parse("ccq_testground:structure/engineering/duty_lodge.nbt")).open()) {
            raw=NbtIo.readCompressed(stream,NbtAccounter.unlimitedHeap());
        }
        var palette=new ArrayList<BlockState>();
        for(var p:raw.getList("palette",10)) palette.add(NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK),(CompoundTag)p));
        helper.assertTrue(template.placeInWorld(level,ORIGIN.offset(29,0,14),ORIGIN.offset(29,0,14),settings,level.random,2),"Base template placed");
        helper.runAfterDelay(50,()->{
            var issues=new ArrayList<String>();
            int checked=0,blockEntities=0;
            for(var element:raw.getList("blocks",10)) {
                var block=(CompoundTag)element;var coords=block.getList("pos",3);
                var local=new BlockPos(coords.getInt(0),coords.getInt(1),coords.getInt(2));
                var pos=ORIGIN.offset(29,0,14).offset(local);var expected=palette.get(block.getInt("state"));var actual=level.getBlockState(pos);
                checked++;
                if(!actual.is(expected.getBlock())) issues.add("BLOCK CHANGED "+local+" expected="+expected+" actual="+actual);
                if(!actual.canSurvive(level,pos)) issues.add("UNSUPPORTED "+local+" "+actual);
                String key=BuiltInRegistries.BLOCK.getKey(actual.getBlock()).toString();
                if(key.startsWith("kaleidoscope_")&&(key.contains("table")||key.contains("sofa"))) {
                    var connected=Block.updateFromNeighbourShapes(actual,level,pos);
                    if(!actual.equals(connected))issues.add("FURNITURE CONNECTION "+local+" "+actual+" -> "+connected);
                }
                if(block.contains("nbt",10)) {
                    blockEntities++;
                    var be=level.getBlockEntity(pos);var data=block.getCompound("nbt");
                    if(be==null) issues.add("MISSING BLOCK ENTITY "+local+" "+expected);
                    else if(data.contains("LootTable") && !data.getString("LootTable").equals(be.saveWithoutMetadata(level.registryAccess()).getString("LootTable")))
                        issues.add("LOOT LOST "+local);
                }
            }
            // Doors are opened for the circulation check, as a player would open them.
            for(int y:new int[]{2,3,7,8}) {
                var p=ORIGIN.offset(43,y,y<5?23:27);var s=level.getBlockState(p);
                if(s.hasProperty(BlockStateProperties.OPEN))level.setBlock(p,s.setValue(BlockStateProperties.OPEN,true),2);
            }
            var routes=walkability(level);
            for(var entry:routes.entrySet()) if(!entry.getValue())issues.add("UNREACHABLE "+entry.getKey());
            for(int x:new int[]{34,36,38,40})for(int z:new int[]{16,17}) {
                var bed=level.getBlockState(ORIGIN.offset(x,7,z));
                helper.assertTrue(bed.is(Blocks.BLUE_BED)&&bed.getValue(BlockStateProperties.HORIZONTAL_FACING)==Direction.NORTH,"Four identical beds share one straight row");
            }
            String report="BLOCKS="+checked+" BLOCK_ENTITIES="+blockEntities+"\nROUTES="+routes+"\nISSUES="+issues.size()+"\n"+String.join("\n",issues);
            org.slf4j.LoggerFactory.getLogger("ccq_testground").info("Lodge audit: {}",report);
            var potPosition=ORIGIN.offset(40,3,15);
            var pot=level.getBlockEntity(potPosition);
            try {
                helper.assertTrue((boolean)pot.getClass().getMethod("isHeated").invoke(pot),"Kitchen pot receives real heat from stove");
                var inventory=(net.neoforged.neoforge.items.ItemStackHandler)pot.getClass().getMethod("getInventory").invoke(pot);
                inventory.setStackInSlot(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BEEF));
                inventory.setStackInSlot(1,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CARROT));
                inventory.setStackInSlot(2,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.POTATO));
                inventory.setStackInSlot(pot.getClass().getField("CONTAINER_SLOT").getInt(null),new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BOWL));
                var tick=pot.getClass().getMethod("cookingTick",net.minecraft.world.level.Level.class,BlockPos.class,BlockState.class,pot.getClass());
                for(int i=0;i<450;i++)tick.invoke(null,level,potPosition,level.getBlockState(potPosition),pot);
                var output=inventory.getStackInSlot(pot.getClass().getField("OUTPUT_SLOT").getInt(null));
                helper.assertTrue(BuiltInRegistries.ITEM.getKey(output.getItem()).toString().equals("farmersdelight:beef_stew"),"Kitchen actually cooks beef stew from ingredients");
            }catch(ReflectiveOperationException exception){throw new IllegalStateException(exception);}
            helper.assertTrue(level.getBlockEntity(ORIGIN.offset(30,3,17))!=null,"Duty computer has a real block entity");
            helper.assertTrue(level.getBlockEntity(ORIGIN.offset(30,3,19))!=null,"Printer has a real block entity");
            for(int cx=(box.minX()>>4)-1;cx<=(box.maxX()>>4)+1;cx++) for(int cz=(box.minZ()>>4)-1;cz<=(box.maxZ()>>4)+1;cz++) level.setChunkForced(cx,cz,false);
            helper.assertTrue(issues.isEmpty(),"Base audit found "+issues.size()+" issues; first="+(issues.isEmpty()?"none":issues.getFirst()));
            helper.succeed();
        });
    }

    record Node(int x,int z,double y) {}
    private static Map<String,Boolean> walkability(ServerLevel level) {
        // Half-block grid and actual collision shapes, with a 0.6×1.8 player body.
        var columns=new HashMap<String,List<Node>>();
        for(int x=57;x<=95;x++) for(int z=27;z<=69;z++) {
            double px=x/2.0,pz=z/2.0;
            var heights=new TreeSet<Double>();
            for(int bx=(int)Math.floor(px-.3);bx<=Math.floor(px+.3);bx++)
                for(int bz=(int)Math.floor(pz-.3);bz<=Math.floor(pz+.3);bz++) for(int y=0;y<=10;y++) {
                    var pos=ORIGIN.offset(bx,y,bz);
                    for(var a:level.getBlockState(pos).getCollisionShape(level,pos).toAabbs())
                        if(bx+a.maxX>px-.299 && bx+a.minX<px+.299 && bz+a.maxZ>pz-.299 && bz+a.minZ<pz+.299)heights.add(y+a.maxY);
                }
            var nodes=new ArrayList<Node>();
            for(double feet:heights) {
                if(feet<1 || feet>9)continue;
                var body=new AABB(ORIGIN.getX()+px-.299,ORIGIN.getY()+feet+.001,ORIGIN.getZ()+pz-.299,
                        ORIGIN.getX()+px+.299,ORIGIN.getY()+feet+1.799,ORIGIN.getZ()+pz+.299);
                if(level.noCollision(null,body))nodes.add(new Node(x,z,feet));
            }
            columns.put(x+":"+z,nodes);
        }
        Node start=null;
        for(var list:columns.values())for(var node:list)
            if(node.x>=88 && node.x<=90 && node.z>=46 && node.z<=48 && Math.abs(node.y-2)<.01)start=node;
        var visited=new HashSet<Node>();var queue=new ArrayDeque<Node>();
        if(start!=null){visited.add(start);queue.add(start);}
        while(!queue.isEmpty()) {
            var node=queue.remove();
            for(int[] diff:new int[][]{{1,0},{-1,0},{0,1},{0,-1}})
                for(var next:columns.getOrDefault((node.x+diff[0])+":"+(node.z+diff[1]),List.of()))
                    if(next.y-node.y<=.601 && node.y-next.y<=1.1 && visited.add(next))queue.add(next);
        }
        var result=new LinkedHashMap<String,Boolean>();
        result.put("front porch",start!=null);
        result.put("ground floor",visited.stream().anyMatch(n->n.x>=67&&n.x<=71&&n.z>=41&&n.z<=45&&Math.abs(n.y-2)<.01));
        result.put("upper landing",visited.stream().anyMatch(n->n.x>=62&&n.x<=65&&n.z>=42&&n.z<=45&&Math.abs(n.y-7)<.01));
        result.put("bedroom",visited.stream().anyMatch(n->n.x>=73&&n.x<=79&&n.z>=43&&n.z<=46&&Math.abs(n.y-7)<.1));
        result.put("balcony",visited.stream().anyMatch(n->n.x>=88&&n.x<=90&&n.z>=54&&n.z<=57&&Math.abs(n.y-7)<.01));
        for(int x:new int[]{34,36,38,40}) {
            int bedX=x;
            result.put("bed "+x,visited.stream().anyMatch(n->n.x>=bedX*2&&n.x<=bedX*2+2&&n.z>=36&&n.z<=38&&Math.abs(n.y-7)<.1));
        }
        return result;
    }
}

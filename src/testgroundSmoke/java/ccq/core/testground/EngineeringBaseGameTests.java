package ccq.core.testground;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ccq_testground")
@PrefixGameTestTemplate(false)
public class EngineeringBaseGameTests {
    @GameTest(template="empty",batch="structurify_compat",timeoutTicks=100)
    public static void overlapExceptionOnlyAppliesToEngineeringBase(GameTestHelper helper) throws Exception {
        if(!net.neoforged.fml.ModList.get().isLoaded("structurify")){helper.succeed();return;}
        var config=Class.forName("com.faboslav.structurify.common.Structurify").getMethod("getConfig").invoke(null);
        helper.assertTrue(config.getClass().getField("preventStructureOverlap").getBoolean(config),"Global overlap prevention remains enabled in the compatibility fixture");
        var level=helper.getLevel().getServer().getLevel(TestGroundModule.DIMENSION);
        var registry=level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        var dataType=Class.forName("com.faboslav.structurify.common.world.level.structure.checks.StructureCheckData");
        var overlapType=Class.forName("com.faboslav.structurify.common.config.data.structure.OverlapCheckData");
        var checker=Class.forName("com.faboslav.structurify.common.world.level.structure.checks.StructureOverlapCheck").getMethod("canDoOverlapCheck",dataType,overlapType);
        for(String id:new String[]{"ccq_testground:engineering_base","minecraft:desert_pyramid"}) {
            var key=ResourceLocation.parse(id);var structure=registry.get(key);
            var piece=new EngineeringStructures.Piece(level.getServer().getStructureManager(),ResourceLocation.parse("ccq_testground:engineering/survey_tower"),new BlockPos(0,196,0),Rotation.NONE);
            var start=new net.minecraft.world.level.levelgen.structure.StructureStart(structure,new ChunkPos(0,0),0,
                    new net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer(List.of(piece)));
            var data=dataType.getConstructors()[0].newInstance(1L,key,structure,start,null);
            boolean checks=(boolean)checker.invoke(null,data,overlapType.getConstructor().newInstance());
            helper.assertTrue(checks==id.startsWith("minecraft:"),"Only the engineering base is exempt from section overlap rejection: "+id);
        }
        helper.succeed();
    }

    @GameTest(template="empty",batch="base_generation",timeoutTicks=1000)
    public static void completeBaseGeneratesFromEitherEnd(GameTestHelper helper) throws Exception {
        checkBase(helper,Rotation.CLOCKWISE_180,300);
    }

    @GameTest(template="empty",batch="base_connections_none",timeoutTicks=1500)
    public static void unrotatedBaseConnectionsAndLighting(GameTestHelper helper)throws Exception {
        checkBase(helper,Rotation.NONE,330);
    }

    @GameTest(template="empty",batch="base_connections_counterclockwise",timeoutTicks=1500)
    public static void counterclockwiseBaseConnectionsAndLighting(GameTestHelper helper)throws Exception {
        checkBase(helper,Rotation.COUNTERCLOCKWISE_90,-350);
    }

    @GameTest(template="empty",batch="base_generation_negative",timeoutTicks=1000)
    public static void negativeCoordinatesBaseGeneratesComplete(GameTestHelper helper) throws Exception {
        var placement=new EngineeringBasePlacement(64,32,91827361);
        long seed=-4103457699211345949L;
        var root=placement.getPotentialStructureChunk(seed,-64,-64);
        for(int i=0;i<3;i++) {
            var section=placement.section(seed,root,i);
            helper.assertTrue(placement.findSection(seed,section.chunk()).orElseThrow().equals(section),"Negative-coordinate section lookup is consistent for the reported seed");
        }
        checkBase(helper,Rotation.CLOCKWISE_90,-320);
    }

    private static void checkBase(GameTestHelper helper,Rotation wantedRotation,int grid) throws Exception {
        var level=helper.getLevel().getServer().getLevel(TestGroundModule.DIMENSION);
        var flag=net.minecraft.world.level.levelgen.WorldOptions.class.getDeclaredField("generateStructures");
        flag.setAccessible(true);flag.setBoolean(level.getServer().getWorldData().worldGenOptions(),true);
        var structures=level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        var id=ResourceLocation.parse("ccq_testground:engineering_base");
        var structure=structures.get(id);
        var sets=level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET);
        var set=sets.get(ResourceLocation.parse("ccq_testground:engineering_facilities"));
        helper.assertTrue(set.structures().size()==1&&set.structures().getFirst().structure().value()==structure,"Only the merged base participates in natural generation");
        for(String old:new String[]{"survey_tower","calibration_range","assembly_pad"})
            helper.assertTrue(structures.get(ResourceLocation.parse("ccq_testground:"+old)).biomes().size()==0,"Legacy facility is disabled: "+old);
        var placement=(EngineeringBasePlacement)set.placement();
        helper.assertTrue(placement.spacing()>=40&&placement.separation()<placement.spacing(),"Sparse base grid");
        ChunkPos found=null;
        for(int x=grid;x<grid+18&&found==null;x++)for(int z=grid;z<grid+18&&found==null;z++) {
            var candidate=placement.getPotentialStructureChunk(level.getSeed(),x*placement.spacing(),z*placement.spacing());
            if(placement.section(level.getSeed(),candidate,1).rotation()!=wantedRotation)continue;
            var chunk=level.getChunk(candidate.x,candidate.z,net.minecraft.world.level.chunk.status.ChunkStatus.STRUCTURE_STARTS);
            if(chunk.getPersistedStatus().isOrAfter(net.minecraft.world.level.chunk.status.ChunkStatus.FULL))continue;
            var start=chunk.getStartForStructure(structure);
            if(start!=null&&start.isValid())found=candidate;
        }
        helper.assertTrue(found!=null,"A suitable full-base site exists");
        var root=found;var forced=new HashSet<ChunkPos>();
        var pieces=new ArrayList<net.minecraft.world.level.levelgen.structure.TemplateStructurePiece>();
        var serialization=net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext.fromLevel(level);
        Integer y=null;
        for(int index:new int[]{2,0,1}) {
            var section=placement.section(level.getSeed(),root,index);
            var chunk=level.getChunk(section.chunk().x,section.chunk().z,net.minecraft.world.level.chunk.status.ChunkStatus.STRUCTURE_STARTS);
            var start=chunk.getStartForStructure(structure);
            helper.assertTrue(start!=null&&start.isValid(),"Section exists when requested from the far end: "+index);
            var piece=(net.minecraft.world.level.levelgen.structure.TemplateStructurePiece)start.getPieces().getFirst();
            pieces.add(piece);
            if(y==null)y=piece.templatePosition().getY();
            helper.assertTrue(piece.templatePosition().equals(section.origin(y)),"Sections share height, origin and rotation");
            var tag=piece.createTag(serialization);
            var restored=(net.minecraft.world.level.levelgen.structure.TemplateStructurePiece)piece.getType().load(serialization,tag);
            helper.assertTrue(restored.templatePosition().equals(piece.templatePosition())&&restored.getRotation()==piece.getRotation(),"Section survives save/load");
            var bounds=start.getBoundingBox();
            for(int cx=(bounds.maxX()>>4)+1;cx>=(bounds.minX()>>4)-1;cx--)for(int cz=(bounds.maxZ()>>4)+1;cz>=(bounds.minZ()>>4)-1;cz--) {
                level.getChunk(cx,cz);level.setChunkForced(cx,cz,true);forced.add(new ChunkPos(cx,cz));
            }
        }
        var anchor=placement.section(level.getSeed(),root,1);
        var origin=anchor.fullOrigin().atY(y);
        var rotation=anchor.rotation();
        var holder=structures.getHolderOrThrow(ResourceKey.create(Registries.STRUCTURE,id));
        var located=level.getChunkSource().getGenerator().findNearestMapStructure(level,HolderSet.direct(holder),origin,1,false);
        helper.assertTrue(located!=null,"Native locate supports the grouped placement");
        CompoundTag data;
        try(var stream=level.getServer().getResourceManager().getResourceOrThrow(ResourceLocation.parse("ccq_testground:structure/engineering/engineering_base.nbt")).open()) {
            data=NbtIo.readCompressed(stream,NbtAccounter.unlimitedHeap());
        }
        var palette=new ArrayList<net.minecraft.world.level.block.state.BlockState>();
        for(var value:data.getList("palette",10))palette.add(NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK),(CompoundTag)value));
        helper.runAfterDelay(60,()->{
            EngineeringTemplateLighting.assertConnections(helper,level,origin,rotation,data.getList("blocks",10));
            EngineeringTemplateLighting.assertFloorsAndLighting(helper,level,origin,rotation);
            for(var piece:pieces)helper.assertTrue(piece.placeSettings().getBoundingBox()==null,"Generation must not mutate shared placement settings across C2ME workers");
            int containers=0,blocks=0,fixedStock=0,fixedItems=0;
            var openedLoot=new HashMap<BlockPos,CompoundTag>();
            for(var entry:data.getList("blocks",10)) {
                var block=(CompoundTag)entry;var coords=block.getList("pos",3);
                var local=new BlockPos(coords.getInt(0),coords.getInt(1),coords.getInt(2));
                var pos=origin.offset(local.rotate(rotation));var expected=palette.get(block.getInt("state"));var actual=level.getBlockState(pos);
                helper.assertTrue(actual.is(expected.getBlock()),"Natural base block missing or clipped at "+local+": "+expected+" -> "+actual);
                helper.assertTrue(actual.canSurvive(level,pos),"Unsupported base block at "+local);
                var presentEntity=level.getBlockEntity(pos);
                if(BuiltInRegistries.BLOCK.getKey(actual.getBlock()).toString().equals("farmersdelight:spruce_cabinet")) {
                    helper.assertTrue(block.contains("nbt",10)&&block.getCompound("nbt").contains("Items",9),"Every cabinet has curated physical stock: "+local);
                    helper.assertTrue(presentEntity instanceof net.minecraft.world.Container cabinet&&!cabinet.isEmpty(),"Cabinet stock actually loads: "+local);
                }
                if(presentEntity instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity)
                    helper.assertTrue(!presentEntity.saveWithoutMetadata(level.registryAccess()).contains("CustomName"),"Base containers use their default names: "+local);
                helper.assertTrue(!(actual.getBlock() instanceof net.minecraft.world.level.block.SignBlock)
                        &&!BuiltInRegistries.BLOCK.getKey(actual.getBlock()).toString().equals("supplementaries:notice_board"),"Base has no signs or notice boards");
                blocks++;
                if(block.contains("nbt",10)) {
                    var be=level.getBlockEntity(pos);helper.assertTrue(be!=null,"Block entity survives section generation: "+local);
                    var nbt=block.getCompound("nbt");
                    if(nbt.contains("HeldItem",10)) {
                        helper.assertTrue(be instanceof com.simibubi.create.content.logistics.depot.DepotBlockEntity,"Workpiece uses a real depot");
                        var held=((com.simibubi.create.content.logistics.depot.DepotBlockEntity)be).getHeldItem();
                        var expectedItem=nbt.getCompound("HeldItem").getCompound("Item");
                        helper.assertTrue(!held.isEmpty()&&BuiltInRegistries.ITEM.getKey(held.getItem()).toString().equals(expectedItem.getString("id"))&&held.getCount()==expectedItem.getInt("count"),"Actual workpiece survives generation: "+local);
                    }
                    if(nbt.contains("LootTable")) {
                        var container=(net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity)be;
                        helper.assertTrue(nbt.getString("LootTable").equals(container.saveWithoutMetadata(level.registryAccess()).getString("LootTable")),"Loot table survives: "+local);
                        container.unpackLootTable(null);helper.assertTrue(!container.isEmpty(),"Base container supplies actual loot");
                        var first=container.saveWithoutMetadata(level.registryAccess());container.unpackLootTable(null);
                        helper.assertTrue(first.equals(container.saveWithoutMetadata(level.registryAccess())),"Base loot does not reroll");containers++;
                        openedLoot.put(pos,first);
                    }
                    if(nbt.contains("Items",9)) {
                        fixedStock++;
                        helper.assertTrue(!nbt.contains("LootTable"),"Warehouse stock is fixed, never a loot roll");
                        var container=(net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity)be;
                        var seenSlots=new HashSet<Integer>();
                        for(var value:nbt.getList("Items",10)) {
                            var item=(CompoundTag)value;int slot=item.getByte("Slot");
                            helper.assertTrue(seenSlots.add(slot),"Warehouse stock has unique slots");
                            var actualItem=container.getItem(slot);
                            helper.assertTrue(!actualItem.isEmpty()&&BuiltInRegistries.ITEM.getKey(actualItem.getItem()).toString().equals(item.getString("id")),"Fixed warehouse item loads: "+item);
                            helper.assertTrue(actualItem.getCount()==item.getInt("count")&&actualItem.getCount()<=actualItem.getMaxStackSize(),"Fixed stock quantity is valid: "+item);
                            fixedItems+=actualItem.getCount();
                        }
                        helper.assertTrue(!container.saveWithoutMetadata(level.registryAccess()).contains("CustomName"),"Warehouse container uses its default name");
                        var before=container.saveWithoutMetadata(level.registryAccess());container.unpackLootTable(null);
                        helper.assertTrue(before.equals(container.saveWithoutMetadata(level.registryAccess())),"Opening fixed stock does not change its items");
                        var reloaded=(net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity)net.minecraft.world.level.block.entity.BlockEntity.loadStatic(
                                pos,actual,container.saveWithFullMetadata(level.registryAccess()),level.registryAccess());
                        helper.assertTrue(reloaded!=null&&before.equals(reloaded.saveWithoutMetadata(level.registryAccess())),"Fixed stock survives serialized block entity reload");
                        openedLoot.put(pos,before);
                    }
                }
            }
            helper.assertTrue(containers==21&&fixedStock>=200,"Original random supplies and stocked industrial cabinets/racks are present");
            for(int x:new int[]{61,65})for(int z:new int[]{72,85})for(int height=2;height<=5;height++)
                helper.assertTrue(level.getBlockState(origin.offset(new BlockPos(x,height,z).rotate(rotation))).is(Blocks.STONE_BRICKS),"All four range building corner columns are complete");
            helper.assertTrue(fixedItems>=3000,"Warehouse contains substantial actual stock");
            var pantry=(net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity)level.getBlockEntity(origin.offset(new BlockPos(35,2,15).rotate(rotation)));
            helper.assertTrue(!pantry.isEmpty(),"Lodge kitchen supplies are present");
            var medicine=(net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity)level.getBlockEntity(origin.offset(new BlockPos(30,2,21).rotate(rotation)));
            int potions=0;
            for(int slot=0;slot<medicine.getContainerSize();slot++) {
                var item=medicine.getItem(slot);
                if(item.is(net.minecraft.world.item.Items.POTION)) {
                    var contents=item.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
                    helper.assertTrue(contents!=null&&contents.potion().isPresent(),"Medical potions contain real effects");potions++;
                }
            }
            helper.assertTrue(potions>=3,"Medical chest guarantees healing and regeneration supplies");
            for(int x=0;x<120;x++)for(int z=0;z<300;z++) {
                var pos=origin.offset(new BlockPos(x,0,z).rotate(rotation));
                helper.assertTrue(level.getBlockState(pos).isSolid(),"Foundation seam is continuous at "+pos);
                for(int depth=1;depth<=4;depth++)helper.assertTrue(!level.getBlockState(pos.below(depth)).isAir(),"No floating foundation gap at "+pos);
            }
            helper.assertTrue(structure.biomes().stream().noneMatch(helper.getLevel().getChunkSource().getGenerator().getBiomeSource().possibleBiomes()::contains),"Base is restricted to the engineering dimension biome");
            org.slf4j.LoggerFactory.getLogger("ccq_testground").info("Natural complete base at {} rotation={}, verified {} blocks, {} random loot containers, {} fixed containers / {} items",origin,rotation,blocks,containers,fixedStock,fixedItems);
            // Reproduce a saved missing chunk, then recover it without rerolling loot
            // or overwriting a player's replacement block elsewhere in the base.
            var hole=new ChunkPos(origin.offset(new BlockPos(65,0,250).rotate(rotation)));
            for(var pos:BlockPos.betweenClosed(hole.getMinBlockX(),origin.getY(),hole.getMinBlockZ(),hole.getMaxBlockX(),origin.getY()+29,hole.getMaxBlockZ()))
                if(!level.getBlockState(pos).isAir())level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
            var changed=origin.offset(new BlockPos(1,0,1).rotate(rotation));
            level.setBlock(changed,Blocks.DIAMOND_BLOCK.defaultBlockState(),2);
            var commandPosition=origin.offset(new BlockPos(60,0,150).rotate(rotation));
            try {
                helper.assertTrue(EngineeringBaseRepair.repair(level,commandPosition).orElseThrow()>0,"Recovery fills an actually missing chunk");
                helper.assertTrue(EngineeringBaseRepair.repair(level,commandPosition).orElseThrow()==0,"Recovery is idempotent");
            } catch(java.io.IOException exception) {throw new java.io.UncheckedIOException(exception);}
            helper.runAfterDelay(20,()->{
                EngineeringTemplateLighting.assertConnections(helper,level,origin,rotation,data.getList("blocks",10));
                helper.assertTrue(level.getBlockState(changed).is(Blocks.DIAMOND_BLOCK),"Recovery preserves player blocks");
                for(var entry:data.getList("blocks",10)) {
                    var block=(CompoundTag)entry;var coords=block.getList("pos",3);
                    var pos=origin.offset(new BlockPos(coords.getInt(0),coords.getInt(1),coords.getInt(2)).rotate(rotation));
                    if(pos.equals(changed))continue;
                    helper.assertTrue(level.getBlockState(pos).is(palette.get(block.getInt("state")).getBlock()),"Recovered base is complete at "+pos);
                    helper.assertTrue(level.getBlockState(pos).canSurvive(level,pos),"Recovered block survives at "+pos);
                }
                openedLoot.forEach((pos,tag)->helper.assertTrue(tag.equals(level.getBlockEntity(pos).saveWithoutMetadata(level.registryAccess())),"Recovery preserves existing container inventory and loot state"));
                forced.forEach(pos->level.setChunkForced(pos.x,pos.z,false));helper.succeed();
            });
        });
    }
}

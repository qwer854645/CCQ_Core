package ccq.core.testground;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ccq_base_placement")
@PrefixGameTestTemplate(false)
public class EngineeringPlacementGameTests {
    @GameTest(template="empty",batch="command_intact",timeoutTicks=1200)
    public static void structureCommandWorksOffGridAndLoadsWholeBase(GameTestHelper helper)throws Exception {
        checkCommand(helper,false,false,Rotation.NONE,new BlockPos(12000,180,12000));
    }

    @GameTest(template="empty",batch="command_rotated_template",timeoutTicks=1200)
    public static void templateCommandLoadsWholeRotatedBase(GameTestHelper helper)throws Exception {
        checkCommand(helper,false,true,Rotation.CLOCKWISE_90,new BlockPos(16000,180,16000));
    }

    @GameTest(template="empty",batch="command_ruined",timeoutTicks=1200)
    public static void ruinedCommandPlacesUndergroundAtCorrectHeight(GameTestHelper helper)throws Exception {
        checkCommand(helper,true,false,Rotation.NONE,new BlockPos(20000,180,20000));
    }

    private static void checkCommand(GameTestHelper helper,boolean ruined,boolean template,Rotation rotation,BlockPos requested)throws Exception {
        var level=helper.getLevel().getServer().getLevel(TestGroundModule.DIMENSION);
        if(ruined)level.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL,true);
        var placement=new EngineeringBasePlacement(64,32,91827361);
        helper.assertTrue(placement.findSection(level.getSeed(),new ChunkPos(requested)).isEmpty(),"Regression target is outside the natural grid");
        var forced=new HashSet<Long>();for(long chunk:level.getForcedChunks())forced.add(chunk);
        level.getChunk(requested.getX()>>4,requested.getZ()>>4);
        var source=level.getServer().createCommandSourceStack().withLevel(level).withPosition(Vec3.atCenterOf(requested)).withPermission(4).withSuppressedOutput();
        String command=template?"place template ccq_testground:engineering/engineering_base":"place structure ccq_testground:engineering_base"+(ruined?"_ruined":"");
        command+=" "+requested.getX()+" "+requested.getY()+" "+requested.getZ();
        if(template)command+=" clockwise_90";
        int result=level.getServer().getCommands().getDispatcher().execute(command,source);
        helper.assertTrue(result==1,"Actual vanilla /place command succeeds");
        var after=new HashSet<Long>();for(long chunk:level.getForcedChunks())after.add(chunk);
        helper.assertTrue(forced.equals(after),"Manual placement leaves persistent forced chunks unchanged");
        var origin=requested.below(ruined?12:0);
        CompoundTag raw;
        try(var stream=level.getServer().getResourceManager().getResourceOrThrow(ResourceLocation.parse("ccq_testground:structure/engineering/engineering_base"+(ruined?"_ruined":"")+".nbt")).open()) {
            raw=NbtIo.readCompressed(stream,NbtAccounter.unlimitedHeap());
        }
        var palette=raw.getList("palette",10);int solids=0,air=0,containers=0;
        for(var value:raw.getList("blocks",10)) {
            var block=(CompoundTag)value;var p=block.getList("pos",3);
            var pos=origin.offset(new BlockPos(p.getInt(0),p.getInt(1),p.getInt(2)).rotate(rotation));
            String expected=palette.getCompound(block.getInt("state")).getString("Name");
            if(expected.equals("minecraft:air")) {
                if(ruined&&p.getInt(1)<12){helper.assertTrue(level.getBlockState(pos).isAir(),"Underground crater air is actually excavated at "+p);air++;}
                continue;
            }
            helper.assertTrue(BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString().equals(expected),"Whole command result matches template at "+p);solids++;
            var actualState=level.getBlockState(pos);
            EngineeringCrankChecks.check(helper,level,pos,actualState);
            if(actualState.getBlock() instanceof net.minecraft.world.level.block.SlabBlock
                    &&actualState.getValue(net.minecraft.world.level.block.SlabBlock.TYPE)==net.minecraft.world.level.block.state.properties.SlabType.BOTTOM)
                helper.assertTrue(level.getBlockState(pos.above()).isAir(),"No upper block floats over a bottom slab: "+p);
            if(expected.contains("industrial_iron_lamp")) {
                java.util.function.Predicate<BlockPos> envelope=q->{String id=BuiltInRegistries.BLOCK.getKey(level.getBlockState(q).getBlock()).toString();return id.endsWith("_bricks")||id.endsWith("_slab");};
                helper.assertTrue(!(envelope.test(pos.east())&&envelope.test(pos.west()))&&!(envelope.test(pos.north())&&envelope.test(pos.south())),"Lamps do not replace a continuous wall or roof: "+p);
            }
            if(block.getCompound("nbt").contains("Items",9)) {
                var container=(net.minecraft.world.Container)level.getBlockEntity(pos);helper.assertTrue(container!=null,"Stock container exists");
                for(var itemValue:block.getCompound("nbt").getList("Items",10)) {
                    var item=(CompoundTag)itemValue;var actual=container.getItem(item.getByte("Slot"));
                    helper.assertTrue(!actual.isEmpty()&&BuiltInRegistries.ITEM.getKey(actual.getItem()).toString().equals(item.getString("id"))&&actual.getCount()==item.getInt("count"),"Fixed inventory survives /place at "+p);
                }
                containers++;
            }
        }
        helper.assertTrue(solids>50000&&containers>50,"All sections and substantial stock placed");
        if(ruined)helper.assertTrue(air>10000,"Deep crater air is encoded and placed");
        EngineeringTemplateLighting.assertConnections(helper,level,origin,rotation,raw.getList("blocks",10));
        org.slf4j.LoggerFactory.getLogger("ccq_testground").info("/place regression passed: ruined={}, rotation={}, solids={}, underground air={}, fixed containers={}",ruined,rotation,solids,air,containers);
        helper.succeed();
    }

    @GameTest(template="empty",batch="ruined_natural",timeoutTicks=1800)
    public static void ruinedSectionsGenerateTogetherFromFarEnd(GameTestHelper helper)throws Exception {
        var level=helper.getLevel().getServer().getLevel(TestGroundModule.DIMENSION);
        level.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL,true);
        var flag=net.minecraft.world.level.levelgen.WorldOptions.class.getDeclaredField("generateStructures");flag.setAccessible(true);flag.setBoolean(level.getServer().getWorldData().worldGenOptions(),true);
        var registry=level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        var ruin=registry.get(ResourceLocation.parse("ccq_testground:engineering_base_ruined"));
        var intact=registry.get(ResourceLocation.parse("ccq_testground:engineering_base"));
        var set=level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).get(ResourceLocation.parse("ccq_testground:engineering_ruins"));
        var placement=(EngineeringBasePlacement)set.placement();ChunkPos root=null;
        for(int x=510;x<528&&root==null;x++)for(int z=510;z<528&&root==null;z++) {
            var candidate=placement.getPotentialStructureChunk(level.getSeed(),x*placement.spacing(),z*placement.spacing());
            if(!placement.matchesVariant(level.getSeed(),candidate))continue;
            var section=placement.section(level.getSeed(),candidate,1);
            var chunk=level.getChunk(section.chunk().x,section.chunk().z,ChunkStatus.STRUCTURE_STARTS);
            var start=chunk.getStartForStructure(ruin);
            if(start!=null&&start.isValid())root=candidate;
        }
        helper.assertTrue(root!=null,"A terrain-eligible ruined site exists");
        var serialization=StructurePieceSerializationContext.fromLevel(level);Integer y=null;var origins=new ArrayList<BlockPos>();
        for(int index:new int[]{2,0,1}) {
            var section=placement.section(level.getSeed(),root,index);
            var chunk=level.getChunk(section.chunk().x,section.chunk().z,ChunkStatus.STRUCTURE_STARTS);
            var start=chunk.getStartForStructure(ruin);helper.assertTrue(start!=null&&start.isValid(),"Ruined section exists from either end");
            var conflicting=chunk.getStartForStructure(intact);helper.assertTrue(conflicting==null||!conflicting.isValid(),"Intact and ruined starts never mix at a shared site");
            var piece=(TemplateStructurePiece)start.getPieces().getFirst();
            if(y==null)y=piece.templatePosition().getY();
            helper.assertTrue(piece.templatePosition().equals(section.origin(y)),"All ruined sections share origin, height and rotation");
            var tag=piece.createTag(serialization);helper.assertTrue(tag.getString("Template").contains("engineering/ruined_base/section_"+index),"Correct ruined section resource");
            var restored=(TemplateStructurePiece)piece.getType().load(serialization,tag);
            helper.assertTrue(restored.templatePosition().equals(piece.templatePosition()),"Ruined offset survives serialization");
            origins.add(piece.templatePosition());
            var box=start.getBoundingBox();
            for(int cx=box.minX()>>4;cx<=box.maxX()>>4;cx++)for(int cz=box.minZ()>>4;cz<=box.maxZ()>>4;cz++)level.getChunk(cx,cz);
        }
        var rotation=placement.section(level.getSeed(),root,1).rotation();var origin=placement.section(level.getSeed(),root,1).fullOrigin().atY(y);
        CompoundTag raw;
        try(var stream=level.getServer().getResourceManager().getResourceOrThrow(ResourceLocation.parse("ccq_testground:structure/engineering/engineering_base_ruined.nbt")).open()) {
            raw=NbtIo.readCompressed(stream,NbtAccounter.unlimitedHeap());
        }
        int excavated=0;
        for(var value:raw.getList("blocks",10)) {
            var block=(CompoundTag)value;var p=block.getList("pos",3);var pos=origin.offset(new BlockPos(p.getInt(0),p.getInt(1),p.getInt(2)).rotate(rotation));
            String expected=raw.getList("palette",10).getCompound(block.getInt("state")).getString("Name");
            var actual=level.getBlockState(pos);
            // Underground ores are populated after surface structures. They may
            // replace authored substrate, but must never refill excavated air.
            boolean oreReplacement=p.getInt(1)<12&&Set.of("minecraft:stone","minecraft:deepslate","minecraft:andesite","minecraft:tuff").contains(expected)
                    &&actual.is(net.neoforged.neoforge.common.Tags.Blocks.ORES);
            helper.assertTrue(BuiltInRegistries.BLOCK.getKey(actual.getBlock()).toString().equals(expected)||oreReplacement,"Natural ruined template at "+p+" expected="+expected+" actual="+BuiltInRegistries.BLOCK.getKey(actual.getBlock())+" origin="+origin+" rotation="+rotation);
            if(expected.equals("minecraft:air")&&p.getInt(1)<12)excavated++;
        }
        helper.assertTrue(excavated>10000,"Natural generation leaves deep craters open");
        EngineeringTemplateLighting.assertConnections(helper,level,origin,rotation,raw.getList("blocks",10));
        var bounds=placement.siteBounds(level.getSeed(),root);
        var area=new net.minecraft.world.phys.AABB(bounds.minX(),origin.getY(),bounds.minZ(),bounds.maxX()+1,origin.getY()+42,bounds.maxZ()+1);
        var forced=new ArrayList<ChunkPos>();
        for(int cx=bounds.minX()>>4;cx<=bounds.maxX()>>4;cx++)for(int cz=bounds.minZ()>>4;cz<=bounds.maxZ()>>4;cz++) {
            level.setChunkForced(cx,cz,true);forced.add(new ChunkPos(cx,cz));
        }
        helper.runAfterDelay(10,()->{
            try {
                var mobs=level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,area);
                helper.assertTrue(mobs.size()==16,"Natural generation spawns exactly sixteen encounters across all three sections: "+mobs.size());
                for(var mob:mobs)helper.assertTrue(mob.isPersistenceRequired()&&level.noCollision(mob),"Natural ruin mobs are persistent and not embedded in blocks");
                helper.succeed();
            } finally {for(var chunk:forced)level.setChunkForced(chunk.x,chunk.z,false);}
        });
    }

    @GameTest(template="empty",batch="ruin_encounters",timeoutTicks=500)
    public static void placedRuinsContainSafePersistentEncounters(GameTestHelper helper)throws Exception {
        var level=helper.getLevel().getServer().getLevel(TestGroundModule.DIMENSION);
        level.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL,true);
        var position=new BlockPos(28000,180,28000);
        var source=level.getServer().createCommandSourceStack().withLevel(level).withPosition(Vec3.atCenterOf(position)).withPermission(4).withSuppressedOutput();
        EngineeringBaseCommands.place(source,position,true,Rotation.CLOCKWISE_90,net.minecraft.world.level.block.Mirror.NONE,1,0);
        var corner=position.offset(new BlockPos(119,0,299).rotate(Rotation.CLOCKWISE_90));
        var area=new net.minecraft.world.phys.AABB(Math.min(position.getX(),corner.getX()),168,Math.min(position.getZ(),corner.getZ()),Math.max(position.getX(),corner.getX())+1,210,Math.max(position.getZ(),corner.getZ())+1);
        var forced=new ArrayList<ChunkPos>();
        for(int cx=((int)area.minX)>>4;cx<=((int)area.maxX)>>4;cx++)for(int cz=((int)area.minZ)>>4;cz<=((int)area.maxZ)>>4;cz++) {
            level.setChunkForced(cx,cz,true);forced.add(new ChunkPos(cx,cz));
        }
        helper.runAfterDelay(10,()->{
            try {
                var mobs=level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,area);
                helper.assertTrue(mobs.size()==16,"Rotated manual placement creates sixteen mobs, not duplicated per section: "+mobs.size());
                int zombies=0,skeletons=0,spiders=0;
                for(var mob:mobs) {
                    helper.assertTrue(mob.isPersistenceRequired()&&level.noCollision(mob),"Encounter survives reload and has collision-free space");
                    float health=mob instanceof net.minecraft.world.entity.monster.Zombie?40:mob instanceof net.minecraft.world.entity.monster.Skeleton?36:28;
                    helper.assertTrue(mob.getMaxHealth()==health&&mob.getHealth()==health,"Authored health is actually applied: "+mob.getType()+" "+mob.getHealth()+"/"+mob.getMaxHealth());
                    var saved=mob.saveWithoutId(new CompoundTag());var restored=(net.minecraft.world.entity.Mob)mob.getType().create(level);restored.load(saved);
                    helper.assertTrue(restored.getMaxHealth()==health&&restored.getHealth()==health,"Increased health survives entity serialization");
                    if(mob instanceof net.minecraft.world.entity.monster.Zombie)zombies++;
                    else if(mob instanceof net.minecraft.world.entity.monster.Skeleton)skeletons++;
                    else if(mob instanceof net.minecraft.world.entity.monster.Spider)spiders++;
                    if(!(mob instanceof net.minecraft.world.entity.monster.Spider))helper.assertTrue(!mob.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).isEmpty(),"Sun-sensitive mobs have protective helmets");
                    if(!(mob instanceof net.minecraft.world.entity.monster.Spider)) {
                        helper.assertTrue(mob.getArmorValue()>=12,"Guards wear functional mixed armor");
                        var hand=mob.getMainHandItem();
                        helper.assertTrue(mob instanceof net.minecraft.world.entity.monster.Skeleton?hand.is(net.minecraft.world.item.Items.BOW):hand.is(net.minecraft.world.item.Items.IRON_SWORD)||hand.is(net.minecraft.world.item.Items.IRON_AXE),"Guards have the correct bow or melee weapon");
                        helper.assertTrue(restored.getMainHandItem().is(hand.getItem()),"Weapon survives serialization");
                        for(var slot:List.of(net.minecraft.world.entity.EquipmentSlot.HEAD,net.minecraft.world.entity.EquipmentSlot.CHEST,net.minecraft.world.entity.EquipmentSlot.LEGS,net.minecraft.world.entity.EquipmentSlot.FEET))
                            helper.assertTrue(restored.getItemBySlot(slot).is(mob.getItemBySlot(slot).getItem()),"Armor item survives serialization");
                    }
                }
                helper.assertTrue(zombies==8&&skeletons==5&&spiders==3,"Authored monster distribution is preserved");
                helper.succeed();
            } finally {for(var chunk:forced)level.setChunkForced(chunk.x,chunk.z,false);}
        });
    }

    @GameTest(template="empty",batch="variant_grid",timeoutTicks=100)
    public static void legacyIntactGridIsPreservedAndRuinsAvoidIt(GameTestHelper helper) {
        var intact=new EngineeringBasePlacement(64,32,EngineeringBasePlacement.INTACT_SALT,false);
        var legacy=new EngineeringBasePlacement(64,32,91827361);
        var ruined=new EngineeringBasePlacement(64,32,EngineeringBasePlacement.RUINED_SALT,true);
        int ruins=0,rejected=0;
        for(long seed:new long[]{-4103457699211345949L,0,735267L})for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++) {
            var root=intact.getPotentialStructureChunk(seed,x*intact.spacing(),z*intact.spacing());
            helper.assertTrue(root.equals(legacy.getPotentialStructureChunk(seed,x*legacy.spacing(),z*legacy.spacing()))&&intact.matchesVariant(seed,root),"Every legacy intact site stays intact after upgrading");
            var ruinRoot=ruined.getPotentialStructureChunk(seed,x*ruined.spacing(),z*ruined.spacing());
            for(int index=0;index<3;index++) {
                var section=ruined.section(seed,ruinRoot,index);
                helper.assertTrue(ruined.findSection(seed,section.chunk()).orElseThrow().root().equals(ruinRoot),"All ruined sections resolve to one shared site");
            }
            if(ruined.matchesVariant(seed,ruinRoot,intact)) {
                ruins++;var box=ruined.siteBounds(seed,ruinRoot);
                for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++) {
                    var other=intact.siteBounds(seed,intact.getPotentialStructureChunk(seed,(x+dx)*intact.spacing(),(z+dz)*intact.spacing()));
                    helper.assertTrue(!box.intersects(other),"Accepted ruins never overlap existing intact-grid footprints");
                }
            } else rejected++;
        }
        helper.assertTrue(ruins>50&&rejected>0,"Ruin candidates include usable sites and rejected overlaps");helper.succeed();
    }
}

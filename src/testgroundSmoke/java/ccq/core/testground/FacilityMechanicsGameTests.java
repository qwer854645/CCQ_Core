package ccq.core.testground;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.crank.HandCrankBlockEntity;
import com.simibubi.create.content.contraptions.pulley.PulleyBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ccq_testground")
@PrefixGameTestTemplate(false)
public class FacilityMechanicsGameTests {
    record Fixture(String key, BlockPos origin, StructurePlaceSettings settings) {
        BlockPos at(int x,int y,int z) {
            return origin.offset(StructureTemplate.calculateRelativePosition(settings,new BlockPos(x,y,z)));
        }
    }

    @GameTest(template="empty", batch="mechanics", timeoutTicks=500)
    public static void actualTransmissionLiftAndCannonAssembly(GameTestHelper helper) {
        var level=helper.getLevel().getServer().getLevel(TestGroundModule.DIMENSION);
        var fixtures=new ArrayList<Fixture>();
        var forced=new java.util.HashSet<net.minecraft.world.level.ChunkPos>();
        for(var rotation:Rotation.values()) {
            for(int i=0;i<3;i++) {
                String key=new String[]{"survey_tower","assembly_pad","calibration_range"}[i];
                var template=level.getServer().getStructureManager().getOrCreate(ResourceLocation.parse("ccq_testground:engineering/"+key));
                var origin=new BlockPos(8192+i*96,196,8192+rotation.ordinal()*96);
                var settings=new StructurePlaceSettings().setRotation(rotation).setIgnoreEntities(true);
                var full=template.getBoundingBox(settings,origin);
                var end=origin.offset(StructureTemplate.calculateRelativePosition(settings,
                        new BlockPos(template.getSize().getX()-1,template.getSize().getY()-1,key.equals("calibration_range")?24:template.getSize().getZ()-1)));
                var box=new BoundingBox(Math.min(origin.getX(),end.getX()),full.minY(),Math.min(origin.getZ(),end.getZ()),
                        Math.max(origin.getX(),end.getX()),full.maxY(),Math.max(origin.getZ(),end.getZ()));
                for(int cx=(box.minX()>>4)-1;cx<=(box.maxX()>>4)+1;cx++) for(int cz=(box.minZ()>>4)-1;cz<=(box.maxZ()>>4)+1;cz++) {
                    level.getChunk(cx,cz);level.setChunkForced(cx,cz,true);forced.add(new net.minecraft.world.level.ChunkPos(cx,cz));
                }
                // Reset reused test sites, including moving entities left by failed runs.
                level.getEntitiesOfClass(com.simibubi.create.content.contraptions.AbstractContraptionEntity.class,
                        net.minecraft.world.phys.AABB.of(box).inflate(32)).forEach(net.minecraft.world.entity.Entity::discard);
                for(var pos:BlockPos.betweenClosed(box.minX(),box.minY(),box.minZ(),box.maxX(),box.maxY(),box.maxZ()))
                    if(!level.getBlockState(pos).isAir()) level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
                settings.setBoundingBox(box);
                placeBaseSections(helper,level,new Fixture(key,origin,settings));
                fixtures.add(new Fixture(key,origin,settings));
            }
        }
        var cranks=new ArrayList<HandCrankBlockEntity>();
        boolean[] powered={false};
        var backwards=new java.util.HashSet<HandCrankBlockEntity>();
        helper.onEachTick(()->{if(powered[0]) cranks.forEach(crank->crank.turn(backwards.contains(crank)));});
        helper.runAfterDelay(40,()->{
            for(var fixture:fixtures) {
                var settings=fixture.settings.copy().setBoundingBox(null);
                var crankBlock=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(ResourceLocation.parse("create:hand_crank"));
                for(var info:baseCranks(level,fixture,crankBlock)) {
                    if(!fixture.settings.getBoundingBox().isInside(info.pos())) continue;
                    var state=level.getBlockState(info.pos());
                    helper.assertTrue(state.is(crankBlock),"Crank survives placement: "+info.pos());
                    var inward=state.getValue(BlockStateProperties.FACING).getOpposite();
                    var target=info.pos().relative(inward);var neighbor=level.getBlockState(target);
                    helper.assertTrue(neighbor.getBlock() instanceof IRotate rotate && rotate.hasShaftTowards(level,target,neighbor,inward.getOpposite()),
                            "Crank connected to a real shaft face: "+fixture.key+" "+info.pos()+" -> "+neighbor);
                    cranks.add((HandCrankBlockEntity)level.getBlockEntity(info.pos()));
                }
                if(fixture.key.equals("calibration_range")) {
                    for(int x:new int[]{10,25,40}) {
                        var controller=(com.simibubi.create.content.kinetics.speedController.SpeedControllerBlockEntity)level.getBlockEntity(fixture.at(x+2,2,8));
                        helper.assertTrue(controller.targetSpeed.getValue()==1,"Controller loads its 1 RPM preset");
                        var mount=level.getBlockEntity(fixture.at(x,3,8));
                        loadRound(level,fixture.at(x,5,6),"big_cartridge",fixture.settings.getRotation().rotate(Direction.SOUTH));
                        loadRound(level,fixture.at(x,5,7),"solid_shot",fixture.settings.getRotation().rotate(Direction.SOUTH));
                        var lever=fixture.at(x,3,7);
                        level.setBlock(lever,level.getBlockState(lever).setValue(BlockStateProperties.POWERED,true),3);
                        call(mount,"onRedstoneUpdate",new Class<?>[]{boolean.class,boolean.class,boolean.class,boolean.class,int.class},true,false,false,false,0);
                        helper.assertTrue((boolean)call(mount,"isRunning",new Class<?>[]{}),"Cannon assembles in "+fixture.settings.getRotation()+": "+call(mount,"getLastAssemblyException",new Class<?>[]{}));
                        var entity=(com.simibubi.create.content.contraptions.AbstractContraptionEntity)call(mount,"getContraption",new Class<?>[]{});
                        helper.assertTrue(entity.getContraption().getBlocks().size()==(x==10?8:x==25?7:10),"All connected gun segments are assembled");
                    }
                }
            }
            powered[0]=true;
            helper.runAfterDelay(4,()->{
                // Hand cranks support both directions. Select the lifting direction from actual output.
                for(var fixture:fixtures) {
                    if(fixture.key.equals("assembly_pad")) {
                        var initialLeft=(PulleyBlockEntity)level.getBlockEntity(fixture.at(11,14,17));
                        if(initialLeft.getSpeed()>0)
                            backwards.add((HandCrankBlockEntity)level.getBlockEntity(fixture.at(6,14,17)));
                    } else if(fixture.key.equals("survey_tower")) {
                        var initialWinch=(PulleyBlockEntity)level.getBlockEntity(fixture.at(17,10,6));
                        if(initialWinch.getSpeed()>0)
                            backwards.add((HandCrankBlockEntity)level.getBlockEntity(fixture.at(18,10,4)));
                    }
                }
                helper.runAfterDelay(35,()->{
                    for(var f:fixtures) {
                        if(f.key.equals("assembly_pad")) {
                            var left=(PulleyBlockEntity)level.getBlockEntity(f.at(11,14,17));
                            var right=(PulleyBlockEntity)level.getBlockEntity(f.at(19,14,17));
                            helper.assertTrue(left.getSpeed()!=0 && left.getSpeed()==right.getSpeed(),"Two pulleys share the same shaft speed");
                            helper.assertTrue(left.getMirrorParent()!=null || right.getMirrorParent()!=null,"Second hook uses native mirroring");
                            var parent=left.getMirrorParent()==null?left:right;
                            var child=parent==left?right:left;
                            helper.assertTrue(parent.getBlockPos().equals(child.getMirrorParent()),"Both hooks belong to one native lift");
                            var load=parent.getAttachedContraption();
                            helper.assertTrue(load!=null && parent.getInterpolatedOffset(1)<7.9f,"Lift actually raises its load in "+f.settings.getRotation());
                            helper.assertTrue(load.getContraption().getBlocks().size()==9,"All nine chassis form one moving beam: "+f.settings.getRotation()+" count="+load.getContraption().getBlocks().size());
                            helper.assertTrue(load.getContraption().getBlocks().keySet().stream().map(BlockPos::getY).distinct().count()==1,"The moving beam stays level");
                            var gear=(KineticBlockEntity)level.getBlockEntity(f.at(26,6,17));
                            var press=(KineticBlockEntity)level.getBlockEntity(f.at(4,6,23));
                            helper.assertTrue(gear.getSpeed()!=0 && press.getSpeed()!=0,"Gear bench and press receive actual rotation");
                        }
                        if(f.key.equals("survey_tower")) {
                            var winch=(PulleyBlockEntity)level.getBlockEntity(f.at(17,10,6));
                            helper.assertTrue(winch.getSpeed()!=0,"Tower winch receives actual rotation");
                            boolean parked=false;
                            for(int y=4;y<=9;y++) parked|=level.getBlockState(f.at(17,y,6)).is(Blocks.BARREL);
                            helper.assertTrue(winch.getAttachedContraption()!=null || parked,"Tower winch moves or parks the raised supply crate: "+winch.saveWithoutMetadata(level.registryAccess()));
                            helper.assertTrue(!level.getBlockState(f.at(17,3,6)).is(Blocks.BARREL),"Supply crate left its original landing");
                        }
                        if(f.key.equals("calibration_range")) for(int x:new int[]{10,25,40}) {
                            var mount=level.getBlockEntity(f.at(x,3,8));
                            var pitch=(KineticBlockEntity)call(mount,"getPitchInterface",new Class<?>[]{});
                            var yaw=(KineticBlockEntity)call(mount,"getYawInterface",new Class<?>[]{});
                            helper.assertTrue(pitch.getSpeed()!=0 && yaw.getSpeed()!=0,"Cannon yaw and pitch inputs both work: "+f.settings.getRotation()+" x="+x+" pitch="+pitch.getSpeed()+"/"+pitch.getTheoreticalSpeed()+" yaw="+yaw.getSpeed()+"/"+yaw.getTheoreticalSpeed());
                            helper.assertTrue(Math.abs(pitch.getSpeed())==1 && Math.abs(yaw.getSpeed())==1,"Speed controller delivers 1 RPM to both gun axes");
                            var entity=call(mount,"getContraption",new Class<?>[]{});
                            var cannon=((com.simibubi.create.content.contraptions.AbstractContraptionEntity)entity).getContraption();
                            call(cannon,"fireShot",new Class<?>[]{ServerLevel.class,entity.getClass()},level,entity);
                            try {helper.assertTrue(cannon.getClass().getField("hasFired").getBoolean(cannon),"Loaded cannon actually fires: "+f.settings.getRotation()+" x="+x);}
                            catch(ReflectiveOperationException exception) {throw new IllegalStateException(exception);}
                            call(mount,"disassemble",new Class<?>[]{});
                        }
                    }
                    powered[0]=false;
                    for(var f:fixtures) if(f.key.equals("assembly_pad")) {
                        ((PulleyBlockEntity)level.getBlockEntity(f.at(11,14,17))).disassemble();
                        ((PulleyBlockEntity)level.getBlockEntity(f.at(19,14,17))).disassemble();
                    } else if(f.key.equals("survey_tower")) ((PulleyBlockEntity)level.getBlockEntity(f.at(17,10,6))).disassemble();
                    forced.forEach(pos->level.setChunkForced(pos.x,pos.z,false));
                    helper.succeed();
                });
            });
        });
    }

    private static BlockPos baseOrigin(Fixture fixture) {
        var offset=switch(fixture.key) {
            case "survey_tower" -> new BlockPos(8,0,14);
            case "assembly_pad" -> new BlockPos(10,0,77);
            default -> new BlockPos(60,0,68);
        };
        return fixture.origin.subtract(offset.rotate(fixture.settings.getRotation()));
    }

    private static void placeBaseSections(GameTestHelper helper,ServerLevel level,Fixture fixture) {
        var origin=baseOrigin(fixture);
        for(int i=0;i<3;i++) {
            var template=level.getServer().getStructureManager().getOrCreate(ResourceLocation.fromNamespaceAndPath("ccq_testground",EngineeringBasePlacement.sectionPath(i,fixture.settings.getRotation())));
            var part=origin.offset(new BlockPos(0,0,i*100).rotate(fixture.settings.getRotation()));
            var settings=fixture.settings.copy().setKnownShape(true);
            if(template.getBoundingBox(settings,part).intersects(settings.getBoundingBox()))
                helper.assertTrue(template.placeInWorld(level,part,part,settings,level.random,2),"Actual merged base section placed: "+fixture.key+"/"+i);
        }
    }

    private static java.util.List<StructureTemplate.StructureBlockInfo> baseCranks(ServerLevel level,Fixture fixture,net.minecraft.world.level.block.Block block) {
        var result=new ArrayList<StructureTemplate.StructureBlockInfo>();
        var origin=baseOrigin(fixture);
        for(int i=0;i<3;i++) {
            var template=level.getServer().getStructureManager().getOrCreate(ResourceLocation.fromNamespaceAndPath("ccq_testground",EngineeringBasePlacement.sectionPath(i,fixture.settings.getRotation())));
            var part=origin.offset(new BlockPos(0,0,i*100).rotate(fixture.settings.getRotation()));
            result.addAll(template.filterBlocks(part,fixture.settings.copy().setBoundingBox(null),block));
        }
        return result;
    }

    private static Object call(Object target,String name,Class<?>[] types,Object... args) {
        try { return target.getClass().getMethod(name,types).invoke(target,args); }
        catch(ReflectiveOperationException exception) {throw new IllegalStateException(name,exception);}
    }

    private static void loadRound(ServerLevel level,BlockPos pos,String ammunition,Direction facing) {
        var be=level.getBlockEntity(pos);
        var behavior=call(be,"cannonBehavior",new Class<?>[]{});
        var state=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(ResourceLocation.parse("createbigcannons:"+ammunition)).defaultBlockState();
        if(state.hasProperty(BlockStateProperties.AXIS)) state=state.setValue(BlockStateProperties.AXIS,facing.getAxis());
        if(state.hasProperty(BlockStateProperties.FACING)) state=state.setValue(BlockStateProperties.FACING,facing);
        net.minecraft.nbt.CompoundTag tag=null;
        if(ammunition.equals("big_cartridge")) {
            @SuppressWarnings("rawtypes") var filled=(net.minecraft.world.level.block.state.properties.Property)state.getBlock().getStateDefinition().getProperty("filled");
            state=state.setValue(filled,true);
            tag=new net.minecraft.nbt.CompoundTag();tag.putInt("Power",4);
        }
        var info=new StructureTemplate.StructureBlockInfo(BlockPos.ZERO,state,tag);
        if(!(boolean)call(behavior,"tryLoadingBlock",new Class<?>[]{StructureTemplate.StructureBlockInfo.class},info))
            throw new IllegalStateException("Cannot load test round at "+pos);
    }
}

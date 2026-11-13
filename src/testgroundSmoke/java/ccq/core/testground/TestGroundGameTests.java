package ccq.core.testground;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ccq_testground")
@PrefixGameTestTemplate(false)
public class TestGroundGameTests {
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void stationaryDropsAndExperienceTeleport(GameTestHelper helper) {
        var level=helper.getLevel();
        var iron=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("create:industrial_iron_block"));
        var frame=new PortalFrame(helper.absolutePos(new BlockPos(2,2,3)),Direction.Axis.X,3,4);
        for(int x=-1;x<=3;x++) for(int y=-1;y<=4;y++) level.setBlock(frame.at(x,y),
                x==-1||x==3||y==-1||y==4 ? iron.defaultBlockState() : Blocks.AIR.defaultBlockState(),3);
        frame.fill(level);
        var destination=PortalTravel.resolveExit(level,frame);
        var target=destination.level(); var pos=destination.frame().origin();
        for(int x=-2;x<=2;x++) for(int z=-2;z<=2;z++) target.getChunk((pos.getX()>>4)+x,(pos.getZ()>>4)+z);
        target.setChunkForced(pos.getX()>>4,pos.getZ()>>4,true);
        var stack=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND,32);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Portal Regression"));
        var start=Vec3.atBottomCenterOf(frame.at(1,1));
        var drop=new net.minecraft.world.entity.item.ItemEntity(level,start.x,start.y,start.z,stack.copy());
        drop.setNoGravity(true); drop.setDeltaMovement(Vec3.ZERO); drop.setNeverPickUp();
        var xp=new net.minecraft.world.entity.ExperienceOrb(level,start.x,start.y,start.z,37);
        xp.setNoGravity(true); xp.setDeltaMovement(Vec3.ZERO);
        level.addFreshEntity(drop); level.addFreshEntity(xp);
        helper.succeedWhen(()->{
            var moved=target.getEntity(drop.getUUID()); var orb=target.getEntity(xp.getUUID());
            helper.assertTrue(moved instanceof net.minecraft.world.entity.item.ItemEntity,"Stationary item enters target dimension");
            helper.assertTrue(orb instanceof net.minecraft.world.entity.ExperienceOrb,"Experience orb enters target dimension");
            var item=(net.minecraft.world.entity.item.ItemEntity)moved;
            helper.assertTrue(item.getItem().getCount()==32 && net.minecraft.world.item.ItemStack.isSameItemSameComponents(stack,item.getItem()),"Item count and components preserved");
            helper.assertTrue(((net.minecraft.world.entity.ExperienceOrb)orb).getValue()==37,"XP value preserved");
            helper.assertTrue(level.getEntity(drop.getUUID())==null && level.getEntity(xp.getUUID())==null,"No duplicate entities left behind");
            helper.assertTrue(item.isOnPortalCooldown() && orb.isOnPortalCooldown(),"Vanilla portal cooldown is applied to arriving entities");
            item.discard();orb.discard();target.setChunkForced(pos.getX()>>4,pos.getZ()>>4,false);
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void broadPlainsAndRichOres(GameTestHelper helper) {
        var level=helper.getLevel().getServer().getLevel(TestGroundModule.DIMENSION);
        var generator=level.getChunkSource().getGenerator();
        var heights=new java.util.HashMap<Integer,Integer>();
        for(int x=-8;x<=8;x++) for(int z=-8;z<=8;z++) {
            int y=generator.getBaseHeight(x*256,z*256,Heightmap.Types.WORLD_SURFACE_WG,level,level.getChunkSource().randomState());
            heights.merge(y,1,Integer::sum);
        }
        int flat=heights.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        helper.assertTrue(flat>=116 && flat<=217,"Sampled flat terrain should occupy 40–75% of the area: "+heights);
        int ores=0;
        var types=new java.util.HashSet<String>();
        for(int cx=0;cx<2;cx++) for(int cz=0;cz<2;cz++) {
            var chunk=level.getChunk(cx,cz);
            for(int x=0;x<16;x++) for(int z=0;z<16;z++) for(int y=-56;y<=120;y++) {
                var state=chunk.getBlockState(new BlockPos(cx*16+x,y,cz*16+z));
                String id=BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
                if(id.endsWith("_ore")) { ores++; types.add(id); }
            }
        }
        helper.assertTrue(ores>=800,"Four chunks should contain plentiful ores, found "+ores);
        org.slf4j.LoggerFactory.getLogger("ccq_testground").info("Terrain sample: flat={}/289, heights={}; ores in four chunks={}, ore variants={}",flat,heights,ores,types.size());
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void andesiteSurfaceWithoutAnimals(GameTestHelper helper) {
        var level=helper.getLevel().getServer().getLevel(TestGroundModule.DIMENSION);
        for(int x : new int[]{1024,1056,1088}) {
            level.getChunk(x >> 4,64);
            int top=level.getHeight(Heightmap.Types.WORLD_SURFACE,x,1024)-1;
            var pos=new BlockPos(x,top,1024);
            for(int depth=0;depth<5;depth++) helper.assertTrue(level.getBlockState(pos.below(depth)).is(Blocks.ANDESITE),
                    "Surface must have five andesite layers at "+pos+", depth "+depth);
            helper.assertTrue(level.getBlockState(pos.below(5)).is(Blocks.STONE),"Stone beneath andesite surface at "+pos);
            var spawns=level.getBiome(pos).value().getMobSettings();
            for(var category : net.minecraft.world.entity.MobCategory.values()) {
                if(category!=net.minecraft.world.entity.MobCategory.MONSTER && category!=net.minecraft.world.entity.MobCategory.MISC)
                    helper.assertTrue(spawns.getMobs(category).isEmpty(),"No animal spawn entries for "+category);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void nativeCreatePortalTracks(GameTestHelper helper) {
        var level=helper.getLevel();
        var iron=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("create:industrial_iron_block"));
        var frame=new PortalFrame(helper.absolutePos(new BlockPos(2,2,3)),Direction.Axis.X,4,4);
        for(int x=-1;x<=frame.width();x++) for(int y=-1;y<=frame.height();y++) level.setBlock(frame.at(x,y),
                x==-1||x==frame.width()||y==-1||y==frame.height() ? iron.defaultBlockState() : Blocks.AIR.defaultBlockState(),3);
        frame.fill(level);
        helper.assertTrue(com.simibubi.create.api.contraption.train.PortalTrackProvider.isSupportedPortal(level.getBlockState(frame.origin())),"Create recognizes the custom portal");
        var trackPos=frame.at(2,0).north();
        var face=new net.createmod.catnip.math.BlockFace(trackPos,Direction.SOUTH);
        var exit=com.simibubi.create.api.contraption.train.PortalTrackProvider.getOtherSide(level,face);
        helper.assertTrue(exit!=null && exit.level().dimension().equals(TestGroundModule.DIMENSION),"Track finds paired test dimension");
        var target=PortalFrame.find(exit.level(),exit.face().getConnectedPos(),Direction.Axis.X);
        helper.assertTrue(target!=null && target.width()==4 && target.height()==4,"Generated exit preserves aperture size");
        helper.assertTrue(exit.face().getConnectedPos().equals(target.at(2,0)),"Track lane offset preserved");
        var reverse=com.simibubi.create.api.contraption.train.PortalTrackProvider.getOtherSide(exit.level(),exit.face());
        helper.assertTrue(reverse!=null && reverse.level()==level && reverse.face().equals(face),"Track exit maps back to exact source lane");
        level.setBlock(trackPos.below(),iron.defaultBlockState(),3);
        var track=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("create:track"));
        level.setBlock(trackPos,track.defaultBlockState().setValue(com.simibubi.create.content.trains.track.TrackBlock.SHAPE,
                com.simibubi.create.content.trains.track.TrackShape.ZO),3);
        helper.runAfterDelay(12,()->{
            helper.assertTrue(level.getBlockEntity(trackPos) instanceof com.simibubi.create.content.trains.track.TrackBlockEntity,"Native portal track block entity created");
            var be=(com.simibubi.create.content.trains.track.TrackBlockEntity)level.getBlockEntity(trackPos);
            helper.assertTrue(be.boundLocation!=null && be.boundLocation.getFirst().equals(exit.level().dimension())
                    && be.boundLocation.getSecond().equals(exit.face().getPos()),"Native cross-dimension track binding");
            var other=exit.level().getBlockEntity(exit.face().getPos());
            helper.assertTrue(other instanceof com.simibubi.create.content.trains.track.TrackBlockEntity,"Native exit track auto-created");
            helper.assertTrue(((com.simibubi.create.content.trains.track.TrackBlockEntity)other).boundLocation.getSecond().equals(trackPos),"Exit track bound back");
            level.destroyBlock(frame.at(-1,1),false);
            helper.runAfterDelay(25,()->{
                helper.assertTrue(!level.getBlockState(trackPos).hasProperty(com.simibubi.create.content.trains.track.TrackBlock.SHAPE)
                        || !level.getBlockState(trackPos).getValue(com.simibubi.create.content.trains.track.TrackBlock.SHAPE).isPortal(),"Broken gate invalidates portal track");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void stationaryPlayerChargesWithoutMovementPackets(GameTestHelper helper) {
        var level=helper.getLevel();
        var iron=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("create:industrial_iron_block"));
        var frame=new PortalFrame(helper.absolutePos(new BlockPos(1,2,2)),Direction.Axis.X,2,3);
        for(int x=-1;x<=2;x++) for(int y=-1;y<=3;y++) level.setBlock(frame.at(x,y),
                x==-1||x==2||y==-1||y==3 ? iron.defaultBlockState() : Blocks.AIR.defaultBlockState(),3);
        frame.fill(level);
        var player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"StaticPortalTest"));
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.moveTo(Vec3.atBottomCenterOf(frame.origin()));
        helper.assertTrue(TestGroundModule.PORTAL.get() instanceof net.minecraft.world.level.block.Portal,"Portal implements vanilla interface");
        helper.assertTrue(TestGroundModule.PORTAL.get().getPortalTransitionTime(level,player)
                ==((net.minecraft.world.level.block.Portal)Blocks.NETHER_PORTAL).getPortalTransitionTime(level,player),"Delay follows vanilla gamerules");
        helper.onEachTick(()->{
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre(player));
            player.baseTick(); // Fake players do not receive the normal network-driven doTick.
        });
        helper.runAfterDelay(30,()->{
            helper.assertTrue(player.portalProcess!=null && player.portalProcess.isSamePortal(TestGroundModule.PORTAL.get()) && player.portalProcess.getPortalTime()>=25,
                    "Stationary player charges through vanilla PortalProcessor");
            player.moveTo(player.getX(),player.getY(),player.getZ()+3);
            helper.runAfterDelay(2,()->{
                helper.assertTrue(player.portalProcess==null || player.portalProcess.getPortalTime()<30,"Vanilla processor decays after leaving");
                player.moveTo(Vec3.atBottomCenterOf(frame.origin()));
                helper.runAfterDelay(30,()->{
                    helper.assertTrue(player.portalProcess!=null && player.portalProcess.getPortalTime()>=25,
                            "Native processor resumes charging on reentry");
                    player.moveTo(player.getX(),player.getY(),player.getZ()+3);
                    helper.succeed();
                });
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void brokenSurfaceAndFrameCollapse(GameTestHelper helper) {
        var level=helper.getLevel();
        var iron=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("create:industrial_iron_block"));
        var frames=new java.util.ArrayList<PortalFrame>();
        for(int i=0;i<4;i++) {
            var frame=new PortalFrame(helper.absolutePos(new BlockPos(1+i*4,2,3)),Direction.Axis.Z,2,3);
            for(int x=-1;x<=2;x++) for(int y=-1;y<=3;y++) level.setBlock(frame.at(x,y),
                    x==-1||x==2||y==-1||y==3 ? iron.defaultBlockState() : Blocks.AIR.defaultBlockState(),3);
            frame.fill(level); frames.add(frame);
        }
        helper.runAfterDelay(2,()->{
            for(var frame:frames) helper.assertTrue(frame.isComplete(level),"Ignition survives initial scheduled validation");
            var player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            var portal=level.getBlockState(frames.get(0).origin());
            helper.assertTrue(portal.getDestroyProgress(player,level,frames.get(0).origin())==0,"Survival cannot mine portal surface");
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            helper.assertTrue(player.gameMode.destroyBlock(frames.get(0).origin()),"Creative can remove a portal tile");
            level.destroyBlock(frames.get(1).at(-1,1),false);
            level.destroyBlock(frames.get(2).at(-1,-1),false);
            helper.runAfterDelay(25,()->{
                for(int i=0;i<3;i++) for(int x=0;x<2;x++) for(int y=0;y<3;y++)
                    helper.assertTrue(!level.getBlockState(frames.get(i).at(x,y)).is(TestGroundModule.PORTAL.get()),"All portal tiles collapse for broken case "+i);
                helper.assertTrue(frames.get(3).isComplete(level),"An unrelated intact portal stays lit");
                frames.get(0).fill(level);
                helper.runAfterDelay(2,()->{
                    helper.assertTrue(frames.get(0).isComplete(level),"Intact frame can be relit after surface destruction");
                    helper.succeed();
                });
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void frameAndPairPersistence(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var iron = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("create:industrial_iron_block"));
        helper.assertTrue(iron != Blocks.AIR, "Create industrial iron block must be installed");
        for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
            PortalFrame frame = new PortalFrame(helper.absolutePos(new BlockPos(axis == Direction.Axis.X ? 1 : 9,2,1)), axis,2,3);
            for (int x=-1;x<=2;x++) for(int y=-1;y<=3;y++) level.setBlock(frame.at(x,y),
                    x==-1 || x==2 || y==-1 || y==3 ? iron.defaultBlockState() : Blocks.AIR.defaultBlockState(),3);
            helper.assertTrue(frame.equals(PortalFrame.find(level,frame.at(1,2),axis)), "Recognize complete frame in both orientations");
            var player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                    new net.minecraft.world.item.ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("create:wrench"))));
            var click=new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock(player,
                    net.minecraft.world.InteractionHand.MAIN_HAND,frame.at(0,-1),
                    new net.minecraft.world.phys.BlockHitResult(frame.at(0,-1).getCenter(),Direction.UP,frame.at(0,-1),false));
            PortalTravel.activate(click);
            helper.assertTrue(click.isCanceled(),"Wrench right click activates the frame");
            helper.assertTrue(level.getBlockState(frame.origin()).is(TestGroundModule.PORTAL.get()),"Portal lights without collapsing");
            level.setBlock(frame.at(-1,1),Blocks.AIR.defaultBlockState(),3);
            helper.assertTrue(PortalFrame.find(level,frame.origin(),axis)==null,"Broken frame rejected");
        }
        PortalLinks links = new PortalLinks();
        var a = new PortalLinks.Endpoint(level.dimension(),new BlockPos(100,128,100));
        var b = new PortalLinks.Endpoint(TestGroundModule.DIMENSION,new BlockPos(100,130,100));
        links.pair(a,b);
        var saved = links.save(new net.minecraft.nbt.CompoundTag(),level.registryAccess());
        helper.assertTrue(saved.getList("links",10).size()==2,"Both link directions saved");
        var reloaded=PortalLinks.load(saved,level.registryAccess());
        helper.assertTrue(b.equals(reloaded.target(a)) && a.equals(reloaded.target(b)),"Pair survives reload");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 1400)
    public static void terrainAndEntityRoundTrip(GameTestHelper helper) {
        ServerLevel dimension=helper.getLevel().getServer().getLevel(TestGroundModule.DIMENSION);
        helper.assertTrue(dimension!=null,"Dimension loaded");
        var features=dimension.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.PLACED_FEATURE);
        var ores=features.getTag(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.PLACED_FEATURE,
                ResourceLocation.parse("ccq_testground:ores"))).orElseThrow();
        for(String feature : new String[]{"create:zinc_ore","createnuclear:lead_ore","createnuclear:uranium_ore",
                "createnuclear:thorium_ore","createnuclear:nitrate_ore","createpropulsion:ore_platinum","expandeddelight:salt_ore"}) {
            var id=ResourceLocation.parse(feature);
            String mineral=id.getPath().replace("ore_","").replace("_ore","");
            var rich=ResourceLocation.parse("ccq_testground:rich_"+mineral);
            if(features.containsKey(id)) helper.assertTrue(ores.stream().anyMatch(holder->holder.is(rich)),"Rich ore feature linked: "+rich);
        }
        for(int x : new int[]{0,32,64}) {
            dimension.getChunk(x >> 4,0);
            int y=dimension.getHeight(Heightmap.Types.WORLD_SURFACE,x,0);
            helper.assertTrue(y>96 && y<160,"Ground should be near Y128, was "+y);
            for(int h=-58;h<y-5;h++) helper.assertTrue(!dimension.getBlockState(new BlockPos(x,h,0)).isAir(),"No underground cavity at "+x+","+h);
        }
        var level=helper.getLevel();
        var iron=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("create:industrial_iron_block"));
        PortalFrame frame=new PortalFrame(helper.absolutePos(new BlockPos(1,2,1)),Direction.Axis.X,2,3);
        for(int x=-1;x<=2;x++) for(int y=-1;y<=3;y++) level.setBlock(frame.at(x,y),
                x==-1||x==2||y==-1||y==3 ? iron.defaultBlockState() : Blocks.AIR.defaultBlockState(),3);
        frame.fill(level);
        var pig=EntityType.PIG.create(level);
        // Preload the destination before creating a stationary mob; GameTest has no real
        // player tickets and can advance hundreds of ticks before async chunk work finishes.
        var prepared=PortalTravel.resolveExit(level,frame);
        var ready=prepared.frame().origin();
        for(int x=-2;x<=2;x++) for(int z=-2;z<=2;z++) {
            dimension.getChunk((ready.getX()>>4)+x,(ready.getZ()>>4)+z);
            dimension.setChunkForced((ready.getX()>>4)+x,(ready.getZ()>>4)+z,true);
        }
        pig.setNoAi(true);
        pig.moveTo(Vec3.atBottomCenterOf(frame.origin()));
        level.addFreshEntity(pig);
        boolean[] returning={false};
        // Keep the mob in the entry during warm-up without disabling its collision checks.
        helper.onEachTick(()->{
            var original=level.getEntity(pig.getUUID());
            if(original!=null && helper.getTick()<45) original.moveTo(Vec3.atBottomCenterOf(frame.origin()));
            if(returning[0]) {
                var target=PortalLinks.get(level.getServer()).target(new PortalLinks.Endpoint(level.dimension(),frame.origin()));
                var traveler=dimension.getEntity(pig.getUUID());
                if(traveler!=null) traveler.moveTo(Vec3.atBottomCenterOf(target.pos()));
            }
        });
        helper.runAfterDelay(160,()->{
            var links=PortalLinks.get(level.getServer());
            var target=links.target(new PortalLinks.Endpoint(level.dimension(),frame.origin()));
            helper.assertTrue(target!=null && target.dimension().equals(TestGroundModule.DIMENSION),"Exit paired");
            // GameTest runs without players and advances ticks faster than asynchronous terrain
            // generation. Make the remote entity-ticking neighborhood ready explicitly.
            for(int dx=-2;dx<=2;dx++) for(int dz=-2;dz<=2;dz++)
                dimension.getChunk((target.pos().getX()>>4)+dx,(target.pos().getZ()>>4)+dz);
            dimension.setChunkForced(target.pos().getX()>>4,target.pos().getZ()>>4,true);
            helper.runAfterDelay(20,()->{
            var moved=dimension.getEntity(pig.getUUID());
            helper.assertTrue(moved!=null,"Entity must enter test dimension after destination chunks become ready: "+pig.position()+" alive="+pig.isAlive()+" data="+pig.getPersistentData());
            moved.moveTo(moved.getX(),moved.getY(),moved.getZ()+3);
            // Wait for entity ticks, rather than assuming remote chunks tick once per
            // GameTest tick while their asynchronous tickets are becoming ready.
            helper.startSequence()
                .thenWaitUntil(()->helper.assertTrue(!moved.isOnPortalCooldown(),"Native portal cooldown expired"))
                .thenExecute(()->{
                    returning[0]=true;
                    moved.moveTo(Vec3.atBottomCenterOf(target.pos()));
                })
                .thenWaitUntil(()->helper.assertTrue(level.getEntity(pig.getUUID())!=null,"Entity returns to original dimension"))
                .thenExecute(()->{
                    level.getEntity(pig.getUUID()).discard();
                    for(int dx=-2;dx<=2;dx++) for(int dz=-2;dz<=2;dz++)
                        dimension.setChunkForced((target.pos().getX()>>4)+dx,(target.pos().getZ()>>4)+dz,false);
                })
                .thenSucceed();
            });
        });
    }
}

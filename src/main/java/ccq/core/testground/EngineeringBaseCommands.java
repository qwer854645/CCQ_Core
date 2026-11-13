package ccq.core.testground;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;

/** Manual placement is independent of natural grid/terrain eligibility. */
public final class EngineeringBaseCommands {
    private static final TicketType<ChunkPos> TICKET=TicketType.create("ccq_base_command",Comparator.comparingLong(ChunkPos::toLong),200);
    private static final DynamicCommandExceptionType INVALID=new DynamicCommandExceptionType(id->Component.translatable("commands.place.template.invalid",id));
    private static final SimpleCommandExceptionType FAILED=new SimpleCommandExceptionType(Component.translatable("commands.place.template.failed"));
    private static final SimpleCommandExceptionType OUT_OF_WORLD=new SimpleCommandExceptionType(Component.translatable("argument.pos.outofworld"));
    private EngineeringBaseCommands() {}

    public static boolean isBaseTemplate(ResourceLocation id) {
        return id.getNamespace().equals(TestGroundModule.ID)&&(id.getPath().equals("engineering/engineering_base")||id.getPath().equals("engineering/engineering_base_ruined"));
    }

    public static boolean isBunkerTemplate(ResourceLocation id) {
        return id.toString().equals("ccq_testground:engineering/nuclear_bunker");
    }

    /** Bunker commands use the requested Y as the surrounding ground surface. */
    public static int placeBunker(CommandSourceStack source,BlockPos position,Rotation rotation,Mirror mirror,float integrity,int seed)throws CommandSyntaxException {
        var level=source.getLevel();var id=ResourceLocation.parse("ccq_testground:engineering/nuclear_bunker");
        var template=level.getStructureManager().get(id).orElseThrow(()->INVALID.create(id.toString()));
        var origin=position.below(8);
        var settings=new StructurePlaceSettings().setRotation(rotation).setMirror(mirror).setIgnoreEntities(false).setFinalizeEntities(false)
                .setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING);
        var bounds=template.getBoundingBox(settings,origin);
        if(bounds.minY()<level.getMinBuildHeight()||bounds.maxY()>=level.getMaxBuildHeight()
                ||!level.getWorldBorder().isWithinBounds(new BlockPos(bounds.minX(),origin.getY(),bounds.minZ()))
                ||!level.getWorldBorder().isWithinBounds(new BlockPos(bounds.maxX(),origin.getY(),bounds.maxZ())))throw OUT_OF_WORLD.create();
        var acquired=new ArrayList<ChunkPos>();
        try {
            for(var chunk:ChunkPos.rangeClosed(new ChunkPos(bounds.minX()>>4,bounds.minZ()>>4),new ChunkPos(bounds.maxX()>>4,bounds.maxZ()>>4)).toList()) {
                level.getChunkSource().addRegionTicket(TICKET,chunk,2,chunk);acquired.add(chunk);level.getChunk(chunk.x,chunk.z);
            }
            // Recover a docked or moving breech bridge before replacing the cannon.
            for(int x:new int[]{19,76})for(int z:new int[]{19,76}) {
                int inward=x==19?1:-1,sign=z==19?-1:1;
                var lift=origin.offset(StructureTemplate.transform(new BlockPos(x+inward*6,25,z-sign*3),mirror,rotation,BlockPos.ZERO));
                if(level.getBlockEntity(lift) instanceof com.simibubi.create.content.contraptions.piston.MechanicalPistonBlockEntity piston)piston.disassemble();
                var a=origin.offset(StructureTemplate.transform(new BlockPos(x,19,z-sign*3),mirror,rotation,BlockPos.ZERO));
                var b=origin.offset(StructureTemplate.transform(new BlockPos(x+inward*6,24,z-sign*3),mirror,rotation,BlockPos.ZERO));
                var bridge=new net.minecraft.world.phys.AABB(Math.min(a.getX(),b.getX()),Math.min(a.getY(),b.getY()),Math.min(a.getZ(),b.getZ()),
                        Math.max(a.getX(),b.getX())+1,Math.max(a.getY(),b.getY())+1,Math.max(a.getZ(),b.getZ())+1);
                level.getEntitiesOfClass(com.simibubi.create.content.contraptions.glue.SuperGlueEntity.class,bridge).forEach(net.minecraft.world.entity.Entity::discard);
                for(var cell:BlockPos.betweenClosed(Math.min(a.getX(),b.getX()),19+origin.getY(),Math.min(a.getZ(),b.getZ()),Math.max(a.getX(),b.getX()),24+origin.getY(),Math.max(a.getZ(),b.getZ())))
                    level.setBlock(cell,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2|16);
                level.setBlock(lift,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2|16);
            }
            // Recover both the former indoor guns and the new roof guns before
            // rebuilding, including guns currently assembled or tilted.
            for(int x:new int[]{19,76})for(int z:new int[]{19,76}) {
                for(int y:new int[]{10,16}) {
                    var pos=origin.offset(StructureTemplate.transform(new BlockPos(x,y,z),mirror,rotation,BlockPos.ZERO));
                    var be=level.getBlockEntity(pos);
                    if(be!=null&&net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(be.getBlockState().getBlock()).toString().equals("createbigcannons:cannon_mount")) {
                        try {be.getClass().getMethod("disassemble").invoke(be);}
                        catch(ReflectiveOperationException e){throw FAILED.create();}
                    }
                }
                var a=origin.offset(StructureTemplate.transform(new BlockPos(x-5,9,z-14),mirror,rotation,BlockPos.ZERO));
                var b=origin.offset(StructureTemplate.transform(new BlockPos(x+5,20,z+14),mirror,rotation,BlockPos.ZERO));
                for(var pos:BlockPos.betweenClosed(Math.min(a.getX(),b.getX()),Math.min(a.getY(),b.getY()),Math.min(a.getZ(),b.getZ()),Math.max(a.getX(),b.getX()),Math.max(a.getY(),b.getY()),Math.max(a.getZ(),b.getZ())))
                    level.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2|16);
            }
            // Finish the old authored doors before overwriting their blocks, so
            // moving contraptions cannot later put the previous leaf back.
            for(var local:java.util.List.of(new BlockPos(54,13,85),new BlockPos(52,13,79),new BlockPos(52,13,89),new BlockPos(52,6,51),new BlockPos(48,18,79),new BlockPos(47,19,79),new BlockPos(48,18,89),new BlockPos(47,19,89))) {
                var pos=origin.offset(StructureTemplate.transform(local,mirror,rotation,BlockPos.ZERO));
                if(level.getBlockEntity(pos) instanceof com.simibubi.create.content.contraptions.piston.MechanicalPistonBlockEntity piston)
                    piston.disassemble();
            }
            for(var region:java.util.List.of(new BlockPos[]{new BlockPos(34,9,76),new BlockPos(63,23,79)},
                    new BlockPos[]{new BlockPos(34,9,80),new BlockPos(63,23,95)},
                    new BlockPos[]{new BlockPos(36,2,48),new BlockPos(59,7,54)},
                    new BlockPos[]{new BlockPos(69,8,17),new BlockPos(80,12,21)})) {
            var first=origin.offset(StructureTemplate.transform(region[0],mirror,rotation,BlockPos.ZERO));
            var last=origin.offset(StructureTemplate.transform(region[1],mirror,rotation,BlockPos.ZERO));
            var entry=new net.minecraft.world.phys.AABB(Math.min(first.getX(),last.getX()),Math.min(first.getY(),last.getY()),Math.min(first.getZ(),last.getZ()),
                    Math.max(first.getX(),last.getX())+1,Math.max(first.getY(),last.getY())+1,Math.max(first.getZ(),last.getZ())+1);
            level.getEntitiesOfClass(com.simibubi.create.content.contraptions.glue.SuperGlueEntity.class,entry).forEach(net.minecraft.world.entity.Entity::discard);
            // Replacing a kinetic block with the same block type can retain its
            // previous block entity (extension offset, crank use and network).
            // Clear this authored entry volume first to rebuild fresh machines.
            for(var pos:BlockPos.betweenClosed(Math.min(first.getX(),last.getX()),Math.min(first.getY(),last.getY()),Math.min(first.getZ(),last.getZ()),
                    Math.max(first.getX(),last.getX()),Math.max(first.getY(),last.getY()),Math.max(first.getZ(),last.getZ())))
                level.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2|16);
            }
            var random=RandomSource.create(seed);
            if(integrity<1)settings.addProcessor(new BlockRotProcessor(integrity)).setRandom(random);
            if(!template.placeInWorld(level,origin,origin,settings,random,2))throw FAILED.create();
            return 1;
        } finally {
            for(var chunk:acquired)level.getChunkSource().removeRegionTicket(TICKET,chunk,2,chunk);
        }
    }

    /** The requested position is the original platform base, for both variants. */
    public static int place(CommandSourceStack source,BlockPos position,boolean ruined,Rotation rotation,Mirror mirror,float integrity,int seed)throws CommandSyntaxException {
        var level=source.getLevel();var manager=level.getStructureManager();
        var origin=position.below(ruined?EngineeringStructures.RUINED_GROUND_OFFSET:0);
        var settings=new StructurePlaceSettings().setRotation(rotation).setMirror(mirror).setIgnoreEntities(!ruined).setFinalizeEntities(false)
                .setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING).setKnownShape(mirror==Mirror.NONE&&integrity==1);
        var masterId=ResourceLocation.fromNamespaceAndPath(TestGroundModule.ID,"engineering/engineering_base"+(ruined?"_ruined":""));
        var master=manager.get(masterId).orElseThrow(()->INVALID.create(masterId.toString()));
        BoundingBox bounds=master.getBoundingBox(settings,origin);
        if(bounds.minY()<level.getMinBuildHeight()||bounds.maxY()>=level.getMaxBuildHeight()
                ||!level.getWorldBorder().isWithinBounds(new BlockPos(bounds.minX(),origin.getY(),bounds.minZ()))
                ||!level.getWorldBorder().isWithinBounds(new BlockPos(bounds.maxX(),origin.getY(),bounds.maxZ())))throw OUT_OF_WORLD.create();
        var templates=new ArrayList<StructureTemplate>();var origins=new ArrayList<BlockPos>();
        for(int section=0;section<3;section++) {
            var id=ResourceLocation.fromNamespaceAndPath(TestGroundModule.ID,EngineeringBasePlacement.sectionPath(section,mirror==Mirror.NONE?rotation:Rotation.NONE,ruined));
            templates.add(manager.get(id).orElseThrow(()->INVALID.create(id.toString())));
            origins.add(origin.offset(StructureTemplate.transform(new BlockPos(0,0,section*100),mirror,rotation,BlockPos.ZERO)));
        }
        var chunks=ChunkPos.rangeClosed(new ChunkPos(bounds.minX()>>4,bounds.minZ()>>4),new ChunkPos(bounds.maxX()>>4,bounds.maxZ()>>4)).toList();
        var acquired=new ArrayList<ChunkPos>();
        try {
            // Short-lived tickets hold the entire footprint during this command;
            // player-created /forceload entries are never added or removed.
            for(var chunk:chunks) {
                level.getChunkSource().addRegionTicket(TICKET,chunk,2,chunk);acquired.add(chunk);
                level.getChunk(chunk.x,chunk.z);
            }
            var random=RandomSource.create(seed);
            if(integrity<1)settings.addProcessor(new BlockRotProcessor(integrity)).setRandom(random);
            for(int section=0;section<3;section++) {
                if(!templates.get(section).placeInWorld(level,origins.get(section),origin,settings.copy(),random,2))throw FAILED.create();
            }
            return 1;
        } finally {
            for(var chunk:acquired)level.getChunkSource().removeRegionTicket(TICKET,chunk,2,chunk);
        }
    }
}

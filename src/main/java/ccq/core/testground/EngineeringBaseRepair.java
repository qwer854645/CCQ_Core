package ccq.core.testground;

import java.io.IOException;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;

/** Explicit admin recovery of saved partial bases; never runs automatically. */
public final class EngineeringBaseRepair {
    private EngineeringBaseRepair() {}

    public static OptionalInt repair(ServerLevel level,BlockPos position) throws IOException {
        if(!level.dimension().equals(TestGroundModule.DIMENSION))return OptionalInt.empty();
        var structure=level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .get(ResourceLocation.parse("ccq_testground:engineering_base"));
        var serialization=StructurePieceSerializationContext.fromLevel(level);
        // Read actual starts rather than infer them from today's world seed/grid.
        for(int dx=-10;dx<=10;dx++)for(int dz=-10;dz<=10;dz++) {
            var chunk=level.getChunk((position.getX()>>4)+dx,(position.getZ()>>4)+dz,ChunkStatus.STRUCTURE_STARTS,false);
            if(chunk==null)continue;
            var start=chunk.getStartForStructure(structure);
            if(start==null||!start.isValid())continue;
            for(var piece:start.getPieces())if(piece instanceof TemplateStructurePiece templatePiece) {
                var tag=piece.createTag(serialization);
                String name=tag.getString("Template");
                if(!name.matches("ccq_testground:engineering/base/section_[0-2](_clockwise_90|_clockwise_180|_counterclockwise_90)?"))continue;
                int index=name.charAt("ccq_testground:engineering/base/section_".length())-'0';
                var rotation=templatePiece.getRotation();
                var origin=templatePiece.templatePosition().subtract(new BlockPos(0,0,index*100).rotate(rotation));
                var corner=origin.offset(new BlockPos(119,0,299).rotate(rotation));
                if(position.getX()<Math.min(origin.getX(),corner.getX())||position.getX()>Math.max(origin.getX(),corner.getX())
                        ||position.getZ()<Math.min(origin.getZ(),corner.getZ())||position.getZ()>Math.max(origin.getZ(),corner.getZ()))continue;
                return OptionalInt.of(restoreAir(level,origin,rotation));
            }
        }
        return OptionalInt.empty();
    }

    private static int restoreAir(ServerLevel level,BlockPos origin,Rotation rotation) throws IOException {
        var data=new CompoundTag();var palette=new ListTag();var blocks=new ListTag();
        for(int section=0;section<3;section++) {
            CompoundTag part;
            var path=EngineeringBasePlacement.sectionPath(section,rotation);
            try(var stream=level.getServer().getResourceManager().getResourceOrThrow(
                    ResourceLocation.fromNamespaceAndPath(TestGroundModule.ID,"structure/"+path+".nbt")).open()) {
                part=NbtIo.readCompressed(stream,NbtAccounter.unlimitedHeap());
            }
            int offset=palette.size();var partPalette=part.getList("palette",10);
            partPalette.forEach(value->palette.add(value.copy()));
            for(var value:part.getList("blocks",10)) {
                var block=((CompoundTag)value).copy();int state=block.getInt("state");
                if(partPalette.getCompound(state).getString("Name").equals("minecraft:air"))continue;
                block.putInt("state",state+offset);
                var pos=block.getList("pos",3);pos.set(2,IntTag.valueOf(pos.getInt(2)+section*100));
                blocks.add(block);
            }
        }
        data.put("palette",palette);data.put("blocks",blocks);
        var size=new ListTag();for(int dimension:new int[]{120,30,300})size.add(IntTag.valueOf(dimension));
        data.put("size",size);data.put("entities",new ListTag());
        var missing=new ListTag();
        for(var value:data.getList("blocks",10)) {
            var block=(CompoundTag)value;var coordinates=block.getList("pos",3);
            var pos=origin.offset(new BlockPos(coordinates.getInt(0),coordinates.getInt(1),coordinates.getInt(2)).rotate(rotation));
            // Existing block entities are excluded too: opened loot never resets.
            if(level.getBlockState(pos).isAir())missing.add(block);
        }
        if(missing.isEmpty())return 0;
        data.put("blocks",missing);
        var template=new StructureTemplate();
        template.load(level.holderLookup(Registries.BLOCK),data);
        var settings=new StructurePlaceSettings().setRotation(rotation).setIgnoreEntities(true).setKnownShape(true)
                .setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING);
        template.placeInWorld(level,origin,origin,settings,level.random,2);
        for(int x=0;x<120;x++)for(int z=0;z<300;z++) {
            var ground=origin.offset(new BlockPos(x,0,z).rotate(rotation));
            if(!level.getBlockState(ground).isSolid())continue;
            for(int depth=1;depth<=3;depth++) {
                var below=ground.below(depth);
                if(!level.getBlockState(below).isAir())break;
                level.setBlock(below,Blocks.ANDESITE.defaultBlockState(),2);
            }
        }
        return missing.size();
    }
}

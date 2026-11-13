package ccq.core.testground;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.placement.*;

/** Three nearby starts keep every part inside vanilla's eight-chunk reference radius. */
public final class EngineeringBasePlacement extends RandomSpreadStructurePlacement {
    public static final int WIDTH=120, LENGTH=300, SECTION_LENGTH=100;
    public static final int INTACT_SALT=91827361,RUINED_SALT=91827362;
    public static final MapCodec<EngineeringBasePlacement> CODEC=RecordCodecBuilder.<EngineeringBasePlacement>mapCodec(i->i.group(
            Codec.intRange(40,4096).fieldOf("spacing").forGetter(EngineeringBasePlacement::spacing),
            Codec.intRange(24,4095).fieldOf("separation").forGetter(EngineeringBasePlacement::separation),
            Codec.intRange(0,Integer.MAX_VALUE).fieldOf("salt").forGetter(p->p.salt()),
            Codec.BOOL.optionalFieldOf("ruined",false).forGetter(p->p.ruined)
    ).apply(i,EngineeringBasePlacement::new)).validate(p->p.spacing()>p.separation()?DataResult.success(p):DataResult.error(()->"spacing must exceed separation"));

    private final boolean ruined;
    private final int authoredSpacing,authoredSeparation;
    public EngineeringBasePlacement(int spacing,int separation,int salt) {this(spacing,separation,salt,false);}
    public EngineeringBasePlacement(int spacing,int separation,int salt,boolean ruined) {
        super(spacing,separation,RandomSpreadType.LINEAR,salt);
        this.ruined=ruined;
        this.authoredSpacing=spacing;this.authoredSeparation=separation;
    }

    public record Section(ChunkPos root,ChunkPos chunk,Rotation rotation,BlockPos fullOrigin,int index) {
        public BlockPos origin(int y) {
            return fullOrigin.offset(new BlockPos(0,0,index*SECTION_LENGTH).rotate(rotation)).atY(y);
        }
    }

    public static String sectionPath(int index,Rotation rotation) {
        return sectionPath(index,rotation,false);
    }

    public static String sectionPath(int index,Rotation rotation,boolean ruined) {
        return "engineering/"+(ruined?"ruined_base":"base")+"/section_"+index+(rotation==Rotation.NONE?"":"_"+rotation.name().toLowerCase(java.util.Locale.ROOT));
    }

    public boolean matchesVariant(long seed,ChunkPos root) {
        return matchesVariant(seed,root,new EngineeringBasePlacement(authoredSpacing,authoredSeparation,INTACT_SALT,false));
    }

    public boolean matchesVariant(long seed,ChunkPos root,EngineeringBasePlacement intact) {
        // Preserve every intact site from older releases. Changing a site's
        // variant would mix already-saved intact starts with newly ruined ends.
        if(!ruined)return true;
        var mine=siteBounds(seed,root);
        int rx=Math.floorDiv(root.x,intact.spacing()),rz=Math.floorDiv(root.z,intact.spacing());
        for(int x=rx-1;x<=rx+1;x++)for(int z=rz-1;z<=rz+1;z++) {
            var other=intact.siteBounds(seed,intact.getPotentialStructureChunk(seed,x*intact.spacing(),z*intact.spacing()));
            if(mine.minX()<=other.maxX()+24&&mine.maxX()>=other.minX()-24
                    &&mine.minZ()<=other.maxZ()+24&&mine.maxZ()>=other.minZ()-24)return false;
        }
        return true;
    }

    public net.minecraft.world.level.levelgen.structure.BoundingBox siteBounds(long seed,ChunkPos root) {
        var section=section(seed,root,0);var origin=section.fullOrigin();
        var corner=origin.offset(new BlockPos(WIDTH-1,0,LENGTH-1).rotate(section.rotation()));
        return net.minecraft.world.level.levelgen.structure.BoundingBox.fromCorners(origin,corner);
    }

    public Section section(long seed,ChunkPos root,int index) {
        var random=new WorldgenRandom(new LegacyRandomSource(0));
        random.setLargeFeatureSeed(seed,root.x,root.z);
        var rotation=Rotation.getRandom(random);
        var corner=new BlockPos(WIDTH-1,0,LENGTH-1).rotate(rotation);
        var origin=new BlockPos(root.getMiddleBlockX()-corner.getX()/2,0,root.getMiddleBlockZ()-corner.getZ()/2);
        var center=origin.offset(new BlockPos(WIDTH/2,0,index*SECTION_LENGTH+SECTION_LENGTH/2).rotate(rotation));
        return new Section(root,new ChunkPos(center),rotation,origin,index);
    }

    public Optional<Section> findSection(long seed,ChunkPos chunk) {
        int rx=Math.floorDiv(chunk.x,spacing()),rz=Math.floorDiv(chunk.z,spacing());
        for(int x=rx-1;x<=rx+1;x++)for(int z=rz-1;z<=rz+1;z++) {
            var root=getPotentialStructureChunk(seed,x*spacing(),z*spacing());
            for(int i=0;i<3;i++) {
                var section=section(seed,root,i);
                if(section.chunk().equals(chunk))return Optional.of(section);
            }
        }
        return Optional.empty();
    }

    @Override protected boolean isPlacementChunk(ChunkGeneratorStructureState state,int x,int z) {
        var section=findSection(state.getLevelSeed(),new ChunkPos(x,z));
        if(section.isEmpty())return false;
        if(!ruined)return true;
        // Use the registered intact placement, including Structurify's per-set
        // modifiers, rather than applying a global multiplier for a second time.
        for(var holder:state.possibleStructureSets()) {
            if(holder.unwrapKey().map(key->key.location().toString().equals("ccq_testground:engineering_facilities")).orElse(false)
                    &&holder.value().placement() instanceof EngineeringBasePlacement intact)
                return matchesVariant(state.getLevelSeed(),section.get().root(),intact);
        }
        return false;
    }

    @Override public StructurePlacementType<?> type(){return EngineeringStructures.BASE_PLACEMENT.get();}
}

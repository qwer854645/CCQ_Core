package ccq.core.testground;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

/** A single saved anchor, retaining vanilla random-spread /locate support. */
public final class SpawnFacilityPlacement extends RandomSpreadStructurePlacement {
    public static final MapCodec<SpawnFacilityPlacement> CODEC=MapCodec.unit(SpawnFacilityPlacement::new);
    public SpawnFacilityPlacement(){super(32,16,RandomSpreadType.LINEAR,91827366);}
    @Override public ChunkPos getPotentialStructureChunk(long seed,int x,int z){
        var anchor=SpawnFacilityAnchor.position();
        return anchor==null?new ChunkPos(0,0):new ChunkPos(anchor);
    }
    @Override protected boolean isPlacementChunk(ChunkGeneratorStructureState state,int x,int z){
        var anchor=SpawnFacilityAnchor.position();
        return anchor!=null && (anchor.getX()>>4)==x && (anchor.getZ()>>4)==z;
    }
    @Override public BlockPos getLocatePos(ChunkPos chunk){
        var anchor=SpawnFacilityAnchor.position();
        return anchor==null?super.getLocatePos(chunk):anchor;
    }
    @Override public StructurePlacementType<?> type(){return EngineeringStructures.SPAWN_PLACEMENT.get();}
}

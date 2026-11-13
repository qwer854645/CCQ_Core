package ccq.core.testground;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.*;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class EngineeringStructures {
    public static final int RUINED_GROUND_OFFSET=12;
    private static final DeferredRegister<StructureType<?>> TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, TestGroundModule.ID);
    private static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, TestGroundModule.ID);
    private static final DeferredRegister<StructurePlacementType<?>> PLACEMENTS = DeferredRegister.create(Registries.STRUCTURE_PLACEMENT, TestGroundModule.ID);
    private static final DeferredRegister<StructureProcessorType<?>> PROCESSORS = DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, TestGroundModule.ID);
    static final java.util.function.Supplier<StructureProcessorType<FacilityLootProcessor>> FACILITY_LOOT_PROCESSOR = PROCESSORS.register("facility_loot",()->()->FacilityLootProcessor.CODEC);
    static final java.util.function.Supplier<StructurePlacementType<EngineeringBasePlacement>> BASE_PLACEMENT = PLACEMENTS.register("engineering_base",()->()->EngineeringBasePlacement.CODEC);
    static final java.util.function.Supplier<StructurePlacementType<SpawnFacilityPlacement>> SPAWN_PLACEMENT = PLACEMENTS.register("spawn_facility",()->()->SpawnFacilityPlacement.CODEC);
    private static final java.util.function.Supplier<StructureType<SpawnFacility>> SPAWN_TYPE = TYPES.register("spawn_facility",()->()->SpawnFacility.CODEC);
    private static final java.util.function.Supplier<StructureType<Base>> BASE_TYPE = TYPES.register("engineering_base",()->()->Base.CODEC);
    private static final java.util.function.Supplier<StructureType<Facility>> TYPE = TYPES.register("engineering_facility", () -> () -> Facility.CODEC);
    private static final java.util.function.Supplier<StructurePieceType> PIECE = PIECES.register("engineering_facility",
            () -> (StructurePieceType.StructureTemplateType) Piece::new);

    private EngineeringStructures() {}
    public static void register(IEventBus bus) { TYPES.register(bus); PIECES.register(bus); PLACEMENTS.register(bus); PROCESSORS.register(bus); }

    public static final class Base extends Structure {
        public static final MapCodec<Base> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
                settingsCodec(i),com.mojang.serialization.Codec.BOOL.optionalFieldOf("ruined",false).forGetter(base->base.ruined)
        ).apply(i,Base::new));
        private final boolean ruined;
        public Base(StructureSettings settings){this(settings,false);}
        public Base(StructureSettings settings,boolean ruined){super(settings);this.ruined=ruined;}
        public boolean ruined(){return ruined;}

        @Override protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
            var set=context.registryAccess().registryOrThrow(Registries.STRUCTURE_SET)
                    .get(ResourceLocation.fromNamespaceAndPath(TestGroundModule.ID,ruined?"engineering_ruins":"engineering_facilities"));
            if(set==null || !(set.placement() instanceof EngineeringBasePlacement placement))return Optional.empty();
            var match=placement.findSection(context.seed(),context.chunkPos());
            if(match.isEmpty())return Optional.empty();
            var section=match.get();
            if(ruined) {
                var intactSet=context.registryAccess().registryOrThrow(Registries.STRUCTURE_SET)
                        .get(ResourceLocation.fromNamespaceAndPath(TestGroundModule.ID,"engineering_facilities"));
                if(intactSet==null||!(intactSet.placement() instanceof EngineeringBasePlacement intact)
                        ||!placement.matchesVariant(context.seed(),section.root(),intact))return Optional.empty();
            }
            var corner=section.fullOrigin().offset(new BlockPos(EngineeringBasePlacement.WIDTH-1,0,EngineeringBasePlacement.LENGTH-1).rotate(section.rotation()));
            int x0=Math.min(corner.getX(),section.fullOrigin().getX()),x1=Math.max(corner.getX(),section.fullOrigin().getX());
            int z0=Math.min(corner.getZ(),section.fullOrigin().getZ()),z1=Math.max(corner.getZ(),section.fullOrigin().getZ());
            int min=Integer.MAX_VALUE,max=Integer.MIN_VALUE;
            // Every section makes the same terrain decision using the entire footprint.
            for(int dx=x0;dx<x1+8;dx+=8)for(int dz=z0;dz<z1+8;dz+=8) {
                int y=context.chunkGenerator().getBaseHeight(Math.min(dx,x1),Math.min(dz,z1),Heightmap.Types.WORLD_SURFACE_WG,
                        context.heightAccessor(),context.randomState());
                min=Math.min(min,y);max=Math.max(max,y);
                if(max-min>2)return Optional.empty();
            }
            var id=ResourceLocation.fromNamespaceAndPath(TestGroundModule.ID,EngineeringBasePlacement.sectionPath(section.index(),section.rotation(),ruined));
            var origin=section.origin(max-1-(ruined?RUINED_GROUND_OFFSET:0));
            if(SpawnFacilityAnchor.overlaps(BoundingBox.fromCorners(new BlockPos(x0,0,z0),new BlockPos(x1,0,z1)),24))return Optional.empty();
            if(origin.getY()<context.heightAccessor().getMinBuildHeight())return Optional.empty();
            return Optional.of(new GenerationStub(origin,builder->builder.addPiece(
                    new Piece(context.structureTemplateManager(),id,origin,section.rotation()))));
        }

        @Override public StructureType<?> type(){return BASE_TYPE.get();}
    }

    public static final class SpawnFacility extends Structure {
        public static final MapCodec<SpawnFacility> CODEC=simpleCodec(SpawnFacility::new);
        public SpawnFacility(StructureSettings settings){super(settings);}
        @Override protected Optional<GenerationStub> findGenerationPoint(GenerationContext context){
            var anchor=SpawnFacilityAnchor.position();
            if(anchor==null||!context.chunkPos().equals(new ChunkPos(anchor)))return Optional.empty();
            var id=ResourceLocation.parse("ccq_testground:engineering/abandoned_facility");
            var template=context.structureTemplateManager().getOrCreate(id);
            var size=template.getSize();
            if(size.getX()==0)return Optional.empty();
            int x=anchor.getX()-size.getX()/2,z=anchor.getZ()-size.getZ()/2;
            int ground=context.heightAccessor().getMinBuildHeight();
            for(int dx=0;dx<size.getX()+4;dx+=4)for(int dz=0;dz<size.getZ()+4;dz+=4)
                ground=Math.max(ground,context.chunkGenerator().getBaseHeight(x+Math.min(dx,size.getX()-1),
                        z+Math.min(dz,size.getZ()-1),Heightmap.Types.WORLD_SURFACE_WG,context.heightAccessor(),context.randomState()));
            var origin=new BlockPos(x,ground-1,z);
            if(origin.getY()+size.getY()>context.heightAccessor().getMaxBuildHeight())return Optional.empty();
            return Optional.of(new GenerationStub(origin,builder->builder.addPiece(new Piece(context.structureTemplateManager(),id,origin,Rotation.NONE))));
        }
        @Override public StructureType<?> type(){return SPAWN_TYPE.get();}
    }

    public static final class Facility extends Structure {
        public static final MapCodec<Facility> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                settingsCodec(instance), ResourceLocation.CODEC.fieldOf("template").forGetter(value -> value.templateId),
                com.mojang.serialization.Codec.intRange(0,64).optionalFieldOf("ground_offset",0).forGetter(value->value.groundOffset)
        ).apply(instance, Facility::new));
        private final ResourceLocation templateId;
        private final int groundOffset;

        public Facility(StructureSettings settings, ResourceLocation templateId) {
            this(settings,templateId,0);
        }
        public Facility(StructureSettings settings, ResourceLocation templateId,int groundOffset) {
            super(settings);
            this.templateId = templateId;
            this.groundOffset=groundOffset;
        }
        public ResourceLocation templateId(){return templateId;}
        public int groundOffset(){return groundOffset;}

        @Override protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
            var template = context.structureTemplateManager().getOrCreate(templateId);
            if (template.getSize().getX() == 0) return Optional.empty();
            var rotation = Rotation.getRandom(context.random());
            var settings = Piece.settings(rotation);
            var bounds = template.getBoundingBox(settings, BlockPos.ZERO);
            int x = context.chunkPos().getMiddleBlockX() - (bounds.minX() + bounds.maxX()) / 2;
            int z = context.chunkPos().getMiddleBlockZ() - (bounds.minZ() + bounds.maxZ()) / 2;
            int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
            // Sample the complete footprint, including edges, without loading adjacent chunks.
            for (int dx = bounds.minX(); dx <= bounds.maxX(); dx++) {
                for (int dz = bounds.minZ(); dz <= bounds.maxZ(); dz++) {
                    if ((dx - bounds.minX()) % 4 != 0 && dx != bounds.maxX()) continue;
                    if ((dz - bounds.minZ()) % 4 != 0 && dz != bounds.maxZ()) continue;
                    int y = context.chunkGenerator().getBaseHeight(x + dx, z + dz, Heightmap.Types.WORLD_SURFACE_WG,
                            context.heightAccessor(), context.randomState());
                    min = Math.min(min, y); max = Math.max(max, y);
                    if (max - min > 2) return Optional.empty();
                }
            }
            var origin = new BlockPos(x, max - 1-groundOffset, z);
            if(origin.getY()<context.heightAccessor().getMinBuildHeight())return Optional.empty();
            if(SpawnFacilityAnchor.overlaps(template.getBoundingBox(settings,origin),24))return Optional.empty();
            if(templateId.getPath().equals("engineering/nuclear_bunker")||templateId.getPath().startsWith("statues/")||templateId.getPath().equals("engineering/wild_amethyst_facility")) {
                var candidate=template.getBoundingBox(settings,origin);
                // Keep independent landmarks clear of both existing base layouts.
                for(String setId:new String[]{"engineering_facilities","engineering_ruins"}) {
                    var set=context.registryAccess().registryOrThrow(Registries.STRUCTURE_SET)
                            .get(ResourceLocation.fromNamespaceAndPath(TestGroundModule.ID,setId));
                    if(set==null||!(set.placement() instanceof EngineeringBasePlacement placement))continue;
                    int rx=Math.floorDiv(context.chunkPos().x,placement.spacing()),rz=Math.floorDiv(context.chunkPos().z,placement.spacing());
                    for(int dx=rx-1;dx<=rx+1;dx++)for(int dz=rz-1;dz<=rz+1;dz++) {
                        var root=placement.getPotentialStructureChunk(context.seed(),dx*placement.spacing(),dz*placement.spacing());
                        var other=placement.siteBounds(context.seed(),root);
                        if(candidate.minX()<=other.maxX()+24&&candidate.maxX()>=other.minX()-24
                                &&candidate.minZ()<=other.maxZ()+24&&candidate.maxZ()>=other.minZ()-24)return Optional.empty();
                    }
                }
            }
            return Optional.of(new GenerationStub(origin, builder -> builder.addPiece(
                    new Piece(context.structureTemplateManager(), templateId, origin, rotation))));
        }

        @Override public StructureType<?> type() { return TYPE.get(); }
    }

    public static final class Piece extends TemplateStructurePiece {
        Piece(StructureTemplateManager manager, ResourceLocation id, BlockPos origin, Rotation rotation) {
            super(PIECE.get(), 0, manager, id, id.toString(), settings(rotation,id), origin);
        }

        Piece(StructureTemplateManager manager, CompoundTag tag) {
            super(PIECE.get(), tag, manager, id -> settings(Rotation.valueOf(tag.getString("Rot")),id));
        }

        static StructurePlaceSettings settings(Rotation rotation) {
            return new StructurePlaceSettings().setRotation(rotation).setIgnoreEntities(true)
                    .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK)
                    .setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING);
        }

        private static StructurePlaceSettings settings(Rotation rotation,ResourceLocation id) {
            boolean ruined=id.getPath().startsWith("engineering/ruined_base/")||id.getPath().equals("engineering/engineering_base_ruined");
            boolean bunker=id.getPath().equals("engineering/nuclear_bunker");
            boolean abandoned=id.getPath().equals("engineering/abandoned_facility");
            var settings=settings(rotation).setKnownShape(id.getPath().startsWith("engineering/base/")||ruined||abandoned)
                    .setIgnoreEntities(!ruined&&!bunker&&!abandoned).setFinalizeEntities(false);
            if(abandoned||id.getPath().equals("engineering/wild_amethyst_facility"))settings.addProcessor(FacilityLootProcessor.INSTANCE);
            return settings;
        }

        @Override protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
            tag.putString("Rot", getRotation().name());
        }

        @Override public void postProcess(WorldGenLevel level, StructureManager manager, ChunkGenerator generator,
                RandomSource random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
            // C2ME can place the same piece in several chunks concurrently. Vanilla's
            // implementation mutates the shared placeSettings bounding box, causing
            // another worker to clip this chunk against its neighbor's bounds.
            // These authored templates contain no jigsaw blocks or data markers.
            var localSettings = placeSettings.copy().setBoundingBox(chunkBox);
            template.placeInWorld(level, templatePosition, pivot, localSettings, random, 2);
            // Only bridge small gaps under the platform; never reshape a whole hillside.
            for (int x = Math.max(boundingBox.minX(), chunkBox.minX()); x <= Math.min(boundingBox.maxX(), chunkBox.maxX()); x++) {
                for (int z = Math.max(boundingBox.minZ(), chunkBox.minZ()); z <= Math.min(boundingBox.maxZ(), chunkBox.maxZ()); z++) {
                    int foundationDepth=templateName.equals("ccq_testground:engineering/abandoned_facility")?64:3;
                    for (int depth = 1; depth <= foundationDepth; depth++) {
                        var pos = new BlockPos(x, templatePosition.getY() - depth, z);
                        if(pos.getY()<level.getMinBuildHeight())break;
                        if (level.getBlockState(pos).isSolid()) break;
                        level.setBlock(pos, Blocks.ANDESITE.defaultBlockState(), 2);
                    }
                }
            }
        }

        @Override protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level,
                RandomSource random, BoundingBox box) {}
    }
}

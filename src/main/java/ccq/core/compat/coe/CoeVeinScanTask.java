package ccq.core.compat.coe;

import com.tom.createores.Config;
import com.tom.createores.CreateOreExcavation;
import com.tom.createores.OreDataAttachment;
import com.tom.createores.OreVeinGenerator;
import com.tom.createores.recipe.VeinRecipe;
import com.tom.createores.util.RandomSpreadGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class CoeVeinScanTask {
    private final ServerLevel level;
    private final BlockPos origin;
    private final ChunkPos center;
    @Nullable
    private final ResourceLocation filterRecipeId;
    private final RandomSpreadGenerator picker;
    private final long seed;
    private final List<RecipeHolder<VeinRecipe>> candidates;
    private final List<OreVeinScanEntry> results = new ArrayList<>();
    private final Set<String> seen = new HashSet<>();

    private final int radius = CoeScannerConfig.CHUNK_RADIUS;
    private final int radiusSq = radius * radius;
    private int dx = -radius;
    private int dz = -radius;
    private boolean traceAdded;
    private boolean cancelled;

    CoeVeinScanTask(ServerLevel level, BlockPos origin, @Nullable ResourceLocation filterRecipeId) {
        this.level = level;
        this.origin = origin;
        this.center = new ChunkPos(origin);
        this.filterRecipeId = filterRecipeId;
        this.picker = OreVeinGenerator.getPicker(level);
        this.seed = level.getSeed();
        this.candidates = resolveCandidates(level, filterRecipeId);
    }

    void cancel() {
        cancelled = true;
    }

    boolean isCancelled() {
        return cancelled;
    }

    List<OreVeinScanEntry> results() {
        return results;
    }

    /** @return true when the scan has finished */
    boolean tick(int maxSteps) {
        if (cancelled) {
            return true;
        }

        int steps = 0;
        while (steps < maxSteps) {
            if (!advanceChunk()) {
                finalizeResults();
                return true;
            }
            steps++;
        }
        return false;
    }

    private boolean advanceChunk() {
        while (dx <= radius) {
            if (dz <= radius) {
                processChunk(dx, dz);
                dz++;
                return true;
            }
            dx++;
            dz = -radius;
        }
        return false;
    }

    private void processChunk(int dx, int dz) {
        if (results.size() >= CoeScannerConfig.MAX_RESULTS) {
            return;
        }
        if (dx * dx + dz * dz > radiusSq) {
            return;
        }

        int chunkX = center.x + dx;
        int chunkZ = center.z + dz;
        RecipeHolder<VeinRecipe> recipe = pickWithoutChunkLoad(chunkX, chunkZ);
        if (recipe == null) {
            return;
        }

        String key = recipe.id() + "@" + chunkX + ":" + chunkZ;
        if (!seen.add(key)) {
            return;
        }

        int chunkDistance = Math.max(Math.abs(dx), Math.abs(dz));
        LevelChunk loaded = level.getChunkSource().getChunkNow(chunkX, chunkZ);
        results.add(toEntry(
                origin,
                loaded,
                recipe,
                chunkX,
                chunkZ,
                classifyKind(dx, dz, chunkDistance)
        ));
    }

    /**
     * Matches Create Ore Excavation's {@code RandomSpreadGenerator.pick(ServerLevel, ChunkPos)}:
     * structure spacing plus noise-biome sampling. Does not call {@code getChunk}, so dedicated
     * servers are not forced to generate hundreds of chunks per scan.
     */
    @Nullable
    private RecipeHolder<VeinRecipe> pickWithoutChunkLoad(int chunkX, int chunkZ) {
        int minQuart = QuartPos.fromBlock(level.getMinBuildHeight());
        int maxQuart = minQuart + QuartPos.fromBlock(level.getHeight()) - 1;

        for (RecipeHolder<VeinRecipe> holder : candidates) {
            VeinRecipe vein = holder.value();
            RandomSpreadStructurePlacement placement = vein.getPlacement();
            if (placement == null) {
                continue;
            }
            ChunkPos potential = placement.getPotentialStructureChunk(seed, chunkX, chunkZ);
            if (potential.x != chunkX || potential.z != chunkZ) {
                continue;
            }

            WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
            random.setLargeFeatureSeed(seed, chunkX, chunkZ);
            Holder<Biome> biome = level.getNoiseBiome(
                    QuartPos.fromSection(chunkX) + random.nextInt(4),
                    minQuart + random.nextInt(Math.max(1, maxQuart)),
                    QuartPos.fromSection(chunkZ) + random.nextInt(4)
            );
            if (vein.canGenerate(level, biome)) {
                return holder;
            }
        }
        return null;
    }

    private void finalizeResults() {
        if (cancelled || traceAdded || filterRecipeId == null || !results.isEmpty()) {
            sortResults();
            return;
        }

        traceAdded = true;
        var trace = picker.locate(
                origin,
                level,
                CoeScannerConfig.CHUNK_RADIUS,
                holder -> holder.id().equals(filterRecipeId)
        );
        if (trace != null) {
            BlockPos pos = trace.getFirst();
            RecipeHolder<VeinRecipe> recipe = trace.getSecond();
            ChunkPos chunkPos = new ChunkPos(pos);
            String key = recipe.id() + "@" + chunkPos.x + ":" + chunkPos.z;
            if (seen.add(key)) {
                int tdx = chunkPos.x - center.x;
                int tdz = chunkPos.z - center.z;
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkPos.x, chunkPos.z);
                results.add(toEntry(
                        origin,
                        chunk,
                        recipe,
                        chunkPos.x,
                        chunkPos.z,
                        classifyKind(tdx, tdz, Math.max(Math.abs(tdx), Math.abs(tdz)))
                ));
            }
        }
        sortResults();
    }

    private void sortResults() {
        results.sort(Comparator
                .comparingInt((OreVeinScanEntry entry) -> entry.kind().ordinal())
                .thenComparingInt(OreVeinScanEntry::distance));
        if (results.size() > CoeScannerConfig.MAX_RESULTS) {
            results.subList(CoeScannerConfig.MAX_RESULTS, results.size()).clear();
        }
    }

    private static List<RecipeHolder<VeinRecipe>> resolveCandidates(
            ServerLevel level,
            @Nullable ResourceLocation filterRecipeId
    ) {
        if (filterRecipeId == null) {
            return level.getRecipeManager().getAllRecipesFor(CreateOreExcavation.VEIN_RECIPES.getRecipeType());
        }
        List<RecipeHolder<VeinRecipe>> filtered = new ArrayList<>();
        level.getRecipeManager().byKey(filterRecipeId).ifPresent(holder -> {
            if (holder.value() instanceof VeinRecipe) {
                @SuppressWarnings("unchecked")
                RecipeHolder<VeinRecipe> veinHolder = (RecipeHolder<VeinRecipe>) holder;
                filtered.add(veinHolder);
            }
        });
        return filtered;
    }

    private static OreVeinScanEntry.ScanKind classifyKind(int dx, int dz, int chunkDistance) {
        if (dx == 0 && dz == 0) {
            return OreVeinScanEntry.ScanKind.CURRENT;
        }
        if (chunkDistance <= CoeScannerConfig.NEARBY_CHUNK_RADIUS) {
            return OreVeinScanEntry.ScanKind.NEARBY;
        }
        return OreVeinScanEntry.ScanKind.DISTANT;
    }

    private static OreVeinScanEntry toEntry(
            BlockPos origin,
            @Nullable LevelChunk chunk,
            RecipeHolder<VeinRecipe> recipe,
            int chunkX,
            int chunkZ,
            OreVeinScanEntry.ScanKind kind
    ) {
        VeinRecipe vein = recipe.value();
        int centerX = (chunkX << 4) + 8;
        int centerZ = (chunkZ << 4) + 8;
        BlockPos veinCenter = new BlockPos(centerX, origin.getY(), centerZ);
        int distance = (int) Math.sqrt(origin.distSqr(veinCenter));
        boolean infinite = vein.isInfiniteClient() || Config.defaultInfinite;
        long remaining = resolveRemaining(chunk, vein, infinite);
        return new OreVeinScanEntry(
                recipe.id(),
                vein.getName(),
                chunkX,
                chunkZ,
                centerX,
                centerZ,
                distance,
                kind,
                vein.getIcon().copy(),
                infinite,
                remaining
        );
    }

    private static long resolveRemaining(@Nullable LevelChunk chunk, VeinRecipe vein, boolean infinite) {
        if (infinite) {
            return -1L;
        }
        if (chunk != null) {
            var data = OreDataAttachment.getData(chunk);
            if (data != null && data.isLoaded()) {
                return Math.max(0L, data.getResourcesRemaining(vein));
            }
        }
        return Math.max(0L, vein.getMaxAmountClient());
    }
}

package ccq.core.compat.coe;

import com.tom.createores.Config;
import com.tom.createores.CreateOreExcavation;
import com.tom.createores.OreDataAttachment;
import com.tom.createores.OreVeinGenerator;
import com.tom.createores.recipe.VeinRecipe;
import com.tom.createores.util.RandomSpreadGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
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
    private final List<RecipeHolder<VeinRecipe>> allRecipes;
    private final List<OreVeinScanEntry> results = new ArrayList<>();
    private final Set<String> seen = new HashSet<>();

    private final int radius = CoeScannerConfig.CHUNK_RADIUS;
    private final int radiusSq = radius * radius;
    private int dx = -radius;
    private int dz = -radius;
    private int chunksLoaded;
    private boolean traceAdded;
    private boolean cancelled;

    CoeVeinScanTask(ServerLevel level, BlockPos origin, @Nullable ResourceLocation filterRecipeId) {
        this.level = level;
        this.origin = origin;
        this.center = new ChunkPos(origin);
        this.filterRecipeId = filterRecipeId;
        this.picker = OreVeinGenerator.getPicker(level);
        this.seed = level.getSeed();
        this.allRecipes = filterRecipeId == null ? allVeinRecipes(level) : List.of();
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
        if (dx * dx + dz * dz > radiusSq) {
            return;
        }

        int chunkX = center.x + dx;
        int chunkZ = center.z + dz;
        if (!couldContainVein(chunkX, chunkZ)) {
            return;
        }

        LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
        if (chunk == null) {
            if (chunksLoaded >= CoeScannerConfig.MAX_CHUNK_LOADS_PER_SCAN) {
                return;
            }
            chunk = level.getChunk(chunkX, chunkZ);
            chunksLoaded++;
        }

        RecipeHolder<VeinRecipe> recipe = OreVeinGenerator.pick(chunk);
        if (recipe == null) {
            return;
        }
        if (filterRecipeId != null && !recipe.id().equals(filterRecipeId)) {
            return;
        }

        String key = recipe.id() + "@" + chunkX + ":" + chunkZ;
        if (!seen.add(key)) {
            return;
        }

        int chunkDistance = Math.max(Math.abs(dx), Math.abs(dz));
        results.add(toEntry(
                origin,
                chunk,
                recipe,
                chunkX,
                chunkZ,
                classifyKind(dx, dz, chunkDistance)
        ));
    }

    private void finalizeResults() {
        if (cancelled || traceAdded || filterRecipeId == null) {
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
    }

    private boolean couldContainVein(int chunkX, int chunkZ) {
        if (filterRecipeId != null) {
            return level.getRecipeManager()
                    .byKey(filterRecipeId)
                    .filter(holder -> holder.value() instanceof VeinRecipe)
                    .map(holder -> matchesPlacement((VeinRecipe) holder.value(), chunkX, chunkZ))
                    .orElse(false);
        }

        for (RecipeHolder<VeinRecipe> holder : allRecipes) {
            if (matchesPlacement(holder.value(), chunkX, chunkZ)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesPlacement(VeinRecipe recipe, int chunkX, int chunkZ) {
        RandomSpreadStructurePlacement placement = recipe.getPlacement();
        if (placement == null) {
            return false;
        }
        ChunkPos potential = placement.getPotentialStructureChunk(seed, chunkX, chunkZ);
        return potential.x == chunkX && potential.z == chunkZ;
    }

    private static List<RecipeHolder<VeinRecipe>> allVeinRecipes(ServerLevel level) {
        return level.getRecipeManager().getAllRecipesFor(CreateOreExcavation.VEIN_RECIPES.getRecipeType());
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

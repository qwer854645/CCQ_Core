package ccq.core.compat.structurify.mixin;

import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets="com.faboslav.structurify.common.world.level.structure.checks.StructureCheckData",remap=false)
public interface StructureCheckDataAccessor {
    @Accessor("structureStart")
    StructureStart ccq$getStructureStart();
}

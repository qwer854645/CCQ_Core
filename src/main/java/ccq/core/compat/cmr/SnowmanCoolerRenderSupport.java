package ccq.core.compat.cmr;

import fr.iglee42.cmr.cooler.SnowmanCoolerBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

final class SnowmanCoolerRenderSupport {
    private SnowmanCoolerRenderSupport() {
    }

    static BlockState renderBlockState(BlockState actual) {
        Block block = BuiltInRegistries.BLOCK.get(CmrCcaCompat.SNOWMAN_COOLER_BLOCK);
        if (!(block instanceof SnowmanCoolerBlock snowmanCooler)) {
            return actual;
        }

        BlockState remapped = snowmanCooler.defaultBlockState();
        if (actual.hasProperty(SnowmanCoolerBlock.FACING)) {
            remapped = remapped.setValue(SnowmanCoolerBlock.FACING, actual.getValue(SnowmanCoolerBlock.FACING));
        }
        if (actual.hasProperty(SnowmanCoolerBlock.HEAT_LEVEL)) {
            remapped = remapped.setValue(SnowmanCoolerBlock.HEAT_LEVEL, actual.getValue(SnowmanCoolerBlock.HEAT_LEVEL));
        }
        return remapped;
    }
}

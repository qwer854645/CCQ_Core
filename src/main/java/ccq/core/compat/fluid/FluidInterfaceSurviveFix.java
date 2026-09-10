package ccq.core.compat.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Create: Fluid's fluid / smart fluid interfaces destroy themselves via
 * {@code updateShape -> AIR} when the attached tank is briefly missing.
 * During minecart / contraption disassembly that race deletes the interface.
 * Defer the break by one tick so placement order can finish first.
 */
public final class FluidInterfaceSurviveFix {
    private FluidInterfaceSurviveFix() {
    }

    public static boolean wouldBreakNow(BlockState state, Direction direction, LevelAccessor level, BlockPos pos) {
        return direction.getOpposite() == state.getValue(HorizontalDirectionalBlock.FACING)
                && !state.canSurvive(level, pos);
    }

    public static void deferBreak(Block block, BlockState state, LevelAccessor level, BlockPos pos,
                                  CallbackInfoReturnable<BlockState> cir) {
        level.scheduleTick(pos, block, 1);
        cir.setReturnValue(state);
    }

    public static void breakIfUnsupported(BlockState state, ServerLevel level, BlockPos pos) {
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }
}

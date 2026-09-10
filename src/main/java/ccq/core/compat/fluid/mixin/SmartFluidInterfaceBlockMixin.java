package ccq.core.compat.fluid.mixin;

import ccq.core.compat.fluid.FluidInterfaceSurviveFix;
import com.adonis.fluid.block.SmartFluidInterface.SmartFluidInterfaceBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SmartFluidInterfaceBlock.class)
public abstract class SmartFluidInterfaceBlockMixin extends HorizontalDirectionalBlock {
    private SmartFluidInterfaceBlockMixin(Properties properties) {
        super(properties);
    }

    @Inject(method = "updateShape", at = @At("HEAD"), cancellable = true)
    private void ccq$deferUnstableBreak(BlockState state, Direction direction, BlockState neighborState,
                                        LevelAccessor level, BlockPos currentPos, BlockPos neighborPos,
                                        CallbackInfoReturnable<BlockState> cir) {
        if (FluidInterfaceSurviveFix.wouldBreakNow(state, direction, level, currentPos)) {
            FluidInterfaceSurviveFix.deferBreak((Block) (Object) this, state, level, currentPos, cir);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        FluidInterfaceSurviveFix.breakIfUnsupported(state, level, pos);
    }
}
